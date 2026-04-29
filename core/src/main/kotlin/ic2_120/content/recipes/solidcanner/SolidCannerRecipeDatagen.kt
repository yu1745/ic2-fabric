package ic2_120.content.recipes.solidcanner

import ic2_120.content.item.EmptyFuelRodItem
import ic2_120.content.item.Mox
import ic2_120.content.item.MoxFuelRodItem
import ic2_120.content.item.Uranium
import ic2_120.content.item.UraniumFuelRodItem
import ic2_120.registry.instance
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.recipe.Ingredient
import net.minecraft.util.Identifier

/**
 * 固体装罐机配方数据生成（仅燃料棒装填）。
 *
 * 食物装罐由 [ic2_120.content.block.machines.SolidCannerBlockEntity] 运行时兜底处理。
 */
object SolidCannerRecipeDatagen {

    data class Entry(
        val name: String,
        val slot0Ingredient: Item,
        val slot0Count: Int,
        val slot1Ingredient: Item,
        val slot1Count: Int,
        val outputItem: Item,
        val outputCount: Int
    )

    private val entries = listOf(
        // ===== 燃料棒装填 =====
        // 空燃料棒 + 浓缩铀 -> 铀燃料棒
        Entry("fuel_rod_uranium",
              EmptyFuelRodItem::class.instance(), 1,
              Uranium::class.instance(), 1,
              UraniumFuelRodItem::class.instance(), 1),
        // 空燃料棒 + MOX -> MOX燃料棒
        Entry("fuel_rod_mox",
              EmptyFuelRodItem::class.instance(), 1,
              Mox::class.instance(), 1,
              MoxFuelRodItem::class.instance(), 1)
    )

    fun allEntries(): List<Entry> = entries

    fun generateRecipes(exporter: RecipeExporter) {
        entries.forEach { entry ->
            val id = Identifier.of("ic2_120", "solid_canning/${entry.name}")
            val recipe = SolidCannerRecipe(
                id = id,
                slot0Ingredient = Ingredient.ofItems(entry.slot0Ingredient),
                slot0Count = entry.slot0Count,
                slot1Ingredient = Ingredient.ofItems(entry.slot1Ingredient),
                slot1Count = entry.slot1Count,
                output = ItemStack(entry.outputItem, entry.outputCount)
            )
            exporter.accept(id, recipe, null)
        }
    }
}
