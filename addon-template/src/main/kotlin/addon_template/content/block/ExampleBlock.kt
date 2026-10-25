package addon_template.content.block

import ic2_120.content.block.MachineBlock
import ic2_120.registry.annotation.ModBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.core.BlockPos

@ModBlock(name = "example_block")
class ExampleBlock : MachineBlock() {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? = null
}
