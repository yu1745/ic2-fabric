package ic2_120.content.worldgen

import ic2_120.compat.world

import com.mojang.serialization.MapCodec
import ic2_120.content.block.RubberFaceState
import ic2_120.content.block.RubberLogBlock
import ic2_120.registry.instance
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.core.Direction
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType

/**
 * 在树特性生成阶段直接初始化橡胶孔，避免依赖 onBlockAdded 或 BlockEntity tick。
 */
class RubberHoleTreeDecorator : TreeDecorator() {

    override fun type(): TreeDecoratorType<*> = ModWorldgen.RUBBER_HOLE_TREE_DECORATOR_TYPE

    override fun place(context: TreeDecorator.Context) {
        val rubberLog = RubberLogBlock::class.instance()
        val baseState = rubberLog.defaultBlockState()
            .setValue(BlockStateProperties.AXIS, net.minecraft.core.Direction.Axis.Y)
            .setValue(RubberLogBlock.NATURAL, true)

        val initializedLogs = context.logs()
            .sortedBy { it.y }
            // 只处理最终仍然保留为原木的位置。
            // 树叶放置器可能会把最顶端那根原木覆盖成树叶；这里不能再把它写回原木。
            .filter { pos -> context.level().isStateAtPosition(pos) { it.block is RubberLogBlock } }
            .map { pos ->
                pos.immutable() to RubberLogBlock.initializeNaturalState(baseState, context.random())
            }

        if (initializedLogs.isEmpty()) return

        val finalLogs =
            if (initializedLogs.any { (_, state) -> hasWetRubberFace(state) }) {
                initializedLogs
            } else {
                // 保底孔优先落在树干下半部，避免刷到顶部后被树叶挡住。
                val lowerHalfEndExclusive = maxOf(1, (initializedLogs.size + 1) / 2)
                val fallbackIndex = context.random().nextInt(lowerHalfEndExclusive)
                initializedLogs.mapIndexed { index, entry ->
                    if (index == fallbackIndex) {
                        entry.first to addFallbackHole(entry.second, context.random())
                    } else {
                        entry
                    }
                }
            }

        finalLogs.forEach { (pos, state) ->
            context.setBlock(pos, state)
        }
    }

    companion object {
        val CODEC: MapCodec<RubberHoleTreeDecorator> = MapCodec.unit(::RubberHoleTreeDecorator)

        private fun hasWetRubberFace(state: net.minecraft.world.level.block.state.BlockState): Boolean =
            listOf(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)
                .any { state.getValue(RubberLogBlock.propFor(it)) == RubberFaceState.WET }

        private fun addFallbackHole(
            state: net.minecraft.world.level.block.state.BlockState,
            random: net.minecraft.util.RandomSource
        ): net.minecraft.world.level.block.state.BlockState {
            val faces = listOf(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)
            val fallbackFace = faces[random.nextInt(faces.size)]
            return state.setValue(RubberLogBlock.propFor(fallbackFace), RubberFaceState.WET)
        }
    }
}
