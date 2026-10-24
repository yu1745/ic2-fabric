package ic2_120.content.block

import net.minecraft.data.recipes.RecipeProvider.getHasName

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.content.item.*
import ic2_120.content.recipes.ModTags
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.recipeId
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.FenceBlock
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.data.recipes.ShapelessRecipeBuilder
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.core.Direction
import java.util.function.Consumer

// 铜方块：使用原版 minecraft:copper_block，此处不再注册

/**
 * 锡方块。
 */
@ModBlock(name = "tin_block", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "metal_blocks", materialTags = ["storage_blocks/tin"])
class TinBlock : Block(
    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(4.0f, 5.0f)
) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val ingot = TinIngot::class.instance()
            if (ingot != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, TinBlock::class.item(), 1)
                    .pattern("III").pattern("III").pattern("III")
                    .define('I', Ingredient.of(ModTags.Compat.Items.INGOTS_TIN))
                    .unlockedBy(getHasName(ingot), has(ingot))
                    .save(exporter, TinBlock::class.id())
            }
        }
    }
}

/**
 * 青铜方块。
 */
@ModBlock(name = "bronze_block", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "metal_blocks", materialTags = ["storage_blocks/bronze"])
class BronzeBlock : Block(
    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)
) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val ingot = BronzeIngot::class.instance()
            if (ingot != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, BronzeBlock::class.item(), 1)
                    .pattern("III").pattern("III").pattern("III")
                    .define('I', Ingredient.of(ModTags.Compat.Items.INGOTS_BRONZE))
                    .unlockedBy(getHasName(ingot), has(ingot))
                    .save(exporter, BronzeBlock::class.id())
            }
        }
    }
}

// ========== 粗金属块（raw） ==========

@ModBlock(name = "raw_lead_block", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "raw_blocks")
class RawLeadBlock : Block(BlockBehaviour.Properties.ofFullCopy(Blocks.RAW_IRON_BLOCK).strength(5.0f, 6.0f)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val raw = RawLead::class.instance()
            val block = RawLeadBlock::class.instance()
            if (raw != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, RawLeadBlock::class.item(), 1)
                    .pattern("III").pattern("III").pattern("III")
                    .define('I', raw)
                    .unlockedBy(getHasName(raw), has(raw))
                    .save(exporter, RawLeadBlock::class.id())

                ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, raw, 9)
                    .requires(block)
                    .unlockedBy(getHasName(block), has(block))
                    .save(exporter, RawLeadBlock::class.recipeId("to_raw"))
            }
        }
    }
}

@ModBlock(name = "raw_tin_block", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "raw_blocks")
class RawTinBlock : Block(BlockBehaviour.Properties.ofFullCopy(Blocks.RAW_IRON_BLOCK).strength(5.0f, 6.0f)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val raw = RawTin::class.instance()
            val block = RawTinBlock::class.instance()
            if (raw != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, RawTinBlock::class.item(), 1)
                    .pattern("III").pattern("III").pattern("III")
                    .define('I', raw)
                    .unlockedBy(getHasName(raw), has(raw))
                    .save(exporter, RawTinBlock::class.id())

                ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, raw, 9)
                    .requires(block)
                    .unlockedBy(getHasName(block), has(block))
                    .save(exporter, RawTinBlock::class.recipeId("to_raw"))
            }
        }
    }
}

@ModBlock(name = "raw_uranium_block", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "raw_blocks")
class RawUraniumBlock : Block(BlockBehaviour.Properties.ofFullCopy(Blocks.RAW_IRON_BLOCK).strength(5.0f, 6.0f)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val raw = RawUranium::class.instance()
            val block = RawUraniumBlock::class.instance()
            if (raw != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, RawUraniumBlock::class.item(), 1)
                    .pattern("III").pattern("III").pattern("III")
                    .define('I', raw)
                    .unlockedBy(getHasName(raw), has(raw))
                    .save(exporter, RawUraniumBlock::class.id())

                ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, raw, 9)
                    .requires(block)
                    .unlockedBy(getHasName(block), has(block))
                    .save(exporter, RawUraniumBlock::class.recipeId("to_raw"))
            }
        }
    }
}

// ========== 金属块 ==========

@ModBlock(name = "lead_block", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "metal_blocks", materialTags = ["storage_blocks/lead"])
class LeadBlock : Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val ingot = LeadIngot::class.instance()
            if (ingot != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, LeadBlock::class.item(), 1)
                    .pattern("III").pattern("III").pattern("III")
                    .define('I', Ingredient.of(ModTags.Compat.Items.INGOTS_LEAD))
                    .unlockedBy(getHasName(ingot), has(ingot))
                    .save(exporter, LeadBlock::class.id())
            }
        }
    }
}

@ModBlock(name = "steel_block", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "metal_blocks", materialTags = ["storage_blocks/steel"])
class SteelBlock : Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val ingot = SteelIngot::class.instance()
            if (ingot != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, SteelBlock::class.item(), 1)
                    .pattern("III").pattern("III").pattern("III")
                    .define('I', Ingredient.of(ModTags.Compat.Items.INGOTS_STEEL))
                    .unlockedBy(getHasName(ingot), has(ingot))
                    .save(exporter, SteelBlock::class.id())
            }
        }
    }
}

@ModBlock(name = "uranium_block", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "metal_blocks", materialTags = ["storage_blocks/uranium"])
class UraniumBlock : Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val ingot = UraniumIngot::class.instance()
            if (ingot != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, UraniumBlock::class.item(), 1)
                    .pattern("III").pattern("III").pattern("III")
                    .define('I', Ingredient.of(ModTags.Compat.Items.INGOTS_URANIUM))
                    .unlockedBy(getHasName(ingot), has(ingot))
                    .save(exporter, UraniumBlock::class.id())
            }
        }
    }
}

@ModBlock(name = "silver_block", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "metal_blocks", materialTags = ["storage_blocks/silver"])
class SilverBlock : Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val ingot = SilverIngot::class.instance()
            if (ingot != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, SilverBlock::class.item(), 1)
                    .pattern("III").pattern("III").pattern("III")
                    .define('I', Ingredient.of(ModTags.Compat.Items.INGOTS_SILVER))
                    .unlockedBy(getHasName(ingot), has(ingot))
                    .save(exporter, SilverBlock::class.id())
            }
        }
    }
}

@ModBlock(name = "coal_block", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "metal_blocks")
class CompressedCoalBall : Block(BlockBehaviour.Properties.ofFullCopy(Blocks.COAL_BLOCK).strength(5.0f, 6.0f))

/** 防爆石。 */
@ModBlock(name = "reinforced_stone", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "metal_blocks")
class ReinforcedStoneBlock : Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).strength(80.0f, 1200.0f))

/** 玄武石。 */
@ModBlock(name = "basalt", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "metal_blocks")
class BasaltBlock : Block(BlockBehaviour.Properties.ofFullCopy(Blocks.BASALT).strength(4.0f, 6.0f))

/** 铁栅栏。金属成型机挤压：铁板 + 铁外壳 -> 铁栅栏。支持与同类型、原版铁栏杆相互连接。 */
@ModBlock(name = "iron_fence", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "parts")
class IronFenceBlock(
    settings: BlockBehaviour.Properties = BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(5.0f, 6.0f)
) : FenceBlock(settings) {

    override fun canConnect(state: BlockState, neighborIsFullSquare: Boolean, dir: Direction): Boolean {
        val block = state.block
        // 与同类型铁栅栏、原版铁栏杆相互连接
        return block is IronFenceBlock
        // return super.canConnect(state, neighborIsFullSquare, dir)
    }
}
