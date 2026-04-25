package ic2_120.content.recipes.compressor

import com.google.gson.JsonObject
import ic2_120.content.block.*
import ic2_120.content.item.*
import ic2_120.content.item.energy.EnergyCrystalItem
import ic2_120.content.recipes.ModMachineRecipes
import ic2_120.registry.instance
import ic2_120.registry.item
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import java.util.function.Consumer

object CompressorRecipeDatagen {
    data class Entry(
        val name: String,
        val input: Item,
        val inputCount: Int,
        val output: Item,
        val count: Int
    )

    private val entries = listOf(
        // 9 金属锭 -> 1 金属块
        Entry("iron_ingot_to_block", Items.IRON_INGOT, 9, Items.IRON_BLOCK, 1),
        Entry("gold_ingot_to_block", Items.GOLD_INGOT, 9, Items.GOLD_BLOCK, 1),
        Entry("copper_ingot_to_block", Items.COPPER_INGOT, 9, Items.COPPER_BLOCK, 1),
        Entry("tin_ingot_to_block", TinIngot::class.instance(), 9, TinBlock::class.item(), 1),
        Entry("bronze_ingot_to_block", BronzeIngot::class.instance(), 9, BronzeBlock::class.item(), 1),

        // 沙子 -> 沙石
        Entry("sand_to_sandstone", Items.SAND, 4, Items.SANDSTONE, 1),
        Entry("red_sand_to_red_sandstone", Items.RED_SAND, 4, Items.RED_SANDSTONE, 1),

        // 粘土球 -> 砖
        Entry("clay_ball_to_brick", Items.CLAY_BALL, 4, Items.BRICK, 1),

        // 雪球 -> 雪块
        Entry("snowball_to_snow_block", Items.SNOWBALL, 4, Items.SNOW_BLOCK, 1),

        // 萤石粉 -> 萤石
        Entry("glowstone_dust_to_glowstone", Items.GLOWSTONE_DUST, 4, Items.GLOWSTONE, 1),

        // 青金石 -> 青金石块
        Entry("lapis_to_lapis_block", Items.LAPIS_LAZULI, 4, Items.LAPIS_BLOCK, 1),

        // 黑曜石粉 -> 黑曜石板
        Entry("obsidian_to_plate", ObsidianDust::class.instance(), 1, ObsidianPlate::class.instance(), 1),

        // 9 矿物板 -> 1 致密板
        Entry("bronze_plate_to_dense", BronzePlate::class.instance(), 9, DenseBronzePlate::class.instance(), 1),
        Entry("copper_plate_to_dense", CopperPlate::class.instance(), 9, DenseCopperPlate::class.instance(), 1),
        Entry("gold_plate_to_dense", GoldPlate::class.instance(), 9, DenseGoldPlate::class.instance(), 1),
        Entry("iron_plate_to_dense", IronPlate::class.instance(), 9, DenseIronPlate::class.instance(), 1),
        Entry("lapis_plate_to_dense", LapisPlate::class.instance(), 9, DenseLapisPlate::class.instance(), 1),
        Entry("lead_plate_to_dense", LeadPlate::class.instance(), 9, DenseLeadPlate::class.instance(), 1),
        Entry("obsidian_plate_to_dense", ObsidianPlate::class.instance(), 9, DenseObsidianPlate::class.instance(), 1),
        Entry("steel_plate_to_dense", SteelPlate::class.instance(), 9, DenseSteelPlate::class.instance(), 1),
        Entry("tin_plate_to_dense", TinPlate::class.instance(), 9, DenseTinPlate::class.instance(), 1),

        // 粗制碳板 -> 碳板
        Entry("carbon_mesh_to_plate", CarbonMesh::class.instance(), 1, CarbonPlate::class.instance(), 1),

        // 煤球 -> 压缩煤球
        Entry("coal_ball_to_chunk", CoalBall::class.instance(), 1, CompressedCoalBall::class.item(), 1),

        // 能量水晶粉 -> 能量水晶
        Entry("energium_dust_to_crystal", EnergiumDust::class.instance(), 9, EnergyCrystalItem::class.instance(), 1),

        // 烈焰粉 -> 烈焰棒
        Entry("blaze_powder_to_rod", Items.BLAZE_POWDER, 5, Items.BLAZE_ROD, 1),

        // 雪块 -> 冰
        Entry("snow_block_to_ice", Items.SNOW_BLOCK, 1, Items.ICE, 1),

        // 2 冰 -> 浮冰
        Entry("ice_to_packed_ice", Items.ICE, 2, Items.PACKED_ICE, 1),

        // 9 小撮钚 -> 钚
        Entry("small_plutonium_to_plutonium", SmallPlutonium::class.instance(), 9, Plutonium::class.instance(), 1),

        // 9 小撮铀-235 -> 铀-235
        Entry(
            "small_uranium_235_to_uranium_235",
            SmallUranium235::class.instance(),
            9,
            Uranium235::class.instance(),
            1
        ),

        // 9 小撮铀-238 -> 铀-238
        Entry(
            "small_uranium_238_to_uranium_238",
            SmallUranium238::class.instance(),
            9,
            Uranium238::class.instance(),
            1
        ),

        // 混合金属锭 -> 高级合金
        Entry("mixed_metal_ingot_to_alloy", MixedMetalIngot::class.instance(), 1, Alloy::class.instance(), 1),

        // 煤块 -> 钻石
        Entry("coal_block_to_diamond", CoalChunk::class.instance(), 1, Items.DIAMOND, 1),

        // 青铜块 -> 铁柄(青铜)
        Entry("bronze_block_to_bronze_shaft", BronzeBlock::class.item(), 1, ToolHandleBronzeItem::class.instance(), 1),

        // 空单元 -> 压缩空气单元
        Entry("empty_cell_to_air_cell", EmptyCell::class.instance(), 1, AirCell::class.instance(), 1)
    )

    fun allEntries(): List<Entry> = entries

    fun generateRecipes(exporter: Consumer<RecipeExporter>) {
        entries.forEach { entry ->
            CompressorRecipeExporter(
                recipeId = Identifier.of("ic2_120", "compressing/${entry.name}"),
                inputItem = entry.input,
                inputCount = entry.inputCount,
                outputItem = entry.output,
                outputCount = entry.count
            ).also(exporter::accept)
        }
    }

    private class CompressorRecipeExporter(
        private val recipeId: Identifier,
        private val inputItem: Item,
        private val inputCount: Int,
        private val outputItem: Item,
        private val outputCount: Int
    ) : RecipeExporter {
        override fun serialize(json: JsonObject) {
            json.addProperty("type", "${ModMachineRecipes.recipeType(CompressorRecipe::class)}")
            val ingredient = JsonObject()
            ingredient.addProperty("item", Registries.ITEM.getId(inputItem).toString())
            json.add("ingredient", ingredient)
            json.addProperty("input_count", inputCount)

            val result = JsonObject()
            result.addProperty("item", Registries.ITEM.getId(outputItem).toString())
            result.addProperty("count", outputCount)
            json.add("result", result)
        }

        override fun getSerializer() = ModMachineRecipes.recipeSerializer(CompressorRecipe::class)

        override fun getRecipeId(): Identifier = recipeId

        override fun toAdvancementJson(): JsonObject? = null

        override fun getAdvancementId(): Identifier? = null
    }
}
