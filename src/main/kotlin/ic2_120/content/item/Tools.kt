package ic2_120.content.item

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
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemUsageContext
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World
import java.util.UUID
import net.minecraft.util.ItemScatterer
import net.minecraft.util.math.BlockPos
import ic2_120.registry.recipeId
import ic2_120.registry.type
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.item.AxeItem
import net.minecraft.item.HoeItem
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.item.PickaxeItem
import net.minecraft.item.ShovelItem
import net.minecraft.item.SwordItem
import net.minecraft.item.ToolMaterial
import net.minecraft.inventory.Inventories
import net.minecraft.nbt.NbtCompound
import net.minecraft.recipe.Ingredient
import net.minecraft.registry.tag.ItemTags
import net.minecraft.client.item.TooltipContext
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Formatting
import net.minecraft.util.collection.DefaultedList
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.enchantment.Enchantments
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.recipe.book.RecipeCategory
import java.util.function.Consumer
import ic2_120.registry.instance
import ic2_120.registry.item
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import ic2_120.registry.annotation.RecipeProvider

// ========== 工具材料 ==========

/** 青铜工具材料：耐久与挖掘等级同铁，挖掘速度同石头，可用青铜锭修复 */
object BronzeToolMaterial : ToolMaterial {
    override fun getDurability() = 250
    override fun getMiningSpeedMultiplier() = 4.0f
    override fun getAttackDamage() = 2.0f
    override fun getMiningLevel() = 2
    override fun getEnchantability() = 10
    override fun getRepairIngredient(): Ingredient =
        Ingredient.fromTag(ModTags.Compat.Items.INGOTS_BRONZE)
}

// ========== 工具类 ==========

/** 锻造锤 - 将锭锻造成板，将板锻造成外壳 */
@ModItem(name = "forge_hammer", tab = CreativeTab.IC2_TOOLS)
class ForgeHammer : Item(FabricItemSettings().maxDamage(80)) {
    override fun appendTooltip(stack: ItemStack, world: World?, tooltip: MutableList<Text>, context: TooltipContext) {
        val remaining = stack.maxDamage - stack.damage
        tooltip.add(Text.literal("剩余使用次数: $remaining").formatted(Formatting.GRAY))
    }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val iron = Ingredient.fromTag(ModTags.Compat.Items.INGOTS_IRON)
            val stick = Items.STICK
            ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, ForgeHammer::class.instance(), 1)
                .pattern("II").pattern("IS").pattern("II")
                .input('I', iron).input('S', stick)
                .criterion(hasItem(Items.IRON_INGOT), conditionsFromItem(Items.IRON_INGOT))
                .offerTo(exporter, ForgeHammer::class.id())
        }
    }
}

/** 板材切割剪刀 - 将板材切割成导线 */
@ModItem(name = "cutter", tab = CreativeTab.IC2_TOOLS)
class Cutter : Item(FabricItemSettings().maxDamage(60)) {
    override fun appendTooltip(stack: ItemStack, world: World?, tooltip: MutableList<Text>, context: TooltipContext) {
        val remaining = stack.maxDamage - stack.damage
        tooltip.add(Text.literal("剩余使用次数: $remaining").formatted(Formatting.GRAY))
    }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val ironIngot = Ingredient.fromTag(ModTags.Compat.Items.INGOTS_IRON)
            val ironPlate = Ingredient.fromTag(ModTags.Compat.Items.PLATES_IRON)
            val steelIngot = Ingredient.fromTag(ModTags.Compat.Items.INGOTS_STEEL)

            // 配方 1：铁板制作切割机
            ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, Cutter::class.instance(), 1)
                .pattern("P P").pattern(" P ").pattern("I I")
                .input('P', ironPlate).input('I', ironIngot)
                .criterion(hasItem(IronPlate::class.instance()), conditionsFromItem(IronPlate::class.instance()))
                .offerTo(exporter, Cutter::class.recipeId("iron_plate"))

            // 配方 2：钢锭制作切割机
            ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, Cutter::class.instance(), 1)
                .pattern("P P").pattern(" P ").pattern("I I")
                .input('P', steelIngot).input('I', ironIngot)
                .criterion(hasItem(SteelIngot::class.instance()), conditionsFromItem(SteelIngot::class.instance()))
                .offerTo(exporter, Cutter::class.recipeId("steel"))
        }
    }
}

// ========== 青铜工具 ==========

@ModItem(name = "bronze_axe", tab = CreativeTab.IC2_TOOLS, group = "bronze_tools")
class BronzeAxe : AxeItem(BronzeToolMaterial, 5f, -3f, FabricItemSettings().maxCount(1)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val bronze = Ingredient.fromTag(ModTags.Compat.Items.INGOTS_BRONZE)
            val stick = Items.STICK
            ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, BronzeAxe::class.instance(), 1)
                .pattern("MM").pattern("MS").pattern(" S")
                .input('M', bronze).input('S', stick)
                .criterion(hasItem(BronzeIngot::class.instance()), conditionsFromItem(BronzeIngot::class.instance()))
                .offerTo(exporter, BronzeAxe::class.id())
        }
    }
}

@ModItem(name = "bronze_hoe", tab = CreativeTab.IC2_TOOLS, group = "bronze_tools")
class BronzeHoe : HoeItem(BronzeToolMaterial, -1, 0f, FabricItemSettings().maxCount(1)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val bronze = Ingredient.fromTag(ModTags.Compat.Items.INGOTS_BRONZE)
            val stick = Items.STICK
            ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, BronzeHoe::class.instance(), 1)
                .pattern("MM").pattern(" S").pattern(" S")
                .input('M', bronze).input('S', stick)
                .criterion(hasItem(BronzeIngot::class.instance()), conditionsFromItem(BronzeIngot::class.instance()))
                .offerTo(exporter, BronzeHoe::class.id())
        }
    }
}

@ModItem(name = "bronze_sword", tab = CreativeTab.IC2_TOOLS, group = "bronze_tools")
class BronzeSword : SwordItem(BronzeToolMaterial, 3, -2.4f, FabricItemSettings().maxCount(1)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val bronze = Ingredient.fromTag(ModTags.Compat.Items.INGOTS_BRONZE)
            val stick = Items.STICK
            ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, BronzeSword::class.instance(), 1)
                .pattern("M").pattern("M").pattern("S")
                .input('M', bronze).input('S', stick)
                .criterion(hasItem(BronzeIngot::class.instance()), conditionsFromItem(BronzeIngot::class.instance()))
                .offerTo(exporter, BronzeSword::class.id())
        }
    }
}

@ModItem(name = "bronze_shovel", tab = CreativeTab.IC2_TOOLS, group = "bronze_tools")
class BronzeShovel : ShovelItem(BronzeToolMaterial, 1.5f, -3f, FabricItemSettings().maxCount(1)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val bronze = Ingredient.fromTag(ModTags.Compat.Items.INGOTS_BRONZE)
            val stick = Items.STICK
            ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, BronzeShovel::class.instance(), 1)
                .pattern("M").pattern("S").pattern("S")
                .input('M', bronze).input('S', stick)
                .criterion(hasItem(BronzeIngot::class.instance()), conditionsFromItem(BronzeIngot::class.instance()))
                .offerTo(exporter, BronzeShovel::class.id())
        }
    }
}

@ModItem(name = "bronze_pickaxe", tab = CreativeTab.IC2_TOOLS, group = "bronze_tools")
class BronzePickaxe : PickaxeItem(BronzeToolMaterial, 1, -2.8f, FabricItemSettings().maxCount(1)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val bronze = Ingredient.fromTag(ModTags.Compat.Items.INGOTS_BRONZE)
            val stick = Items.STICK
            ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, BronzePickaxe::class.instance(), 1)
                .pattern("MMM").pattern(" S ").pattern(" S ")
                .input('M', bronze).input('S', stick)
                .criterion(hasItem(BronzeIngot::class.instance()), conditionsFromItem(BronzeIngot::class.instance()))
                .offerTo(exporter, BronzePickaxe::class.id())
        }
    }
}

/** 除草铲：右键作物架上的杂草可清除并恢复空架（杂草无法用手采摘，需用本工具或破坏方块） */
@ModItem(name = "weeding_spade", tab = CreativeTab.IC2_TOOLS, group = "bronze_tools")
class WeedingSpade : Item(FabricItemSettings().maxDamage(120)) {
    override fun useOnBlock(context: ItemUsageContext): ActionResult {
        val world = context.world
        val pos = context.blockPos
        val state = world.getBlockState(pos)
        if (state.block !is CropBlock) return ActionResult.PASS
        if (state.get(CropBlock.CROP_TYPE) != CropType.WEED) return ActionResult.PASS
        if (world.isClient) return ActionResult.SUCCESS
        val player = context.player ?: return ActionResult.PASS
        val stack = context.stack
        val be = world.getBlockEntity(pos) as? CropBlockEntity ?: return ActionResult.PASS
        val isCreative = player.abilities.creativeMode

        ItemScatterer.spawn(
            world,
            pos.x.toDouble(),
            pos.y.toDouble(),
            pos.z.toDouble(),
            Weed::class.instance().defaultStack
        )
        world.setBlockState(pos, CropStickBlock.defaultStickState(), Block.NOTIFY_ALL)
        if (!isCreative) {
            stack.damage(1, player) { it.sendToolBreakStatus(context.hand) }
        }
        return ActionResult.SUCCESS
    }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val bronze = Ingredient.fromTag(ModTags.Compat.Items.INGOTS_BRONZE)
            val stick = Items.STICK
            ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, WeedingSpade::class.instance(), 1)
                .pattern("M").pattern("S")
                .input('M', bronze).input('S', stick)
                .criterion(hasItem(BronzeIngot::class.instance()), conditionsFromItem(BronzeIngot::class.instance()))
                .offerTo(exporter, WeedingSpade::class.id())
        }
    }
}

// ========== 其他工具（占位实现） ==========

/** 测试工具 - 开发调试用 */
@ModItem(name = "debug_item", tab = CreativeTab.IC2_TOOLS, group = "tools")
class DebugItem : Item(FabricItemSettings().maxCount(1))

/** 工具箱：右键收纳快捷栏工具（最多 8 格）；潜行右键向背包释放，能放多少放多少，放不下的留在箱内。无 GUI。 */
@ModItem(name = "tool_box", tab = CreativeTab.IC2_TOOLS, group = "tools")
class ToolBox : Item(FabricItemSettings().maxCount(1)) {

    override fun use(world: World, user: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val stack = user.getStackInHand(hand)
        if (world.isClient) return TypedActionResult.success(stack)

        val internal = loadInternal(stack)
        if (user.isSneaking) {
            var transferredAny = false
            for (i in internal.indices) {
                val s = internal[i]
                if (s.isEmpty) continue
                val beforeAmount = s.count
                val leftover = s.copy()
                user.inventory.insertStack(leftover)
                internal[i] = leftover
                if (leftover.count < beforeAmount || leftover.isEmpty) {
                    transferredAny = true
                }
            }
            if (transferredAny) {
                user.playSound(SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.2f, 1.1f)
            }
            if (internal.any { !it.isEmpty }) {
                user.sendMessage(Text.translatable("message.ic2_120.tool_box.inventory_partial"), true)
            }
            saveInternal(stack, internal)
            return TypedActionResult.success(stack, true)
        }

        val inv = user.inventory
        val skipHotbarIndex = if (hand == Hand.MAIN_HAND) inv.selectedSlot else -1
        var deposited = false
        for (idx in 0..8) {
            if (idx == skipHotbarIndex) continue
            val slotStack = inv.main[idx]
            if (!slotStack.isStorableForToolBox()) continue
            val dest = internal.indexOfFirst { it.isEmpty }
            if (dest == -1) break
            internal[dest] = slotStack.copy()
            inv.main[idx] = ItemStack.EMPTY
            deposited = true
        }
        if (deposited) {
            user.playSound(SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.PLAYERS, 0.35f, 1.25f)
        }
        saveInternal(stack, internal)
        return TypedActionResult.success(stack, true)
    }

    override fun appendTooltip(
        stack: ItemStack,
        world: net.minecraft.world.World?,
        tooltip: MutableList<Text>,
        context: TooltipContext
    ) {
        super.appendTooltip(stack, world, tooltip, context)
        tooltip.add(Text.translatable("tooltip.ic2_120.tool_box.hint").formatted(Formatting.DARK_GRAY))
        tooltip.add(Text.translatable("tooltip.ic2_120.tool_box.hint_detail").formatted(Formatting.DARK_GRAY))
        val internal = loadInternal(stack)
        val filled = internal.count { !it.isEmpty }
        if (filled == 0) {
            tooltip.add(Text.translatable("tooltip.ic2_120.tool_box.empty").formatted(Formatting.GRAY))
            return
        }
        tooltip.add(
            Text.translatable("tooltip.ic2_120.tool_box.summary", filled, CAPACITY).formatted(Formatting.GRAY)
        )
        for (s in internal) {
            if (s.isEmpty) continue
            val line = Text.literal("· ").formatted(Formatting.DARK_GRAY).append(s.name)
            if (s.count > 1) {
                line.append(Text.literal(" ×${s.count}").formatted(Formatting.DARK_GRAY))
            }
            tooltip.add(line)
        }
    }

    companion object {
        const val CAPACITY = 8
        const val NBT_KEY = "ToolBoxItems"

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val casing = BronzeCasing::class.instance()
            val chest = Items.CHEST
            ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, ToolBox::class.instance(), 1)
                .pattern("BBB")
                .pattern("BCB")
                .pattern("BBB")
                .input('B', casing)
                .input('C', chest)
                .criterion(hasItem(chest), conditionsFromItem(chest))
                .criterion(hasItem(casing), conditionsFromItem(casing))
                .offerTo(exporter, ToolBox::class.id())
        }

        internal fun loadInternal(stack: ItemStack): DefaultedList<ItemStack> {
            val list = DefaultedList.ofSize(CAPACITY, ItemStack.EMPTY)
            val root = stack.nbt ?: return list
            if (!root.contains(NBT_KEY)) return list
            val tag = root.getCompound(NBT_KEY)
            Inventories.readNbt(tag, list)
            return list
        }

        internal fun saveInternal(stack: ItemStack, list: DefaultedList<ItemStack>) {
            val nbt = stack.orCreateNbt
            val tag = NbtCompound()
            Inventories.writeNbt(tag, list)
            nbt.put(NBT_KEY, tag)
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
            if (i is ForgeHammer || i is Cutter || i is WeedingSpade || i is Treetap || i is Wrench ||
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
class Meter : Item(FabricItemSettings().maxCount(1))

/** 木龙头 - 从橡胶树原木湿面提取粘性树脂，寿命 10 次 */
@ModItem(name = "treetap", tab = CreativeTab.IC2_TOOLS, group = "tools")
class Treetap : Item(FabricItemSettings().maxDamage(10)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, Treetap::class.instance(), 1)
                .pattern(" P ")
                .pattern("PPP")
                .pattern("P  ")
                .input('P', ItemTags.PLANKS)
                .criterion(hasItem(Items.OAK_PLANKS), conditionsFromItem(Items.OAK_PLANKS))
                .offerTo(exporter, Treetap::class.id())
        }
    }
}

/** 扳手 - 拆卸机器、旋转方块 */
@ModItem(name = "wrench", tab = CreativeTab.IC2_TOOLS, group = "tools")
class Wrench : Item(FabricItemSettings().maxDamage(120)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val bronze = Ingredient.fromTag(ModTags.Compat.Items.INGOTS_BRONZE)
            ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, Wrench::class.instance(), 1)
                .pattern("B B")
                .pattern("BBB")
                .pattern(" B ")
                .input('B', bronze)
                .criterion(hasItem(BronzeIngot::class.instance()), conditionsFromItem(BronzeIngot::class.instance()))
                .offerTo(exporter, Wrench::class.id())
        }
    }
}

/** 遥控器 - 远程控制机器频率 */
@ModItem(name = "frequency_transmitter", tab = CreativeTab.IC2_TOOLS, group = "tools")
class FrequencyTransmitter : Item(FabricItemSettings().maxCount(1)) {
    companion object {
        private const val NBT_BIND_X = "BindX"
        private const val NBT_BIND_Y = "BindY"
        private const val NBT_BIND_Z = "BindZ"
        private const val NBT_BIND_DIM = "BindDim"
        private const val NBT_HAS_BIND = "HasBind"

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val cable = InsulatedCopperCableBlock::class.item()
            val circuit = Circuit::class.instance()
            if (cable == Items.AIR || circuit == Items.AIR) return
            ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, FrequencyTransmitter::class.instance(), 1)
                .pattern("   ")
                .pattern(" C ")
                .pattern(" E ")
                .input('C', cable)
                .input('E', circuit)
                .criterion(hasItem(circuit), conditionsFromItem(circuit))
                .offerTo(exporter, FrequencyTransmitter::class.id())
        }
    }

    override fun useOnBlock(context: ItemUsageContext): net.minecraft.util.ActionResult {
        val world = context.world
        val pos = context.blockPos
        val stack = context.stack
        val player = context.player
        val state = world.getBlockState(pos)

        if (state.block !is TeleporterBlock) return net.minecraft.util.ActionResult.PASS
        if (player == null) return net.minecraft.util.ActionResult.SUCCESS

        val nbt = stack.orCreateNbt
        val dim = world.registryKey.value.toString()

        if (player.isSneaking || !nbt.getBoolean(NBT_HAS_BIND)) {
            nbt.putBoolean(NBT_HAS_BIND, true)
            nbt.putInt(NBT_BIND_X, pos.x)
            nbt.putInt(NBT_BIND_Y, pos.y)
            nbt.putInt(NBT_BIND_Z, pos.z)
            nbt.putString(NBT_BIND_DIM, dim)
            if (!world.isClient) {
                player.sendMessage(Text.literal("已记录第一台传送机: ${pos.x}, ${pos.y}, ${pos.z}"), true)
            }
            return net.minecraft.util.ActionResult.SUCCESS
        }

        val bindDim = nbt.getString(NBT_BIND_DIM)
        if (bindDim != dim) {
            if (!world.isClient) player.sendMessage(Text.literal("维度不一致，无法绑定。"), true)
            return net.minecraft.util.ActionResult.SUCCESS
        }

        val bindPos = BlockPos(nbt.getInt(NBT_BIND_X), nbt.getInt(NBT_BIND_Y), nbt.getInt(NBT_BIND_Z))
        if (bindPos == pos) {
            if (!world.isClient) player.sendMessage(Text.literal("不能将传送机绑定到自身。"), true)
            return net.minecraft.util.ActionResult.SUCCESS
        }

        val be = world.getBlockEntity(pos) as? TeleporterBlockEntity
        val targetBe = world.getBlockEntity(bindPos) as? TeleporterBlockEntity
        if (be == null || targetBe == null) {
            if (!world.isClient) player.sendMessage(Text.literal("目标坐标不是有效的传送机。"), true)
            return net.minecraft.util.ActionResult.SUCCESS
        }

        if (!world.isClient) {
            be.setTarget(bindPos, bindDim)
            targetBe.setTarget(pos, dim)
            player.sendMessage(
                Text.literal("双向链接成功: (${pos.x},${pos.y},${pos.z}) <-> (${bindPos.x},${bindPos.y},${bindPos.z})"),
                true
            )
            nbt.putBoolean(NBT_HAS_BIND, false)
        }
        return net.minecraft.util.ActionResult.SUCCESS
    }

    override fun appendTooltip(
        stack: ItemStack,
        world: net.minecraft.world.World?,
        tooltip: MutableList<Text>,
        context: net.minecraft.client.item.TooltipContext
    ) {
        super.appendTooltip(stack, world, tooltip, context)
        val nbt = stack.nbt
        val hasBind = nbt?.getBoolean(NBT_HAS_BIND) == true
        if (!hasBind) {
            tooltip.add(Text.literal("未绑定传送机"))
            tooltip.add(
                Text.literal("右击传送机记录第一台，再右击另一台完成双向绑定")
                    .formatted(net.minecraft.util.Formatting.DARK_GRAY)
            )
            return
        }

        val x = nbt!!.getInt(NBT_BIND_X)
        val y = nbt.getInt(NBT_BIND_Y)
        val z = nbt.getInt(NBT_BIND_Z)
        val dim = nbt.getString(NBT_BIND_DIM).ifBlank { "unknown" }
        tooltip.add(Text.literal("已记录第一台传送机").formatted(net.minecraft.util.Formatting.AQUA))
        tooltip.add(Text.literal("坐标: $x, $y, $z"))
        tooltip.add(Text.literal("维度: $dim").formatted(net.minecraft.util.Formatting.DARK_GRAY))
    }
}

/** 链锯 - 电动伐木工具（等级 1，30k EU） */
@ModItem(name = "chainsaw", tab = CreativeTab.IC2_TOOLS, group = "electric_tools")
class Chainsaw : ElectricMiningDrillItem(
    FabricItemSettings(),
    miningToolFactory = { ItemStack(Items.DIAMOND_AXE) },
    baseEnergyPerBlock = 250L
) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
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
    override fun getEnergy(stack: ItemStack) = IElectricTool.getEnergy(stack)
    override fun setEnergy(stack: ItemStack, energy: Long) = IElectricTool.setEnergy(stack, energy, maxCapacity)
    override fun appendTooltip(
        stack: ItemStack,
        world: net.minecraft.world.World?,
        tooltip: MutableList<net.minecraft.text.Text>,
        context: net.minecraft.client.item.TooltipContext
    ) {
        super.appendTooltip(stack, world, tooltip, context)
        appendEnergyTooltip(stack, tooltip)
    }

    override fun isItemBarVisible(stack: ItemStack) = true
    override fun getItemBarStep(stack: ItemStack) = getEnergyBarStep(stack)
    override fun getItemBarColor(stack: ItemStack) = getEnergyBarColor(stack)
}

abstract class ElectricMiningDrillItem(
    settings: FabricItemSettings,
    private val miningToolFactory: () -> ItemStack,
    private val baseEnergyPerBlock: Long
) : Item(settings.maxCount(1)), IElectricTool {

    protected open fun getEnergyCostPerBlock(stack: ItemStack): Long = baseEnergyPerBlock

    protected fun hasEnoughEnergyForMining(stack: ItemStack): Boolean =
        getEnergy(stack) >= getEnergyCostPerBlock(stack)

    override fun getMiningSpeedMultiplier(stack: ItemStack, state: BlockState): Float {
        if (!hasEnoughEnergyForMining(stack)) return 1.0f
        val tool = miningToolFactory()
        return tool.item.getMiningSpeedMultiplier(tool, state)
    }

    override fun isSuitableFor(state: BlockState): Boolean {
        val tool = miningToolFactory()
        return tool.item.isSuitableFor(state)
    }

    override fun postMine(
        stack: ItemStack,
        world: World,
        state: BlockState,
        pos: BlockPos,
        miner: LivingEntity
    ): Boolean {
        if (!world.isClient && state.getHardness(world, pos) > 0f && (miner !is PlayerEntity || !miner.isCreative)) {
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
@ModItem(name = "diamond_drill", tab = CreativeTab.IC2_TOOLS, group = "electric_tools")
class DiamondDrill : ElectricMiningDrillItem(
    FabricItemSettings(),
    miningToolFactory = { ItemStack(Items.DIAMOND_PICKAXE) },
    baseEnergyPerBlock = 80L
) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val diamond = Items.DIAMOND
            val drill = Drill::class.instance()
            ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, DiamondDrill::class.instance(), 1)
                .pattern("   ").pattern(" D ").pattern("DVD")
                .input('D', diamond).input('V', drill)
                .criterion(hasItem(drill), conditionsFromItem(drill))
                .offerTo(exporter, DiamondDrill::class.id())
        }
    }

    override val tier = 1
    override val maxCapacity = 10_000L
    override fun getEnergy(stack: ItemStack) = IElectricTool.getEnergy(stack)
    override fun setEnergy(stack: ItemStack, energy: Long) = IElectricTool.setEnergy(stack, energy, maxCapacity)
    override fun appendTooltip(
        stack: ItemStack,
        world: net.minecraft.world.World?,
        tooltip: MutableList<net.minecraft.text.Text>,
        context: net.minecraft.client.item.TooltipContext
    ) {
        super.appendTooltip(stack, world, tooltip, context)
        appendEnergyTooltip(stack, tooltip)
    }

    override fun isItemBarVisible(stack: ItemStack) = true
    override fun getItemBarStep(stack: ItemStack) = getEnergyBarStep(stack)
    override fun getItemBarColor(stack: ItemStack) = getEnergyBarColor(stack)
}

/** 采矿钻头 - 电动采矿工具（等级 1，10k EU） */
@ModItem(name = "drill", tab = CreativeTab.IC2_TOOLS, group = "electric_tools")
class Drill : ElectricMiningDrillItem(
    FabricItemSettings(),
    miningToolFactory = { ItemStack(Items.IRON_PICKAXE) },
    baseEnergyPerBlock = 50L
) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
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
    override val maxCapacity = 10_000L
    override fun getEnergy(stack: ItemStack) = IElectricTool.getEnergy(stack)
    override fun setEnergy(stack: ItemStack, energy: Long) = IElectricTool.setEnergy(stack, energy, maxCapacity)
    override fun appendTooltip(
        stack: ItemStack,
        world: net.minecraft.world.World?,
        tooltip: MutableList<net.minecraft.text.Text>,
        context: net.minecraft.client.item.TooltipContext
    ) {
        super.appendTooltip(stack, world, tooltip, context)
        appendEnergyTooltip(stack, tooltip)
    }

    override fun isItemBarVisible(stack: ItemStack) = true
    override fun getItemBarStep(stack: ItemStack) = getEnergyBarStep(stack)
    override fun getItemBarColor(stack: ItemStack) = getEnergyBarColor(stack)
}

/** 电动树脂提取器 - 从橡胶木提取树脂（等级 1，10k EU） */
@ModItem(name = "electric_treetap", tab = CreativeTab.IC2_TOOLS, group = "electric_tools")
class ElectricTreetap : Item(FabricItemSettings().maxCount(1)), IElectricTool {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
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
    override fun getEnergy(stack: ItemStack) = IElectricTool.getEnergy(stack)
    override fun setEnergy(stack: ItemStack, energy: Long) = IElectricTool.setEnergy(stack, energy, maxCapacity)
    override fun appendTooltip(
        stack: ItemStack,
        world: net.minecraft.world.World?,
        tooltip: MutableList<net.minecraft.text.Text>,
        context: net.minecraft.client.item.TooltipContext
    ) {
        super.appendTooltip(stack, world, tooltip, context)
        appendEnergyTooltip(stack, tooltip)
    }

    override fun isItemBarVisible(stack: ItemStack) = true
    override fun getItemBarStep(stack: ItemStack) = getEnergyBarStep(stack)
    override fun getItemBarColor(stack: ItemStack) = getEnergyBarColor(stack)
}

/** 电动扳手 - 拆卸机器、旋转方块（等级 1，10k EU） */
@ModItem(name = "electric_wrench", tab = CreativeTab.IC2_TOOLS, group = "electric_tools")
class ElectricWrench : Item(FabricItemSettings().maxCount(1)), IElectricTool {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
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
    override val maxCapacity = 10_000L
    override fun getEnergy(stack: ItemStack) = IElectricTool.getEnergy(stack)
    override fun setEnergy(stack: ItemStack, energy: Long) = IElectricTool.setEnergy(stack, energy, maxCapacity)
    override fun appendTooltip(
        stack: ItemStack,
        world: net.minecraft.world.World?,
        tooltip: MutableList<net.minecraft.text.Text>,
        context: net.minecraft.client.item.TooltipContext
    ) {
        super.appendTooltip(stack, world, tooltip, context)
        appendEnergyTooltip(stack, tooltip)
    }

    override fun isItemBarVisible(stack: ItemStack) = true
    override fun getItemBarStep(stack: ItemStack) = getEnergyBarStep(stack)
    override fun getItemBarColor(stack: ItemStack) = getEnergyBarColor(stack)
}

/** 铱钻头 - 高级电动采矿工具（等级 3，1M EU），模式键 + 右键切换精准采集 */
@ModItem(name = "iridium_drill", tab = CreativeTab.IC2_TOOLS, group = "electric_tools")
class IridiumDrill : ElectricMiningDrillItem(
    FabricItemSettings(),
    miningToolFactory = { ItemStack(Items.DIAMOND_PICKAXE) },
    baseEnergyPerBlock = 800L
) {
    override val tier = 3
    override val maxCapacity = 1_000_000L

    companion object {
        private const val ENERGY_PER_BLOCK = 800L
        private const val SILK_TOUCH_MULTIPLIER = 10L

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val reinforcedIridium = IridiumPlate::class.instance()
            val diamondDrill = DiamondDrill::class.instance()
            val energyCrystal = EnergyCrystalItem::class.instance()
            ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, IridiumDrill::class.instance(), 1)
                .pattern(" R ").pattern("RDR").pattern(" E ")
                .input('R', reinforcedIridium).input('D', diamondDrill).input('E', energyCrystal)
                .criterion(hasItem(diamondDrill), conditionsFromItem(diamondDrill))
                .offerTo(exporter, IridiumDrill::class.id())
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

        private fun getExpectedEnchantments(
            stack: ItemStack,
            hasEnergyForCurrentMode: Boolean
        ): Map<net.minecraft.enchantment.Enchantment, Int> {
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
            val enchants = EnchantmentHelper.get(stack).toMutableMap()
            enchants.remove(Enchantments.SILK_TOUCH)
            enchants.remove(Enchantments.FORTUNE)
            enchants.remove(Enchantments.EFFICIENCY)
            enchants.putAll(getExpectedEnchantments(stack, hasEnergyForCurrentMode))
            EnchantmentHelper.set(enchants, stack)
        }
    }

    override fun getEnergyCostPerBlock(stack: ItemStack): Long {
        return if (isSilkTouchEnabled(stack)) ENERGY_PER_BLOCK * SILK_TOUCH_MULTIPLIER else ENERGY_PER_BLOCK
    }

    override fun getEnergy(stack: ItemStack) = IElectricTool.getEnergy(stack)
    override fun setEnergy(stack: ItemStack, energy: Long) = IElectricTool.setEnergy(stack, energy, maxCapacity)
    override fun inventoryTick(
        stack: ItemStack,
        world: World,
        entity: net.minecraft.entity.Entity,
        slot: Int,
        selected: Boolean
    ) {
        super.inventoryTick(stack, world, entity, slot, selected)
        val enoughEnergy = getEnergy(stack) >= getEnergyCostPerBlock(stack)
        syncVirtualEnchantments(stack, enoughEnergy)
    }

    override fun appendTooltip(
        stack: ItemStack,
        world: net.minecraft.world.World?,
        tooltip: MutableList<net.minecraft.text.Text>,
        context: net.minecraft.client.item.TooltipContext
    ) {
        super.appendTooltip(stack, world, tooltip, context)
        appendEnergyTooltip(stack, tooltip)
    }

    override fun isItemBarVisible(stack: ItemStack) = true
    override fun getItemBarStep(stack: ItemStack) = getEnergyBarStep(stack)
    override fun getItemBarColor(stack: ItemStack) = getEnergyBarColor(stack)
}

private object NanoSaberMaterial : ToolMaterial {
    override fun getDurability() = 1
    override fun getMiningSpeedMultiplier() = 1.0f
    override fun getAttackDamage() = 0f
    override fun getMiningLevel() = 0
    override fun getEnchantability() = 15
    override fun getRepairIngredient(): Ingredient = Ingredient.EMPTY
}

/**
 * 纳米剑：等级 3，160k EU。右键切换激活。
 * 激活且有余电时攻击伤害 21；未激活或激活但无电时为 5。激活时每击消耗 1000 EU。
 */
@ModItem(name = "nano_saber", tab = CreativeTab.IC2_TOOLS, group = "electric_tools")
class NanoSaber : SwordItem(
    NanoSaberMaterial,
    0,
    -2.4f,
    FabricItemSettings().maxCount(1)
), IElectricTool {

    companion object {
        private const val NBT_ACTIVE = "NanoSaberActive"
        private const val ENERGY_PER_HIT = 1000L

        private val ATTACK_DAMAGE_MODIFIER_ID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF")
        private val ATTACK_SPEED_MODIFIER_ID = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3")

        private const val DAMAGE_INACTIVE_TOTAL = 5.0
        private const val DAMAGE_ACTIVE_TOTAL = 21.0

        fun isActive(stack: ItemStack): Boolean = stack.orCreateNbt.getBoolean(NBT_ACTIVE)

        fun toggleActive(stack: ItemStack): Boolean {
            val nbt = stack.orCreateNbt
            val v = !nbt.getBoolean(NBT_ACTIVE)
            nbt.putBoolean(NBT_ACTIVE, v)
            return v
        }

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val glow = Items.GLOWSTONE_DUST
            val alloy = Alloy::class.instance()
            val plate = CarbonPlate::class.instance()
            val crystal = EnergyCrystalItem::class.instance()
            ShapedRecipeJsonBuilder.create(RecipeCategory.COMBAT, NanoSaber::class.instance(), 1)
                .pattern("GA ")
                .pattern("GA ")
                .pattern("CEC")
                .input('G', glow)
                .input('A', alloy)
                .input('C', plate)
                .input('E', crystal)
                .criterion(hasItem(crystal), conditionsFromItem(crystal))
                .offerTo(exporter, NanoSaber::class.id())
        }
    }

    override val tier = 3
    override val maxCapacity = 160_000L

    override fun getEnergy(stack: ItemStack) = IElectricTool.getEnergy(stack)
    override fun setEnergy(stack: ItemStack, energy: Long) = IElectricTool.setEnergy(stack, energy, maxCapacity)

    override fun isDamageable() = false

    private fun weaponAttackModifier(stack: ItemStack): Double {
        val active = isActive(stack)
        val fullPower = active && getEnergy(stack) > 0
        val total = if (fullPower) DAMAGE_ACTIVE_TOTAL else DAMAGE_INACTIVE_TOTAL
        return total - 1.0
    }

    override fun getAttributeModifiers(
        stack: ItemStack,
        slot: EquipmentSlot
    ): Multimap<EntityAttribute, EntityAttributeModifier> {
        if (slot != EquipmentSlot.MAINHAND) {
            return super.getAttributeModifiers(stack, slot)
        }
        val builder = ImmutableMultimap.builder<EntityAttribute, EntityAttributeModifier>()
        builder.put(
            EntityAttributes.GENERIC_ATTACK_DAMAGE,
            EntityAttributeModifier(
                ATTACK_DAMAGE_MODIFIER_ID,
                "Weapon modifier",
                weaponAttackModifier(stack),
                EntityAttributeModifier.Operation.ADDITION
            )
        )
        builder.put(
            EntityAttributes.GENERIC_ATTACK_SPEED,
            EntityAttributeModifier(
                ATTACK_SPEED_MODIFIER_ID,
                "Weapon modifier",
                -2.4,
                EntityAttributeModifier.Operation.ADDITION
            )
        )
        return builder.build()
    }

    override fun use(world: World, user: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val stack = user.getStackInHand(hand)
        if (!world.isClient) {
            val on = toggleActive(stack)
            user.sendMessage(
                Text.translatable(if (on) "message.ic2_120.nano_saber.active_on" else "message.ic2_120.nano_saber.active_off"),
                true
            )
        }
        return TypedActionResult.success(stack, world.isClient)
    }

    override fun postHit(stack: ItemStack, target: LivingEntity, attacker: LivingEntity): Boolean {
        if (!attacker.world.isClient && isActive(stack) && getEnergy(stack) >= ENERGY_PER_HIT) {
            setEnergy(stack, getEnergy(stack) - ENERGY_PER_HIT)
        }
        return true
    }

    override fun postMine(
        stack: ItemStack,
        world: World,
        state: BlockState,
        pos: BlockPos,
        miner: LivingEntity
    ): Boolean {
        // 电动工具不使用原版耐久体系，避免无电或误操作时直接损坏。
        return true
    }

    override fun appendTooltip(
        stack: ItemStack,
        world: net.minecraft.world.World?,
        tooltip: MutableList<Text>,
        context: net.minecraft.client.item.TooltipContext
    ) {
        super.appendTooltip(stack, world, tooltip, context)
        appendEnergyTooltip(stack, tooltip)
        val key = if (isActive(stack)) "tooltip.ic2_120.nano_saber.active" else "tooltip.ic2_120.nano_saber.inactive"
        tooltip.add(Text.translatable(key).formatted(net.minecraft.util.Formatting.GRAY))
    }

    override fun isItemBarVisible(stack: ItemStack) = true
    override fun getItemBarStep(stack: ItemStack) = getEnergyBarStep(stack)
    override fun getItemBarColor(stack: ItemStack) = getEnergyBarColor(stack)
}

/** 拟态板 - 伪装方块外观 */
@ModItem(name = "obscurator", tab = CreativeTab.IC2_TOOLS, group = "tools")
class Obscurator : Item(FabricItemSettings().maxCount(1))

/** 风力计 - 测量风力 */
@ModItem(name = "wind_meter", tab = CreativeTab.IC2_TOOLS, group = "tools")
class WindMeter : Item(FabricItemSettings().maxCount(1)) {
    override fun use(world: World, user: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val stack = user.getStackInHand(hand)
        if (world.isClient) return TypedActionResult.success(stack)

        val mean = ic2_120.content.block.machines.WindKineticGeneratorBlockEntity.meanWindFromY(user.blockY)
        val weather = ic2_120.content.block.machines.WindKineticGeneratorBlockEntity.weatherMultiplier(world)
        val gust = ic2_120.content.block.machines.WindKineticGeneratorBlockEntity.worldGustFactor(world, user.blockPos)
        val effective = mean * weather * gust

        fun ku(multiplier: Int): Int =
            kotlin.math.floor(
                ic2_120.content.block.machines.WindKineticGeneratorBlockEntity.BASE_KU_AT_PEAK * multiplier * effective
            ).toInt().coerceAtLeast(0)

        fun requiredStartY(multiplier: Int): Int {
            val threshold =
                ic2_120.content.block.machines.WindKineticGeneratorBlockEntity.startThresholdForMultiplier(multiplier)
            val nowCanStart = effective >= threshold
            if (nowCanStart) return -2

            // 返回当前天气与随机倍率下可启动的最小 Y；找不到则返回 -1。
            val top = world.topY
            for (y in 0..top) {
                val windAtY =
                    ic2_120.content.block.machines.WindKineticGeneratorBlockEntity.meanWindFromY(y) * weather * gust
                if (windAtY >= threshold) return y
            }
            return -1
        }

        user.openHandledScreen(object : net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory {
            override fun getDisplayName(): Text = Text.translatable("item.ic2_120.wind_meter")

            override fun writeScreenOpeningData(
                serverPlayer: net.minecraft.server.network.ServerPlayerEntity,
                buf: net.minecraft.network.PacketByteBuf
            ) {
                buf.writeVarInt((mean * 1000.0).toInt().coerceAtLeast(0))
                buf.writeVarInt((weather * 1000.0).toInt().coerceAtLeast(0))
                buf.writeVarInt((gust * 1000.0).toInt().coerceAtLeast(0))
                buf.writeVarInt((effective * 1000.0).toInt().coerceAtLeast(0))
                buf.writeVarInt(ku(1))
                buf.writeVarInt(ku(2))
                buf.writeVarInt(ku(3))
                buf.writeVarInt(ku(4))
                buf.writeVarInt(requiredStartY(1))
                buf.writeVarInt(requiredStartY(2))
                buf.writeVarInt(requiredStartY(3))
                buf.writeVarInt(requiredStartY(4))
            }

            override fun createMenu(
                syncId: Int,
                playerInventory: net.minecraft.entity.player.PlayerInventory,
                player: PlayerEntity
            ): net.minecraft.screen.ScreenHandler {
                val delegate =
                    net.minecraft.screen.ArrayPropertyDelegate(ic2_120.content.screen.WindMeterScreenHandler.PROP_COUNT)
                delegate[ic2_120.content.screen.WindMeterScreenHandler.IDX_MEAN_PERMILLE] =
                    (mean * 1000.0).toInt().coerceAtLeast(0)
                delegate[ic2_120.content.screen.WindMeterScreenHandler.IDX_WEATHER_PERMILLE] =
                    (weather * 1000.0).toInt().coerceAtLeast(0)
                delegate[ic2_120.content.screen.WindMeterScreenHandler.IDX_GUST_PERMILLE] =
                    (gust * 1000.0).toInt().coerceAtLeast(0)
                delegate[ic2_120.content.screen.WindMeterScreenHandler.IDX_EFFECTIVE_PERMILLE] =
                    (effective * 1000.0).toInt().coerceAtLeast(0)
                delegate[ic2_120.content.screen.WindMeterScreenHandler.IDX_WOOD_KU] = ku(1)
                delegate[ic2_120.content.screen.WindMeterScreenHandler.IDX_IRON_KU] = ku(2)
                delegate[ic2_120.content.screen.WindMeterScreenHandler.IDX_STEEL_KU] = ku(3)
                delegate[ic2_120.content.screen.WindMeterScreenHandler.IDX_CARBON_KU] = ku(4)
                delegate[ic2_120.content.screen.WindMeterScreenHandler.IDX_WOOD_START_Y] = requiredStartY(1)
                delegate[ic2_120.content.screen.WindMeterScreenHandler.IDX_IRON_START_Y] = requiredStartY(2)
                delegate[ic2_120.content.screen.WindMeterScreenHandler.IDX_STEEL_START_Y] = requiredStartY(3)
                delegate[ic2_120.content.screen.WindMeterScreenHandler.IDX_CARBON_START_Y] = requiredStartY(4)
                return ic2_120.content.screen.WindMeterScreenHandler(syncId, playerInventory, delegate)
            }
        })

        return TypedActionResult.success(stack)
    }
}
