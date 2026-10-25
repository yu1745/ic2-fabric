package ic2_120.content.recipes.extractor

import ic2_120.compat.accept

import com.google.gson.JsonObject
import ic2_120.content.block.*
import ic2_120.content.item.*
import ic2_120.content.recipes.IngredientInput
import ic2_120.content.recipes.ModMachineRecipes
import ic2_120.registry.instance
import ic2_120.registry.item
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import java.util.function.Consumer

object ExtractorRecipeDatagen {
    data class Entry(
        val name: String,
        val input: IngredientInput,
        val output: Item,
        val count: Int
    )

    private val entries = listOf(
        // 仅保留白名单合法输入
        Entry("resin_to_rubber", IngredientInput.item(Resin::class.instance()), RubberItem::class.instance(), 3),
        Entry("rubber_log_to_rubber", IngredientInput.item(RubberLogBlock::class.item()), RubberItem::class.instance(), 1),
        Entry("rubber_sapling_to_rubber", IngredientInput.item(RubberSaplingBlock::class.item()), RubberItem::class.instance(), 1),

        Entry("orange_wool_to_wool", IngredientInput.item(Items.WOOL.pick(net.minecraft.world.item.DyeColor.ORANGE)), Items.WOOL.pick(net.minecraft.world.item.DyeColor.WHITE), 1),
        Entry("magenta_wool_to_wool", IngredientInput.item(Items.WOOL.pick(net.minecraft.world.item.DyeColor.MAGENTA)), Items.WOOL.pick(net.minecraft.world.item.DyeColor.WHITE), 1),
        Entry("light_blue_wool_to_wool", IngredientInput.item(Items.WOOL.pick(net.minecraft.world.item.DyeColor.LIGHT_BLUE)), Items.WOOL.pick(net.minecraft.world.item.DyeColor.WHITE), 1),
        Entry("yellow_wool_to_wool", IngredientInput.item(Items.WOOL.pick(net.minecraft.world.item.DyeColor.YELLOW)), Items.WOOL.pick(net.minecraft.world.item.DyeColor.WHITE), 1),
        Entry("lime_wool_to_wool", IngredientInput.item(Items.WOOL.pick(net.minecraft.world.item.DyeColor.LIME)), Items.WOOL.pick(net.minecraft.world.item.DyeColor.WHITE), 1),
        Entry("pink_wool_to_wool", IngredientInput.item(Items.WOOL.pick(net.minecraft.world.item.DyeColor.PINK)), Items.WOOL.pick(net.minecraft.world.item.DyeColor.WHITE), 1),
        Entry("gray_wool_to_wool", IngredientInput.item(Items.WOOL.pick(net.minecraft.world.item.DyeColor.GRAY)), Items.WOOL.pick(net.minecraft.world.item.DyeColor.WHITE), 1),
        Entry("light_gray_wool_to_wool", IngredientInput.item(Items.WOOL.pick(net.minecraft.world.item.DyeColor.LIGHT_GRAY)), Items.WOOL.pick(net.minecraft.world.item.DyeColor.WHITE), 1),
        Entry("cyan_wool_to_wool", IngredientInput.item(Items.WOOL.pick(net.minecraft.world.item.DyeColor.CYAN)), Items.WOOL.pick(net.minecraft.world.item.DyeColor.WHITE), 1),
        Entry("purple_wool_to_wool", IngredientInput.item(Items.WOOL.pick(net.minecraft.world.item.DyeColor.PURPLE)), Items.WOOL.pick(net.minecraft.world.item.DyeColor.WHITE), 1),
        Entry("blue_wool_to_wool", IngredientInput.item(Items.WOOL.pick(net.minecraft.world.item.DyeColor.BLUE)), Items.WOOL.pick(net.minecraft.world.item.DyeColor.WHITE), 1),
        Entry("brown_wool_to_wool", IngredientInput.item(Items.WOOL.pick(net.minecraft.world.item.DyeColor.BROWN)), Items.WOOL.pick(net.minecraft.world.item.DyeColor.WHITE), 1),
        Entry("green_wool_to_wool", IngredientInput.item(Items.WOOL.pick(net.minecraft.world.item.DyeColor.GREEN)), Items.WOOL.pick(net.minecraft.world.item.DyeColor.WHITE), 1),
        Entry("red_wool_to_wool", IngredientInput.item(Items.WOOL.pick(net.minecraft.world.item.DyeColor.RED)), Items.WOOL.pick(net.minecraft.world.item.DyeColor.WHITE), 1),
        Entry("black_wool_to_wool", IngredientInput.item(Items.WOOL.pick(net.minecraft.world.item.DyeColor.BLACK)), Items.WOOL.pick(net.minecraft.world.item.DyeColor.WHITE), 1),

        Entry("air_cell_to_empty", IngredientInput.item(AirCell::class.instance()), EmptyCell::class.instance(), 1),

        Entry("bricks_to_brick", IngredientInput.item(Items.BRICKS), Items.BRICK, 4),
        Entry("nether_bricks_to_nether_brick", IngredientInput.item(Items.NETHER_BRICKS), Items.NETHER_BRICK, 4),
        Entry("clay_block_to_clay_ball", IngredientInput.item(Items.CLAY), Items.CLAY_BALL, 4),
        Entry("snow_block_to_snowball", IngredientInput.item(Items.SNOW_BLOCK), Items.SNOWBALL, 4),
        Entry("gunpowder_to_sulfur", IngredientInput.item(Items.GUNPOWDER), SulfurDust::class.instance(), 1),

        Entry("filled_tin_can_to_tin_can", IngredientInput.item(FilledTinCanItem::class.instance()), EmptyTinCanItem::class.instance(), 1),
        Entry("hydrated_tin_dust_to_iodine", IngredientInput.item(HydratedTinDust::class.instance()), Iodine::class.instance(), 1),
        Entry("netherrack_dust_to_small_sulfur", IngredientInput.item(NetherrackDust::class.instance()), SmallSulfurDust::class.instance(), 1)
    )

    fun allEntries(): List<Entry> = entries

    fun generateRecipes(exporter: RecipeOutput) {
        entries.forEach { entry ->
            val recipeId = Identifier.fromNamespaceAndPath("ic2_120", "extracting/${entry.name}")
            val recipe = ExtractorRecipe(
                recipeId,
                entry.input.toIngredient(),
                ItemStack(entry.output, entry.count)
            )
            exporter.accept(recipeId, recipe, null)
        }
    }
}
