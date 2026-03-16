package ic2_120.content.item

import ic2_120.content.item.energy.IElectricTool
import ic2_120.Ic2_120
import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.type
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.item.AxeItem
import net.minecraft.item.HoeItem
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.PickaxeItem
import net.minecraft.item.ShovelItem
import net.minecraft.item.SwordItem
import net.minecraft.item.ToolMaterial
import net.minecraft.recipe.Ingredient
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

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
class BronzeAxe : AxeItem(BronzeToolMaterial, 5f, -3f, FabricItemSettings().maxCount(1))

@ModItem(name = "bronze_hoe", tab = CreativeTab.IC2_TOOLS, group = "bronze_tools")
class BronzeHoe : HoeItem(BronzeToolMaterial, -1, 0f, FabricItemSettings().maxCount(1))

@ModItem(name = "bronze_sword", tab = CreativeTab.IC2_TOOLS, group = "bronze_tools")
class BronzeSword : SwordItem(BronzeToolMaterial, 3, -2.4f, FabricItemSettings().maxCount(1))

@ModItem(name = "bronze_shovel", tab = CreativeTab.IC2_TOOLS, group = "bronze_tools")
class BronzeShovel : ShovelItem(BronzeToolMaterial, 1.5f, -3f, FabricItemSettings().maxCount(1))

@ModItem(name = "bronze_pickaxe", tab = CreativeTab.IC2_TOOLS, group = "bronze_tools")
class BronzePickaxe : PickaxeItem(BronzeToolMaterial, 1, -2.8f, FabricItemSettings().maxCount(1))

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
class FrequencyTransmitter : Item(FabricItemSettings().maxCount(1))

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

/** 铱钻头 - 高级电动采矿工具（等级 3，100k EU） */
@ModItem(name = "iridium_drill", tab = CreativeTab.IC2_TOOLS, group = "electric_tools")
class IridiumDrill : Item(FabricItemSettings().maxCount(1)), IElectricTool {
    override val tier = 3
    override val maxCapacity = 100_000L
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

/** OD 扫描器 - 矿石密度扫描 */
@ModItem(name = "scanner", tab = CreativeTab.IC2_TOOLS, group = "tools")
class Scanner : Item(FabricItemSettings().maxCount(1))

/** OV 扫描器 - 矿石价值扫描 */
@ModItem(name = "advanced_scanner", tab = CreativeTab.IC2_TOOLS, group = "tools")
class AdvancedScanner : Item(FabricItemSettings().maxCount(1))

/** 风力计 - 测量风力 */
@ModItem(name = "wind_meter", tab = CreativeTab.IC2_TOOLS, group = "tools")
class WindMeter : Item(FabricItemSettings().maxCount(1))
