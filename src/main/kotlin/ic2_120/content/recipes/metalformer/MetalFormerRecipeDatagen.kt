package ic2_120.content.recipes.metalformer

import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.recipe.Ingredient
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

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
        val input: Item,
        val output: Item,
        val outputCount: Int
    )

    private val entries = listOf(
        // ========== 辊压模式 (Rolling)：锭压成板，板压成外壳 ==========
        // 锭 -> 板
        Entry("tin_ingot_to_plate", Mode.ROLLING,
            Registries.ITEM.get(Identifier.of("ic2_120", "tin_ingot")),
            Registries.ITEM.get(Identifier.of("ic2_120", "tin_plate")), 1),
        Entry("iron_ingot_to_plate", Mode.ROLLING,
            Registries.ITEM.get(Identifier.of("minecraft", "iron_ingot")),
            Registries.ITEM.get(Identifier.of("ic2_120", "iron_plate")), 1),
        Entry("copper_ingot_to_plate", Mode.ROLLING,
            Registries.ITEM.get(Identifier.of("minecraft", "copper_ingot")),
            Registries.ITEM.get(Identifier.of("ic2_120", "copper_plate")), 1),
        Entry("bronze_ingot_to_plate", Mode.ROLLING,
            Registries.ITEM.get(Identifier.of("ic2_120", "bronze_ingot")),
            Registries.ITEM.get(Identifier.of("ic2_120", "bronze_plate")), 1),
        Entry("gold_ingot_to_plate", Mode.ROLLING,
            Registries.ITEM.get(Identifier.of("minecraft", "gold_ingot")),
            Registries.ITEM.get(Identifier.of("ic2_120", "gold_plate")), 1),
        Entry("lead_ingot_to_plate", Mode.ROLLING,
            Registries.ITEM.get(Identifier.of("ic2_120", "lead_ingot")),
            Registries.ITEM.get(Identifier.of("ic2_120", "lead_plate")), 1),
        Entry("steel_ingot_to_plate", Mode.ROLLING,
            Registries.ITEM.get(Identifier.of("ic2_120", "steel_ingot")),
            Registries.ITEM.get(Identifier.of("ic2_120", "steel_plate")), 1),

        // 板 -> 外壳
        Entry("tin_plate_to_casing", Mode.ROLLING,
            Registries.ITEM.get(Identifier.of("ic2_120", "tin_plate")),
            Registries.ITEM.get(Identifier.of("ic2_120", "tin_casing")), 2),
        Entry("iron_plate_to_casing", Mode.ROLLING,
            Registries.ITEM.get(Identifier.of("ic2_120", "iron_plate")),
            Registries.ITEM.get(Identifier.of("ic2_120", "iron_casing")), 2),
        Entry("copper_plate_to_casing", Mode.ROLLING,
            Registries.ITEM.get(Identifier.of("ic2_120", "copper_plate")),
            Registries.ITEM.get(Identifier.of("ic2_120", "copper_casing")), 2),
        Entry("bronze_plate_to_casing", Mode.ROLLING,
            Registries.ITEM.get(Identifier.of("ic2_120", "bronze_plate")),
            Registries.ITEM.get(Identifier.of("ic2_120", "bronze_casing")), 2),
        Entry("gold_plate_to_casing", Mode.ROLLING,
            Registries.ITEM.get(Identifier.of("ic2_120", "gold_plate")),
            Registries.ITEM.get(Identifier.of("ic2_120", "gold_casing")), 2),
        Entry("lead_plate_to_casing", Mode.ROLLING,
            Registries.ITEM.get(Identifier.of("ic2_120", "lead_plate")),
            Registries.ITEM.get(Identifier.of("ic2_120", "lead_casing")), 2),
        Entry("steel_plate_to_casing", Mode.ROLLING,
            Registries.ITEM.get(Identifier.of("ic2_120", "steel_plate")),
            Registries.ITEM.get(Identifier.of("ic2_120", "steel_casing")), 2),

        // ========== 切割模式 (Cutting)：板材切割成导线，外壳切割成货币 ==========
        // 板 -> 导线
        Entry("tin_plate_to_cable", Mode.CUTTING,
            Registries.ITEM.get(Identifier.of("ic2_120", "tin_plate")),
            Registries.ITEM.get(Identifier.of("ic2_120", "tin_cable")), 3),
        Entry("copper_plate_to_cable", Mode.CUTTING,
            Registries.ITEM.get(Identifier.of("ic2_120", "copper_plate")),
            Registries.ITEM.get(Identifier.of("ic2_120", "copper_cable")), 2),
        Entry("gold_plate_to_cable", Mode.CUTTING,
            Registries.ITEM.get(Identifier.of("ic2_120", "gold_plate")),
            Registries.ITEM.get(Identifier.of("ic2_120", "gold_cable")), 4),
        Entry("iron_plate_to_cable", Mode.CUTTING,
            Registries.ITEM.get(Identifier.of("ic2_120", "iron_plate")),
            Registries.ITEM.get(Identifier.of("ic2_120", "iron_cable")), 4),

        // 外壳 -> 货币
        Entry("iron_casing_to_coin", Mode.CUTTING,
            Registries.ITEM.get(Identifier.of("ic2_120", "iron_casing")),
            Registries.ITEM.get(Identifier.of("ic2_120", "coin")), 2),

        // ========== 挤压模式 (Extruding)：锭/外壳/板/块挤压成导线或组件 ==========
        // 锭 -> 导线
        Entry("tin_ingot_to_cable", Mode.EXTRUDING,
            Registries.ITEM.get(Identifier.of("ic2_120", "tin_ingot")),
            Registries.ITEM.get(Identifier.of("ic2_120", "tin_cable")), 3),
        Entry("gold_ingot_to_cable", Mode.EXTRUDING,
            Registries.ITEM.get(Identifier.of("minecraft", "gold_ingot")),
            Registries.ITEM.get(Identifier.of("ic2_120", "gold_cable")), 4),
        Entry("copper_ingot_to_cable", Mode.EXTRUDING,
            Registries.ITEM.get(Identifier.of("minecraft", "copper_ingot")),
            Registries.ITEM.get(Identifier.of("ic2_120", "copper_cable")), 3),
        Entry("iron_ingot_to_cable", Mode.EXTRUDING,
            Registries.ITEM.get(Identifier.of("minecraft", "iron_ingot")),
            Registries.ITEM.get(Identifier.of("ic2_120", "iron_cable")), 4),

        // 外壳/板/块 -> 制品
        Entry("tin_casing_to_can", Mode.EXTRUDING,
            Registries.ITEM.get(Identifier.of("ic2_120", "tin_casing")),
            Registries.ITEM.get(Identifier.of("ic2_120", "tin_can")), 1),
        Entry("iron_plate_to_fuel_rod", Mode.EXTRUDING,
            Registries.ITEM.get(Identifier.of("ic2_120", "iron_plate")),
            Registries.ITEM.get(Identifier.of("ic2_120", "fuel_rod")), 1),
        Entry("iron_casing_to_fence", Mode.EXTRUDING,
            Registries.ITEM.get(Identifier.of("ic2_120", "iron_casing")),
            Registries.ITEM.get(Identifier.of("ic2_120", "iron_fence")), 1),
        Entry("iron_block_to_shaft", Mode.EXTRUDING,
            Registries.ITEM.get(Identifier.of("minecraft", "iron_block")),
            Registries.ITEM.get(Identifier.of("ic2_120", "iron_shaft")), 1),
        Entry("steel_block_to_shaft", Mode.EXTRUDING,
            Registries.ITEM.get(Identifier.of("ic2_120", "steel_block")),
            Registries.ITEM.get(Identifier.of("ic2_120", "steel_shaft")), 1)
    )

    fun allEntries(): List<Entry> = entries

    fun generateRecipes(exporter: RecipeExporter) {
        entries.forEach { entry ->
            val id = Identifier.of("ic2_120", "metal_forming/${entry.mode.name.lowercase()}/${entry.name}")
            val recipe = when (entry.mode) {
                Mode.ROLLING -> RollingRecipe(
                    id, Ingredient.ofItems(entry.input), ItemStack(entry.output, entry.outputCount)
                )
                Mode.CUTTING -> CuttingRecipe(
                    id, Ingredient.ofItems(entry.input), ItemStack(entry.output, entry.outputCount)
                )
                Mode.EXTRUDING -> ExtrudingRecipe(
                    id, Ingredient.ofItems(entry.input), ItemStack(entry.output, entry.outputCount)
                )
            }
            exporter.accept(id, recipe, null)
        }
    }
}
