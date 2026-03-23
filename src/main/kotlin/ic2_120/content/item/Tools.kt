package ic2_120.content.item

import ic2_120.content.item.energy.IElectricTool
import ic2_120.Ic2_120
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.content.block.TeleporterBlock
import ic2_120.content.block.machines.TeleporterBlockEntity
import net.minecraft.item.ItemUsageContext
import net.minecraft.text.Text
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
import net.minecraft.recipe.Ingredient
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.recipe.book.RecipeCategory
import java.util.function.Consumer
import ic2_120.registry.instance
import ic2_120.registry.item
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem

// ========== 工具材料 ==========

/** 青铜工具材料：耐久与挖掘等级同铁，挖掘速度同石头，可用青铜锭修复 */
object BronzeToolMaterial : ToolMaterial {
    override fun getDurability() = 250
    override fun getMiningSpeedMultiplier() = 4.0f
    override fun getAttackDamage() = 2.0f
    override fun getMiningLevel() = 2
    override fun getEnchantability() = 10
    override fun getRepairIngredient(): Ingredient =
        Ingredient.ofItems(Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "bronze_ingot")))
}

// ========== 工具类 ==========

/** 锻造锤 - 将锭锻造成板，将板锻造成外壳 */
@ModItem(name = "forge_hammer", tab = CreativeTab.IC2_TOOLS)
class ForgeHammer : Item(FabricItemSettings().maxDamage(80)) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val iron = Items.IRON_INGOT
            val stick = Items.STICK
            ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, ForgeHammer::class.instance(), 1)
                .pattern("II").pattern("IS").pattern("II")
                .input('I', iron).input('S', stick)
                .criterion(hasItem(iron), conditionsFromItem(iron))
                .offerTo(exporter, ForgeHammer::class.id())
        }
    }

    override fun getRecipeRemainder(stack: ItemStack): ItemStack {
        val result = stack.copy()
        if (result.damage < result.maxDamage - 1) {
            result.damage += 1
            return result
        }
        return ItemStack.EMPTY
    }
}

/** 板材切割剪刀 - 将板材切割成导线 */
@ModItem(name = "cutter", tab = CreativeTab.IC2_TOOLS)
class Cutter : Item(FabricItemSettings().maxDamage(60)) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val ironIngot = Items.IRON_INGOT
            val ironPlate = IronPlate::class.instance()
            val steelIngot = SteelIngot::class.instance()

            // 配方 1：铁板制作切割机
            ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, Cutter::class.instance(), 1)
                .pattern("P P").pattern(" P ").pattern("I I")
                .input('P', ironPlate).input('I', ironIngot)
                .criterion(hasItem(ironPlate), conditionsFromItem(ironPlate))
                .offerTo(exporter, Cutter::class.recipeId("iron_plate"))

            // 配方 2：钢锭制作切割机
            ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, Cutter::class.instance(), 1)
                .pattern("P P").pattern(" P ").pattern("I I")
                .input('P', steelIngot).input('I', ironIngot)
                .criterion(hasItem(steelIngot), conditionsFromItem(steelIngot))
                .offerTo(exporter, Cutter::class.recipeId("steel"))
        }
    }

    override fun getRecipeRemainder(stack: ItemStack): ItemStack {
        val result = stack.copy()
        if (result.damage < result.maxDamage - 1) {
            result.damage += 1
            return result
        }
        return ItemStack.EMPTY
    }
}

// ========== 青铜工具 ==========

@ModItem(name = "bronze_axe", tab = CreativeTab.IC2_TOOLS, group = "bronze_tools")
class BronzeAxe : AxeItem(BronzeToolMaterial, 5f, -3f, FabricItemSettings().maxCount(1)) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val bronze = BronzeIngot::class.instance()
            val stick = Items.STICK
            ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, BronzeAxe::class.instance(), 1)
                .pattern("MM").pattern("MS").pattern(" S")
                .input('M', bronze).input('S', stick)
                .criterion(hasItem(bronze), conditionsFromItem(bronze))
                .offerTo(exporter, BronzeAxe::class.id())
        }
    }
}

@ModItem(name = "bronze_hoe", tab = CreativeTab.IC2_TOOLS, group = "bronze_tools")
class BronzeHoe : HoeItem(BronzeToolMaterial, -1, 0f, FabricItemSettings().maxCount(1)) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val bronze = BronzeIngot::class.instance()
            val stick = Items.STICK
            ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, BronzeHoe::class.instance(), 1)
                .pattern("MM").pattern(" S").pattern(" S")
                .input('M', bronze).input('S', stick)
                .criterion(hasItem(bronze), conditionsFromItem(bronze))
                .offerTo(exporter, BronzeHoe::class.id())
        }
    }
}

@ModItem(name = "bronze_sword", tab = CreativeTab.IC2_TOOLS, group = "bronze_tools")
class BronzeSword : SwordItem(BronzeToolMaterial, 3, -2.4f, FabricItemSettings().maxCount(1)) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val bronze = BronzeIngot::class.instance()
            val stick = Items.STICK
            ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, BronzeSword::class.instance(), 1)
                .pattern("M").pattern("M").pattern("S")
                .input('M', bronze).input('S', stick)
                .criterion(hasItem(bronze), conditionsFromItem(bronze))
                .offerTo(exporter, BronzeSword::class.id())
        }
    }
}

@ModItem(name = "bronze_shovel", tab = CreativeTab.IC2_TOOLS, group = "bronze_tools")
class BronzeShovel : ShovelItem(BronzeToolMaterial, 1.5f, -3f, FabricItemSettings().maxCount(1)) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val bronze = BronzeIngot::class.instance()
            val stick = Items.STICK
            ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, BronzeShovel::class.instance(), 1)
                .pattern("M").pattern("S").pattern("S")
                .input('M', bronze).input('S', stick)
                .criterion(hasItem(bronze), conditionsFromItem(bronze))
                .offerTo(exporter, BronzeShovel::class.id())
        }
    }
}

@ModItem(name = "bronze_pickaxe", tab = CreativeTab.IC2_TOOLS, group = "bronze_tools")
class BronzePickaxe : PickaxeItem(BronzeToolMaterial, 1, -2.8f, FabricItemSettings().maxCount(1)) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val bronze = BronzeIngot::class.instance()
            val stick = Items.STICK
            ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, BronzePickaxe::class.instance(), 1)
                .pattern("MMM").pattern(" S ").pattern(" S ")
                .input('M', bronze).input('S', stick)
                .criterion(hasItem(bronze), conditionsFromItem(bronze))
                .offerTo(exporter, BronzePickaxe::class.id())
        }
    }
}

// ========== 其他工具（占位实现） ==========

/** 测试工具 - 开发调试用 */
@ModItem(name = "debug_item", tab = CreativeTab.IC2_TOOLS, group = "tools")
class DebugItem : Item(FabricItemSettings().maxCount(1))

/** 工具箱 - 存储工具 */
@ModItem(name = "tool_box", tab = CreativeTab.IC2_TOOLS, group = "tools")
class ToolBox : Item(FabricItemSettings().maxCount(1))

/** EU 电表 - 测量导线/机器 EU 流量 */
@ModItem(name = "meter", tab = CreativeTab.IC2_TOOLS, group = "tools")
class Meter : Item(FabricItemSettings().maxCount(1))

/** 木龙头 - 从橡胶树原木湿面提取粘性树脂，寿命 10 次 */
@ModItem(name = "treetap", tab = CreativeTab.IC2_TOOLS, group = "tools")
class Treetap : Item(FabricItemSettings().maxDamage(10)) {
    override fun getRecipeRemainder(stack: ItemStack): ItemStack {
        val result = stack.copy()
        if (result.damage < result.maxDamage - 1) {
            result.damage += 1
            return result
        }
        return ItemStack.EMPTY
    }
}

/** 扳手 - 拆卸机器、旋转方块 */
@ModItem(name = "wrench", tab = CreativeTab.IC2_TOOLS, group = "tools")
class Wrench : Item(FabricItemSettings().maxDamage(120)) {
    override fun getRecipeRemainder(stack: ItemStack): ItemStack {
        val result = stack.copy()
        if (result.damage < result.maxDamage - 1) {
            result.damage += 1
            return result
        }
        return ItemStack.EMPTY
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
            player.sendMessage(Text.literal("双向链接成功: (${pos.x},${pos.y},${pos.z}) <-> (${bindPos.x},${bindPos.y},${bindPos.z})"), true)
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
            tooltip.add(Text.literal("右击传送机记录第一台，再右击另一台完成双向绑定").formatted(net.minecraft.util.Formatting.DARK_GRAY))
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

/** 链锯 - 电动伐木工具（等级 1，10k EU） */
@ModItem(name = "chainsaw", tab = CreativeTab.IC2_TOOLS, group = "electric_tools")
class Chainsaw : Item(FabricItemSettings().maxCount(1)), IElectricTool {
    override val tier = 1
    override val maxCapacity = 10_000L
    override fun getEnergy(stack: ItemStack) = IElectricTool.getEnergy(stack)
    override fun setEnergy(stack: ItemStack, energy: Long) = IElectricTool.setEnergy(stack, energy, maxCapacity)
    override fun appendTooltip(stack: ItemStack, world: net.minecraft.world.World?, tooltip: MutableList<net.minecraft.text.Text>, context: net.minecraft.client.item.TooltipContext) {
        super.appendTooltip(stack, world, tooltip, context)
        appendEnergyTooltip(stack, tooltip)
    }
    override fun isItemBarVisible(stack: ItemStack) = true
    override fun getItemBarStep(stack: ItemStack) = getEnergyBarStep(stack)
    override fun getItemBarColor(stack: ItemStack) = getEnergyBarColor(stack)
}

/** 钻石钻头 - 电动采矿工具（等级 1，10k EU） */
@ModItem(name = "diamond_drill", tab = CreativeTab.IC2_TOOLS, group = "electric_tools")
class DiamondDrill : Item(FabricItemSettings().maxCount(1)), IElectricTool {
    companion object {
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
    override fun appendTooltip(stack: ItemStack, world: net.minecraft.world.World?, tooltip: MutableList<net.minecraft.text.Text>, context: net.minecraft.client.item.TooltipContext) {
        super.appendTooltip(stack, world, tooltip, context)
        appendEnergyTooltip(stack, tooltip)
    }
    override fun isItemBarVisible(stack: ItemStack) = true
    override fun getItemBarStep(stack: ItemStack) = getEnergyBarStep(stack)
    override fun getItemBarColor(stack: ItemStack) = getEnergyBarColor(stack)
}

/** 采矿钻头 - 电动采矿工具（等级 1，10k EU） */
@ModItem(name = "drill", tab = CreativeTab.IC2_TOOLS, group = "electric_tools")
class Drill : Item(FabricItemSettings().maxCount(1)), IElectricTool {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val ironPlate = IronPlate::class.instance()
            val powerUnit = PowerUnitItem::class.instance()
            ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, Drill::class.instance(), 1)
                .pattern(" I ").pattern("III").pattern("IPI")
                .input('I', ironPlate).input('P', powerUnit)
                .criterion(hasItem(ironPlate), conditionsFromItem(ironPlate))
                .offerTo(exporter, Drill::class.id())
        }
    }
    override val tier = 1
    override val maxCapacity = 10_000L
    override fun getEnergy(stack: ItemStack) = IElectricTool.getEnergy(stack)
    override fun setEnergy(stack: ItemStack, energy: Long) = IElectricTool.setEnergy(stack, energy, maxCapacity)
    override fun appendTooltip(stack: ItemStack, world: net.minecraft.world.World?, tooltip: MutableList<net.minecraft.text.Text>, context: net.minecraft.client.item.TooltipContext) {
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
    override val tier = 1
    override val maxCapacity = 10_000L
    override fun getEnergy(stack: ItemStack) = IElectricTool.getEnergy(stack)
    override fun setEnergy(stack: ItemStack, energy: Long) = IElectricTool.setEnergy(stack, energy, maxCapacity)
    override fun appendTooltip(stack: ItemStack, world: net.minecraft.world.World?, tooltip: MutableList<net.minecraft.text.Text>, context: net.minecraft.client.item.TooltipContext) {
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
    override val tier = 1
    override val maxCapacity = 10_000L
    override fun getEnergy(stack: ItemStack) = IElectricTool.getEnergy(stack)
    override fun setEnergy(stack: ItemStack, energy: Long) = IElectricTool.setEnergy(stack, energy, maxCapacity)
    override fun appendTooltip(stack: ItemStack, world: net.minecraft.world.World?, tooltip: MutableList<net.minecraft.text.Text>, context: net.minecraft.client.item.TooltipContext) {
        super.appendTooltip(stack, world, tooltip, context)
        appendEnergyTooltip(stack, tooltip)
    }
    override fun isItemBarVisible(stack: ItemStack) = true
    override fun getItemBarStep(stack: ItemStack) = getEnergyBarStep(stack)
    override fun getItemBarColor(stack: ItemStack) = getEnergyBarColor(stack)
}

/** 铱钻头 - 高级电动采矿工具（等级 3，100k EU），Alt+M 切换精准采集 */
@ModItem(name = "iridium_drill", tab = CreativeTab.IC2_TOOLS, group = "electric_tools")
class IridiumDrill : Item(FabricItemSettings().maxCount(1)), IElectricTool {
    override val tier = 3
    override val maxCapacity = 100_000L

    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val reinforcedIridium = IridiumPlate::class.instance()
            val diamondDrill = DiamondDrill::class.instance()
            val energyCrystal = ic2_120.content.item.energy.EnergyCrystalItem::class.instance()
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
    }

    override fun getEnergy(stack: ItemStack) = IElectricTool.getEnergy(stack)
    override fun setEnergy(stack: ItemStack, energy: Long) = IElectricTool.setEnergy(stack, energy, maxCapacity)
    override fun appendTooltip(stack: ItemStack, world: net.minecraft.world.World?, tooltip: MutableList<net.minecraft.text.Text>, context: net.minecraft.client.item.TooltipContext) {
        super.appendTooltip(stack, world, tooltip, context)
        appendEnergyTooltip(stack, tooltip)
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
class WindMeter : Item(FabricItemSettings().maxCount(1))
