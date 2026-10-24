package ic2_120.content.recipes.blastfurnace

import com.google.gson.JsonObject
import ic2_120.content.item.IronDust
import ic2_120.content.recipes.IngredientInput
import ic2_120.content.recipes.ModMachineRecipes
import ic2_120.content.recipes.ModTags
import ic2_120.registry.instance
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import java.util.function.Consumer

/**
 * 高炉配方数据生成
 *
 * 6个配方：铁粉、粉碎铁矿石、纯净粉碎铁矿石、铁锭、铁矿石（含深板岩）、粗铁
 */
object BlastFurnaceRecipeDatagen {
    data class Entry(
        val name: String,
        val input: IngredientInput,
        val steelOutput: Item,
        val steelCount: Int = 1,
        val slagOutput: Item,
        val slagCount: Int = 1
    )

    private val entries = listOf(
        Entry("iron_dust", IngredientInput.tag(ModTags.Compat.Items.DUSTS_IRON, IronDust::class.instance()),
            Registries.ITEM.get(Identifier("ic2_120", "steel_ingot")), 1,
            Registries.ITEM.get(Identifier("ic2_120", "slag")), 1),
        Entry("crushed_iron", IngredientInput.item(Registries.ITEM.get(Identifier("ic2_120", "crushed_iron"))),
            Registries.ITEM.get(Identifier("ic2_120", "steel_ingot")), 1,
            Registries.ITEM.get(Identifier("ic2_120", "slag")), 1),
        Entry("purified_iron", IngredientInput.item(Registries.ITEM.get(Identifier("ic2_120", "purified_iron"))),
            Registries.ITEM.get(Identifier("ic2_120", "steel_ingot")), 1,
            Registries.ITEM.get(Identifier("ic2_120", "slag")), 1),
        Entry("iron_ingot", IngredientInput.tag(ModTags.Compat.Items.INGOTS_IRON, Items.IRON_INGOT),
            Registries.ITEM.get(Identifier("ic2_120", "steel_ingot")), 1,
            Registries.ITEM.get(Identifier("ic2_120", "slag")), 1),
        Entry("iron_ore", IngredientInput.tag(ModTags.Compat.Items.ORES_IRON, Items.IRON_ORE),
            Registries.ITEM.get(Identifier("ic2_120", "steel_ingot")), 1,
            Registries.ITEM.get(Identifier("ic2_120", "slag")), 1),
        Entry("raw_iron", IngredientInput.tag(ModTags.Compat.Items.RAW_MATERIALS_IRON, Items.RAW_IRON),
            Registries.ITEM.get(Identifier("ic2_120", "steel_ingot")), 1,
            Registries.ITEM.get(Identifier("ic2_120", "slag")), 1)
    )

    fun allEntries(): List<Entry> = entries

    fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
        entries.forEach { entry ->
            BlastFurnaceRecipeJsonProvider(
                recipeId = Identifier("ic2_120", "blast_furnacing/${entry.name}"),
                input = entry.input,
                steelOutputItem = entry.steelOutput,
                steelOutputCount = entry.steelCount,
                slagOutputItem = entry.slagOutput,
                slagOutputCount = entry.slagCount
            ).also(exporter::accept)
        }
    }

    private class BlastFurnaceRecipeJsonProvider(
        private val recipeId: Identifier,
        private val input: IngredientInput,
        private val steelOutputItem: Item,
        private val steelOutputCount: Int,
        private val slagOutputItem: Item,
        private val slagOutputCount: Int
    ) : RecipeJsonProvider {
        override fun serialize(json: JsonObject) {
            json.addProperty("type", "${ModMachineRecipes.recipeType(BlastFurnaceRecipe::class)}")
            json.add("ingredient", input.toJson())

            val steelResult = JsonObject()
            steelResult.addProperty("item", Registries.ITEM.getId(steelOutputItem).toString())
            steelResult.addProperty("count", steelOutputCount)
            json.add("steel_output", steelResult)

            val slagResult = JsonObject()
            slagResult.addProperty("item", Registries.ITEM.getId(slagOutputItem).toString())
            slagResult.addProperty("count", slagOutputCount)
            json.add("slag_output", slagResult)
        }

        override fun getSerializer() = ModMachineRecipes.recipeSerializer(BlastFurnaceRecipe::class)

        override fun getRecipeId(): Identifier = recipeId

        override fun toAdvancementJson(): JsonObject? = null

        override fun getAdvancementId(): Identifier? = null
    }
}
