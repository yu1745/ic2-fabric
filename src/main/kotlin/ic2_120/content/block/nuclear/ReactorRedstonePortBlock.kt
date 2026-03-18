package ic2_120.content.block.nuclear

import ic2_120.content.block.MachineBlock
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.type
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Blocks
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

/**
 * 反应堆红石接口。
 * 提供红石控制功能，可以禁用/反转红石信号。
 */
@ModBlock(name = "reactor_redstone_port", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "reactor")
class ReactorRedstonePortBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)) : MachineBlock(settings) {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        ReactorRedstonePortBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, ReactorRedstonePortBlockEntity::class.type()) { w, p, s, be ->
            (be as ReactorRedstonePortBlockEntity).tick(w, p, s)
        }
}
