package ic2_120.content.worldgen

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.core.BlockPos
import net.minecraft.util.valueproviders.IntProvider
import net.minecraft.util.RandomSource
import net.minecraft.world.level.LevelSimulatedReader
import net.minecraft.world.level.levelgen.feature.TreeFeature
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration
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

    override fun type(): FoliagePlacerType<*> = ModWorldgen.RUBBER_TREE_FOLIAGE_PLACER_TYPE

    override fun createFoliage(
        world: net.minecraft.world.level.WorldGenLevel,
        placer: FoliagePlacer.FoliageSetter,
        random: RandomSource,
        config: TreeConfiguration,
        trunkHeight: Int,
        foliageAttachment: FoliagePlacer.FoliageAttachment,
        foliageHeight: Int,
        radius: Int,
        offset: Int
    ) {
        val center = foliageAttachment.pos()
        val collected = LinkedHashMap<BlockPos, BlockState>()

        val collectingPlacer = object : FoliagePlacer.FoliageSetter {
            override fun set(pos: BlockPos, state: BlockState) {
                collected[pos.immutable()] = state
            }

            override fun isSet(pos: BlockPos): Boolean = collected.containsKey(pos)
        }

        // 先按白桦（blob）逻辑生成到缓存
        super.createFoliage(world, collectingPlacer, random, config, trunkHeight, foliageAttachment, foliageHeight, radius, offset)

        val ys = collected.keys.asSequence().map { it.y }.distinct().sortedDescending().toList()
        if (ys.isEmpty()) return
        val highestY = ys.first()
        // 顶部两层单叶显式取“最高层”和“最高层下一层”，
        // 不再依赖 blob 结果里恰好出现哪两个 y，避免偶发把第二层判断成树干层。
        val topYs = setOf(highestY, highestY - 1)

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
            placer.set(pos, state)
        }

        // 确保顶两层中心叶子存在
        for (y in topYs) {
            val centerPos = BlockPos(center.x, y, center.z)
            if (!placer.isSet(centerPos) && TreeFeature.validTreePos(world, centerPos)) {
                forced++
                placer.set(centerPos, config.foliageProvider.getState(world, random, centerPos))
            }
        }

        logger.debug(
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

        val CODEC: MapCodec<RubberTreeFoliagePlacer> = RecordCodecBuilder.mapCodec { instance ->
            foliagePlacerParts(instance).and(
                Codec.INT.fieldOf("height").forGetter { it.height }
            ).apply(instance) { radius, offset, height ->
                RubberTreeFoliagePlacer(radius, offset, height)
            }
        }
    }
}
