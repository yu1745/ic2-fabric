package ic2_120.content.recipes.solidcanner

import ic2_120.compat.accept

import com.google.gson.JsonObject
import ic2_120.content.item.EmptyFuelRodItem
import ic2_120.content.item.Mox
import ic2_120.content.item.MoxFuelRodItem
import ic2_120.content.item.Uranium
import ic2_120.content.item.UraniumFuelRodItem
import ic2_120.content.recipes.IngredientInput
import ic2_120.content.recipes.ModMachineRecipes
import ic2_120.registry.instance
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import java.util.function.Consumer

/**
 * 固体装罐机配方数据生成（仅燃料棒装填）。
 *
 * 食物装罐由 [ic2_120.content.block.machines.SolidCannerBlockEntity] 运行时兜底处理。
 */
object SolidCannerRecipeDatagen {

    data class Entry(
        val name: String,
        val slot0Ingredient: IngredientInput,
        val slot0Count: Int,
        val slot1Ingredient: IngredientInput,
        val slot1Count: Int,
        val outputItem: Item,
        val outputCount: Int
    )

    private val entries = listOf(
        // ===== 燃料棒装填 =====
        // 空燃料棒 + 浓缩铀 -> 铀燃料棒
        Entry("fuel_rod_uranium",
              IngredientInput.item(EmptyFuelRodItem::class.instance()), 1,
              IngredientInput.item(Uranium::class.instance()), 1,
              UraniumFuelRodItem::class.instance(), 1),
        // 空燃料棒 + MOX -> MOX燃料棒
        Entry("fuel_rod_mox",
              IngredientInput.item(EmptyFuelRodItem::class.instance()), 1,
              IngredientInput.item(Mox::class.instance()), 1,
              MoxFuelRodItem::class.instance(), 1)
    )

    fun allEntries(): List<Entry> = entries

    fun generateRecipes(exporter: RecipeOutput) {
        entries.forEach { entry ->
            val recipeId = Identifier.fromNamespaceAndPath("ic2_120", "solid_canning/${entry.name}")
            val recipe = SolidCannerRecipe(
                recipeId,
                entry.slot0Ingredient.toIngredient(),
                entry.slot0Count,
                entry.slot1Ingredient.toIngredient(),
                entry.slot1Count,
                ItemStack(entry.outputItem, entry.outputCount)
            )
            exporter.accept(recipeId, recipe, null)
        }
    }
}
