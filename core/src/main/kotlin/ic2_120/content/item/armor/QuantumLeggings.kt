package ic2_120.content.item.armor

import ic2_120.content.block.MachineCasingBlock
import ic2_120.content.item.IridiumPlate
import ic2_120.content.item.ModArmorMaterials
import ic2_120.content.item.energy.LapotronCrystalItem
import ic2_120.content.recipes.crafting.BatteryEnergyShapedRecipeDatagen
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
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
 * 量子护腿 (Quantum Leggings)
 *
 * 量子套装的护腿部件，提供中等减伤。
 *
 * ## 核心参数
 *
 * - 载电量：10,000,000 EU（10 MEU）
 * - 能量等级：4
 * - 减伤比例：30%
 *
 * ## 待实现功能
 *
 * - 3 倍奔跑速度
 * - 冰上 9 倍速度（双击 W 激活）
 *
 * ## 能量消耗
 *
 * - 每减免 1 点伤害消耗 5000 EU
 * - 能量从所有量子装备均匀扣除
 */
@ModItem(name = "quantum_leggings", tab = CreativeTab.IC2_MATERIALS, group = "quantum_armor")
class QuantumLeggings : QuantumArmorItem(ModArmorMaterials.QUANTUM_ARMOR, ArmorItem.Type.LEGGINGS, Item.Settings().maxCount(1)) {

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val casing = MachineCasingBlock::class.item()
            val lapotron = LapotronCrystalItem::class.instance()
            val iridium = IridiumPlate::class.instance()
            val nano = NanoLeggings::class.instance()
            if (casing == Items.AIR || lapotron == Items.AIR || iridium == Items.AIR || nano == Items.AIR) return
            BatteryEnergyShapedRecipeDatagen.offer(
                exporter = exporter,
                recipeId = QuantumLeggings::class.id(),
                result = QuantumLeggings::class.instance(),
                pattern = listOf("MLM", "INI", "G G"),
                keys = mapOf<Char, Item>(
                    'M' to casing,
                    'L' to lapotron,
                    'I' to iridium,
                    'N' to nano,
                    'G' to Items.GLOWSTONE_DUST
                ),
                category = "equipment"
            )
        }
    }

    override fun appendTooltip(stack: ItemStack, context: Item.TooltipContext, tooltip: MutableList<Text>, type: TooltipType) {
        super.appendTooltip(stack, context, tooltip, type)
        tooltip.add(Text.literal("减伤: 30%").formatted(Formatting.GRAY))
    }
}
