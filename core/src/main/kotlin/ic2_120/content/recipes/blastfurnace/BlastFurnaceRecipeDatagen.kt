package ic2_120.content.recipes.blastfurnace

import com.google.gson.JsonObject
import ic2_120.content.item.IronDust
import ic2_120.content.recipes.IngredientInput
import ic2_120.content.recipes.ModMachineRecipes
import ic2_120.content.recipes.ModTags
import ic2_120.registry.instance
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
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
            BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "steel_ingot")), 1,
            BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "slag")), 1),
        Entry("crushed_iron", IngredientInput.item(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "crushed_iron"))),
            BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "steel_ingot")), 1,
            BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "slag")), 1),
        Entry("purified_iron", IngredientInput.item(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "purified_iron"))),
            BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "steel_ingot")), 1,
            BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "slag")), 1),
        Entry("iron_ingot", IngredientInput.tag(ModTags.Compat.Items.INGOTS_IRON, Items.IRON_INGOT),
            BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "steel_ingot")), 1,
            BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "slag")), 1),
        Entry("iron_ore", IngredientInput.tag(ModTags.Compat.Items.ORES_IRON, Items.IRON_ORE),
            BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "steel_ingot")), 1,
            BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "slag")), 1),
        Entry("raw_iron", IngredientInput.tag(ModTags.Compat.Items.RAW_MATERIALS_IRON, Items.RAW_IRON),
            BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "steel_ingot")), 1,
            BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "slag")), 1)
    )

    fun allEntries(): List<Entry> = entries

    fun generateRecipes(exporter: RecipeOutput) {
        entries.forEach { entry ->
            val recipeId = ResourceLocation.fromNamespaceAndPath("ic2_120", "blast_furnacing/${entry.name}")
            val recipe = BlastFurnaceRecipe(
                recipeId,
                entry.input.toIngredient(),
                ItemStack(entry.steelOutput, entry.steelCount),
                ItemStack(entry.slagOutput, entry.slagCount)
            )
            exporter.accept(recipeId, recipe, null)
        }
    }
}
