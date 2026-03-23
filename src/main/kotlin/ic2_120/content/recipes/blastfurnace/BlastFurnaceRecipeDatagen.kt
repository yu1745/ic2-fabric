package ic2_120.content.recipes.blastfurnace

import com.google.gson.JsonObject
import ic2_120.content.recipes.ModMachineRecipes
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
 * 6个配方：铁粉、粉碎铁矿石、纯净粉碎铁矿石、铁锭、铁矿石、深板岩铁矿石
 */
object BlastFurnaceRecipeDatagen {
    data class Entry(
        val name: String,
        val input: Item,
        val steelOutput: Item,
        val steelCount: Int = 1,
        val slagOutput: Item,
        val slagCount: Int = 1
    )

    private val entries = listOf(
        Entry("iron_dust", Registries.ITEM.get(Identifier("ic2_120", "iron_dust")),
            Registries.ITEM.get(Identifier("ic2_120", "steel_ingot")), 1,
            Registries.ITEM.get(Identifier("ic2_120", "slag")), 1),
        Entry("crushed_iron", Registries.ITEM.get(Identifier("ic2_120", "crushed_iron")),
            Registries.ITEM.get(Identifier("ic2_120", "steel_ingot")), 1,
            Registries.ITEM.get(Identifier("ic2_120", "slag")), 1),
        Entry("purified_iron", Registries.ITEM.get(Identifier("ic2_120", "purified_iron")),
            Registries.ITEM.get(Identifier("ic2_120", "steel_ingot")), 1,
            Registries.ITEM.get(Identifier("ic2_120", "slag")), 1),
        Entry("iron_ingot", Items.IRON_INGOT,
            Registries.ITEM.get(Identifier("ic2_120", "steel_ingot")), 1,
            Registries.ITEM.get(Identifier("ic2_120", "slag")), 1),
        Entry("iron_ore", Items.IRON_ORE,
            Registries.ITEM.get(Identifier("ic2_120", "steel_ingot")), 1,
            Registries.ITEM.get(Identifier("ic2_120", "slag")), 1),
        Entry("deepslate_iron_ore", Items.DEEPSLATE_IRON_ORE,
            Registries.ITEM.get(Identifier("ic2_120", "steel_ingot")), 1,
            Registries.ITEM.get(Identifier("ic2_120", "slag")), 1)
    )

    fun allEntries(): List<Entry> = entries

    fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
        entries.forEach { entry ->
            BlastFurnaceRecipeJsonProvider(
                recipeId = Identifier("ic2_120", "blast_furnacing/${entry.name}"),
                inputItem = entry.input,
                steelOutputItem = entry.steelOutput,
                steelOutputCount = entry.steelCount,
                slagOutputItem = entry.slagOutput,
                slagOutputCount = entry.slagCount
            ).also(exporter::accept)
        }
    }

    private class BlastFurnaceRecipeJsonProvider(
        private val recipeId: Identifier,
        private val inputItem: Item,
        private val steelOutputItem: Item,
        private val steelOutputCount: Int,
        private val slagOutputItem: Item,
        private val slagOutputCount: Int
    ) : RecipeJsonProvider {
        override fun serialize(json: JsonObject) {
            json.addProperty("type", "${ModMachineRecipes.BLAST_FURNACE_TYPE}")

            // 输入成分
            val ingredient = JsonObject()
            ingredient.addProperty("item", Registries.ITEM.getId(inputItem).toString())
            json.add("ingredient", ingredient)

            // 钢锭输出
            val steelResult = JsonObject()
            steelResult.addProperty("item", Registries.ITEM.getId(steelOutputItem).toString())
            steelResult.addProperty("count", steelOutputCount)
            json.add("steel_output", steelResult)

            // 炉渣输出
            val slagResult = JsonObject()
            slagResult.addProperty("item", Registries.ITEM.getId(slagOutputItem).toString())
            slagResult.addProperty("count", slagOutputCount)
            json.add("slag_output", slagResult)
        }

        override fun getSerializer() = ModMachineRecipes.BLAST_FURNACE_SERIALIZER

        override fun getRecipeId(): Identifier = recipeId

        override fun toAdvancementJson(): JsonObject? = null

        override fun getAdvancementId(): Identifier? = null
    }
}
