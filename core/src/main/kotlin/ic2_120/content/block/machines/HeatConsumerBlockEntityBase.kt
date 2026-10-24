package ic2_120.content.block.machines

import ic2_120.compat.cachedState

import ic2_120.content.heat.IHeatConsumer
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.properties.BlockStateProperties

/**
 * 耗热机基类：默认只允许从背面接收热量。
 */
abstract class HeatConsumerBlockEntityBase(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), IHeatConsumer {

    override fun getHeatTransferFace(): Direction {
        return cachedState.getOptionalValue(BlockStateProperties.HORIZONTAL_FACING).orElse(Direction.NORTH)
    }

    final override fun receiveHeat(hu: Long, fromSide: Direction): Long {
        if (hu <= 0L) return 0L
        if (fromSide != getHeatTransferFace()) return 0L
        return receiveHeatInternal(hu)
    }

    protected abstract fun receiveHeatInternal(hu: Long): Long
}
