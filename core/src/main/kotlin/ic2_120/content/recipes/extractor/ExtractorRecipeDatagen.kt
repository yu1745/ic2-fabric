package ic2_120.content.recipes.extractor

import ic2_120.content.block.*
import ic2_120.content.item.*
import ic2_120.content.item.energy.EnergyCrystalItem
import ic2_120.registry.instance
import ic2_120.registry.item
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.recipe.Ingredient
import net.minecraft.util.Identifier

object ExtractorRecipeDatagen {
    data class Entry(
        val name: String,
        val input: Item,
        val output: Item,
        val count: Int
    )

    private val entries = listOf(
        // 仅保留白名单合法输入
        Entry("resin_to_rubber", Resin::class.instance(), RubberItem::class.instance(), 3),
        Entry("rubber_log_to_rubber", RubberLogBlock::class.item(), RubberItem::class.instance(), 1),
        Entry("rubber_sapling_to_rubber", RubberSaplingBlock::class.item(), RubberItem::class.instance(), 1),

        Entry("orange_wool_to_wool", Items.ORANGE_WOOL, Items.WHITE_WOOL, 1),
        Entry("magenta_wool_to_wool", Items.MAGENTA_WOOL, Items.WHITE_WOOL, 1),
        Entry("light_blue_wool_to_wool", Items.LIGHT_BLUE_WOOL, Items.WHITE_WOOL, 1),
        Entry("yellow_wool_to_wool", Items.YELLOW_WOOL, Items.WHITE_WOOL, 1),
        Entry("lime_wool_to_wool", Items.LIME_WOOL, Items.WHITE_WOOL, 1),
        Entry("pink_wool_to_wool", Items.PINK_WOOL, Items.WHITE_WOOL, 1),
        Entry("gray_wool_to_wool", Items.GRAY_WOOL, Items.WHITE_WOOL, 1),
        Entry("light_gray_wool_to_wool", Items.LIGHT_GRAY_WOOL, Items.WHITE_WOOL, 1),
        Entry("cyan_wool_to_wool", Items.CYAN_WOOL, Items.WHITE_WOOL, 1),
        Entry("purple_wool_to_wool", Items.PURPLE_WOOL, Items.WHITE_WOOL, 1),
        Entry("blue_wool_to_wool", Items.BLUE_WOOL, Items.WHITE_WOOL, 1),
        Entry("brown_wool_to_wool", Items.BROWN_WOOL, Items.WHITE_WOOL, 1),
        Entry("green_wool_to_wool", Items.GREEN_WOOL, Items.WHITE_WOOL, 1),
        Entry("red_wool_to_wool", Items.RED_WOOL, Items.WHITE_WOOL, 1),
        Entry("black_wool_to_wool", Items.BLACK_WOOL, Items.WHITE_WOOL, 1),

        Entry("air_cell_to_empty", AirCell::class.instance(), EmptyCell::class.instance(), 1),

        Entry("bricks_to_brick", Items.BRICKS, Items.BRICK, 4),
        Entry("nether_bricks_to_nether_brick", Items.NETHER_BRICKS, Items.NETHER_BRICK, 4),
        Entry("clay_block_to_clay_ball", Items.CLAY, Items.CLAY_BALL, 4),
        Entry("snow_block_to_snowball", Items.SNOW_BLOCK, Items.SNOWBALL, 4),
        Entry("gunpowder_to_sulfur", Items.GUNPOWDER, SulfurDust::class.instance(), 1),

        Entry("filled_tin_can_to_tin_can", FilledTinCanItem::class.instance(), EmptyTinCanItem::class.instance(), 1),
        Entry("hydrated_tin_dust_to_iodine", HydratedTinDust::class.instance(), Iodine::class.instance(), 1),
        Entry("netherrack_dust_to_small_sulfur", NetherrackDust::class.instance(), SmallSulfurDust::class.instance(), 1)
    )

    fun allEntries(): List<Entry> = entries

    fun generateRecipes(exporter: RecipeExporter) {
        entries.forEach { entry ->
            val id = Identifier.of("ic2_120", "extracting/${entry.name}")
            val recipe = ExtractorRecipe(
                id = id,
                ingredient = Ingredient.ofItems(entry.input),
                output = ItemStack(entry.output, entry.count)
            )
            exporter.accept(id, recipe, null)
        }
    }
}
