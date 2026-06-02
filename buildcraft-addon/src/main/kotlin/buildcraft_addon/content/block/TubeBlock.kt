package buildcraft_addon.content.block

import ic2_120.registry.annotation.ModBlock
import net.minecraft.block.Block
import net.minecraft.block.Blocks

/** 液泵/挖掘机延伸的管道方块，仅作为位置标记，无碰撞/无渲染。 */
@ModBlock(name = "tube", registerItem = false)
class TubeBlock : Block(Settings.copy(Blocks.AIR).strength(-1.0f, 0f).dropsNothing().noCollision().nonOpaque())
