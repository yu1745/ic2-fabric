package ic2_120.content.item.armor

import ic2_120.content.item.IridiumPlate
import ic2_120.content.item.ModArmorMaterials
import ic2_120.content.item.RubberBoots
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

/**
 * 量子靴子 (Quantum Boots)
 *
 * 量子套装的靴子部件，提供基础减伤。
 *
 * ## 核心参数
 *
 * - 载电量：10,000,000 EU（10 MEU）
 * - 能量等级：4
 * - 减伤比例：11%
 *
 * ## 待实现功能
 *
 * - 超级跳（Ctrl+空格，最高 9 格，1000 EU/次）
 *
 * ## 能量消耗
 *
 * - 每减免 1 点伤害消耗 5000 EU
 * - 能量从所有量子装备均匀扣除
 */
@ModItem(name = "quantum_boots", tab = CreativeTab.IC2_MATERIALS, group = "quantum_armor")
class QuantumBoots : QuantumArmorItem(ModArmorMaterials.QUANTUM_ARMOR, ArmorItem.Type.BOOTS, Item.Settings().maxCount(1)) {

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val iridium = IridiumPlate::class.instance()
            val nano = NanoBoots::class.instance()
            val rubber = RubberBoots::class.instance()
            val lapotron = LapotronCrystalItem::class.instance()
            if (iridium == Items.AIR || nano == Items.AIR || rubber == Items.AIR || lapotron == Items.AIR) return
            BatteryEnergyShapedRecipeDatagen.offer(
                exporter = exporter,
                recipeId = QuantumBoots::class.id(),
                result = QuantumBoots::class.instance(),
                pattern = listOf("   ", "INI", "RLR"),
                keys = mapOf<Char, Item>(
                    'I' to iridium,
                    'N' to nano,
                    'R' to rubber,
                    'L' to lapotron
                ),
                category = "equipment"
            )
        }
    }

    override fun appendTooltip(stack: ItemStack, context: Item.TooltipContext, tooltip: MutableList<Text>, type: TooltipType) {
        super.appendTooltip(stack, context, tooltip, type)
        tooltip.add(Text.literal("减伤: 11%").formatted(Formatting.GRAY))
    }
}
