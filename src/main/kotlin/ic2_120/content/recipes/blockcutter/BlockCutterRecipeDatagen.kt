package ic2_120.content.recipes.blockcutter

import com.google.gson.JsonObject
import ic2_120.content.item.*
import ic2_120.content.recipes.ModMachineRecipes
import ic2_120.registry.instance
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import java.util.function.Consumer

/**
 * 方块切割机配方数据生成 - 完整版
 *
 * 包含所有100个配方：金属块、石头类、木板类、原木类
 */
object BlockCutterRecipeDatagen {
    data class Entry(
        val name: String,
        val input: Item,
        val inputCount: Int,
        val materialHardness: Float,
        val output: Item,
        val count: Int
    )

    private val entries = listOf(
        // ===== 金属块 → 9 板 =====
        Entry("iron_block_to_plate", Items.IRON_BLOCK, 1, 5.0f, IronPlate::class.instance(), 9),
        Entry("gold_block_to_plate", Items.GOLD_BLOCK, 1, 5.0f, GoldPlate::class.instance(), 9),
        Entry("copper_block_to_plate", Items.COPPER_BLOCK, 1, 3.0f, CopperPlate::class.instance(), 9),
        Entry("tin_block_to_plate", Registries.ITEM.get(Identifier("ic2_120", "tin_block")), 1, 3.0f, TinPlate::class.instance(), 9),
        Entry("bronze_block_to_plate", Registries.ITEM.get(Identifier("ic2_120", "bronze_block")), 1, 5.0f, BronzePlate::class.instance(), 9),
        Entry("lead_block_to_plate", Registries.ITEM.get(Identifier("ic2_120", "lead_block")), 1, 5.0f, LeadPlate::class.instance(), 9),
        Entry("steel_block_to_plate", Registries.ITEM.get(Identifier("ic2_120", "steel_block")), 1, 6.0f, SteelPlate::class.instance(), 9),
        Entry("lapis_block_to_plate", Items.LAPIS_BLOCK, 1, 3.0f, LapisPlate::class.instance(), 9),
        Entry("obsidian_to_plate", Items.OBSIDIAN, 1, 50.0f, ObsidianPlate::class.instance(), 4),

        // ===== 石头类 → 9 台阶 =====
        Entry("stone_to_slab", Items.STONE, 1, 1.5f, Items.STONE_SLAB, 9),
        Entry("cobblestone_to_slab", Items.COBBLESTONE, 1, 2.0f, Items.COBBLESTONE_SLAB, 9),
        Entry("sandstone_to_slab", Items.SANDSTONE, 1, 0.8f, Items.SANDSTONE_SLAB, 9),
        Entry("cut_sandstone_to_slab", Items.CUT_SANDSTONE, 1, 0.8f, Items.CUT_SANDSTONE_SLAB, 9),
        Entry("smooth_sandstone_to_slab", Items.SMOOTH_SANDSTONE, 1, 0.8f, Items.SMOOTH_SANDSTONE_SLAB, 9),
        Entry("red_sandstone_to_slab", Items.RED_SANDSTONE, 1, 0.8f, Items.RED_SANDSTONE_SLAB, 9),
        Entry("cut_red_sandstone_to_slab", Items.CUT_RED_SANDSTONE, 1, 0.8f, Items.CUT_RED_SANDSTONE_SLAB, 9),
        Entry("smooth_red_sandstone_to_slab", Items.SMOOTH_RED_SANDSTONE, 1, 0.8f, Items.SMOOTH_RED_SANDSTONE_SLAB, 9),
        Entry("stone_bricks_to_slab", Items.STONE_BRICKS, 1, 1.5f, Items.STONE_BRICK_SLAB, 9),
        Entry("bricks_to_slab", Items.BRICKS, 1, 2.0f, Items.BRICK_SLAB, 9),
        Entry("nether_bricks_to_slab", Items.NETHER_BRICKS, 1, 2.0f, Items.NETHER_BRICK_SLAB, 9),
        Entry("quartz_block_to_slab", Items.QUARTZ_BLOCK, 1, 0.8f, Items.QUARTZ_SLAB, 9),
        Entry("smooth_quartz_to_slab", Items.SMOOTH_QUARTZ, 1, 0.8f, Items.SMOOTH_QUARTZ_SLAB, 9),
        Entry("red_nether_bricks_to_slab", Items.RED_NETHER_BRICKS, 1, 2.0f, Items.RED_NETHER_BRICK_SLAB, 9),
        Entry("purpur_block_to_slab", Items.PURPUR_BLOCK, 1, 1.5f, Items.PURPUR_SLAB, 9),
        Entry("prismarine_to_slab", Items.PRISMARINE, 1, 1.5f, Items.PRISMARINE_SLAB, 9),
        Entry("dark_prismarine_to_slab", Items.DARK_PRISMARINE, 1, 1.5f, Items.DARK_PRISMARINE_SLAB, 9),
        Entry("prismarine_bricks_to_slab", Items.PRISMARINE_BRICKS, 1, 1.5f, Items.PRISMARINE_BRICK_SLAB, 9),
        Entry("granite_to_slab", Items.GRANITE, 1, 1.5f, Items.GRANITE_SLAB, 9),
        Entry("polished_granite_to_slab", Items.POLISHED_GRANITE, 1, 1.5f, Items.POLISHED_GRANITE_SLAB, 9),
        Entry("diorite_to_slab", Items.DIORITE, 1, 1.5f, Items.DIORITE_SLAB, 9),
        Entry("polished_diorite_to_slab", Items.POLISHED_DIORITE, 1, 1.5f, Items.POLISHED_DIORITE_SLAB, 9),
        Entry("andesite_to_slab", Items.ANDESITE, 1, 1.5f, Items.ANDESITE_SLAB, 9),
        Entry("polished_andesite_to_slab", Items.POLISHED_ANDESITE, 1, 1.5f, Items.POLISHED_ANDESITE_SLAB, 9),
        Entry("blackstone_to_slab", Items.BLACKSTONE, 1, 1.5f, Items.BLACKSTONE_SLAB, 9),
        Entry("polished_blackstone_to_slab", Items.POLISHED_BLACKSTONE, 1, 1.5f, Items.POLISHED_BLACKSTONE_SLAB, 9),
        Entry("polished_blackstone_bricks_to_slab", Items.POLISHED_BLACKSTONE_BRICKS, 1, 1.5f, Items.POLISHED_BLACKSTONE_BRICK_SLAB, 9),
        Entry("deepslate_to_slab", Items.DEEPSLATE, 1, 3.0f, Registries.ITEM.get(Identifier("minecraft", "deepslate_slab")), 9),
        Entry("polished_deepslate_to_slab", Items.POLISHED_DEEPSLATE, 1, 3.0f, Registries.ITEM.get(Identifier("minecraft", "polished_deepslate_slab")), 9),
        Entry("deepslate_bricks_to_slab", Items.DEEPSLATE_BRICKS, 1, 3.0f, Registries.ITEM.get(Identifier("minecraft", "deepslate_brick_slab")), 9),
        Entry("deepslate_tiles_to_slab", Items.DEEPSLATE_TILES, 1, 3.0f, Registries.ITEM.get(Identifier("minecraft", "deepslate_tile_slab")), 9),
        Entry("mud_bricks_to_slab", Items.MUD_BRICKS, 1, 1.5f, Items.MUD_BRICK_SLAB, 9),
        Entry("oxidized_copper_to_slab", Items.OXIDIZED_COPPER, 1, 3.0f, Items.OXIDIZED_CUT_COPPER_SLAB, 9),
        Entry("weathered_copper_to_slab", Items.WEATHERED_COPPER, 1, 3.0f, Items.WEATHERED_CUT_COPPER_SLAB, 9),
        Entry("exposed_copper_to_slab", Items.EXPOSED_COPPER, 1, 3.0f, Items.EXPOSED_CUT_COPPER_SLAB, 9),
        Entry("cut_copper_to_slab", Items.CUT_COPPER, 1, 3.0f, Items.CUT_COPPER_SLAB, 9),
        Entry("waxed_copper_to_slab", Items.WAXED_COPPER_BLOCK, 1, 3.0f, Items.WAXED_CUT_COPPER_SLAB, 9),
        Entry("waxed_weathered_copper_to_slab", Items.WAXED_WEATHERED_COPPER, 1, 3.0f, Items.WAXED_WEATHERED_CUT_COPPER_SLAB, 9),
        Entry("waxed_exposed_copper_to_slab", Items.WAXED_EXPOSED_COPPER, 1, 3.0f, Items.WAXED_EXPOSED_CUT_COPPER_SLAB, 9),
        Entry("waxed_oxidized_copper_to_slab", Items.WAXED_OXIDIZED_COPPER, 1, 3.0f, Items.WAXED_OXIDIZED_CUT_COPPER_SLAB, 9),

        // ===== 木板类 → 9 台阶 =====
        Entry("oak_planks_to_slab", Items.OAK_PLANKS, 1, 2.0f, Items.OAK_SLAB, 9),
        Entry("spruce_planks_to_slab", Items.SPRUCE_PLANKS, 1, 2.0f, Items.SPRUCE_SLAB, 9),
        Entry("birch_planks_to_slab", Items.BIRCH_PLANKS, 1, 2.0f, Items.BIRCH_SLAB, 9),
        Entry("jungle_planks_to_slab", Items.JUNGLE_PLANKS, 1, 2.0f, Items.JUNGLE_SLAB, 9),
        Entry("acacia_planks_to_slab", Items.ACACIA_PLANKS, 1, 2.0f, Items.ACACIA_SLAB, 9),
        Entry("dark_oak_planks_to_slab", Items.DARK_OAK_PLANKS, 1, 2.0f, Items.DARK_OAK_SLAB, 9),
        Entry("mangrove_planks_to_slab", Items.MANGROVE_PLANKS, 1, 2.0f, Items.MANGROVE_SLAB, 9),
        Entry("cherry_planks_to_slab", Items.CHERRY_PLANKS, 1, 2.0f, Items.CHERRY_SLAB, 9),
        Entry("bamboo_planks_to_slab", Items.BAMBOO_PLANKS, 1, 2.0f, Items.BAMBOO_SLAB, 9),
        Entry("crimson_planks_to_slab", Items.CRIMSON_PLANKS, 1, 2.0f, Items.CRIMSON_SLAB, 9),
        Entry("warped_planks_to_slab", Items.WARPED_PLANKS, 1, 2.0f, Items.WARPED_SLAB, 9),
        Entry("rubber_planks_to_slab", Registries.ITEM.get(Identifier("ic2_120", "rubber_planks")), 1, 2.0f, Registries.ITEM.get(Identifier("ic2_120", "rubber_slab")), 9),

        // ===== 木板 → 6 木棍（双配方，inputCount=2） =====
        Entry("oak_planks_to_sticks", Items.OAK_PLANKS, 2, 2.0f, Items.STICK, 6),
        Entry("spruce_planks_to_sticks", Items.SPRUCE_PLANKS, 2, 2.0f, Items.STICK, 6),
        Entry("birch_planks_to_sticks", Items.BIRCH_PLANKS, 2, 2.0f, Items.STICK, 6),
        Entry("jungle_planks_to_sticks", Items.JUNGLE_PLANKS, 2, 2.0f, Items.STICK, 6),
        Entry("acacia_planks_to_sticks", Items.ACACIA_PLANKS, 2, 2.0f, Items.STICK, 6),
        Entry("dark_oak_planks_to_sticks", Items.DARK_OAK_PLANKS, 2, 2.0f, Items.STICK, 6),
        Entry("mangrove_planks_to_sticks", Items.MANGROVE_PLANKS, 2, 2.0f, Items.STICK, 6),
        Entry("cherry_planks_to_sticks", Items.CHERRY_PLANKS, 2, 2.0f, Items.STICK, 6),
        Entry("bamboo_planks_to_sticks", Items.BAMBOO_PLANKS, 2, 2.0f, Items.STICK, 6),
        Entry("crimson_planks_to_sticks", Items.CRIMSON_PLANKS, 2, 2.0f, Items.STICK, 6),
        Entry("warped_planks_to_sticks", Items.WARPED_PLANKS, 2, 2.0f, Items.STICK, 6),
        Entry("rubber_planks_to_sticks", Registries.ITEM.get(Identifier("ic2_120", "rubber_planks")), 2, 2.0f, Items.STICK, 6),

        // ===== 原木 → 6 木板（增产 50%） =====
        Entry("oak_log_to_planks", Items.OAK_LOG, 1, 2.0f, Items.OAK_PLANKS, 6),
        Entry("spruce_log_to_planks", Items.SPRUCE_LOG, 1, 2.0f, Items.SPRUCE_PLANKS, 6),
        Entry("birch_log_to_planks", Items.BIRCH_LOG, 1, 2.0f, Items.BIRCH_PLANKS, 6),
        Entry("jungle_log_to_planks", Items.JUNGLE_LOG, 1, 2.0f, Items.JUNGLE_PLANKS, 6),
        Entry("acacia_log_to_planks", Items.ACACIA_LOG, 1, 2.0f, Items.ACACIA_PLANKS, 6),
        Entry("dark_oak_log_to_planks", Items.DARK_OAK_LOG, 1, 2.0f, Items.DARK_OAK_PLANKS, 6),
        Entry("mangrove_log_to_planks", Items.MANGROVE_LOG, 1, 2.0f, Items.MANGROVE_PLANKS, 6),
        Entry("cherry_log_to_planks", Items.CHERRY_LOG, 1, 2.0f, Items.CHERRY_PLANKS, 6),
        Entry("crimson_stem_to_planks", Items.CRIMSON_STEM, 1, 2.0f, Items.CRIMSON_PLANKS, 6),
        Entry("warped_stem_to_planks", Items.WARPED_STEM, 1, 2.0f, Items.WARPED_PLANKS, 6),
        Entry("rubber_log_to_planks", Registries.ITEM.get(Identifier("ic2_120", "rubber_log")), 1, 2.0f, Registries.ITEM.get(Identifier("ic2_120", "rubber_planks")), 6),
        Entry("stripped_oak_log_to_planks", Items.STRIPPED_OAK_LOG, 1, 2.0f, Items.OAK_PLANKS, 6),
        Entry("stripped_spruce_log_to_planks", Items.STRIPPED_SPRUCE_LOG, 1, 2.0f, Items.SPRUCE_PLANKS, 6),
        Entry("stripped_birch_log_to_planks", Items.STRIPPED_BIRCH_LOG, 1, 2.0f, Items.BIRCH_PLANKS, 6),
        Entry("stripped_jungle_log_to_planks", Items.STRIPPED_JUNGLE_LOG, 1, 2.0f, Items.JUNGLE_PLANKS, 6),
        Entry("stripped_acacia_log_to_planks", Items.STRIPPED_ACACIA_LOG, 1, 2.0f, Items.ACACIA_PLANKS, 6),
        Entry("stripped_dark_oak_log_to_planks", Items.STRIPPED_DARK_OAK_LOG, 1, 2.0f, Items.DARK_OAK_PLANKS, 6),
        Entry("stripped_mangrove_log_to_planks", Items.STRIPPED_MANGROVE_LOG, 1, 2.0f, Items.MANGROVE_PLANKS, 6),
        Entry("stripped_cherry_log_to_planks", Items.STRIPPED_CHERRY_LOG, 1, 2.0f, Items.CHERRY_PLANKS, 6),
        Entry("stripped_crimson_stem_to_planks", Items.STRIPPED_CRIMSON_STEM, 1, 2.0f, Items.CRIMSON_PLANKS, 6),
        Entry("stripped_warped_stem_to_planks", Items.STRIPPED_WARPED_STEM, 1, 2.0f, Items.WARPED_PLANKS, 6),
        Entry("stripped_rubber_log_to_planks", Registries.ITEM.get(Identifier("ic2_120", "stripped_rubber_log")), 1, 2.0f, Registries.ITEM.get(Identifier("ic2_120", "rubber_planks")), 6),
        Entry("bamboo_block_to_planks", Items.BAMBOO_BLOCK, 1, 2.0f, Items.BAMBOO_PLANKS, 6),
        Entry("stripped_bamboo_block_to_planks", Items.STRIPPED_BAMBOO_BLOCK, 1, 2.0f, Items.BAMBOO_PLANKS, 6)
    )

    fun allEntries(): List<Entry> = entries

    fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
        entries.forEach { entry ->
            BlockCutterRecipeJsonProvider(
                recipeId = Identifier("ic2_120", "cutting/${entry.name}"),
                inputItem = entry.input,
                inputCount = entry.inputCount,
                materialHardness = entry.materialHardness,
                outputItem = entry.output,
                outputCount = entry.count
            ).also(exporter::accept)
        }
    }

    private class BlockCutterRecipeJsonProvider(
        private val recipeId: Identifier,
        private val inputItem: Item,
        private val inputCount: Int,
        private val materialHardness: Float,
        private val outputItem: Item,
        private val outputCount: Int
    ) : RecipeJsonProvider {
        override fun serialize(json: JsonObject) {
            json.addProperty("type", "${ModMachineRecipes.BLOCK_CUTTER_TYPE}")

            // 输入成分
            val ingredient = JsonObject()
            ingredient.addProperty("item", Registries.ITEM.getId(inputItem).toString())
            json.add("ingredient", ingredient)

            // 输入数量
            json.addProperty("input_count", inputCount)

            // 材料硬度
            json.addProperty("material_hardness", materialHardness)

            // 输出
            val result = JsonObject()
            result.addProperty("item", Registries.ITEM.getId(outputItem).toString())
            result.addProperty("count", outputCount)
            json.add("result", result)
        }

        override fun getSerializer() = ModMachineRecipes.BLOCK_CUTTER_SERIALIZER

        override fun getRecipeId(): Identifier = recipeId

        override fun toAdvancementJson(): JsonObject? = null

        override fun getAdvancementId(): Identifier? = null
    }
}
