package ic2_120.content.block.machines

import ic2_120.content.heat.IHeatConsumer
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.state.property.Properties

/**
 * 耗热机基类：默认只允许从背面接收热量。
 */
abstract class HeatConsumerBlockEntityBase(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), IHeatConsumer {

    override fun getHeatTransferFace(): Direction {
        return world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH
    }

    final override fun receiveHeat(hu: Long, fromSide: Direction): Long {
        if (hu <= 0L) return 0L
        if (fromSide != getHeatTransferFace()) return 0L
        return receiveHeatInternal(hu)
    }

    protected abstract fun receiveHeatInternal(hu: Long): Long
}
