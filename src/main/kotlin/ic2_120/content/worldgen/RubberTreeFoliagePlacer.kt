package ic2_120.content.worldgen

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.intprovider.IntProvider
import net.minecraft.util.math.random.Random
import net.minecraft.world.TestableWorld
import net.minecraft.world.gen.foliage.BlobFoliagePlacer
import net.minecraft.world.gen.foliage.FoliagePlacer
import net.minecraft.world.gen.foliage.FoliagePlacerType
import net.minecraft.world.gen.feature.TreeFeatureConfig
import org.slf4j.LoggerFactory

/**
 * 橡胶树专用树叶放置器。
 * 低层使用白桦树（blob）风格逻辑，最高两层仅放置中心一个树叶方块。
 */
class RubberTreeFoliagePlacer(
    radius: IntProvider,
    offset: IntProvider,
    height: Int
) : BlobFoliagePlacer(radius, offset, height) {

    override fun getType(): FoliagePlacerType<*> = ModWorldgen.RUBBER_TREE_FOLIAGE_PLACER_TYPE

    override fun generate(
        world: TestableWorld,
        placer: FoliagePlacer.BlockPlacer,
        random: Random,
        config: TreeFeatureConfig,
        trunkHeight: Int,
        treeNode: FoliagePlacer.TreeNode,
        foliageHeight: Int,
        radius: Int,
        offset: Int
    ) {
        val center = treeNode.center
        val collected = LinkedHashMap<BlockPos, BlockState>()

        val collectingPlacer = object : FoliagePlacer.BlockPlacer {
            override fun placeBlock(pos: BlockPos, state: BlockState) {
                collected[pos.toImmutable()] = state
            }

            override fun hasPlacedBlock(pos: BlockPos): Boolean = collected.containsKey(pos)
        }

        // 先按白桦（blob）逻辑生成到缓存
        super.generate(world, collectingPlacer, random, config, trunkHeight, treeNode, foliageHeight, radius, offset)

        val ys = collected.keys.asSequence().map { it.y }.distinct().sortedDescending().toList()
        val topYs = ys.take(2).toSet()

        var filtered = 0
        var placed = 0
        var forced = 0

        for ((pos, state) in collected) {
            if (pos.y in topYs) {
                if (pos.x != center.x || pos.z != center.z) {
                    filtered++
                    continue
                }
            }
            placed++
            placer.placeBlock(pos, state)
        }

        // 确保顶两层中心叶子存在
        for (y in topYs) {
            val centerPos = BlockPos(center.x, y, center.z)
            if (!placer.hasPlacedBlock(centerPos)) {
                forced++
                placer.placeBlock(centerPos, config.foliageProvider.get(random, centerPos))
            }
        }

        logger.info(
            "RubberTreeFoliagePlacer: center={}, trunkHeight={}, foliageHeight={}, radius={}, offset={}, topYs={}, total={}, placed={}, filtered={}, forced={}",
            center,
            trunkHeight,
            foliageHeight,
            radius,
            offset,
            topYs,
            collected.size,
            placed,
            filtered,
            forced
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger("ic2_120/RubberTreeFoliagePlacer")

        val CODEC: Codec<RubberTreeFoliagePlacer> = RecordCodecBuilder.create { instance ->
            fillFoliagePlacerFields(instance).and(
                Codec.INT.fieldOf("height").forGetter { it.height }
            ).apply(instance) { radius, offset, height ->
                RubberTreeFoliagePlacer(radius, offset, height)
            }
        }
    }
}
