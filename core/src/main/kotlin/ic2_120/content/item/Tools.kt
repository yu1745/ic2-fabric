package ic2_120.content.item

import net.minecraft.core.registries.BuiltInRegistries

import ic2_120.compat.orCreateNbt

import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.component.ItemAttributeModifiers
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.item.enchantment.ItemEnchantments

import net.minecraft.data.recipes.RecipeProvider.getHasName

import ic2_120.compat.stack

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.compat.isClient
import ic2_120.compat.world

import ic2_120.integration.ftbchunks.ClaimProtection

import ic2_120.content.block.cables.InsulatedCopperCableBlock
import ic2_120.content.item.energy.EnergyCrystalItem
import ic2_120.content.item.energy.IElectricTool
import ic2_120.content.recipes.crafting.BatteryEnergyShapedRecipeDatagen
import ic2_120.registry.CreativeTab
import ic2_120.content.recipes.ModTags
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.content.block.CropBlock
import ic2_120.content.block.CropBlockEntity
import ic2_120.content.block.CropStickBlock
import ic2_120.content.block.TeleporterBlock
import ic2_120.content.block.machines.TeleporterBlockEntity
import ic2_120.content.crop.CropType
import ic2_120.content.recipes.crafting.ConsumeTreetapShapedRecipeDatagen
import ic2_120.content.recipes.crafting.ConsumeWrenchShapedRecipeDatagen
import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.level.Level
import java.util.UUID
import net.minecraft.world.Containers
import net.minecraft.core.BlockPos
import ic2_120.registry.recipeId
import ic2_120.registry.type
import net.minecraft.world.item.AxeItem
import net.minecraft.world.item.HoeItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.PickaxeItem
import net.minecraft.world.item.ShovelItem
import net.minecraft.world.item.SwordItem
import net.minecraft.world.item.Tier
import net.minecraft.world.ContainerHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.tags.ItemTags
import net.minecraft.tags.BlockTags
import net.minecraft.world.item.TooltipFlag
import net.minecraft.sounds.SoundSource
import net.minecraft.sounds.SoundEvents
import net.minecraft.ChatFormatting
import net.minecraft.core.NonNullList
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.data.recipes.ShapelessRecipeBuilder
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.RecipeCategory
import java.util.function.Consumer
import ic2_120.registry.annotation.RecipeProvider

// ========== 工具材料 ==========

/** 青铜工具材料：耐久与挖掘等级同铁，挖掘速度同石头，可用青铜锭修复 */
object BronzeToolMaterial : Tier {
    override fun getUses() = 250
    override fun getSpeed() = 4.0f
    override fun getAttackDamageBonus() = 2.0f
    override fun getIncorrectBlocksForDrops(): TagKey<Block> = BlockTags.INCORRECT_FOR_IRON
    override fun getEnchantmentValue() = 10
    override fun getRepairIngredient(): Ingredient =
        Ingredient.of(ModTags.Compat.Items.INGOTS_BRONZE)
}

// ========== 工具类 ==========

/** 锻造锤 - 将锭锻造成板，将板锻造成外壳 */
@ModItem(name = "forge_hammer", tab = CreativeTab.IC2_TOOLS)
class ForgeHammer : Item(Item.Properties().durability(80)) {
    override fun appendHoverText(stack: ItemStack, context: Item.TooltipContext, tooltip: MutableList<Component>, type: TooltipFlag) {
        val remaining = stack.maxDamage - stack.damageValue
        tooltip.add(Component.literal("剩余使用次数: $remaining").withStyle(ChatFormatting.GRAY))
    }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val iron = Ingredient.of(ModTags.Compat.Items.INGOTS_IRON)
            val stick = Items.STICK
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ForgeHammer::class.instance(), 1)
                .pattern("II").pattern("IS").pattern("II")
                .define('I', iron).define('S', stick)
                .unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
                .save(exporter, ForgeHammer::class.id())
        }
    }
}

/** 板材切割剪刀 - 将板材切割成导线 */
@ModItem(name = "cutter", tab = CreativeTab.IC2_TOOLS)
class Cutter : Item(Item.Properties().durability(60)) {
    override fun appendHoverText(stack: ItemStack, context: Item.TooltipContext, tooltip: MutableList<Component>, type: TooltipFlag) {
        val remaining = stack.maxDamage - stack.damageValue
        tooltip.add(Component.literal("剩余使用次数: $remaining").withStyle(ChatFormatting.GRAY))
    }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val ironIngot = Ingredient.of(ModTags.Compat.Items.INGOTS_IRON)
            val ironPlate = Ingredient.of(ModTags.Compat.Items.PLATES_IRON)
            val steelIngot = Ingredient.of(ModTags.Compat.Items.INGOTS_STEEL)

            // 配方 1：铁板制作切割机
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, Cutter::class.instance(), 1)
                .pattern("P P").pattern(" P ").pattern("I I")
                .define('P', ironPlate).define('I', ironIngot)
                .unlockedBy(getHasName(IronPlate::class.instance()), has(IronPlate::class.instance()))
                .save(exporter, Cutter::class.recipeId("iron_plate"))

            // 配方 2：钢锭制作切割机
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, Cutter::class.instance(), 1)
                .pattern("P P").pattern(" P ").pattern("I I")
                .define('P', steelIngot).define('I', ironIngot)
                .unlockedBy(getHasName(SteelIngot::class.instance()), has(SteelIngot::class.instance()))
                .save(exporter, Cutter::class.recipeId("steel"))
        }
    }
}

// ========== 青铜工具 ==========

@ModItem(name = "bronze_axe", tab = CreativeTab.IC2_TOOLS, group = "bronze_tools")
class BronzeAxe : AxeItem(BronzeToolMaterial, Item.Properties().stacksTo(1).attributes(AxeItem.createAttributes(BronzeToolMaterial, 6.0f, -3.0f))) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val bronze = Ingredient.of(ModTags.Compat.Items.INGOTS_BRONZE)
            val stick = Items.STICK
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, BronzeAxe::class.instance(), 1)
                .pattern("MM").pattern("MS").pattern(" S")
                .define('M', bronze).define('S', stick)
                .unlockedBy(getHasName(BronzeIngot::class.instance()), has(BronzeIngot::class.instance()))
                .save(exporter, BronzeAxe::class.id())
        }
    }
}

@ModItem(name = "bronze_hoe", tab = CreativeTab.IC2_TOOLS, group = "bronze_tools")
class BronzeHoe : HoeItem(BronzeToolMaterial, Item.Properties().stacksTo(1).attributes(HoeItem.createAttributes(BronzeToolMaterial, 0f, -1f))) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val bronze = Ingredient.of(ModTags.Compat.Items.INGOTS_BRONZE)
            val stick = Items.STICK
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, BronzeHoe::class.instance(), 1)
                .pattern("MM").pattern(" S").pattern(" S")
                .define('M', bronze).define('S', stick)
                .unlockedBy(getHasName(BronzeIngot::class.instance()), has(BronzeIngot::class.instance()))
                .save(exporter, BronzeHoe::class.id())
        }
    }
}

@ModItem(name = "bronze_sword", tab = CreativeTab.IC2_TOOLS, group = "bronze_tools")
class BronzeSword : SwordItem(BronzeToolMaterial, Item.Properties().stacksTo(1).attributes(SwordItem.createAttributes(BronzeToolMaterial, 3, -2.4f))) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val bronze = Ingredient.of(ModTags.Compat.Items.INGOTS_BRONZE)
            val stick = Items.STICK
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, BronzeSword::class.instance(), 1)
                .pattern("M").pattern("M").pattern("S")
                .define('M', bronze).define('S', stick)
                .unlockedBy(getHasName(BronzeIngot::class.instance()), has(BronzeIngot::class.instance()))
                .save(exporter, BronzeSword::class.id())
        }
    }
}

@ModItem(name = "bronze_shovel", tab = CreativeTab.IC2_TOOLS, group = "bronze_tools")
class BronzeShovel : ShovelItem(BronzeToolMaterial, Item.Properties().stacksTo(1).attributes(ShovelItem.createAttributes(BronzeToolMaterial, 1.5f, -3.0f))) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val bronze = Ingredient.of(ModTags.Compat.Items.INGOTS_BRONZE)
            val stick = Items.STICK
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, BronzeShovel::class.instance(), 1)
                .pattern("M").pattern("S").pattern("S")
                .define('M', bronze).define('S', stick)
                .unlockedBy(getHasName(BronzeIngot::class.instance()), has(BronzeIngot::class.instance()))
                .save(exporter, BronzeShovel::class.id())
        }
    }
}

@ModItem(name = "bronze_pickaxe", tab = CreativeTab.IC2_TOOLS, group = "bronze_tools")
class BronzePickaxe : PickaxeItem(BronzeToolMaterial, Item.Properties().stacksTo(1).attributes(PickaxeItem.createAttributes(BronzeToolMaterial, 1, -2.8f))) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val bronze = Ingredient.of(ModTags.Compat.Items.INGOTS_BRONZE)
            val stick = Items.STICK
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, BronzePickaxe::class.instance(), 1)
                .pattern("MMM").pattern(" S ").pattern(" S ")
                .define('M', bronze).define('S', stick)
                .unlockedBy(getHasName(BronzeIngot::class.instance()), has(BronzeIngot::class.instance()))
                .save(exporter, BronzePickaxe::class.id())
        }
    }
}

/** 除草剂：右键作物直接喷洒除草剂，内部储存除草剂容量（mB） */
@ModItem(name = "weed_ex", tab = CreativeTab.IC2_TOOLS, group = "tools")
class WeedEx : Item(Item.Properties().stacksTo(1)) {
    override fun useOn(context: UseOnContext): InteractionResult {
        val world = context.level
        val pos = context.pos
        val state = world.getBlockState(pos)
        if (state.block !is CropBlock) return InteractionResult.PASS
        if (world.isClient) return InteractionResult.SUCCESS
        val player = context.player ?: return InteractionResult.PASS
        if (ClaimProtection.isProtected(world, pos, player, ClaimProtection.EDIT_BLOCK)) return InteractionResult.FAIL
        val stack = context.itemInHand
        val be = world.getBlockEntity(pos) as? CropBlockEntity ?: return InteractionResult.PASS

        val amount = getAmount(stack)
        if (amount <= 0) return InteractionResult.PASS

        val request = amount.coerceAtMost(WEEDEX_PER_USE)
        val used = be.applyWeedEx(request, simulate = true)
        if (used <= 0) return InteractionResult.PASS

        if (!player.abilities.instabuild) {
            be.applyWeedEx(request, simulate = false)
            val remaining = amount - used
            if (remaining <= 0) {
                stack.shrink(1)
            } else {
                setAmount(stack, remaining)
            }
        } else {
            be.applyWeedEx(request, simulate = false)
        }
        return InteractionResult.SUCCESS
    }

    override fun appendHoverText(stack: ItemStack, context: Item.TooltipContext, tooltip: MutableList<Component>, type: TooltipFlag) {
        val amount = getAmount(stack)
        val pct = (amount * 100 / CAPACITY).coerceIn(0, 100)
        tooltip.add(Component.translatable("tooltip.ic2_120.weed_ex.amount", amount, CAPACITY).withStyle(ChatFormatting.GRAY))
        if (pct < 25) {
            tooltip.add(Component.translatable("tooltip.ic2_120.weed_ex.low").withStyle(ChatFormatting.RED))
        }
    }

    override fun isBarVisible(stack: ItemStack): Boolean {
        return getAmount(stack) < CAPACITY
    }

    override fun getBarWidth(stack: ItemStack): Int {
        return (getAmount(stack) * 13 / CAPACITY).coerceIn(0, 13)
    }

    override fun getBarColor(stack: ItemStack): Int {
        val pct = getAmount(stack) * 100 / CAPACITY
        return when {
            pct < 25 -> 0xFF4444
            pct < 50 -> 0xFFAA44
            else -> 0x44FF44
        }
    }

    override fun isEnchantable(stack: ItemStack): Boolean = false

    companion object {
        const val CAPACITY = 1000
        private const val WEEDEX_PER_USE = 150
        private const val NBT_KEY = "WeedExAmount"

        fun getAmount(stack: ItemStack): Int {
            val nbt = stack.orCreateNbt
            if (!nbt.contains(NBT_KEY)) return CAPACITY
            return nbt.getInt(NBT_KEY).coerceIn(0, CAPACITY)
        }

        fun setAmount(stack: ItemStack, amount: Int) {
            if (amount >= CAPACITY) {
                val nbt = stack.orCreateNbt
                nbt.remove(NBT_KEY)
                if (nbt.isEmpty) {
                    stack.remove(DataComponents.CUSTOM_DATA)
                } else {
                    stack.orCreateNbt = nbt
                }
            } else {
                stack.orCreateNbt = stack.orCreateNbt.apply { putInt(NBT_KEY, amount.coerceAtMost(CAPACITY)) }
            }
        }

        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val cell = WeedExCell::class.instance()
            if (cell == Items.AIR) return
            ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, WeedEx::class.instance(), 1)
                .requires(cell, 1)
                .unlockedBy(getHasName(cell), has(cell))
                .save(exporter, WeedEx::class.id())
        }
    }
}

/** 除草铲：右键作物架上的杂草可清除并恢复空架（杂草无法用手采摘，需用本工具或破坏方块） */
@ModItem(name = "weeding_spade", tab = CreativeTab.IC2_TOOLS, group = "bronze_tools")
class WeedingSpade : Item(Item.Properties().durability(120)) {
    override fun useOn(context: UseOnContext): InteractionResult {
        val world = context.level
        val pos = context.pos
        val state = world.getBlockState(pos)
        if (state.block !is CropBlock) return InteractionResult.PASS
        if (state.getValue(CropBlock.CROP_TYPE) != CropType.WEED) return InteractionResult.PASS
        if (world.isClient) return InteractionResult.SUCCESS
        val player = context.player ?: return InteractionResult.PASS
        val stack = context.itemInHand
        val be = world.getBlockEntity(pos) as? CropBlockEntity ?: return InteractionResult.PASS
        val isCreative = player.abilities.instabuild

        Containers.spawn(
            world,
            pos.x.toDouble(),
            pos.y.toDouble(),
            pos.z.toDouble(),
            Weed::class.instance().defaultInstance
        )
        world.setBlock(pos, CropStickBlock.defaultStickState(), Block.UPDATE_ALL)
        if (!isCreative) {
            stack.hurtAndBreak(1, player, if (context.hand == InteractionHand.OFF_HAND) EquipmentSlot.OFFHAND else EquipmentSlot.MAINHAND)
        }
        return InteractionResult.SUCCESS
    }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val bronze = Ingredient.of(ModTags.Compat.Items.INGOTS_BRONZE)
            val stick = Items.STICK
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, WeedingSpade::class.instance(), 1)
                .pattern("M").pattern("S")
                .define('M', bronze).define('S', stick)
                .unlockedBy(getHasName(BronzeIngot::class.instance()), has(BronzeIngot::class.instance()))
                .save(exporter, WeedingSpade::class.id())
        }
    }
}

// ========== 其他工具（占位实现） ==========

/** 测试工具 - 开发调试用 */
@ModItem(name = "debug_item", tab = CreativeTab.IC2_TOOLS, group = "tools")
class DebugItem : Item(Item.Properties().stacksTo(1))

/** 工具箱：右键收纳快捷栏工具（最多 8 格）；潜行右键向背包释放，能放多少放多少，放不下的留在箱内。无 GUI。 */
@ModItem(name = "tool_box", tab = CreativeTab.IC2_TOOLS, group = "tools")
class ToolBox : Item(Item.Properties().stacksTo(1)) {

    override fun use(world: Level, user: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = user.getItemInHand(player.usedItemHand)
        if (world.isClient) return InteractionResultHolder.success(stack)

        val internal = loadInternal(stack)
        if (user.isCrouching) {
            var transferredAny = false
            for (i in internal.indices) {
                val s = internal[i]
                if (s.isEmpty) continue
                val beforeAmount = s.count
                val leftover = s.copy()
                user.inventory.add(leftover)
                internal[i] = leftover
                if (leftover.count < beforeAmount || leftover.isEmpty) {
                    transferredAny = true
                }
            }
            if (transferredAny) {
                user.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 0.2f, 1.1f)
            }
            if (internal.any { !it.isEmpty }) {
                user.displayClientMessage(Component.translatable("message.ic2_120.tool_box.inventory_partial"), true)
            }
            saveInternal(stack, internal)
            return InteractionResultHolder.success(stack)
        }

        val inv = user.inventory
        val skipHotbarIndex = if (hand == InteractionHand.MAIN_HAND) inv.selected else -1
        var deposited = false
        for (idx in 0..8) {
            if (idx == skipHotbarIndex) continue
            val slotStack = inv.items[idx]
            if (!slotStack.isStorableForToolBox()) continue
            val dest = internal.indexOfFirst { it.isEmpty }
            if (dest == -1) break
            internal[dest] = slotStack.copy()
            inv.items[idx] = ItemStack.EMPTY
            deposited = true
        }
        if (deposited) {
            user.playSound(SoundEvents.WOOD_PLACE, 0.35f, 1.25f)
        }
        saveInternal(stack, internal)
        return InteractionResultHolder.success(stack)
    }

    override fun appendHoverText(
        stack: ItemStack,
        context: Item.TooltipContext,
        tooltip: MutableList<Component>,
        type: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltip, type)
        tooltip.add(Component.translatable("tooltip.ic2_120.tool_box.hint").withStyle(ChatFormatting.DARK_GRAY))
        tooltip.add(Component.translatable("tooltip.ic2_120.tool_box.hint_detail").withStyle(ChatFormatting.DARK_GRAY))
        val internal = loadInternal(stack)
        val filled = internal.count { !it.isEmpty }
        if (filled == 0) {
            tooltip.add(Component.translatable("tooltip.ic2_120.tool_box.empty").withStyle(ChatFormatting.GRAY))
            return
        }
        tooltip.add(
            Component.translatable("tooltip.ic2_120.tool_box.summary", filled, CAPACITY).withStyle(ChatFormatting.GRAY)
        )
        for (s in internal) {
            if (s.isEmpty) continue
            val line = Component.literal("· ").withStyle(ChatFormatting.DARK_GRAY).append(s.name)
            if (s.count > 1) {
                line.append(Component.literal(" ×${s.count}").withStyle(ChatFormatting.DARK_GRAY))
            }
            tooltip.add(line)
        }
    }

    companion object {
        const val CAPACITY = 8
        const val NBT_KEY = "ToolBoxItems"

        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val casing = BronzeCasing::class.instance()
            val chest = Items.CHEST
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ToolBox::class.instance(), 1)
                .pattern("BBB")
                .pattern("BCB")
                .pattern("BBB")
                .define('B', casing)
                .define('C', chest)
                .unlockedBy(getHasName(chest), has(chest))
                .unlockedBy(getHasName(casing), has(casing))
                .save(exporter, ToolBox::class.id())
        }

        internal fun loadInternal(stack: ItemStack): NonNullList<ItemStack> {
            val list = NonNullList.withSize(CAPACITY, ItemStack.EMPTY)
            val root = stack.orCreateNbt
            if (!root.contains(NBT_KEY)) return list
            val tag = root.getCompound(NBT_KEY)
            ContainerHelper.loadAllItems(tag, list)
            return list
        }

        internal fun saveInternal(stack: ItemStack, list: NonNullList<ItemStack>) {
            val nbt = stack.orCreateNbt
            val tag = CompoundTag()
            ContainerHelper.saveAllItems(tag, list)
            nbt.put(NBT_KEY, tag)
            stack.orCreateNbt = nbt
        }

        /** 可收入工具箱的物品：原版工具标签、常见工具物品、电动工具、IC2 手持工具等。 */
        internal fun ItemStack.isStorableForToolBox(): Boolean {
            if (isEmpty) return false
            val i = item
            if (i is ToolBox) return false
            if (i is DebugItem || i is EnergyDebugStickItem) return false
            if (i is IElectricTool) return true
            if (isIn(ItemTags.PICKAXES) || isIn(ItemTags.AXES) || isIn(ItemTags.SHOVELS) ||
                isIn(ItemTags.HOES) || isIn(ItemTags.SWORDS)
            ) {
                return true
            }
            if (i === Items.SHEARS || i === Items.FISHING_ROD || i === Items.FLINT_AND_STEEL ||
                i === Items.BRUSH || i === Items.TRIDENT ||
                i === Items.CARROT_ON_A_STICK || i === Items.WARPED_FUNGUS_ON_A_STICK
            ) {
                return true
            }
            if (i is AxeItem || i is PickaxeItem || i is ShovelItem || i is HoeItem || i is SwordItem) return true
            if (i is MiningLaserItem) return true
            if (i is FoamSprayerItem) return true
            if (i is ForgeHammer || i is Cutter || i is WeedingSpade || i is WeedEx || i is Treetap || i is Wrench ||
                i is FrequencyTransmitter || i is Obscurator || i is WindMeter
            ) {
                return true
            }
            return false
        }
    }
}

//已删除
/** EU 电表 - 测量导线/机器 EU 流量 */
// @ModItem(name = "meter", tab = CreativeTab.IC2_TOOLS, group = "tools")
class Meter : Item(Item.Properties().stacksTo(1))

/** 木龙头 - 从橡胶树原木湿面提取粘性树脂，寿命 10 次 */
@ModItem(name = "treetap", tab = CreativeTab.IC2_TOOLS, group = "tools")
class Treetap : Item(Item.Properties().durability(10)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, Treetap::class.instance(), 1)
                .pattern(" P ")
                .pattern("PPP")
                .pattern("P  ")
                .define('P', ItemTags.PLANKS)
                .unlockedBy(getHasName(Items.OAK_PLANKS), has(Items.OAK_PLANKS))
                .save(exporter, Treetap::class.id())
        }
    }
}

/** 扳手 - 拆卸机器、旋转方块 */
@ModItem(name = "wrench", tab = CreativeTab.IC2_TOOLS, group = "tools")
class Wrench : Item(Item.Properties().durability(120)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val bronze = Ingredient.of(ModTags.Compat.Items.INGOTS_BRONZE)
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, Wrench::class.instance(), 1)
                .pattern("B B")
                .pattern("BBB")
                .pattern(" B ")
                .define('B', bronze)
                .unlockedBy(getHasName(BronzeIngot::class.instance()), has(BronzeIngot::class.instance()))
                .save(exporter, Wrench::class.id())
        }
    }
}

/** 遥控器 - 远程控制机器频率 */
@ModItem(name = "frequency_transmitter", tab = CreativeTab.IC2_TOOLS, group = "tools")
class FrequencyTransmitter : Item(Item.Properties().stacksTo(1)) {
    companion object {
        private const val NBT_BIND_X = "BindX"
        private const val NBT_BIND_Y = "BindY"
        private const val NBT_BIND_Z = "BindZ"
        private const val NBT_BIND_DIM = "BindDim"
        private const val NBT_HAS_BIND = "HasBind"

        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val cable = InsulatedCopperCableBlock::class.item()
            val circuit = Circuit::class.instance()
            if (cable == Items.AIR || circuit == Items.AIR) return
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, FrequencyTransmitter::class.instance(), 1)
                .pattern("   ")
                .pattern(" C ")
                .pattern(" E ")
                .define('C', cable)
                .define('E', circuit)
                .unlockedBy(getHasName(circuit), has(circuit))
                .save(exporter, FrequencyTransmitter::class.id())
        }
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        val world = context.level
        val pos = context.pos
        val stack = context.itemInHand
        val player = context.player
        val state = world.getBlockState(pos)

        if (state.block !is TeleporterBlock) return InteractionResult.PASS
        if (player == null) return InteractionResult.SUCCESS

        val nbt = stack.orCreateNbt
        val dim = world.dimension().location().toString()

        if (player.isCrouching || !nbt.getBoolean(NBT_HAS_BIND)) {
            nbt.putBoolean(NBT_HAS_BIND, true)
            nbt.putInt(NBT_BIND_X, pos.x)
            nbt.putInt(NBT_BIND_Y, pos.y)
            nbt.putInt(NBT_BIND_Z, pos.z)
            nbt.putString(NBT_BIND_DIM, dim)
            if (!world.isClient) {
                player.displayClientMessage(Component.literal("已记录第一台传送机: ${pos.x}, ${pos.y}, ${pos.z}"), true)
            }
            return InteractionResult.SUCCESS
        }

        val bindDim = nbt.getString(NBT_BIND_DIM)
        if (bindDim != dim) {
            if (!world.isClient) player.displayClientMessage(Component.literal("维度不一致，无法绑定。"), true)
            return InteractionResult.SUCCESS
        }

        val bindPos = BlockPos(nbt.getInt(NBT_BIND_X), nbt.getInt(NBT_BIND_Y), nbt.getInt(NBT_BIND_Z))
        if (bindPos == pos) {
            if (!world.isClient) player.displayClientMessage(Component.literal("不能将传送机绑定到自身。"), true)
            return InteractionResult.SUCCESS
        }

        val be = world.getBlockEntity(pos) as? TeleporterBlockEntity
        val targetBe = world.getBlockEntity(bindPos) as? TeleporterBlockEntity
        if (be == null || targetBe == null) {
            if (!world.isClient) player.displayClientMessage(Component.literal("目标坐标不是有效的传送机。"), true)
            return InteractionResult.SUCCESS
        }

        if (!world.isClient) {
            be.setTarget(bindPos, bindDim)
            targetBe.setTarget(pos, dim)
            player.displayClientMessage(
                Component.literal("双向链接成功: (${pos.x},${pos.y},${pos.z}) <-> (${bindPos.x},${bindPos.y},${bindPos.z})"),
                true
            )
            nbt.putBoolean(NBT_HAS_BIND, false)
        }
        return InteractionResult.SUCCESS
    }

    override fun appendHoverText(
        stack: ItemStack,
        context: Item.TooltipContext,
        tooltip: MutableList<Component>,
        type: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltip, type)
        val nbt = stack.orCreateNbt
        val hasBind = nbt.getBoolean(NBT_HAS_BIND)
        if (!hasBind) {
            tooltip.add(Component.literal("未绑定传送机"))
            tooltip.add(
                Component.literal("右击传送机记录第一台，再右击另一台完成双向绑定")
                    .withStyle(net.minecraft.ChatFormatting.DARK_GRAY)
            )
            return
        }

        val x = nbt.getInt(NBT_BIND_X)
        val y = nbt.getInt(NBT_BIND_Y)
        val z = nbt.getInt(NBT_BIND_Z)
        val dim = nbt.getString(NBT_BIND_DIM).ifBlank { "unknown" }
        tooltip.add(Component.literal("已记录第一台传送机").withStyle(net.minecraft.ChatFormatting.AQUA))
        tooltip.add(Component.literal("坐标: $x, $y, $z"))
        tooltip.add(Component.literal("维度: $dim").withStyle(net.minecraft.ChatFormatting.DARK_GRAY))
    }
}

/** 链锯 - 电动伐木工具（等级 1，30k EU） */
@ModItem(name = "chainsaw", tab = CreativeTab.IC2_TOOLS, group = "electric_tools", tags = ["minecraft:axes", "minecraft:swords"])
class Chainsaw : ElectricMiningDrillItem(
    Item.Properties(),
    miningToolFactories = listOf({ ItemStack(Items.DIAMOND_AXE) }, { ItemStack(Items.DIAMOND_SWORD) }),
    baseEnergyPerBlock = 100L
) {
    companion object {
        const val NBT_DISABLE_SHEAR = "ChainsawDisableShear"
        const val ENERGY_PER_USE = 100L

        fun isShearEnabled(stack: ItemStack): Boolean =
            !stack.orCreateNbt.getBoolean(NBT_DISABLE_SHEAR)

        fun toggleShear(stack: ItemStack): Boolean {
            val nbt = stack.orCreateNbt
            val disabled = !nbt.getBoolean(NBT_DISABLE_SHEAR)
            nbt.putBoolean(NBT_DISABLE_SHEAR, disabled)
            return !disabled
        }

        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val ironPlate = IronPlate::class.instance()
            val powerUnit = PowerUnitItem::class.instance()
            BatteryEnergyShapedRecipeDatagen.offer(
                exporter = exporter,
                recipeId = Chainsaw::class.id(),
                result = Chainsaw::class.instance(),
                pattern = listOf(" II", "III", "PI "),
                keys = mapOf<Char, Item>(
                    'I' to ironPlate,
                    'P' to powerUnit
                ),
                category = "equipment"
            )
        }
    }

    override val tier = 1
    override val maxCapacity = 30_000L
    override fun nominalEuPerTick() = 100L

    /** 1.21.1：动态属性改为写 ItemStack 的 ATTRIBUTE_MODIFIERS 组件 */
    private fun syncAttributeModifiers(stack: ItemStack) {
        val enabled = getEnergy(stack) >= ENERGY_PER_USE
        val mods = if (!enabled) {
            null
        } else {
            ItemAttributeModifiers.builder()
                .add(
                    Attributes.ATTACK_DAMAGE,
                    AttributeModifier(
                        ResourceLocation.withDefaultNamespace("chainsaw_attack_damage"),
                        9.0,
                        AttributeModifier.Operation.ADD_VALUE
                    ),
                    EquipmentSlotGroup.MAINHAND
                )
                .add(
                    Attributes.ATTACK_SPEED,
                    AttributeModifier(
                        ResourceLocation.withDefaultNamespace("chainsaw_attack_speed"),
                        -3.0,
                        AttributeModifier.Operation.ADD_VALUE
                    ),
                    EquipmentSlotGroup.MAINHAND
                )
                .build()
        }
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, mods)
    }

    override fun inventoryTick(stack: ItemStack, world: Level, entity: Entity, slot: Int, selected: Boolean) {
        super.inventoryTick(stack, world, entity, slot, selected)
        syncAttributeModifiers(stack)
    }

    override fun getDestroySpeed(stack: ItemStack, state: BlockState): Float {
        if (!hasEnoughEnergyForMining(stack)) return 1.0f
        return when {
            state.`is`(Blocks.COBWEB) || state.`is`(BlockTags.LEAVES) -> 15.0f
            state.`is`(BlockTags.WOOL) -> 5.0f
            state.`is`(Blocks.VINE) || state.`is`(Blocks.GLOW_LICHEN) -> 2.0f
            else -> super.getDestroySpeed(stack, state)
        }
    }

    override fun isCorrectToolForDrops(stack: ItemStack, state: BlockState): Boolean =
        state.`is`(Blocks.COBWEB) || state.`is`(Blocks.REDSTONE_WIRE) || state.`is`(Blocks.TRIPWIRE) ||
            super.isCorrectToolForDrops(stack, state)

    override fun postHurtEnemy(stack: ItemStack, target: LivingEntity, attacker: LivingEntity) {
        if (!attacker.level().isClientSide && getEnergy(stack) >= ENERGY_PER_USE && (attacker !is Player || !attacker.isCreative)) {
            setEnergy(stack, getEnergy(stack) - ENERGY_PER_USE)
        }
    }
    override fun getEnergy(stack: ItemStack) = IElectricTool.getEnergy(stack)
    override fun setEnergy(stack: ItemStack, energy: Long) = IElectricTool.setEnergy(stack, energy, maxCapacity)
    override fun appendHoverText(
        stack: ItemStack,
        context: Item.TooltipContext,
        tooltip: MutableList<Component>,
        type: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltip, type)
        appendEnergyTooltip(stack, tooltip)
    }

    override fun isBarVisible(stack: ItemStack) = true
    override fun getBarWidth(stack: ItemStack) = getEnergyBarStep(stack)
    override fun getBarColor(stack: ItemStack) = getEnergyBarColor(stack)
}

abstract class ElectricMiningDrillItem(
    settings: Item.Properties,
    private val miningToolFactories: List<() -> ItemStack>,
    private val baseEnergyPerBlock: Long,
    private val miningEfficiency: Float? = null
) : Item(settings.stacksTo(1)), IElectricTool {

    protected open fun getEnergyCostPerBlock(stack: ItemStack): Long = baseEnergyPerBlock

    protected fun hasEnoughEnergyForMining(stack: ItemStack): Boolean =
        getEnergy(stack) >= getEnergyCostPerBlock(stack)

    override fun getDestroySpeed(stack: ItemStack, state: BlockState): Float {
        if (!hasEnoughEnergyForMining(stack)) return 1.0f
        miningEfficiency?.let { return it }
        return miningToolFactories.maxOf { factory ->
            val tool = factory()
            tool.item.getDestroySpeed(tool, state)
        }
    }

    override fun isCorrectToolForDrops(stack: ItemStack, state: BlockState): Boolean {
        return miningToolFactories.any { factory ->
            val tool = factory()
            tool.item.isCorrectToolForDrops(tool, state)
        }
    }

    override fun mineBlock(
        stack: ItemStack,
        world: Level,
        state: BlockState,
        pos: BlockPos,
        miner: LivingEntity
    ): Boolean {
        if (!world.isClientSide && state.getDestroySpeed(world, pos) > 0f && (miner !is Player || !miner.isCreative)) {
            val cost = getEnergyCostPerBlock(stack)
            val energy = getEnergy(stack)
            if (energy >= cost) {
                setEnergy(stack, energy - cost)
            }
        }
        return true
    }
}

/** 钻石钻头 - 电动采矿工具（等级 1，10k EU） */
@ModItem(name = "diamond_drill", tab = CreativeTab.IC2_TOOLS, group = "electric_tools", tags = ["minecraft:pickaxes", "minecraft:shovels"])
class DiamondDrill : ElectricMiningDrillItem(
    Item.Properties(),
    miningToolFactories = listOf({ ItemStack(Items.DIAMOND_PICKAXE) }, { ItemStack(Items.DIAMOND_SHOVEL) }),
    baseEnergyPerBlock = 80L,
    miningEfficiency = 16.0f
) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val diamond = Items.DIAMOND
            val drill = Drill::class.instance()
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, DiamondDrill::class.instance(), 1)
                .pattern("   ").pattern(" D ").pattern("DVD")
                .define('D', diamond).define('V', drill)
                .unlockedBy(getHasName(drill), has(drill))
                .save(exporter, DiamondDrill::class.id())
        }
    }

    override val tier = 1
    override val maxCapacity = 30_000L
    override fun nominalEuPerTick() = 100L
    override fun getEnergy(stack: ItemStack) = IElectricTool.getEnergy(stack)
    override fun setEnergy(stack: ItemStack, energy: Long) = IElectricTool.setEnergy(stack, energy, maxCapacity)
    override fun appendHoverText(
        stack: ItemStack,
        context: Item.TooltipContext,
        tooltip: MutableList<Component>,
        type: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltip, type)
        appendEnergyTooltip(stack, tooltip)
    }

    override fun isBarVisible(stack: ItemStack) = true
    override fun getBarWidth(stack: ItemStack) = getEnergyBarStep(stack)
    override fun getBarColor(stack: ItemStack) = getEnergyBarColor(stack)
}

/** 采矿钻头 - 电动采矿工具（等级 1，10k EU） */
@ModItem(name = "drill", tab = CreativeTab.IC2_TOOLS, group = "electric_tools", tags = ["minecraft:pickaxes", "minecraft:shovels"])
class Drill : ElectricMiningDrillItem(
    Item.Properties(),
    miningToolFactories = listOf({ ItemStack(Items.IRON_PICKAXE) }, { ItemStack(Items.IRON_SHOVEL) }),
    baseEnergyPerBlock = 50L,
    miningEfficiency = 8.0f
) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val ironPlate = IronPlate::class.instance()
            val powerUnit = PowerUnitItem::class.instance()
            BatteryEnergyShapedRecipeDatagen.offer(
                exporter = exporter,
                recipeId = Drill::class.id(),
                result = Drill::class.instance(),
                pattern = listOf(" I ", "III", "IPI"),
                keys = mapOf<Char, Item>(
                    'I' to ironPlate,
                    'P' to powerUnit
                ),
                category = "equipment"
            )
        }
    }

    override val tier = 1
    override val maxCapacity = 30_000L
    override fun nominalEuPerTick() = 100L
    override fun getEnergy(stack: ItemStack) = IElectricTool.getEnergy(stack)
    override fun setEnergy(stack: ItemStack, energy: Long) = IElectricTool.setEnergy(stack, energy, maxCapacity)
    override fun appendHoverText(
        stack: ItemStack,
        context: Item.TooltipContext,
        tooltip: MutableList<Component>,
        type: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltip, type)
        appendEnergyTooltip(stack, tooltip)
    }

    override fun isBarVisible(stack: ItemStack) = true
    override fun getBarWidth(stack: ItemStack) = getEnergyBarStep(stack)
    override fun getBarColor(stack: ItemStack) = getEnergyBarColor(stack)
}

/** 电动树脂提取器 - 从橡胶木提取树脂（等级 1，10k EU） */
@ModItem(name = "electric_treetap", tab = CreativeTab.IC2_TOOLS, group = "electric_tools")
class ElectricTreetap : Item(Item.Properties().stacksTo(1)), IElectricTool {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val treetap = Treetap::class.instance()
            val smallPower = SmallPowerUnitItem::class.instance()
            ConsumeTreetapShapedRecipeDatagen.offer(
                exporter = exporter,
                recipeId = ElectricTreetap::class.id(),
                result = ElectricTreetap::class.instance(),
                pattern = listOf("   ", " T ", " P "),
                keys = mapOf<Char, Item>(
                    'T' to treetap,
                    'P' to smallPower
                )
            )
        }
    }

    override val tier = 1
    override val maxCapacity = 10_000L
    override fun nominalEuPerTick() = 100L
    override fun getEnergy(stack: ItemStack) = IElectricTool.getEnergy(stack)
    override fun setEnergy(stack: ItemStack, energy: Long) = IElectricTool.setEnergy(stack, energy, maxCapacity)
    override fun appendHoverText(
        stack: ItemStack,
        context: Item.TooltipContext,
        tooltip: MutableList<Component>,
        type: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltip, type)
        appendEnergyTooltip(stack, tooltip)
    }

    override fun isBarVisible(stack: ItemStack) = true
    override fun getBarWidth(stack: ItemStack) = getEnergyBarStep(stack)
    override fun getBarColor(stack: ItemStack) = getEnergyBarColor(stack)
}

/** 电动扳手 - 拆卸机器、旋转方块（等级 1，10k EU） */
@ModItem(name = "electric_wrench", tab = CreativeTab.IC2_TOOLS, group = "electric_tools")
class ElectricWrench : Item(Item.Properties().stacksTo(1)), IElectricTool {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val wrench = Wrench::class.instance()
            val smallPower = SmallPowerUnitItem::class.instance()
            ConsumeWrenchShapedRecipeDatagen.offer(
                exporter = exporter,
                recipeId = ElectricWrench::class.id(),
                result = ElectricWrench::class.instance(),
                pattern = listOf(" W ", " P ", "   "),
                keys = mapOf<Char, Item>(
                    'W' to wrench,
                    'P' to smallPower
                ),
                category = "equipment"
            )
        }
    }

    override val tier = 1
    override val maxCapacity = 12_000L
    override fun nominalEuPerTick() = 250L
    override fun getEnergy(stack: ItemStack) = IElectricTool.getEnergy(stack)
    override fun setEnergy(stack: ItemStack, energy: Long) = IElectricTool.setEnergy(stack, energy, maxCapacity)
    override fun appendHoverText(
        stack: ItemStack,
        context: Item.TooltipContext,
        tooltip: MutableList<Component>,
        type: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltip, type)
        appendEnergyTooltip(stack, tooltip)
    }

    override fun isBarVisible(stack: ItemStack) = true
    override fun getBarWidth(stack: ItemStack) = getEnergyBarStep(stack)
    override fun getBarColor(stack: ItemStack) = getEnergyBarColor(stack)
}

/** 铱钻头 - 高级电动采矿工具（等级 3，300k EU），模式键 + 右键切换精准采集 */
@ModItem(name = "iridium_drill", tab = CreativeTab.IC2_TOOLS, group = "electric_tools", tags = ["minecraft:pickaxes", "minecraft:shovels"])
class IridiumDrill : ElectricMiningDrillItem(
    Item.Properties(),
    miningToolFactories = listOf({ ItemStack(Items.NETHERITE_PICKAXE) }, { ItemStack(Items.NETHERITE_SHOVEL) }),
    baseEnergyPerBlock = 800L,
    miningEfficiency = 24.0f
) {
    override val tier = 3
    override val maxCapacity = 300_000L
    override fun nominalEuPerTick() = 1_000L

    companion object {
        private const val ENERGY_PER_BLOCK = 800L
        private const val SILK_TOUCH_MULTIPLIER = 10L

        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val reinforcedIridium = IridiumPlate::class.instance()
            val diamondDrill = DiamondDrill::class.instance()
            val energyCrystal = EnergyCrystalItem::class.instance()
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, IridiumDrill::class.instance(), 1)
                .pattern(" R ").pattern("RDR").pattern(" E ")
                .define('R', reinforcedIridium).define('D', diamondDrill).define('E', energyCrystal)
                .unlockedBy(getHasName(diamondDrill), has(diamondDrill))
                .save(exporter, IridiumDrill::class.id())
        }

        private const val SILK_TOUCH_KEY = "SilkTouchEnabled"

        fun isSilkTouchEnabled(stack: ItemStack): Boolean =
            stack.orCreateNbt.getBoolean(SILK_TOUCH_KEY)

        fun toggleSilkTouch(stack: ItemStack): Boolean {
            val nbt = stack.orCreateNbt
            val enabled = !nbt.getBoolean(SILK_TOUCH_KEY)
            nbt.putBoolean(SILK_TOUCH_KEY, enabled)
            return enabled
        }

        private val VIRTUAL_ENCHANT_KEYS = setOf(
            Enchantments.SILK_TOUCH,
            Enchantments.FORTUNE,
            Enchantments.EFFICIENCY
        )

        private fun getExpectedEnchantments(
            stack: ItemStack,
            hasEnergyForCurrentMode: Boolean
        ): Map<ResourceKey<net.minecraft.world.item.enchantment.Enchantment>, Int> {
            if (!hasEnergyForCurrentMode) return emptyMap()
            return if (isSilkTouchEnabled(stack)) {
                mapOf(Enchantments.SILK_TOUCH to 1)
            } else {
                mapOf(
                    Enchantments.FORTUNE to 3,
                    Enchantments.EFFICIENCY to 3
                )
            }
        }

        fun syncVirtualEnchantments(stack: ItemStack, hasEnergyForCurrentMode: Boolean) {
            val mutable = ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(stack))
            mutable.removeIf { holder ->
                val key = BuiltInRegistries.ENCHANTMENT.getKey(holder.value())
                key != null && key in VIRTUAL_ENCHANT_KEYS
            }
            for ((key, level) in getExpectedEnchantments(stack, hasEnergyForCurrentMode)) {
                mutable.set(BuiltInRegistries.ENCHANTMENT.wrapAsHolder(key), level)
            }
            EnchantmentHelper.setEnchantments(stack, mutable.immutable())
        }
    }

    override fun getEnergyCostPerBlock(stack: ItemStack): Long {
        return if (isSilkTouchEnabled(stack)) ENERGY_PER_BLOCK * SILK_TOUCH_MULTIPLIER else ENERGY_PER_BLOCK
    }

    override fun getEnergy(stack: ItemStack) = IElectricTool.getEnergy(stack)
    override fun setEnergy(stack: ItemStack, energy: Long) = IElectricTool.setEnergy(stack, energy, maxCapacity)
    override fun inventoryTick(
        stack: ItemStack,
        world: Level,
        entity: net.minecraft.world.entity.Entity,
        slot: Int,
        selected: Boolean
    ) {
        super.inventoryTick(stack, world, entity, slot, selected)
        val enoughEnergy = getEnergy(stack) >= getEnergyCostPerBlock(stack)
        syncVirtualEnchantments(stack, enoughEnergy)
    }

    override fun appendHoverText(
        stack: ItemStack,
        context: Item.TooltipContext,
        tooltip: MutableList<Component>,
        type: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltip, type)
        appendEnergyTooltip(stack, tooltip)
    }

    override fun isBarVisible(stack: ItemStack) = true
    override fun getBarWidth(stack: ItemStack) = getEnergyBarStep(stack)
    override fun getBarColor(stack: ItemStack) = getEnergyBarColor(stack)
}

private object NanoSaberMaterial : Tier {
    override fun getUses() = 1
    override fun getSpeed() = 1.0f
    override fun getAttackDamageBonus() = 0f
    override fun getIncorrectBlocksForDrops(): TagKey<Block> = BlockTags.INCORRECT_FOR_WOOD
    override fun getEnchantmentValue() = 15
    override fun getRepairIngredient(): Ingredient = Ingredient.EMPTY
}

/**
 * 纳米剑：等级 3，160k EU。右键切换激活。
 * 激活且至少有 400 EU 时攻击伤害 21；未激活或电量不足时为 5。激活时每击消耗 400 EU。
 */
@ModItem(name = "nano_saber", tab = CreativeTab.IC2_TOOLS, group = "electric_tools")
class NanoSaber : SwordItem(
    NanoSaberMaterial,
    Item.Properties().stacksTo(1).attributes(SwordItem.createAttributes(NanoSaberMaterial, 0, -2.4f))
), IElectricTool {

    companion object {
        private const val NBT_ACTIVE = "NanoSaberActive"
        private const val ENERGY_PER_HIT = 400L
        private const val NANO_SABER_MAX_CAPACITY = 160_000L

        private const val DAMAGE_INACTIVE_TOTAL = 5.0
        private const val DAMAGE_ACTIVE_TOTAL = 21.0

        // 空闲消耗：按 tick 累积，每 FLUSH_INTERVAL tick 批量写入 NBT 一次
        private const val IDLE_COST_HOTBAR = 4L     // 快捷栏 4 EU/t
        private const val IDLE_COST_INVENTORY = 1L  // 背包 1 EU/t（每 4 tick 累计 ≈ 0.25 EU/t）
        private const val FLUSH_INTERVAL = 100      // 100 tick = 5 秒批量刷盘
        private const val INVENTORY_TICK_MOD = 4    // 背包每 4 tick 才累计一次

        /** 待机累计待扣电量：entityUUID → (slot → PendingDrain) */
        private val pendingDrains = mutableMapOf<java.util.UUID, MutableMap<Int, PendingDrain>>()

        data class PendingDrain(var drain: Long = 0, var ticks: Int = 0)

        /** 动画 tick 计数器，激活时每 tick +1，未激活归零 */
        var ticker = 0
        private const val ANIMATION_TICKS_PER_FRAME = 5

        fun isActive(stack: ItemStack): Boolean = stack.orCreateNbt.getBoolean(NBT_ACTIVE)

        fun toggleActive(stack: ItemStack, entity: Entity?): Boolean {
            val nbt = stack.orCreateNbt
            val v = !nbt.getBoolean(NBT_ACTIVE)
            nbt.putBoolean(NBT_ACTIVE, v)
            if (!v) {
                ticker = 0
                if (entity != null) {
                    removeAllDrainsFor(entity)
                }
            }
            return v
        }

        /** 返回 0.0（未激活）或 0.1~1.0（激活时对应 10 帧） */
        fun getActiveData(): Float {
            if (ticker <= 0) return 0.0f
            val frame = (ticker / ANIMATION_TICKS_PER_FRAME % 10 + 1)
            return frame / 10.0f
        }

        /** 按快捷栏空闲消耗率（4 EU/t）估算剩余续航分钟数 */
        fun getEstimatedMinutes(stack: ItemStack): Int {
            val energy = IElectricTool.getEnergy(stack)
            return (energy / 4800).toInt()
        }

        private fun flushDrain(stack: ItemStack, state: PendingDrain) {
            if (state.drain <= 0L) return
            val newEnergy = (IElectricTool.getEnergy(stack) - state.drain).coerceAtLeast(0)
            IElectricTool.setEnergy(stack, newEnergy, NANO_SABER_MAX_CAPACITY)
            state.drain = 0L
            state.ticks = 0
        }

        fun removeAllDrainsFor(entity: Entity) {
            pendingDrains.remove(entity.uuid)
        }

        /** 刷新实体的所有待扣槽位（用于关闭时） */
        fun flushAllPendingDrainsFor(entity: Entity, stack: ItemStack) {
            val perSlot = pendingDrains[entity.uuid] ?: return
            for ((_, state) in perSlot) {
                if (state.drain > 0L) flushDrain(stack, state)
            }
        }

        /** 刷新实体的第一个待扣槽位（用于挥砍命中等无法确定槽位的场景） */
        fun flushAnyPendingDrainFor(entity: Entity, stack: ItemStack) {
            val perSlot = pendingDrains[entity.uuid] ?: return
            for ((_, state) in perSlot) {
                if (state.drain > 0L) {
                    flushDrain(stack, state)
                    return
                }
            }
        }

        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val glow = Items.GLOWSTONE_DUST
            val alloy = Alloy::class.instance()
            val plate = CarbonPlate::class.instance()
            val crystal = EnergyCrystalItem::class.instance()
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NanoSaber::class.instance(), 1)
                .pattern("GA ")
                .pattern("GA ")
                .pattern("CEC")
                .define('G', glow)
                .define('A', alloy)
                .define('C', plate)
                .define('E', crystal)
                .unlockedBy(getHasName(crystal), has(crystal))
                .save(exporter, NanoSaber::class.id())
        }
    }

    override val tier = 3
    override val maxCapacity = 160_000L
    override fun nominalEuPerTick() = 500L

    override fun getEnergy(stack: ItemStack) = IElectricTool.getEnergy(stack)
    override fun setEnergy(stack: ItemStack, energy: Long) = IElectricTool.setEnergy(stack, energy, maxCapacity)

    override fun isDamageable() = false

    private fun weaponAttackModifier(stack: ItemStack): Double {
        val active = isActive(stack)
        val fullPower = active && getEnergy(stack) >= ENERGY_PER_HIT
        val total = if (fullPower) DAMAGE_ACTIVE_TOTAL else DAMAGE_INACTIVE_TOTAL
        return total - 1.0
    }

    /** 1.21.1：动态攻击属性改为同步 ItemStack 的 ATTRIBUTE_MODIFIERS 组件 */
    private fun syncAttributeModifiers(stack: ItemStack) {
        val mods = ItemAttributeModifiers.builder()
            .add(
                Attributes.ATTACK_DAMAGE,
                AttributeModifier(
                    ResourceLocation.withDefaultNamespace("nano_saber_attack_damage"),
                    weaponAttackModifier(stack),
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            )
            .add(
                Attributes.ATTACK_SPEED,
                AttributeModifier(
                    ResourceLocation.withDefaultNamespace("nano_saber_attack_speed"),
                    -2.4,
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            )
            .build()
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, mods)
    }

    override fun use(world: Level, user: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = user.getItemInHand(player.usedItemHand)
        if (!world.isClient) {
            // 关闭前先把待机累计扣了
            if (isActive(stack)) {
                flushAllPendingDrainsFor(user, stack)
            }
            if (!isActive(stack) && getEnergy(stack) < 16L) {
                return InteractionResultHolder.pass(stack)
            }
            val on = toggleActive(stack, user)
            user.displayClientMessage(
                Component.translatable(if (on) "message.ic2_120.nano_saber.active_on" else "message.ic2_120.nano_saber.active_off"),
                true
            )
        }
        return InteractionResultHolder.success(stack, world.isClient)
    }

    override fun inventoryTick(stack: ItemStack, world: Level, entity: Entity, slot: Int, selected: Boolean) {
        syncAttributeModifiers(stack)
        if (!isActive(stack)) {
            ticker = 0
            return
        }
        ticker++
        if (world.isClient) return  // 客户端只驱动动画

        // --- 服务端：累积待扣电量 ---
        val inHotbar = slot in 0..8
        if (!inHotbar && ticker % INVENTORY_TICK_MOD != 0) return  // 背包每 4 tick 才计一次

        val perSlot = pendingDrains.getOrPut(entity.uuid) { mutableMapOf() }
        val state = perSlot.getOrPut(slot) { PendingDrain() }
        state.drain += if (inHotbar) IDLE_COST_HOTBAR else IDLE_COST_INVENTORY
        state.ticks++

        // 每 FLUSH_INTERVAL tick 批量写入 NBT
        if (state.ticks >= FLUSH_INTERVAL) {
            flushDrain(stack, state)
        }

        // 电量耗尽自动关闭
        val currentEnergy = getEnergy(stack)
        if (currentEnergy <= 0L) {
            toggleActive(stack, entity)
            perSlot.remove(slot)
            if (perSlot.isEmpty()) pendingDrains.remove(entity.uuid)
        }
    }

    override fun postHurtEnemy(stack: ItemStack, target: LivingEntity, attacker: LivingEntity) {
        if (!attacker.level().isClientSide && isActive(stack)) {
            // 先把待机累计扣了
            flushAnyPendingDrainFor(attacker, stack)
            // 再扣当次挥砍
            if (getEnergy(stack) >= ENERGY_PER_HIT) {
                setEnergy(stack, getEnergy(stack) - ENERGY_PER_HIT)
            }
        }
    }

    override fun mineBlock(
        stack: ItemStack,
        world: Level,
        state: BlockState,
        pos: BlockPos,
        miner: LivingEntity
    ): Boolean {
        // 电动工具不使用原版耐久体系，避免无电或误操作时直接损坏。
        return true
    }

    override fun appendHoverText(
        stack: ItemStack,
        context: Item.TooltipContext,
        tooltip: MutableList<Component>,
        type: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltip, type)
        appendEnergyTooltip(stack, tooltip)
        if (isActive(stack)) {
            val minutes = getEstimatedMinutes(stack)
            tooltip.add(Component.translatable("tooltip.ic2_120.nano_saber.est_time", minutes)
                .withStyle(net.minecraft.ChatFormatting.GRAY))
        }
    }

    override fun isBarVisible(stack: ItemStack) = true
    override fun getBarWidth(stack: ItemStack) = getEnergyBarStep(stack)
    override fun getBarColor(stack: ItemStack) = getEnergyBarColor(stack)
}

/** 拟态板 - 伪装方块外观 */
@ModItem(name = "obscurator", tab = CreativeTab.IC2_TOOLS, group = "tools")
class Obscurator : Item(Item.Properties().stacksTo(1))

/** 风力计 - 测量风力，客户端本地计算并输出至聊天栏 */
@ModItem(name = "wind_meter", tab = CreativeTab.IC2_TOOLS, group = "tools")
class WindMeter : Item(Item.Properties().stacksTo(1)) {
    override fun use(world: Level, user: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = user.getItemInHand(player.usedItemHand)
        if (world.isClient) {
            clientOutput?.invoke(world, user)
        }
        return InteractionResultHolder.success(stack)
    }

    companion object {
        /** 客户端注入的输出回调，由 WindMeterClientInitializer 在 onInitializeClient 中设置 */
        var clientOutput: ((Level, Player) -> Unit)? = null

        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val tinCasing = TinCasing::class.instance()
            val bronzeCasing = BronzeCasing::class.instance()
            val smallPowerUnit = SmallPowerUnitItem::class.instance()
            BatteryEnergyShapedRecipeDatagen.offer(
                exporter = exporter,
                recipeId = WindMeter::class.id(),
                result = WindMeter::class.instance(),
                pattern = listOf(" T ", "TBT", " TP"),
                keys = mapOf<Char, Item>(
                    'T' to tinCasing,
                    'B' to bronzeCasing,
                    'P' to smallPowerUnit
                ),
                category = "tools"
            )
        }
    }
}
