package ic2_120.content.block

import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.type
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.Blocks

/**
 * 基础机械外壳。用于建造机器的结构方块。
 */
@ModBlock(name = "machine", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "machine_casing")
class MachineCasingBlock : Block(
    AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)
)

/**
 * 高级机械外壳。用于建造高级机器的结构方块。
 */
@ModBlock(name = "advanced_machine", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "machine_casing")
class AdvancedMachineCasingBlock : Block(
    AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)
)
