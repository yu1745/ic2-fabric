package addon_template.content.block

import ic2_120.content.block.MachineBlock
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.CreativeTab
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockBehaviour

@ModBlock(name = "example_block", tab = CreativeTab.ADDON_TEMPLATE)
class ExampleBlock(settings: BlockBehaviour.Properties = BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.IRON_BLOCK)) : MachineBlock(settings) {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? = null
}
