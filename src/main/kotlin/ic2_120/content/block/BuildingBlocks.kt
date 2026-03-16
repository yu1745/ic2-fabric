package ic2_120.content.block

import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.type
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.block.PillarBlock

// ========== 建筑：防爆玻璃、泡沫、墙、垫、管道、TNT ==========

@ModBlock(name = "reinforced_glass", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "building", transparent = true)
class ReinforcedGlassBlock : Block(AbstractBlock.Settings.copy(Blocks.GLASS).strength(10.0f, 1200.0f).nonOpaque())

@ModBlock(name = "foam", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "building")
class FoamBlock : Block(AbstractBlock.Settings.copy(Blocks.WHITE_WOOL).strength(0.5f))

@ModBlock(name = "resin_sheet", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "building")
class ResinSheetBlock : Block(AbstractBlock.Settings.copy(Blocks.WHITE_CARPET).strength(0.5f))

@ModBlock(name = "rubber_sheet", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "building")
class RubberSheetBlock : Block(AbstractBlock.Settings.copy(Blocks.WHITE_CARPET).strength(0.5f))

@ModBlock(name = "wool_sheet", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "building")
class WoolSheetBlock : Block(AbstractBlock.Settings.copy(Blocks.WHITE_CARPET).strength(0.5f))

@ModBlock(name = "mining_pipe", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "building")
class MiningPipeBlock : PillarBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(3.0f))

@ModBlock(name = "itnt", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "building")
class ItntBlock : Block(AbstractBlock.Settings.copy(Blocks.TNT).strength(0.0f))

// ========== 建筑泡沫墙（16 色） ==========

@ModBlock(name = "white_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class WhiteWallBlock : Block(AbstractBlock.Settings.copy(Blocks.WHITE_CONCRETE).strength(2.0f))

@ModBlock(name = "orange_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class OrangeWallBlock : Block(AbstractBlock.Settings.copy(Blocks.ORANGE_CONCRETE).strength(2.0f))

@ModBlock(name = "magenta_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class MagentaWallBlock : Block(AbstractBlock.Settings.copy(Blocks.MAGENTA_CONCRETE).strength(2.0f))

@ModBlock(name = "light_blue_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class LightBlueWallBlock : Block(AbstractBlock.Settings.copy(Blocks.LIGHT_BLUE_CONCRETE).strength(2.0f))

@ModBlock(name = "yellow_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class YellowWallBlock : Block(AbstractBlock.Settings.copy(Blocks.YELLOW_CONCRETE).strength(2.0f))

@ModBlock(name = "lime_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class LimeWallBlock : Block(AbstractBlock.Settings.copy(Blocks.LIME_CONCRETE).strength(2.0f))

@ModBlock(name = "pink_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class PinkWallBlock : Block(AbstractBlock.Settings.copy(Blocks.PINK_CONCRETE).strength(2.0f))

@ModBlock(name = "gray_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class GrayWallBlock : Block(AbstractBlock.Settings.copy(Blocks.GRAY_CONCRETE).strength(2.0f))

@ModBlock(name = "light_gray_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class LightGrayWallBlock : Block(AbstractBlock.Settings.copy(Blocks.LIGHT_GRAY_CONCRETE).strength(2.0f))

@ModBlock(name = "cyan_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class CyanWallBlock : Block(AbstractBlock.Settings.copy(Blocks.CYAN_CONCRETE).strength(2.0f))

@ModBlock(name = "purple_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class PurpleWallBlock : Block(AbstractBlock.Settings.copy(Blocks.PURPLE_CONCRETE).strength(2.0f))

@ModBlock(name = "blue_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class BlueWallBlock : Block(AbstractBlock.Settings.copy(Blocks.BLUE_CONCRETE).strength(2.0f))

@ModBlock(name = "brown_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class BrownWallBlock : Block(AbstractBlock.Settings.copy(Blocks.BROWN_CONCRETE).strength(2.0f))

@ModBlock(name = "green_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class GreenWallBlock : Block(AbstractBlock.Settings.copy(Blocks.GREEN_CONCRETE).strength(2.0f))

@ModBlock(name = "red_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class RedWallBlock : Block(AbstractBlock.Settings.copy(Blocks.RED_CONCRETE).strength(2.0f))

@ModBlock(name = "black_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class BlackWallBlock : Block(AbstractBlock.Settings.copy(Blocks.BLACK_CONCRETE).strength(2.0f))

// ========== 脚手架 ==========

@ModBlock(name = "wooden_scaffold", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "scaffold", transparent = true)
class WoodenScaffoldBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).strength(1.0f).nonOpaque()) : PillarBlock(settings)

@ModBlock(name = "reinforced_wooden_scaffold", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "scaffold", transparent = true)
class ReinforcedWoodenScaffoldBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).strength(2.0f).nonOpaque()) : PillarBlock(settings)

@ModBlock(name = "iron_scaffold", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "scaffold", transparent = true)
class IronScaffoldBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(3.0f).nonOpaque()) : PillarBlock(settings)

@ModBlock(name = "reinforced_iron_scaffold", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "scaffold", transparent = true)
class ReinforcedIronScaffoldBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f).nonOpaque()) : PillarBlock(settings)
