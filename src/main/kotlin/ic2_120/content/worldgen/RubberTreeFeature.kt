package ic2_120.content.worldgen

import ic2_120.config.Ic2Config
import ic2_120.content.block.RubberSaplingBlock
import ic2_120.content.block.RubberLeavesBlock
import net.minecraft.block.Blocks
import net.minecraft.registry.tag.BlockTags
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.intprovider.ConstantIntProvider
import net.minecraft.world.gen.feature.Feature
import net.minecraft.world.gen.feature.TreeFeature
import net.minecraft.world.gen.feature.TreeFeatureConfig
import net.minecraft.world.gen.feature.util.FeatureContext
import net.minecraft.world.gen.trunk.StraightTrunkPlacer
import org.slf4j.LoggerFactory
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicInteger

/**
 * 橡胶树生成前额外检查周围是否已有其他树的树干/树叶，避免与现有树冠重叠。
 * 若当前落点不合适，会在同区块内重试若干候选位置，尽量不降低整体生成量。
 */
class RubberTreeFeature : Feature<TreeFeatureConfig>(TreeFeatureConfig.CODEC) {

    override fun generate(context: FeatureContext<TreeFeatureConfig>): Boolean {
        val runtimeContext = FeatureContext(
            context.feature,
            context.world,
            context.generator,
            context.random,
            context.origin,
            buildRuntimeConfig(context.config)
        )

        // 树苗生长保持原版行为，避免为了世界生成避让逻辑把玩家种下的树“挪位”或删掉周围树木。
        if (isSaplingGrowth(runtimeContext)) {
            val saplingResult = Feature.TREE.generate(runtimeContext)
            if (SAPLING_LOG_COUNTER.getAndIncrement() < FEATURE_LOG_LIMIT) {
                logger.info("Rubber tree sapling growth at origin={} result={}", runtimeContext.origin, saplingResult)
            }
            return saplingResult
        }

        val origin = runtimeContext.origin
        val overlappingTreeBlocks = findOverlappingTreeBlocks(runtimeContext, origin) ?: return false

        // 自然世界生成时，只在原始落点处理重叠树木：
        // 被树挡住就替换；不是树挡住就直接放弃，不再改落点重试。
        overlappingTreeBlocks.forEach { pos ->
            setBlockState(runtimeContext.world, pos, Blocks.AIR.defaultState)
        }

        if (!Feature.TREE.generate(runtimeContext)) {
            if (FEATURE_FAILURE_LOG_COUNTER.getAndIncrement() < FEATURE_LOG_LIMIT) {
                logger.info(
                    "Rubber tree feature generation failed at origin={} after clearing {} overlapping tree blocks",
                    origin,
                    overlappingTreeBlocks.size
                )
            }
            return false
        }

        cleanupOrphanLeaves(runtimeContext, origin)
        if (FEATURE_SUCCESS_LOG_COUNTER.getAndIncrement() < FEATURE_LOG_LIMIT) {
            logger.info(
                "Rubber tree feature generation succeeded at origin={} clearedOverlaps={}",
                origin,
                overlappingTreeBlocks.size
            )
        }
        return true
    }

    private fun isSaplingGrowth(context: FeatureContext<TreeFeatureConfig>): Boolean =
        context.world.getBlockState(context.origin).block is RubberSaplingBlock

    private fun findOverlappingTreeBlocks(context: FeatureContext<TreeFeatureConfig>, origin: BlockPos): List<BlockPos>? {
        val world = context.world
        val overlappingTreeBlocks = mutableListOf<BlockPos>()

        // 允许覆盖已有树木，但仍然拒绝非树类硬障碍，避免橡胶树插进地形或其他结构里。
        for (x in -CLEARANCE_RADIUS..CLEARANCE_RADIUS) {
            for (z in -CLEARANCE_RADIUS..CLEARANCE_RADIUS) {
                // 不检查 origin 同层的周边地表，避免把草方块/泥土误判成“阻挡树生成”的硬障碍。
                // 这里真正需要保证的是树干和树冠将要占用的上方空间可用。
                for (y in 1..CLEARANCE_HEIGHT) {
                    val pos = origin.add(x, y, z)
                    val state = world.getBlockState(pos)
                    if (state.isIn(BlockTags.LOGS) || state.isIn(BlockTags.LEAVES)) {
                        overlappingTreeBlocks += pos.toImmutable()
                        continue
                    }
                    // 与原版 TreeFeature 的 getTopPosition 保持一致：
                    // 配置里 ignore_vines=true 时，藤蔓不应阻止橡胶树生成。
                    if (context.config.ignoreVines && state.isOf(Blocks.VINE)) {
                        continue
                    }
                    if (!TreeFeature.canReplace(world, pos)) {
                        if (CLEARANCE_BLOCK_LOG_COUNTER.getAndIncrement() < FEATURE_LOG_LIMIT) {
                            logger.info(
                                "Rubber tree feature blocked at origin={} by non-replaceable state={} at pos={}",
                                origin,
                                state,
                                pos
                            )
                        }
                        return null
                    }
                }
            }
        }

        return overlappingTreeBlocks
    }

    private fun cleanupOrphanLeaves(context: FeatureContext<TreeFeatureConfig>, origin: BlockPos) {
        val world = context.world

        for (x in -LEAF_CLEANUP_RADIUS..LEAF_CLEANUP_RADIUS) {
            for (z in -LEAF_CLEANUP_RADIUS..LEAF_CLEANUP_RADIUS) {
                for (y in LEAF_CLEANUP_MIN_Y..LEAF_CLEANUP_MAX_Y) {
                    val pos = origin.add(x, y, z)
                    val state = world.getBlockState(pos)
                    if (!state.isIn(BlockTags.LEAVES)) continue
                    // 这里只清理被替换旧树遗留下来的叶子，不碰新生成的橡胶树叶，
                    // 否则会把橡胶树顶部那几片单独叶子误判成残叶删掉。
                    if (state.block is RubberLeavesBlock) continue
                    if (hasSupportingLog(world, pos)) continue
                    setBlockState(world, pos, Blocks.AIR.defaultState)
                }
            }
        }
    }

    // 按叶子衰减的思路做一次局部搜索：只有还能经由叶子链在 6 格内连到原木的树叶才保留。
    private fun hasSupportingLog(world: net.minecraft.world.StructureWorldAccess, start: BlockPos): Boolean {
        val queue = ArrayDeque<Pair<BlockPos, Int>>()
        val visited = HashSet<BlockPos>()

        queue.add(start.toImmutable() to 0)
        visited.add(start.toImmutable())

        while (queue.isNotEmpty()) {
            val (pos, depth) = queue.removeFirst()
            if (depth >= LEAF_SUPPORT_DISTANCE) continue

            for (direction in Direction.values()) {
                val nextPos = pos.offset(direction).toImmutable()
                if (!visited.add(nextPos)) continue

                val nextState = world.getBlockState(nextPos)
                if (nextState.isIn(BlockTags.LOGS)) {
                    return true
                }
                if (nextState.isIn(BlockTags.LEAVES)) {
                    queue.add(nextPos to (depth + 1))
                }
            }
        }

        return false
    }

    /**
     * JSON 里的 configured_feature 继续作为基底存在，但真正参与生成的数值在这里由配置覆盖。
     * 这样既保留 data-driven 注册入口，又能让树高、树冠等参数跟随 Ic2Config。
     */
    private fun buildRuntimeConfig(baseConfig: TreeFeatureConfig): TreeFeatureConfig {
        val config = Ic2Config.current.worldgen.rubberTree.normalized()
        val builder = TreeFeatureConfig.Builder(
            baseConfig.trunkProvider,
            StraightTrunkPlacer(config.baseHeight, config.heightRandA, config.heightRandB),
            baseConfig.foliageProvider,
            RubberTreeFoliagePlacer(
                ConstantIntProvider.create(config.foliageRadius),
                ConstantIntProvider.create(config.foliageOffset),
                config.foliageHeight
            ),
            baseConfig.rootPlacer,
            baseConfig.minimumSize
        )
            .dirtProvider(baseConfig.dirtProvider)
            .decorators(baseConfig.decorators)

        if (config.ignoreVines) {
            builder.ignoreVines()
        }
        if (config.forceDirt) {
            builder.forceDirt()
        }

        return builder.build()
    }

    companion object {
        private val logger = LoggerFactory.getLogger("ic2_120.rubber_tree.feature")
        // 当前橡胶树树冠半径为 2，这里额外留 1 格缓冲，避免与其他树冠/树干贴脸生成。
        private const val FEATURE_LOG_LIMIT = 64
        private const val CLEARANCE_RADIUS = 3
        private const val CLEARANCE_HEIGHT = 12
        private const val LEAF_SUPPORT_DISTANCE = 6
        private const val LEAF_CLEANUP_RADIUS = CLEARANCE_RADIUS + LEAF_SUPPORT_DISTANCE
        private const val LEAF_CLEANUP_MIN_Y = -2
        private const val LEAF_CLEANUP_MAX_Y = CLEARANCE_HEIGHT + 4
        private val SAPLING_LOG_COUNTER = AtomicInteger()
        private val CLEARANCE_BLOCK_LOG_COUNTER = AtomicInteger()
        private val FEATURE_FAILURE_LOG_COUNTER = AtomicInteger()
        private val FEATURE_SUCCESS_LOG_COUNTER = AtomicInteger()
    }
}
