package ic2_120.content.block

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.block.PillarBlock

/**
 * 铜方块。
 */
@ModBlock(name = "copper_block", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class CopperBlock : Block(
    AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)
)

/**
 * 锡方块。
 */
@ModBlock(name = "tin_block", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class TinBlock : Block(
    AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(4.0f, 5.0f)
)

/**
 * 青铜方块。
 */
@ModBlock(name = "bronze_block", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class BronzeBlock : Block(
    AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)
)

/**
 * 橡胶原木。
 */
@ModBlock(name = "rubber_wood", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class RubberWood : PillarBlock(
    AbstractBlock.Settings.copy(Blocks.OAK_LOG).strength(2.0f)
)
