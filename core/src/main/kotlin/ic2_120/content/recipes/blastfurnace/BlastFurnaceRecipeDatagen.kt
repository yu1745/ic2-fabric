package ic2_120.content.recipes.blastfurnace

import com.google.gson.JsonObject
import ic2_120.content.item.IronDust
import ic2_120.content.recipes.IngredientInput
import ic2_120.content.recipes.ModMachineRecipes
import ic2_120.content.recipes.ModTags
import ic2_120.registry.instance
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

/**
 * 高炉配方数据生成
 *
 * 6个配方：铁粉、粉碎铁矿石、纯净粉碎铁矿石、铁锭、铁矿石、深板岩铁矿石
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
            Registries.ITEM.get(Identifier.of("ic2_120", "steel_ingot")), 1,
            Registries.ITEM.get(Identifier.of("ic2_120", "slag")), 1),
        Entry("crushed_iron", IngredientInput.item(Registries.ITEM.get(Identifier.of("ic2_120", "crushed_iron"))),
            Registries.ITEM.get(Identifier.of("ic2_120", "steel_ingot")), 1,
            Registries.ITEM.get(Identifier.of("ic2_120", "slag")), 1),
        Entry("purified_iron", IngredientInput.item(Registries.ITEM.get(Identifier.of("ic2_120", "purified_iron"))),
            Registries.ITEM.get(Identifier.of("ic2_120", "steel_ingot")), 1,
            Registries.ITEM.get(Identifier.of("ic2_120", "slag")), 1),
        Entry("iron_ingot", IngredientInput.tag(ModTags.Compat.Items.INGOTS_IRON, Items.IRON_INGOT),
            Registries.ITEM.get(Identifier.of("ic2_120", "steel_ingot")), 1,
            Registries.ITEM.get(Identifier.of("ic2_120", "slag")), 1),
        Entry("iron_ore", IngredientInput.tag(ModTags.Compat.Items.ORES_IRON, Items.IRON_ORE),
            Registries.ITEM.get(Identifier.of("ic2_120", "steel_ingot")), 1,
            Registries.ITEM.get(Identifier.of("ic2_120", "slag")), 1)
    )

    fun allEntries(): List<Entry> = entries

    fun generateRecipes(exporter: RecipeExporter) {
        entries.forEach { entry ->
            val id = Identifier.of("ic2_120", "blast_furnacing/${entry.name}")
            exporter.accept(
                id,
                BlastFurnaceRecipe(
                    id,
                    entry.input.toIngredient(),
                    ItemStack(entry.steelOutput, entry.steelCount),
                    ItemStack(entry.slagOutput, entry.slagCount)
                ),
                null
            )
        }
    }
}
