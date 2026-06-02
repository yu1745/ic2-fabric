package ic2_120.content.recipes.solidcanner

import com.google.gson.JsonObject
import ic2_120.content.item.EmptyFuelRodItem
import ic2_120.content.item.Mox
import ic2_120.content.item.MoxFuelRodItem
import ic2_120.content.item.Uranium
import ic2_120.content.item.UraniumFuelRodItem
import ic2_120.content.recipes.IngredientInput
import ic2_120.content.recipes.ModMachineRecipes
import ic2_120.registry.instance
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
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

    fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
        entries.forEach { entry ->
            SolidCannerRecipeJsonProvider(
                recipeId = Identifier("ic2_120", "solid_canning/${entry.name}"),
                slot0Ingredient = entry.slot0Ingredient,
                slot0Count = entry.slot0Count,
                slot1Ingredient = entry.slot1Ingredient,
                slot1Count = entry.slot1Count,
                outputItem = entry.outputItem,
                outputCount = entry.outputCount
            ).also(exporter::accept)
        }
    }

    private class SolidCannerRecipeJsonProvider(
        private val recipeId: Identifier,
        private val slot0Ingredient: IngredientInput,
        private val slot0Count: Int,
        private val slot1Ingredient: IngredientInput,
        private val slot1Count: Int,
        private val outputItem: Item,
        private val outputCount: Int
    ) : RecipeJsonProvider {
        override fun serialize(json: JsonObject) {
            json.addProperty("type", "${ModMachineRecipes.recipeType(SolidCannerRecipe::class)}")
            json.add("slot0_ingredient", slot0Ingredient.toJson())
            json.addProperty("slot0_count", slot0Count)
            json.add("slot1_ingredient", slot1Ingredient.toJson())
            json.addProperty("slot1_count", slot1Count)

            val result = JsonObject()
            result.addProperty("item", Registries.ITEM.getId(outputItem).toString())
            result.addProperty("count", outputCount)
            json.add("result", result)
        }

        override fun getSerializer() = ModMachineRecipes.recipeSerializer(SolidCannerRecipe::class)

        override fun getRecipeId(): Identifier = recipeId

        override fun toAdvancementJson(): JsonObject? = null

        override fun getAdvancementId(): Identifier? = null
    }
}
