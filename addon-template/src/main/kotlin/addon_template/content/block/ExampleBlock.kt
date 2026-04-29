package addon_template.content.block

import ic2_120.content.block.MachineBlock
import ic2_120.registry.annotation.ModBlock
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.util.math.BlockPos

@ModBlock(name = "example_block")
class ExampleBlock(settings: Settings) : MachineBlock(settings) {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? = null
}
