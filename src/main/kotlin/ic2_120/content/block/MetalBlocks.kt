package ic2_120.content.block

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.FenceBlock
import net.minecraft.util.math.Direction

// 铜方块：使用原版 minecraft:copper_block，此处不再注册

/**
 * 锡方块。
 */
@ModBlock(name = "tin_block", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "metal_blocks", materialTags = ["storage_blocks/tin"])
class TinBlock : Block(
    AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(4.0f, 5.0f)
)

/**
 * 青铜方块。
 */
@ModBlock(name = "bronze_block", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "metal_blocks", materialTags = ["storage_blocks/bronze"])
class BronzeBlock : Block(
    AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)
)

// ========== 粗金属块（raw） ==========

@ModBlock(name = "raw_lead_block", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "raw_blocks")
class RawLeadBlock : Block(AbstractBlock.Settings.copy(Blocks.RAW_IRON_BLOCK).strength(5.0f, 6.0f))

@ModBlock(name = "raw_tin_block", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "raw_blocks")
class RawTinBlock : Block(AbstractBlock.Settings.copy(Blocks.RAW_IRON_BLOCK).strength(5.0f, 6.0f))

@ModBlock(name = "raw_uranium_block", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "raw_blocks")
class RawUraniumBlock : Block(AbstractBlock.Settings.copy(Blocks.RAW_IRON_BLOCK).strength(5.0f, 6.0f))

// ========== 金属块 ==========

@ModBlock(name = "lead_block", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "metal_blocks", materialTags = ["storage_blocks/lead"])
class LeadBlock : Block(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f))

@ModBlock(name = "steel_block", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "metal_blocks", materialTags = ["storage_blocks/steel"])
class SteelBlock : Block(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f))

@ModBlock(name = "uranium_block", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "metal_blocks", materialTags = ["storage_blocks/uranium"])
class UraniumBlock : Block(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f))

@ModBlock(name = "silver_block", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "metal_blocks", materialTags = ["storage_blocks/silver"])
class SilverBlock : Block(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f))

@ModBlock(name = "coal_block", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "metal_blocks")
class CompressedCoalBall : Block(AbstractBlock.Settings.copy(Blocks.COAL_BLOCK).strength(5.0f, 6.0f))

/** 防爆石。 */
@ModBlock(name = "reinforced_stone", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "metal_blocks")
class ReinforcedStoneBlock : Block(AbstractBlock.Settings.copy(Blocks.STONE).strength(50.0f, 1200.0f))

/** 玄武石。 */
@ModBlock(name = "basalt", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "metal_blocks")
class BasaltBlock : Block(AbstractBlock.Settings.copy(Blocks.BASALT).strength(4.0f, 6.0f))

/** 铁栅栏。金属成型机挤压：铁板 + 铁外壳 -> 铁栅栏。支持与同类型、原版铁栏杆相互连接。 */
@ModBlock(name = "iron_fence", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "parts")
class IronFenceBlock(
    settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.IRON_BARS).strength(5.0f, 6.0f)
) : FenceBlock(settings) {

    override fun canConnect(state: BlockState, neighborIsFullSquare: Boolean, dir: Direction): Boolean {
        val block = state.block
        // 与同类型铁栅栏、原版铁栏杆相互连接
        return block is IronFenceBlock
        // return super.canConnect(state, neighborIsFullSquare, dir)
    }
}
