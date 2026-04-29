package ic2_120.content.item.armor

import ic2_120.config.Ic2Config
import ic2_120.content.item.Alloy
import ic2_120.content.item.ElectricJetpack
import ic2_120.content.item.IridiumPlate
import ic2_120.content.item.ModArmorMaterials
import ic2_120.content.item.energy.LapotronCrystalItem
import ic2_120.content.recipes.crafting.BatteryEnergyShapedRecipeDatagen
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.type
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.item.ArmorItem
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import ic2_120.editCustomData
import ic2_120.getCustomData

/**
 * 量子胸甲 (Quantum Chestplate)
 *
 * 量子套装的胸甲部件，提供飞行功能。
 *
 * ## 核心参数
 *
 * - 载电量：10,000,000 EU（10 MEU）
 * - 能量等级：4
 * - 减伤比例：44%
 *
 * ## 飞行功能
 *
 * - 耗电：8333 EU/tick（10M EU / 20分钟 = 24000 ticks）
 * - 要求全套量子护甲
 * - 快捷键：Alt + F
 *
 * ## 能量消耗
 *
 * - 每减免 1 点伤害消耗 5000 EU
 * - 能量从所有量子装备均匀扣除
 */
@ModItem(name = "quantum_chestplate", tab = CreativeTab.IC2_MATERIALS, group = "quantum_armor")
class QuantumChestplate : QuantumArmorItem(ModArmorMaterials.QUANTUM_ARMOR, ArmorItem.Type.CHESTPLATE, Item.Settings().maxCount(1)) {

    companion object {
        private const val FLIGHT_KEY = "QuantumFlightEnabled"

        val flightCostPerTick: Long
            get() = Ic2Config.getQuantumChestplateEuPerTick()

        fun toggleFlight(stack: ItemStack): Boolean {
            val enabled = !(stack.getCustomData()?.getBoolean(FLIGHT_KEY) ?: false)
            stack.editCustomData { it.putBoolean(FLIGHT_KEY, enabled) }
            return enabled
        }

        fun isFlightEnabled(stack: ItemStack): Boolean =
            stack.getCustomData()?.getBoolean(FLIGHT_KEY) ?: false

        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val alloy = Alloy::class.instance()
            val nano = NanoChestplate::class.instance()
            val iridium = IridiumPlate::class.instance()
            val lapotron = LapotronCrystalItem::class.instance()
            val jetpack = ElectricJetpack::class.instance()
            if (alloy == Items.AIR || nano == Items.AIR || iridium == Items.AIR || lapotron == Items.AIR || jetpack == Items.AIR) return
            BatteryEnergyShapedRecipeDatagen.offer(
                exporter = exporter,
                recipeId = QuantumChestplate::class.id(),
                result = QuantumChestplate::class.instance(),
                pattern = listOf("ANA", "ILI", "IJI"),
                keys = mapOf<Char, Item>(
                    'A' to alloy,
                    'N' to nano,
                    'I' to iridium,
                    'L' to lapotron,
                    'J' to jetpack
                ),
                category = "equipment"
            )
        }
    }

    override fun appendTooltip(stack: ItemStack, context: Item.TooltipContext, tooltip: MutableList<Text>, type: TooltipType) {
        super.appendTooltip(stack, context, tooltip, type)
        val flightEnabled = stack.getCustomData()?.getBoolean(FLIGHT_KEY) ?: false
        val energy = getEnergy(stack)

        // 计算飞行剩余时间（分钟）
        val remainingMinutes = if (energy > 0 && flightEnabled) {
            val ticks = energy / flightCostPerTick
            val seconds = ticks / 20.0
            val minutes = seconds / 60.0
            "%.1f".format(minutes)
        } else "N/A"

        tooltip.add(Text.literal("飞行: ${if (flightEnabled) "§aON" else "§cOFF"} §8[${remainingMinutes}分钟]").formatted(Formatting.GRAY))
        tooltip.add(Text.literal("减伤: 44% | 需全套量子护甲").formatted(Formatting.GRAY))
    }
}
