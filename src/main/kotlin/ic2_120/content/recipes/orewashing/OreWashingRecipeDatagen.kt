package ic2_120.content.recipes.orewashing

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import ic2_120.content.recipes.ModMachineRecipes
import ic2_120.registry.instance
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import java.util.function.Consumer

/**
 * 洗矿机配方数据生成
 *
 * 8个配方：7种粉碎矿石 + 沙砾
 */
object OreWashingRecipeDatagen {
    data class Entry(
        val name: String,
        val input: Item,
        val outputs: List<OutputItem>,
        val waterConsumptionMb: Long = 1000L
    )

    data class OutputItem(
        val item: Item,
        val count: Int
    )

    private val entries = listOf(
        // 粉碎铜矿石
        Entry(
            "crushed_copper",
            Registries.ITEM.get(Identifier.of("ic2_120", "crushed_copper")),
            listOf(
                OutputItem(Registries.ITEM.get(Identifier.of("ic2_120", "purified_copper")), 1),
                OutputItem(Registries.ITEM.get(Identifier.of("ic2_120", "stone_dust")), 1),
                OutputItem(Registries.ITEM.get(Identifier.of("ic2_120", "small_copper_dust")), 2)
            ),
            1000L
        ),
        // 粉碎锡矿石
        Entry(
            "crushed_tin",
            Registries.ITEM.get(Identifier.of("ic2_120", "crushed_tin")),
            listOf(
                OutputItem(Registries.ITEM.get(Identifier.of("ic2_120", "purified_tin")), 1),
                OutputItem(Registries.ITEM.get(Identifier.of("ic2_120", "stone_dust")), 1),
                OutputItem(Registries.ITEM.get(Identifier.of("ic2_120", "small_tin_dust")), 2)
            ),
            1000L
        ),
        // 粉碎铁矿石
        Entry(
            "crushed_iron",
            Registries.ITEM.get(Identifier.of("ic2_120", "crushed_iron")),
            listOf(
                OutputItem(Registries.ITEM.get(Identifier.of("ic2_120", "purified_iron")), 1),
                OutputItem(Registries.ITEM.get(Identifier.of("ic2_120", "stone_dust")), 1),
                OutputItem(Registries.ITEM.get(Identifier.of("ic2_120", "small_iron_dust")), 2)
            ),
            1000L
        ),
        // 粉碎金矿石
        Entry(
            "crushed_gold",
            Registries.ITEM.get(Identifier.of("ic2_120", "crushed_gold")),
            listOf(
                OutputItem(Registries.ITEM.get(Identifier.of("ic2_120", "purified_gold")), 1),
                OutputItem(Registries.ITEM.get(Identifier.of("ic2_120", "stone_dust")), 1),
                OutputItem(Registries.ITEM.get(Identifier.of("ic2_120", "small_gold_dust")), 2)
            ),
            1000L
        ),
        // 粉碎铀矿石（副产物是小撮铅粉）
        Entry(
            "crushed_uranium",
            Registries.ITEM.get(Identifier.of("ic2_120", "crushed_uranium")),
            listOf(
                OutputItem(Registries.ITEM.get(Identifier.of("ic2_120", "purified_uranium")), 1),
                OutputItem(Registries.ITEM.get(Identifier.of("ic2_120", "stone_dust")), 1),
                OutputItem(Registries.ITEM.get(Identifier.of("ic2_120", "small_lead_dust")), 2)
            ),
            1000L
        ),
        // 粉碎铅矿石（副产物是小撮硫粉*3）
        Entry(
            "crushed_lead",
            Registries.ITEM.get(Identifier.of("ic2_120", "crushed_lead")),
            listOf(
                OutputItem(Registries.ITEM.get(Identifier.of("ic2_120", "purified_lead")), 1),
                OutputItem(Registries.ITEM.get(Identifier.of("ic2_120", "stone_dust")), 1),
                OutputItem(Registries.ITEM.get(Identifier.of("ic2_120", "small_sulfur_dust")), 3)
            ),
            1000L
        ),
        // 粉碎银矿石
        Entry(
            "crushed_silver",
            Registries.ITEM.get(Identifier.of("ic2_120", "crushed_silver")),
            listOf(
                OutputItem(Registries.ITEM.get(Identifier.of("ic2_120", "purified_silver")), 1),
                OutputItem(Registries.ITEM.get(Identifier.of("ic2_120", "stone_dust")), 1),
                OutputItem(Registries.ITEM.get(Identifier.of("ic2_120", "small_silver_dust")), 2)
            ),
            1000L
        ),
        // 沙砾（只有石粉输出）
        Entry(
            "gravel",
            Registries.ITEM.get(Identifier.of("minecraft", "gravel")),
            listOf(
                OutputItem(Registries.ITEM.get(Identifier.of("ic2_120", "stone_dust")), 1)
            ),
            1000L
        )
    )

    fun allEntries(): List<Entry> = entries

    fun generateRecipes(exporter: Consumer<RecipeExporter>) {
        entries.forEach { entry ->
            OreWashingRecipeExporter(
                recipeId = Identifier.of("ic2_120", "ore_washing/${entry.name}"),
                inputItem = entry.input,
                outputs = entry.outputs,
                waterConsumptionMb = entry.waterConsumptionMb
            ).also(exporter::accept)
        }
    }

    private class OreWashingRecipeExporter(
        private val recipeId: Identifier,
        private val inputItem: Item,
        private val outputs: List<OutputItem>,
        private val waterConsumptionMb: Long
    ) : RecipeExporter {
        override fun serialize(json: JsonObject) {
            json.addProperty("type", "${ModMachineRecipes.recipeType(OreWashingRecipe::class)}")

            // 输入成分
            val ingredient = JsonObject()
            ingredient.addProperty("item", Registries.ITEM.getId(inputItem).toString())
            json.add("ingredient", ingredient)

            // 输出数组
            val outputsArray = JsonArray()
            outputs.forEach { output ->
                val outputObj = JsonObject()
                outputObj.addProperty("item", Registries.ITEM.getId(output.item).toString())
                outputObj.addProperty("count", output.count)
                outputsArray.add(outputObj)
            }
            json.add("outputs", outputsArray)

            // 水消耗
            json.addProperty("water_consumption_mb", waterConsumptionMb)
        }

        override fun getSerializer() = ModMachineRecipes.recipeSerializer(OreWashingRecipe::class)

        override fun getRecipeId(): Identifier = recipeId

        override fun toAdvancementJson(): JsonObject? = null

        override fun getAdvancementId(): Identifier? = null
    }
}
