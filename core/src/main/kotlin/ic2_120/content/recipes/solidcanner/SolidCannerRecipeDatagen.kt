package ic2_120.content.recipes.solidcanner

import com.google.gson.JsonObject
import ic2_120.content.item.EmptyFuelRodItem
import ic2_120.content.item.Mox
import ic2_120.content.item.MoxFuelRodItem
import ic2_120.content.item.Uranium
import ic2_120.content.item.UraniumFuelRodItem
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

    fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
        entries.forEach { entry ->
            SolidCannerRecipeJsonProvider(
                recipeId = Identifier("ic2_120", "solid_canning/${entry.name}"),
                slot0Item = entry.slot0Ingredient,
                slot0Count = entry.slot0Count,
                slot1Item = entry.slot1Ingredient,
                slot1Count = entry.slot1Count,
                outputItem = entry.outputItem,
                outputCount = entry.outputCount
            ).also(exporter::accept)
        }
    }

    private class SolidCannerRecipeJsonProvider(
        private val recipeId: Identifier,
        private val slot0Item: Item,
        private val slot0Count: Int,
        private val slot1Item: Item,
        private val slot1Count: Int,
        private val outputItem: Item,
        private val outputCount: Int
    ) : RecipeJsonProvider {
        override fun serialize(json: JsonObject) {
            json.addProperty("type", "${ModMachineRecipes.recipeType(SolidCannerRecipe::class)}")

            // 槽0 成分（锡罐 或 空燃料棒）
            val slot0Ingredient = JsonObject()
            slot0Ingredient.addProperty("item", Registries.ITEM.getId(slot0Item).toString())
            json.add("slot0_ingredient", slot0Ingredient)
            json.addProperty("slot0_count", slot0Count)

            // 槽1 成分（食物 或 核燃料）
            val slot1Ingredient = JsonObject()
            slot1Ingredient.addProperty("item", Registries.ITEM.getId(slot1Item).toString())
            json.add("slot1_ingredient", slot1Ingredient)
            json.addProperty("slot1_count", slot1Count)

            // 产出
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
