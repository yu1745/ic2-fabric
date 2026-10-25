package ic2_120.content.recipes.metalformer

import ic2_120.compat.accept

import com.google.gson.JsonObject
import ic2_120.content.recipes.IngredientInput
import ic2_120.content.recipes.ModMachineRecipes
import ic2_120.content.recipes.ModTags
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import java.util.function.Consumer

/**
 * 金属成型机配方数据生成
 *
 * 三种模式：辊压(Rolling)、切割(Cutting)、挤压(Extruding)
 */
object MetalFormerRecipeDatagen {
    enum class Mode { ROLLING, CUTTING, EXTRUDING }

    data class Entry(
        val name: String,
        val mode: Mode,
        val input: IngredientInput,
        val output: Item,
        val outputCount: Int
    )

    private val entries = listOf(
        // ========== 辊压模式 (Rolling)：锭压成板，板压成外壳 ==========
        // 锭 -> 板（用 tag 跨 mod 兼容）
        Entry("tin_ingot_to_plate", Mode.ROLLING,
            IngredientInput.tag(ModTags.Compat.Items.INGOTS_TIN, net.minecraft.world.item.Items.IRON_INGOT.let { BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "tin_ingot")) }),
            BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "tin_plate")), 1),
        Entry("iron_ingot_to_plate", Mode.ROLLING,
            IngredientInput.tag(ModTags.Compat.Items.INGOTS_IRON, net.minecraft.world.item.Items.IRON_INGOT),
            BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "iron_plate")), 1),
        Entry("copper_ingot_to_plate", Mode.ROLLING,
            IngredientInput.tag(ModTags.Compat.Items.INGOTS_COPPER, BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("minecraft", "copper_ingot"))),
            BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "copper_plate")), 1),
        Entry("bronze_ingot_to_plate", Mode.ROLLING,
            IngredientInput.tag(ModTags.Compat.Items.INGOTS_BRONZE, BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "bronze_ingot"))),
            BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "bronze_plate")), 1),
        Entry("gold_ingot_to_plate", Mode.ROLLING,
            IngredientInput.tag(ModTags.Compat.Items.INGOTS_GOLD, BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("minecraft", "gold_ingot"))),
            BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "gold_plate")), 1),
        Entry("lead_ingot_to_plate", Mode.ROLLING,
            IngredientInput.tag(ModTags.Compat.Items.INGOTS_LEAD, BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "lead_ingot"))),
            BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "lead_plate")), 1),
        Entry("steel_ingot_to_plate", Mode.ROLLING,
            IngredientInput.tag(ModTags.Compat.Items.INGOTS_STEEL, BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "steel_ingot"))),
            BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "steel_plate")), 1),

        // 板 -> 外壳
        Entry("tin_plate_to_casing", Mode.ROLLING,
            IngredientInput.tag(ModTags.Compat.Items.PLATES_TIN, BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "tin_plate"))),
            BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "tin_casing")), 2),
        Entry("iron_plate_to_casing", Mode.ROLLING,
            IngredientInput.tag(ModTags.Compat.Items.PLATES_IRON, BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "iron_plate"))),
            BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "iron_casing")), 2),
        Entry("copper_plate_to_casing", Mode.ROLLING,
            IngredientInput.tag(ModTags.Compat.Items.PLATES_COPPER, BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "copper_plate"))),
            BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "copper_casing")), 2),
        Entry("bronze_plate_to_casing", Mode.ROLLING,
            IngredientInput.tag(ModTags.Compat.Items.PLATES_BRONZE, BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "bronze_plate"))),
            BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "bronze_casing")), 2),
        Entry("gold_plate_to_casing", Mode.ROLLING,
            IngredientInput.tag(ModTags.Compat.Items.PLATES_GOLD, BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "gold_plate"))),
            BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "gold_casing")), 2),
        Entry("lead_plate_to_casing", Mode.ROLLING,
            IngredientInput.tag(ModTags.Compat.Items.PLATES_LEAD, BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "lead_plate"))),
            BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "lead_casing")), 2),
        Entry("steel_plate_to_casing", Mode.ROLLING,
            IngredientInput.tag(ModTags.Compat.Items.PLATES_STEEL, BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "steel_plate"))),
            BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "steel_casing")), 2),

        // ========== 切割模式 (Cutting)：板材切割成导线，外壳切割成货币 ==========
        // 板 -> 导线
        Entry("tin_plate_to_cable", Mode.CUTTING,
            IngredientInput.tag(ModTags.Compat.Items.PLATES_TIN, BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "tin_plate"))),
            BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "tin_cable")), 3),
        Entry("copper_plate_to_cable", Mode.CUTTING,
            IngredientInput.tag(ModTags.Compat.Items.PLATES_COPPER, BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "copper_plate"))),
            BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "copper_cable")), 2),
        Entry("gold_plate_to_cable", Mode.CUTTING,
            IngredientInput.tag(ModTags.Compat.Items.PLATES_GOLD, BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "gold_plate"))),
            BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "gold_cable")), 4),
        Entry("iron_plate_to_cable", Mode.CUTTING,
            IngredientInput.tag(ModTags.Compat.Items.PLATES_IRON, BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "iron_plate"))),
            BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "iron_cable")), 4),

        // 外壳 -> 货币
        Entry("iron_casing_to_coin", Mode.CUTTING,
            IngredientInput.item(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "iron_casing"))),
            BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "coin")), 2),

        // ========== 挤压模式 (Extruding)：锭/外壳/板/块挤压成导线或组件 ==========
        // 锭 -> 导线
        Entry("tin_ingot_to_cable", Mode.EXTRUDING,
            IngredientInput.tag(ModTags.Compat.Items.INGOTS_TIN, BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "tin_ingot"))),
            BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "tin_cable")), 3),
        Entry("gold_ingot_to_cable", Mode.EXTRUDING,
            IngredientInput.tag(ModTags.Compat.Items.INGOTS_GOLD, BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("minecraft", "gold_ingot"))),
            BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "gold_cable")), 4),
        Entry("copper_ingot_to_cable", Mode.EXTRUDING,
            IngredientInput.tag(ModTags.Compat.Items.INGOTS_COPPER, BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("minecraft", "copper_ingot"))),
            BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "copper_cable")), 3),
        Entry("iron_ingot_to_cable", Mode.EXTRUDING,
            IngredientInput.tag(ModTags.Compat.Items.INGOTS_IRON, net.minecraft.world.item.Items.IRON_INGOT),
            BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "iron_cable")), 4),

        // 外壳/板/块 -> 制品
        Entry("tin_casing_to_can", Mode.EXTRUDING,
            IngredientInput.item(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "tin_casing"))),
            BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "tin_can")), 1),
        Entry("iron_plate_to_fuel_rod", Mode.EXTRUDING,
            IngredientInput.tag(ModTags.Compat.Items.PLATES_IRON, BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "iron_plate"))),
            BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "fuel_rod")), 1),
        Entry("iron_casing_to_fence", Mode.EXTRUDING,
            IngredientInput.item(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "iron_casing"))),
            BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "iron_fence")), 1),
        Entry("iron_block_to_shaft", Mode.EXTRUDING,
            IngredientInput.item(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("minecraft", "iron_block"))),
            BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "iron_shaft")), 1),
        Entry("steel_block_to_shaft", Mode.EXTRUDING,
            IngredientInput.item(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "steel_block"))),
            BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "steel_shaft")), 1)
    )

    fun allEntries(): List<Entry> = entries

    fun generateRecipes(exporter: RecipeOutput) {
        entries.forEach { entry ->
            val recipeId = Identifier.fromNamespaceAndPath(
                "ic2_120", "metal_forming/${entry.mode.name.lowercase()}/${entry.name}"
            )
            val output = ItemStack(entry.output, entry.outputCount)
            val recipe: MetalFormerRecipe = when (entry.mode) {
                Mode.ROLLING -> RollingRecipe(recipeId, entry.input.toIngredient(), output)
                Mode.CUTTING -> CuttingRecipe(recipeId, entry.input.toIngredient(), output)
                Mode.EXTRUDING -> ExtrudingRecipe(recipeId, entry.input.toIngredient(), output)
            }
            exporter.accept(recipeId, recipe, null)
        }
    }
}
