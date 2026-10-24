package ic2_120.content.recipes.orewashing

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import ic2_120.content.recipes.IngredientInput
import ic2_120.content.recipes.ModMachineRecipes
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import java.util.function.Consumer

/**
 * 洗矿机配方数据生成
 *
 * 8个配方：7种粉碎矿石 + 沙砾
 */
object OreWashingRecipeDatagen {
    data class Entry(
        val name: String,
        val input: IngredientInput,
        val outputs: List<OutputItem>,
        val waterConsumptionDroplets: Long = FluidConstants.BUCKET
    )

    data class OutputItem(
        val item: Item,
        val count: Int
    )

    private val entries = listOf(
        // 粉碎铜矿石
        Entry(
            "crushed_copper",
            IngredientInput.item(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "crushed_copper"))),
            listOf(
                OutputItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "purified_copper")), 1),
                OutputItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "small_copper_dust")), 2),
                OutputItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "stone_dust")), 1)
            ),
            1000L
        ),
        // 粉碎锡矿石
        Entry(
            "crushed_tin",
            IngredientInput.item(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "crushed_tin"))),
            listOf(
                OutputItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "purified_tin")), 1),
                OutputItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "small_tin_dust")), 2),
                OutputItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "stone_dust")), 1)
            ),
            1000L
        ),
        // 粉碎铁矿石
        Entry(
            "crushed_iron",
            IngredientInput.item(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "crushed_iron"))),
            listOf(
                OutputItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "purified_iron")), 1),
                OutputItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "small_iron_dust")), 2),
                OutputItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "stone_dust")), 1)
            ),
            1000L
        ),
        // 粉碎金矿石
        Entry(
            "crushed_gold",
            IngredientInput.item(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "crushed_gold"))),
            listOf(
                OutputItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "purified_gold")), 1),
                OutputItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "small_gold_dust")), 2),
                OutputItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "stone_dust")), 1)
            ),
            1000L
        ),
        // 粉碎铀矿石（副产物是小撮铅粉）
        Entry(
            "crushed_uranium",
            IngredientInput.item(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "crushed_uranium"))),
            listOf(
                OutputItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "purified_uranium")), 1),
                OutputItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "small_lead_dust")), 2),
                OutputItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "stone_dust")), 1)
            ),
            1000L
        ),
        // 粉碎铅矿石（副产物是小撮硫粉*3）
        Entry(
            "crushed_lead",
            IngredientInput.item(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "crushed_lead"))),
            listOf(
                OutputItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "purified_lead")), 1),
                OutputItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "small_sulfur_dust")), 3),
                OutputItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "stone_dust")), 1)
            ),
            1000L
        ),
        // 粉碎银矿石
        Entry(
            "crushed_silver",
            IngredientInput.item(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "crushed_silver"))),
            listOf(
                OutputItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "purified_silver")), 1),
                OutputItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "small_silver_dust")), 2),
                OutputItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "stone_dust")), 1)
            ),
            1000L
        ),
        // 沙砾（只有石粉输出）
        Entry(
            "gravel",
            IngredientInput.item(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("minecraft", "gravel"))),
            listOf(
                OutputItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "stone_dust")), 1)
            ),
            1000L
        )
    )

    fun allEntries(): List<Entry> = entries

    fun generateRecipes(exporter: RecipeOutput) {
        entries.forEach { entry ->
            val recipeId = ResourceLocation.fromNamespaceAndPath("ic2_120", "ore_washing/${entry.name}")
            val recipe = OreWashingRecipe(
                recipeId,
                entry.input.toIngredient(),
                entry.outputs.map { ItemStack(it.item, it.count) },
                entry.waterConsumptionDroplets
            )
            exporter.accept(recipeId, recipe, null)
        }
    }
}
