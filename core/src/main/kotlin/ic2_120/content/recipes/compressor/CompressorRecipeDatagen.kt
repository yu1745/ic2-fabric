package ic2_120.content.recipes.compressor

import com.google.gson.JsonObject
import ic2_120.content.block.*
import ic2_120.content.item.*
import ic2_120.content.item.energy.EnergyCrystalItem
import ic2_120.content.recipes.IngredientInput
import ic2_120.content.recipes.ModMachineRecipes
import ic2_120.content.recipes.ModTags
import ic2_120.registry.instance
import ic2_120.registry.item
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

object CompressorRecipeDatagen {
    data class Entry(
        val name: String,
        val input: IngredientInput,
        val inputCount: Int,
        val output: Item,
        val count: Int,
        val containerReturn: ItemStack = ItemStack.EMPTY
    )

    private val entries = listOf(
        // ===== 9 金属锭 -> 1 金属块（用 tag 跨 mod 兼容） =====
        Entry("iron_ingot_to_block", IngredientInput.tag(ModTags.Compat.Items.INGOTS_IRON, Items.IRON_INGOT), 9, Items.IRON_BLOCK, 1),
        Entry("gold_ingot_to_block", IngredientInput.tag(ModTags.Compat.Items.INGOTS_GOLD, Items.GOLD_INGOT), 9, Items.GOLD_BLOCK, 1),
        Entry("copper_ingot_to_block", IngredientInput.tag(ModTags.Compat.Items.INGOTS_COPPER, Items.COPPER_INGOT), 9, Items.COPPER_BLOCK, 1),
        Entry("tin_ingot_to_block", IngredientInput.tag(ModTags.Compat.Items.INGOTS_TIN, TinIngot::class.instance()), 9, TinBlock::class.item(), 1),
        Entry("bronze_ingot_to_block", IngredientInput.tag(ModTags.Compat.Items.INGOTS_BRONZE, BronzeIngot::class.instance()), 9, BronzeBlock::class.item(), 1),
        Entry("steel_ingot_to_block", IngredientInput.tag(ModTags.Compat.Items.INGOTS_STEEL, SteelIngot::class.instance()), 9, SteelBlock::class.item(), 1),
        Entry("lead_ingot_to_block", IngredientInput.tag(ModTags.Compat.Items.INGOTS_LEAD, LeadIngot::class.instance()), 9, LeadBlock::class.item(), 1),
        Entry("silver_ingot_to_block", IngredientInput.tag(ModTags.Compat.Items.INGOTS_SILVER, SilverIngot::class.instance()), 9, SilverBlock::class.item(), 1),
        Entry("lapis_to_block", IngredientInput.tag(ModTags.Compat.Items.GEMS_LAPIS, Items.LAPIS_LAZULI), 9, Items.LAPIS_BLOCK, 1),
        Entry("redstone_to_block", IngredientInput.tag(ModTags.Compat.Items.DUSTS_REDSTONE, Items.REDSTONE), 9, Items.REDSTONE_BLOCK, 1),

        // ===== 4 沙子/红沙 -> 1 沙石/红沙石 =====
        Entry("sand_to_sandstone", IngredientInput.item(Items.SAND), 4, Items.SANDSTONE, 1),
        Entry("red_sand_to_red_sandstone", IngredientInput.item(Items.RED_SAND), 4, Items.RED_SANDSTONE, 1),

        // ===== 4 粘土球 -> 1 粘土块 =====
        Entry("clay_ball_to_clay_block", IngredientInput.item(Items.CLAY_BALL), 4, Items.CLAY, 1),

        // ===== 4 砖 -> 1 砖块 =====
        Entry("brick_to_bricks", IngredientInput.item(Items.BRICK), 4, Items.BRICKS, 1),

        // ===== 4 下界砖 -> 1 下界砖块 =====
        Entry("nether_brick_to_nether_bricks", IngredientInput.item(Items.NETHER_BRICK), 4, Items.NETHER_BRICKS, 1),

        // ===== 4 雪球 -> 1 雪块 =====
        Entry("snowball_to_snow_block", IngredientInput.item(Items.SNOWBALL), 4, Items.SNOW_BLOCK, 1),

        // ===== 4 萤石粉 -> 1 萤石 =====
        Entry("glowstone_dust_to_glowstone", IngredientInput.item(Items.GLOWSTONE_DUST), 4, Items.GLOWSTONE, 1),

        // ===== 5 烈焰粉 -> 1 烈焰棒 =====
        Entry("blaze_powder_to_rod", IngredientInput.item(Items.BLAZE_POWDER), 5, Items.BLAZE_ROD, 1),

        // ===== 雪/冰转化 =====
        Entry("snow_block_to_ice", IngredientInput.item(Items.SNOW_BLOCK), 1, Items.ICE, 1),
        Entry("ice_to_packed_ice", IngredientInput.item(Items.ICE), 2, Items.PACKED_ICE, 1),

        // ===== 黑曜石粉 -> 黑曜石板 =====
        Entry("obsidian_dust_to_plate", IngredientInput.tag(ModTags.Compat.Items.DUSTS_OBSIDIAN, ObsidianDust::class.instance()), 1, ObsidianPlate::class.instance(), 1),

        // ===== 青金石粉 -> 青金石板 =====
        Entry("lapis_dust_to_plate", IngredientInput.tag(ModTags.Compat.Items.DUSTS_LAPIS, LapisDust::class.instance()), 1, LapisPlate::class.instance(), 1),

        // ===== 9 矿物板 -> 1 致密板 =====
        Entry("bronze_plate_to_dense", IngredientInput.tag(ModTags.Compat.Items.PLATES_BRONZE, BronzePlate::class.instance()), 9, DenseBronzePlate::class.instance(), 1),
        Entry("copper_plate_to_dense", IngredientInput.tag(ModTags.Compat.Items.PLATES_COPPER, CopperPlate::class.instance()), 9, DenseCopperPlate::class.instance(), 1),
        Entry("gold_plate_to_dense", IngredientInput.tag(ModTags.Compat.Items.PLATES_GOLD, GoldPlate::class.instance()), 9, DenseGoldPlate::class.instance(), 1),
        Entry("iron_plate_to_dense", IngredientInput.tag(ModTags.Compat.Items.PLATES_IRON, IronPlate::class.instance()), 9, DenseIronPlate::class.instance(), 1),
        Entry("lapis_plate_to_dense", IngredientInput.tag(ModTags.Compat.Items.PLATES_LAPIS, LapisPlate::class.instance()), 9, DenseLapisPlate::class.instance(), 1),
        Entry("lead_plate_to_dense", IngredientInput.tag(ModTags.Compat.Items.PLATES_LEAD, LeadPlate::class.instance()), 9, DenseLeadPlate::class.instance(), 1),
        Entry("obsidian_plate_to_dense", IngredientInput.tag(ModTags.Compat.Items.PLATES_OBSIDIAN, ObsidianPlate::class.instance()), 9, DenseObsidianPlate::class.instance(), 1),
        Entry("steel_plate_to_dense", IngredientInput.tag(ModTags.Compat.Items.PLATES_STEEL, SteelPlate::class.instance()), 9, DenseSteelPlate::class.instance(), 1),
        Entry("tin_plate_to_dense", IngredientInput.tag(ModTags.Compat.Items.PLATES_TIN, TinPlate::class.instance()), 9, DenseTinPlate::class.instance(), 1),

        // ===== 粗制碳板 -> 碳板 =====
        Entry("carbon_mesh_to_plate", IngredientInput.item(CarbonMesh::class.instance()), 1, CarbonPlate::class.instance(), 1),

        // ===== 煤球 -> 压缩煤球 =====
        Entry("coal_ball_to_chunk", IngredientInput.item(CoalBall::class.instance()), 1, CompressedCoalBall::class.item(), 1),

        // ===== 煤块 -> 钻石 =====
        Entry("coal_chunk_to_diamond", IngredientInput.item(CoalChunk::class.instance()), 1, Items.DIAMOND, 1),

        // ===== 能量水晶粉 -> 能量水晶 =====
        Entry("energium_dust_to_crystal", IngredientInput.item(EnergiumDust::class.instance()), 9, EnergyCrystalItem::class.instance(), 1),

        // ===== 9 小撮粉 -> 1 粉 =====
        Entry("small_bronze_dust_to_dust", IngredientInput.tag(ModTags.Compat.Items.SMALL_DUSTS_BRONZE, SmallBronzeDust::class.instance()), 9, BronzeDust::class.instance(), 1),
        Entry("small_copper_dust_to_dust", IngredientInput.tag(ModTags.Compat.Items.SMALL_DUSTS_COPPER, SmallCopperDust::class.instance()), 9, CopperDust::class.instance(), 1),
        Entry("small_gold_dust_to_dust", IngredientInput.tag(ModTags.Compat.Items.SMALL_DUSTS_GOLD, SmallGoldDust::class.instance()), 9, GoldDust::class.instance(), 1),
        Entry("small_iron_dust_to_dust", IngredientInput.tag(ModTags.Compat.Items.SMALL_DUSTS_IRON, SmallIronDust::class.instance()), 9, IronDust::class.instance(), 1),
        Entry("small_lapis_dust_to_dust", IngredientInput.tag(ModTags.Compat.Items.SMALL_DUSTS_LAPIS, SmallLapisDust::class.instance()), 9, LapisDust::class.instance(), 1),
        Entry("small_lead_dust_to_dust", IngredientInput.tag(ModTags.Compat.Items.SMALL_DUSTS_LEAD, SmallLeadDust::class.instance()), 9, LeadDust::class.instance(), 1),
        Entry("small_lithium_dust_to_dust", IngredientInput.tag(ModTags.Compat.Items.SMALL_DUSTS_LITHIUM, SmallLithiumDust::class.instance()), 9, LithiumDust::class.instance(), 1),
        Entry("small_obsidian_dust_to_dust", IngredientInput.tag(ModTags.Compat.Items.SMALL_DUSTS_OBSIDIAN, SmallObsidianDust::class.instance()), 9, ObsidianDust::class.instance(), 1),
        Entry("small_silver_dust_to_dust", IngredientInput.tag(ModTags.Compat.Items.SMALL_DUSTS_SILVER, SmallSilverDust::class.instance()), 9, SilverDust::class.instance(), 1),
        Entry("small_sulfur_dust_to_dust", IngredientInput.tag(ModTags.Compat.Items.SMALL_DUSTS_SULFUR, SmallSulfurDust::class.instance()), 9, SulfurDust::class.instance(), 1),
        Entry("small_tin_dust_to_dust", IngredientInput.tag(ModTags.Compat.Items.SMALL_DUSTS_TIN, SmallTinDust::class.instance()), 9, TinDust::class.instance(), 1),

        // ===== 9 小撮核燃料 -> 1 核燃料 =====
        Entry("small_plutonium_to_plutonium", IngredientInput.item(SmallPlutonium::class.instance()), 9, Plutonium::class.instance(), 1),
        Entry("small_uranium_235_to_uranium_235", IngredientInput.item(SmallUranium235::class.instance()), 9, Uranium235::class.instance(), 1),
        Entry("small_uranium_238_to_uranium_238", IngredientInput.item(SmallUranium238::class.instance()), 9, Uranium238::class.instance(), 1),

        // ===== 9 铱碎片 -> 1 铱矿 =====
        Entry("iridium_shard_to_ore", IngredientInput.item(IridiumShard::class.instance()), 9, IridiumOreItem::class.instance(), 1),

        // ===== 混合金属锭 -> 高级合金 =====
        Entry("mixed_metal_ingot_to_alloy", IngredientInput.item(MixedMetalIngot::class.instance()), 1, Alloy::class.instance(), 1),

        // ===== 青铜块 -> 铁柄（青铜） =====
        Entry("bronze_block_to_bronze_shaft", IngredientInput.item(BronzeBlock::class.item()), 1, ToolHandleBronzeItem::class.instance(), 1),

        // ===== 空单元 -> 压缩空气单元 =====
        Entry("empty_cell_to_air_cell", IngredientInput.item(EmptyCell::class.instance()), 1, AirCell::class.instance(), 1),

        // ===== 水单元/水桶 -> 雪块（返还容器） =====
        Entry("water_cell_to_snow_block", IngredientInput.item(WaterCell::class.instance()), 1, Items.SNOW_BLOCK, 1, ItemStack(EmptyCell::class.instance(), 1)),
        Entry("water_bucket_to_snow_block", IngredientInput.item(Items.WATER_BUCKET), 1, Items.SNOW_BLOCK, 1, ItemStack(Items.BUCKET, 1)),
    )

    fun allEntries(): List<Entry> = entries

    fun generateRecipes(exporter: RecipeExporter) {
        entries.forEach { entry ->
            val id = Identifier.of("ic2_120", "compressing/${entry.name}")
            exporter.accept(
                id,
                CompressorRecipe(
                    id,
                    entry.input.toIngredient(),
                    entry.inputCount,
                    ItemStack(entry.output, entry.count),
                    entry.containerReturn
                ),
                null
            )
        }
    }
}
