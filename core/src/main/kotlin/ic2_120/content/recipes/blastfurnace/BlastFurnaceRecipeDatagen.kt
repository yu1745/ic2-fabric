package ic2_120.content.recipes.blastfurnace

import ic2_120.content.recipes.ModMachineRecipes
import ic2_120.registry.instance
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.recipe.Ingredient
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
        val input: Item,
        val steelOutput: Item,
        val steelCount: Int = 1,
        val slagOutput: Item,
        val slagCount: Int = 1
    )

    private val entries = listOf(
        Entry("iron_dust", Registries.ITEM.get(Identifier.of("ic2_120", "iron_dust")),
            Registries.ITEM.get(Identifier.of("ic2_120", "steel_ingot")), 1,
            Registries.ITEM.get(Identifier.of("ic2_120", "slag")), 1),
        Entry("crushed_iron", Registries.ITEM.get(Identifier.of("ic2_120", "crushed_iron")),
            Registries.ITEM.get(Identifier.of("ic2_120", "steel_ingot")), 1,
            Registries.ITEM.get(Identifier.of("ic2_120", "slag")), 1),
        Entry("purified_iron", Registries.ITEM.get(Identifier.of("ic2_120", "purified_iron")),
            Registries.ITEM.get(Identifier.of("ic2_120", "steel_ingot")), 1,
            Registries.ITEM.get(Identifier.of("ic2_120", "slag")), 1),
        Entry("iron_ingot", Items.IRON_INGOT,
            Registries.ITEM.get(Identifier.of("ic2_120", "steel_ingot")), 1,
            Registries.ITEM.get(Identifier.of("ic2_120", "slag")), 1),
        Entry("iron_ore", Items.IRON_ORE,
            Registries.ITEM.get(Identifier.of("ic2_120", "steel_ingot")), 1,
            Registries.ITEM.get(Identifier.of("ic2_120", "slag")), 1),
        Entry("deepslate_iron_ore", Items.DEEPSLATE_IRON_ORE,
            Registries.ITEM.get(Identifier.of("ic2_120", "steel_ingot")), 1,
            Registries.ITEM.get(Identifier.of("ic2_120", "slag")), 1)
    )

    fun allEntries(): List<Entry> = entries

    fun generateRecipes(exporter: RecipeExporter) {
        entries.forEach { entry ->
            val id = Identifier.of("ic2_120", "blast_furnacing/${entry.name}")
            val recipe = BlastFurnaceRecipe(
                id = id,
                ingredient = Ingredient.ofItems(entry.input),
                steelOutput = ItemStack(entry.steelOutput, entry.steelCount),
                slagOutput = ItemStack(entry.slagOutput, entry.slagCount)
            )
            exporter.accept(id, recipe, null)
        }
    }
}
