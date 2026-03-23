package ic2_120.content.recipes.extractor

import com.google.gson.JsonObject
import ic2_120.content.block.*
import ic2_120.content.item.*
import ic2_120.content.item.energy.EnergyCrystalItem
import ic2_120.content.recipes.ModMachineRecipes
import ic2_120.registry.instance
import ic2_120.registry.item
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import java.util.function.Consumer

object ExtractorRecipeDatagen {
    data class Entry(
        val name: String,
        val input: Item,
        val output: Item,
        val count: Int
    )

    private val entries = listOf(
        // 基础材料
        Entry("gravel_to_flint", Items.GRAVEL, Items.FLINT, 1),
        Entry("cobblestone_to_sand", Items.COBBLESTONE, Items.SAND, 1),
        Entry("stone_to_cobblestone", Items.STONE, Items.COBBLESTONE, 1),
        Entry("granite_to_cobblestone", Items.GRANITE, Items.COBBLESTONE, 1),
        Entry("diorite_to_cobblestone", Items.DIORITE, Items.COBBLESTONE, 1),
        Entry("andesite_to_cobblestone", Items.ANDESITE, Items.COBBLESTONE, 1),

        // 矿石 -> 更多产物
        Entry("redstone_ore_to_redstone", Items.REDSTONE_ORE, Items.REDSTONE, 6),
        Entry("deepslate_redstone_ore", Items.DEEPSLATE_REDSTONE_ORE, Items.REDSTONE, 6),
        Entry("diamond_ore", Items.DIAMOND_ORE, Items.DIAMOND, 2),
        Entry("deepslate_diamond_ore", Items.DEEPSLATE_DIAMOND_ORE, Items.DIAMOND, 2),
        Entry("lapis_ore", Items.LAPIS_ORE, Items.LAPIS_LAZULI, 6),
        Entry("deepslate_lapis_ore", Items.DEEPSLATE_LAPIS_ORE, Items.LAPIS_LAZULI, 6),

        // 粉碎矿石
        Entry("iron_ore_to_crushed", Items.IRON_ORE, CrushedIron::class.instance(), 2),
        Entry("gold_ore_to_crushed", Items.GOLD_ORE, CrushedGold::class.instance(), 2),
        Entry("copper_ore_to_crushed", Items.COPPER_ORE, CrushedCopper::class.instance(), 2),
        Entry("deepslate_iron_ore", Items.DEEPSLATE_IRON_ORE, CrushedIron::class.instance(), 2),
        Entry("deepslate_gold_ore", Items.DEEPSLATE_GOLD_ORE, CrushedGold::class.instance(), 2),
        Entry("deepslate_copper_ore", Items.DEEPSLATE_COPPER_ORE, CrushedCopper::class.instance(), 2),

        // 锡矿石
        Entry("tin_ore_to_crushed", TinOreBlock::class.item(), CrushedTin::class.instance(), 2),
        Entry("deepslate_tin_ore", DeepslateTinOreBlock::class.item(), CrushedTin::class.instance(), 2),

        // 矿锭 -> 粉末
        Entry("iron_ingot_to_dust", Items.IRON_INGOT, IronDust::class.instance(), 1),
        Entry("gold_ingot_to_dust", Items.GOLD_INGOT, GoldDust::class.instance(), 1),
        Entry("copper_ingot_to_dust", Items.COPPER_INGOT, CopperDust::class.instance(), 1),
        Entry("tin_ingot_to_dust", TinIngot::class.instance(), TinDust::class.instance(), 1),
        Entry("bronze_ingot_to_dust", BronzeIngot::class.instance(), BronzeDust::class.instance(), 1),
        Entry("lead_ingot_to_dust", LeadIngot::class.instance(), LeadDust::class.instance(), 1),
        Entry("silver_ingot_to_dust", SilverIngot::class.instance(), SilverDust::class.instance(), 1),
        Entry("steel_ingot_to_dust", SteelIngot::class.instance(), IronDust::class.instance(), 1),

        // 板 -> 粉末
        Entry("iron_plate_to_dust", IronPlate::class.instance(), IronDust::class.instance(), 1),
        Entry("gold_plate_to_dust", GoldPlate::class.instance(), GoldDust::class.instance(), 1),
        Entry("copper_plate_to_dust", CopperPlate::class.instance(), CopperDust::class.instance(), 1),
        Entry("tin_plate_to_dust", TinPlate::class.instance(), TinDust::class.instance(), 1),
        Entry("bronze_plate_to_dust", BronzePlate::class.instance(), BronzeDust::class.instance(), 1),
        Entry("lead_plate_to_dust", LeadPlate::class.instance(), LeadDust::class.instance(), 1),

        // 矿石块 -> 9 粉末
        Entry("iron_block_to_dust", Items.IRON_BLOCK, IronDust::class.instance(), 9),
        Entry("gold_block_to_dust", Items.GOLD_BLOCK, GoldDust::class.instance(), 9),
        Entry("copper_block_to_dust", Items.COPPER_BLOCK, CopperDust::class.instance(), 9),
        Entry("tin_block_to_dust", TinBlock::class.item(), TinDust::class.instance(), 9),
        Entry("bronze_block_to_dust", BronzeBlock::class.item(), BronzeDust::class.instance(), 9),
        Entry("lead_block_to_dust", LeadBlock::class.item(), LeadDust::class.instance(), 9),
        Entry("silver_block_to_dust", SilverBlock::class.item(), SilverDust::class.instance(), 9),

        // 石英加工品
        Entry("quartz_block", Items.QUARTZ_BLOCK, Items.QUARTZ, 4),
        Entry("chiseled_quartz_block", Items.CHISELED_QUARTZ_BLOCK, Items.QUARTZ, 4),
        Entry("quartz_pillar", Items.QUARTZ_PILLAR, Items.QUARTZ, 4),
        Entry("smooth_quartz", Items.SMOOTH_QUARTZ, Items.QUARTZ, 4),
        Entry("quartz_slab", Items.QUARTZ_SLAB, Items.QUARTZ, 2),
        Entry("smooth_quartz_slab", Items.SMOOTH_QUARTZ_SLAB, Items.QUARTZ, 2),
        Entry("nether_quartz_ore", Items.NETHER_QUARTZ_ORE, Items.QUARTZ, 2),

        // 沙石
        Entry("sandstone_to_sand", Items.SANDSTONE, Items.SAND, 1),
        Entry("cut_sandstone", Items.CUT_SANDSTONE, Items.SAND, 1),
        Entry("chiseled_sandstone", Items.CHISELED_SANDSTONE, Items.SAND, 1),
        Entry("smooth_sandstone", Items.SMOOTH_SANDSTONE, Items.SAND, 1),
        Entry("red_sandstone", Items.RED_SANDSTONE, Items.RED_SAND, 1),
        Entry("cut_red_sandstone", Items.CUT_RED_SANDSTONE, Items.RED_SAND, 1),
        Entry("chiseled_red_sandstone", Items.CHISELED_RED_SANDSTONE, Items.RED_SAND, 1),
        Entry("smooth_red_sandstone", Items.SMOOTH_RED_SANDSTONE, Items.RED_SAND, 1),

        // 羊毛 -> 线
        Entry("white_wool_to_string", Items.WHITE_WOOL, Items.STRING, 2),
        Entry("orange_wool_to_string", Items.ORANGE_WOOL, Items.STRING, 2),
        Entry("magenta_wool_to_string", Items.MAGENTA_WOOL, Items.STRING, 2),
        Entry("light_blue_wool_to_string", Items.LIGHT_BLUE_WOOL, Items.STRING, 2),
        Entry("yellow_wool_to_string", Items.YELLOW_WOOL, Items.STRING, 2),
        Entry("lime_wool_to_string", Items.LIME_WOOL, Items.STRING, 2),
        Entry("pink_wool_to_string", Items.PINK_WOOL, Items.STRING, 2),
        Entry("gray_wool_to_string", Items.GRAY_WOOL, Items.STRING, 2),
        Entry("light_gray_wool_to_string", Items.LIGHT_GRAY_WOOL, Items.STRING, 2),
        Entry("cyan_wool_to_string", Items.CYAN_WOOL, Items.STRING, 2),
        Entry("purple_wool_to_string", Items.PURPLE_WOOL, Items.STRING, 2),
        Entry("blue_wool_to_string", Items.BLUE_WOOL, Items.STRING, 2),
        Entry("brown_wool_to_string", Items.BROWN_WOOL, Items.STRING, 2),
        Entry("green_wool_to_string", Items.GREEN_WOOL, Items.STRING, 2),
        Entry("red_wool_to_string", Items.RED_WOOL, Items.STRING, 2),
        Entry("black_wool_to_string", Items.BLACK_WOOL, Items.STRING, 2),

        // 其他
        Entry("clay_to_clay_dust", Items.CLAY, ClayDust::class.instance(), 2),
        Entry("spider_eye_to_grin_powder", Items.SPIDER_EYE, GrinPowder::class.instance(), 2),
        Entry("poisonous_potato", Items.POISONOUS_POTATO, GrinPowder::class.instance(), 1),
        Entry("ice_to_snowball", Items.ICE, Items.SNOWBALL, 1),
        Entry("packed_ice_to_snowball", Items.PACKED_ICE, Items.SNOWBALL, 1),
        Entry("ender_pearl_to_dust", Items.ENDER_PEARL, EnderPearlDust::class.instance(), 1),
        Entry("wheat_to_flour", Items.WHEAT, Flour::class.instance(), 1),
        Entry("blaze_rod_to_powder", Items.BLAZE_ROD, Items.BLAZE_POWDER, 5),
        Entry("bone_to_bone_meal", Items.BONE, Items.BONE_MEAL, 5),
        Entry("glowstone_to_dust", Items.GLOWSTONE, Items.GLOWSTONE_DUST, 4),
        Entry("plant_ball_to_dirt", PlantBall::class.instance(), Items.DIRT, 1),
        Entry("bricks_to_brick", Items.BRICKS, Items.BRICK, 4),
        Entry("nether_bricks", Items.NETHER_BRICKS, Items.NETHER_BRICK, 4),
        Entry("snow_block_to_snowball", Items.SNOW_BLOCK, Items.SNOWBALL, 4),
        Entry("gunpowder_to_sulfur", Items.GUNPOWDER, SulfurDust::class.instance(), 1),

        // 咖啡
        Entry("coffee_beans_to_powder", CoffeeBeans::class.instance(), CoffeePowder::class.instance(), 3),

        // 橡胶类
        Entry("resin_to_rubber", Resin::class.instance(), RubberItem::class.instance(), 3),
        Entry("rubber_log_to_rubber", RubberLogBlock::class.item(), RubberItem::class.instance(), 1),
        Entry("stripped_rubber_log", StrippedRubberLogBlock::class.item(), RubberItem::class.instance(), 1),
        Entry("rubber_wood_to_rubber", RubberWood::class.item(), RubberItem::class.instance(), 1),
        Entry("stripped_rubber_wood", StrippedRubberWoodBlock::class.item(), RubberItem::class.instance(), 1),
        Entry("rubber_sapling", RubberSaplingBlock::class.item(), RubberItem::class.instance(), 1),

        // 能量
        Entry("energy_crystal_to_dust", EnergyCrystalItem::class.instance(), EnergiumDust::class.instance(), 9),

        // 单元类
        Entry("air_cell_to_empty", AirCell::class.instance(), EmptyCell::class.instance(), 1),
        Entry("bio_cell_to_biofuel", BioCell::class.instance(), BiofuelCell::class.instance(), 1),
        Entry("compressed_hydrated_coal", CompressedHydratedCoal::class.instance(), CoalFuelDust::class.instance(), 1),

        // 其他
        Entry("filled_tin_can_to_tin_can", FilledTinCanItem::class.instance(), EmptyTinCanItem::class.instance(), 1),
        Entry("hydrated_tin_dust_to_iodine", HydratedTinDust::class.instance(), Iodine::class.instance(), 1),
        Entry("netherrack_dust_to_small_sulfur", NetherrackDust::class.instance(), SmallSulfurDust::class.instance(), 1)
    )

    fun allEntries(): List<Entry> = entries

    fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
        entries.forEach { entry ->
            ExtractorRecipeJsonProvider(
                recipeId = Identifier("ic2_120", "extracting/${entry.name}"),
                inputItem = entry.input,
                outputItem = entry.output,
                outputCount = entry.count
            ).also(exporter::accept)
        }
    }

    private class ExtractorRecipeJsonProvider(
        private val recipeId: Identifier,
        private val inputItem: Item,
        private val outputItem: Item,
        private val outputCount: Int
    ) : RecipeJsonProvider {
        override fun serialize(json: JsonObject) {
            json.addProperty("type", "${ModMachineRecipes.EXTRACTOR_TYPE}")
            val ingredient = JsonObject()
            ingredient.addProperty("item", Registries.ITEM.getId(inputItem).toString())
            json.add("ingredient", ingredient)

            val result = JsonObject()
            result.addProperty("item", Registries.ITEM.getId(outputItem).toString())
            result.addProperty("count", outputCount)
            json.add("result", result)
        }

        override fun getSerializer() = ModMachineRecipes.EXTRACTOR_SERIALIZER

        override fun getRecipeId(): Identifier = recipeId

        override fun toAdvancementJson(): JsonObject? = null

        override fun getAdvancementId(): Identifier? = null
    }
}
