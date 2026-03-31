package ic2_120.content.block.transmission

import net.minecraft.block.BlockState
import net.minecraft.state.property.Properties
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.BlockView

object KineticConnectionRules {
    fun isTransmissionNode(state: BlockState): Boolean {
        val block = state.block
        return block is TransmissionShaftBlock || block is BevelGearBlock
    }

    fun canConnectFromDirection(state: BlockState, fromDirection: Direction): Boolean {
        return when (state.block) {
            is TransmissionShaftBlock -> state.get(Properties.AXIS) == fromDirection.axis
            is BevelGearBlock -> true
            else -> false
        }
    }

    fun nodeDirections(world: BlockView, pos: BlockPos, state: BlockState): List<Direction> {
        return when (state.block) {
            is TransmissionShaftBlock -> when (state.get(Properties.AXIS)) {
                Direction.Axis.X -> listOf(Direction.EAST, Direction.WEST)
                Direction.Axis.Y -> listOf(Direction.UP, Direction.DOWN)
                Direction.Axis.Z -> listOf(Direction.SOUTH, Direction.NORTH)
            }

            is BevelGearBlock -> bevelDirections(world, pos)
            else -> emptyList()
        }
    }

    fun isMachinePortFacing(world: BlockView, machinePos: BlockPos, directionFromNode: Direction): Boolean {
        val be = world.getBlockEntity(machinePos) as? IKineticMachinePort ?: return false
        val sideFromMachine = directionFromNode.opposite
        return be.canOutputKuTo(sideFromMachine) || be.canInputKuFrom(sideFromMachine)
    }

    private fun bevelDirections(world: BlockView, pos: BlockPos): List<Direction> {
        val candidates = Direction.entries.filter { direction ->
            val neighborPos = pos.offset(direction)
            val neighborState = world.getBlockState(neighborPos)
            when {
                isTransmissionNode(neighborState) -> canConnectFromDirection(neighborState, direction.opposite)
                else -> isMachinePortFacing(world, neighborPos, direction)
            }
        }

        if (candidates.size == 2) {
            val first = candidates[0]
            val second = candidates[1]
            if (first.axis == second.axis) return emptyList()
            return candidates.sortedBy(::directionPriority)
        }

        if (candidates.size != 3) return emptyList()

        val byAxis = candidates.groupBy { it.axis }
        if (byAxis.size == 3) return emptyList()

        val throughAxis = byAxis.entries.firstOrNull { it.value.size == 2 }?.key ?: return emptyList()
        val pair = byAxis[throughAxis].orEmpty().sortedBy(::directionPriority)
        val branch = candidates
            .asSequence()
            .filter { it.axis != throughAxis }
            .sortedBy(::directionPriority)
            .firstOrNull() ?: return emptyList()

        return listOf(pair[0], pair[1], branch)
    }

    private fun directionPriority(direction: Direction): Int = when (direction) {
        Direction.EAST -> 0
        Direction.SOUTH -> 1
        Direction.UP -> 2
        Direction.WEST -> 3
        Direction.NORTH -> 4
        Direction.DOWN -> 5
    }
}
