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

// ========== 粗金属块（raw） ==========

@ModBlock(name = "raw_lead_block", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class RawLeadBlock : Block(AbstractBlock.Settings.copy(Blocks.RAW_IRON_BLOCK).strength(5.0f, 6.0f))

@ModBlock(name = "raw_tin_block", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class RawTinBlock : Block(AbstractBlock.Settings.copy(Blocks.RAW_IRON_BLOCK).strength(5.0f, 6.0f))

@ModBlock(name = "raw_uranium_block", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class RawUraniumBlock : Block(AbstractBlock.Settings.copy(Blocks.RAW_IRON_BLOCK).strength(5.0f, 6.0f))

// ========== 金属块 ==========

@ModBlock(name = "lead_block", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class LeadBlock : Block(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f))

@ModBlock(name = "steel_block", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class SteelBlock : Block(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f))

@ModBlock(name = "uranium_block", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class UraniumBlock : Block(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f))

@ModBlock(name = "silver_block", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class SilverBlock : Block(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f))

@ModBlock(name = "coal_block", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class CoalBlock : Block(AbstractBlock.Settings.copy(Blocks.COAL_BLOCK).strength(5.0f, 6.0f))
