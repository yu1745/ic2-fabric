package ic2_120.content.worldgen

import com.mojang.serialization.MapCodec
import ic2_120.content.block.RubberFaceState
import ic2_120.content.block.RubberLogBlock
import ic2_120.registry.instance
import net.minecraft.state.property.Properties
import net.minecraft.util.math.Direction
import net.minecraft.world.gen.treedecorator.TreeDecorator
import net.minecraft.world.gen.treedecorator.TreeDecoratorType

/**
 * 在树特性生成阶段直接初始化橡胶孔，避免依赖 onBlockAdded 或 BlockEntity tick。
 */
class RubberHoleTreeDecorator : TreeDecorator() {

    override fun getType(): TreeDecoratorType<*> = ModWorldgen.RUBBER_HOLE_TREE_DECORATOR_TYPE

    override fun generate(generator: TreeDecorator.Generator) {
        val rubberLog = RubberLogBlock::class.instance()
        val baseState = rubberLog.defaultState
            .with(Properties.AXIS, net.minecraft.util.math.Direction.Axis.Y)
            .with(RubberLogBlock.NATURAL, true)

        val initializedLogs = generator.logPositions
            .sortedBy { it.y }
            // 只处理最终仍然保留为原木的位置。
            // 树叶放置器可能会把最顶端那根原木覆盖成树叶；这里不能再把它写回原木。
            .filter { pos -> generator.world.testBlockState(pos) { it.block is RubberLogBlock } }
            .map { pos ->
                pos.toImmutable() to RubberLogBlock.initializeNaturalState(baseState, generator.random)
            }

        if (initializedLogs.isEmpty()) return

        val finalLogs =
            if (initializedLogs.any { (_, state) -> hasWetRubberFace(state) }) {
                initializedLogs
            } else {
                // 保底孔优先落在树干下半部，避免刷到顶部后被树叶挡住。
                val lowerHalfEndExclusive = maxOf(1, (initializedLogs.size + 1) / 2)
                val fallbackIndex = generator.random.nextInt(lowerHalfEndExclusive)
                initializedLogs.mapIndexed { index, entry ->
                    if (index == fallbackIndex) {
                        entry.first to addFallbackHole(entry.second, generator.random)
                    } else {
                        entry
                    }
                }
            }

        finalLogs.forEach { (pos, state) ->
            generator.replace(pos, state)
        }
    }

    companion object {
        val CODEC: MapCodec<RubberHoleTreeDecorator> = MapCodec.unit(::RubberHoleTreeDecorator)

        private fun hasWetRubberFace(state: net.minecraft.block.BlockState): Boolean =
            listOf(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)
                .any { state.get(RubberLogBlock.propFor(it)) == RubberFaceState.WET }

        private fun addFallbackHole(
            state: net.minecraft.block.BlockState,
            random: net.minecraft.util.math.random.Random
        ): net.minecraft.block.BlockState {
            val faces = listOf(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)
            val fallbackFace = faces[random.nextInt(faces.size)]
            return state.with(RubberLogBlock.propFor(fallbackFace), RubberFaceState.WET)
        }
    }
}
