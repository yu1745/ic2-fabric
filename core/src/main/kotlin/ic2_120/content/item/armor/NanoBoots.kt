package ic2_120.content.item.armor

import ic2_120.content.item.CarbonPlate
import ic2_120.content.item.ModArmorMaterials
import ic2_120.content.item.energy.EnergyCrystalItem
import ic2_120.content.recipes.crafting.BatteryEnergyShapedRecipeDatagen
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.type
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.item.ArmorItem
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.function.Consumer

/**
 * 纳米靴子 (Nano Boots)
 *
 * 纳米套装的靴子部件，提供基础减伤。
 *
 * ## 核心参数
 *
 * - 载电量：1,000,000 EU（1 MEU）
 * - 能量等级：3
 * - 减伤比例：15%
 *
 * ## 能量消耗
 *
 * - 每减免 1 点伤害消耗 5000 EU
 * - 能量从所有纳米装备均匀扣除
 */
@ModItem(name = "nano_boots", tab = CreativeTab.IC2_MATERIALS, group = "nano_armor")
class NanoBoots : NanoArmorItem(ModArmorMaterials.NANO_ARMOR, ArmorItem.Type.BOOTS, FabricItemSettings().maxCount(1)) {

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val plate = CarbonPlate::class.instance()
            val crystal = EnergyCrystalItem::class.instance()
            if (plate == Items.AIR || crystal == Items.AIR) return
            BatteryEnergyShapedRecipeDatagen.offer(
                exporter = exporter,
                recipeId = NanoBoots::class.id(),
                result = NanoBoots::class.instance(),
                pattern = listOf("   ", "C C", "CEC"),
                keys = mapOf<Char, Item>(
                    'C' to plate,
                    'E' to crystal
                ),
                category = "equipment"
            )
        }
    }

    override fun appendTooltip(stack: ItemStack, world: net.minecraft.world.World?, tooltip: MutableList<Text>, context: net.minecraft.client.item.TooltipContext) {
        super.appendTooltip(stack, world, tooltip, context)
        tooltip.add(Text.literal("减伤: 11.54%").formatted(Formatting.GRAY))
    }
}
