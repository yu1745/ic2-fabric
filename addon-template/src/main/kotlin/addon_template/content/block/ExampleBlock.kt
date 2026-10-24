package addon_template.content.block

import ic2_120.content.block.MachineBlock
import ic2_120.registry.annotation.ModBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockBehaviour

@ModBlock(name = "example_block")
class ExampleBlock(settings: BlockBehaviour.Properties) : MachineBlock(settings) {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? = null
}
