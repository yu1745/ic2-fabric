package ic2_120.content.worldgen

import ic2_120.config.Ic2Config
import ic2_120.config.RubberTreeWorldgenConfig
import ic2_120.content.block.RubberFaceState
import ic2_120.content.block.RubberLeavesBlock
import ic2_120.content.block.RubberLogBlock
import ic2_120.content.block.RubberSaplingBlock
import ic2_120.content.block.RubberSaplingGenerator
import ic2_120.content.block.RubberTreeSaplingDrop
import ic2_120.registry.instance
import net.minecraft.block.Blocks
import net.minecraft.block.Block
import net.minecraft.block.LeavesBlock
import net.minecraft.registry.tag.BlockTags
import net.minecraft.state.property.Properties
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.random.Random
import net.minecraft.world.BlockView
import net.minecraft.world.StructureWorldAccess
import net.minecraft.world.gen.feature.Feature
import net.minecraft.world.gen.feature.TreeFeature
import net.minecraft.world.gen.feature.TreeFeatureConfig
import net.minecraft.world.gen.feature.util.FeatureContext
import java.util.ArrayDeque
import kotlin.math.abs

/**
 * 橡胶树生成前额外检查周围是否已有其他树的树干/树叶，避免与现有树冠重叠。
 * 若当前落点不合适，会在同区块内重试若干候选位置，尽量不降低整体生成量。
 */
class RubberTreeFeature : Feature<TreeFeatureConfig>(TreeFeatureConfig.CODEC) {

    override fun generate(context: FeatureContext<TreeFeatureConfig>): Boolean {
        val world = context.world
        val origin = context.origin
        val random = context.random

        // 树苗生长保持原版行为，避免为了世界生成避让逻辑把玩家种下的树"挪位"或删掉周围树木。
        if (isSaplingGrowth(context)) {
            if (!hasClearSaplingGrowthArea(context, origin)) {
                return false
            }
            return growRubberTree(world, origin, random)
        }

        // 自然世界生成绝不替换已有树木；若与其他树重叠，直接放弃本次生成。
        if (!hasClearNaturalGenerationArea(context, origin)) return false

        if (!growRubberTree(world, origin, random)) {
            return false
        }

        return true
    }

   /**
     * 对每个叶子做 BFS 计算到最近原木的步距，并用 FORCE_STATE 直接写入 DISTANCE。
     * 与 vanilla TreeFeature 的做法一致，避免叶子因默认 DISTANCE=7 被随机刻枯萎。
     */
    private fun updateLeafDistances(
        world: StructureWorldAccess,
        logPositions: List<BlockPos>,
        leafPositions: List<BlockPos>
    ) {
        val queue = ArrayDeque<Pair<BlockPos, Int>>()
        val visited = HashSet<BlockPos>()

        for (logPos in logPositions) {
            queue.add(logPos.toImmutable() to 0)
            visited.add(logPos.toImmutable())
        }

        while (queue.isNotEmpty()) {
            val (pos, dist) = queue.removeFirst()
            if (dist >= 7) continue

            for (dir in Direction.values()) {
                val nextPos = pos.offset(dir).toImmutable()
                if (!visited.add(nextPos)) continue

                val state = world.getBlockState(nextPos)
                if (state.isIn(BlockTags.LOGS)) continue
                if (!state.contains(LeavesBlock.DISTANCE)) continue

                val newDist = dist + 1
                world.setBlockState(nextPos, state.with(LeavesBlock.DISTANCE, newDist.coerceAtMost(7)), Block.NOTIFY_ALL or Block.FORCE_STATE)
                queue.add(nextPos to newDist)
            }
        }
    }

    /**
    * 树苗生长检测。
     *
     * 之所以需要 ThreadLocal（[RubberSaplingGenerator.SAPLING_GROWTH_FLAG]）：
     * vanilla [SaplingGenerator.generate] 会在调用 feature 之前把树苗替换为流体状态
     * （无水时 = AIR），否则 vanilla [TreeFeature.getTopPosition] 会在 i=0 处看到 sapling
     * 方块并直接返回 -2，导致树干放不下。问题是树苗一旦被替换，origin.block 就再也
     * 不是 [RubberSaplingBlock] 了，无法再用 `getBlockState(origin).block is X` 区分
     * "树苗生长"和"世界生成"两种调用来源。
     *
     * [RubberSaplingGenerator] 在调用 feature 前后会 set/clear 这个 ThreadLocal 标记，
     * 本方法先看 ThreadLocal、再 fallback 到方块检查（兼容旧的调用路径）。
     */
    private fun isSaplingGrowth(context: FeatureContext<TreeFeatureConfig>): Boolean {
        if (RubberSaplingGenerator.SAPLING_GROWTH_FLAG.get() == true) return true
        return context.world.getBlockState(context.origin).block is RubberSaplingBlock
    }

    private fun hasClearSaplingGrowthArea(context: FeatureContext<TreeFeatureConfig>, origin: BlockPos): Boolean =
        RubberSaplingGrowth.hasClearArea(context.world, origin)

    private fun hasClearNaturalGenerationArea(context: FeatureContext<TreeFeatureConfig>, origin: BlockPos): Boolean {
        val world = context.world
        val ignoreVines = Ic2Config.current.worldgen.rubberTree.normalized().ignoreVines

        // 树木和其他硬障碍都阻止本次生成，避免破坏原版树木或结构。
        for (x in -CLEARANCE_RADIUS..CLEARANCE_RADIUS) {
            for (z in -CLEARANCE_RADIUS..CLEARANCE_RADIUS) {
                // 不检查 origin 同层的周边地表，避免把草方块/泥土误判成“阻挡树生成”的硬障碍。
                // 这里真正需要保证的是树干和树冠将要占用的上方空间可用。
                for (y in 1..CLEARANCE_HEIGHT) {
                    val pos = origin.add(x, y, z)
                    val state = world.getBlockState(pos)
                    if (state.isIn(BlockTags.LOGS) || state.isIn(BlockTags.LEAVES)) {
                        return false
                    }
                    // 与原版 TreeFeature 的 getTopPosition 保持一致：
                    // 配置里 ignore_vines=true 时，藤蔓不应阻止橡胶树生成。    
                    if (ignoreVines && state.isOf(Blocks.VINE)) {
                        continue
                    }
                    if (!TreeFeature.canReplace(world, pos)) {
                        return false
                    }
                }
            }
        }

        return true
    }

    /**
     * 橡胶树生成核心逻辑，完全对齐 Forge 1.12 WorldGenRubTree.grow()。
     *
     * 与 Forge 一致的数值：
     * - 最大高度 8，实际高度 = 可用空间 - randInt(可用空间/2+1)
     * - 橡胶孔：树级递减概率，初始 25%，每出一个孔 -10%
     * - 树叶：十字形 5×5，中心 3×3 必放，十字臂按 1/chance 概率放置
     * - 顶部中心列额外叶子
     */
    private fun growRubberTree(
        world: StructureWorldAccess,
        origin: BlockPos,
        random: Random
    ): Boolean {
        // 旧版逻辑：从 origin 上方扫描连续空气，上限 MAX_TREE_HEIGHT。
        var height = 0
        var scanPos = origin.up()
        while (world.getBlockState(scanPos).isAir && height < MAX_TREE_HEIGHT) {
            scanPos = scanPos.up()
            height++
        }
        if (height < 2) return false

        // Forge: height2 = height - randInt(height/2+1)
        val height2 = height - random.nextInt(height / 2 + 1)
        if (height2 < 1) return false

       val rubberLog = RubberLogBlock::class.instance()
       val rubberLeaves = RubberLeavesBlock::class.instance()
       val leavesState = rubberLeaves.defaultState
        val leafPositions = mutableListOf<BlockPos>()
        val logPositions = mutableListOf<BlockPos>()
       val baseLogState = rubberLog.defaultState
           .with(Properties.AXIS, Direction.Axis.Y)
           .with(RubberLogBlock.NATURAL, true)

        // 树级橡胶孔概率（对齐 Forge 1.12：初始 25%，每出一个 -10%）
        var treeholechance = 25
        val horizontalFaces = arrayOf(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)

        for (cHeight in 0 until height2) {
            val logPos = origin.up(cHeight)

            if (random.nextInt(100) <= treeholechance) {
                treeholechance -= 10
                val face = horizontalFaces[random.nextInt(4)]
                setBlockState(world, logPos, baseLogState.with(RubberLogBlock.propFor(face), RubberFaceState.WET))
           } else {
               setBlockState(world, logPos, baseLogState)
           }
            logPositions.add(logPos)

           // 树叶放置（对齐 Forge 1.12 十字形 5×5）
           if (height2 < 4 || (height2 < 7 && cHeight > 1) || cHeight > 2) {
               val chance = maxOf(1, cHeight + 4 - height2)
               for (dx in -2..2) {
                   for (dz in -2..2) {
                       val adx = abs(dx)
                       val adz = abs(dz)
                       if ((adx <= 1 && adz <= 1) ||
                           (adx <= 1 && random.nextInt(chance) == 0) ||
                           (adz <= 1 && random.nextInt(chance) == 0)
                       ) {
                           val leafPos = origin.add(dx, cHeight, dz)
                           if (world.getBlockState(leafPos).isAir) {
                               setBlockState(world, leafPos, leavesState)
                               leafPositions.add(leafPos.toImmutable())
                           }
                       }
                   }
               }
           }
        }

        // 顶部中心列额外叶子（对齐 Forge 1.12）
        val topLayers = height2 / 4 + random.nextInt(2)
        for (i in 0..topLayers) {
           val leafPos = origin.up(height2 + i)
           if (world.getBlockState(leafPos).isAir) {
               setBlockState(world, leafPos, leavesState)
                leafPositions.add(leafPos.toImmutable())
           }
       }

        // 按整棵树的数学期望，把树苗掉落标记绑定到实际生成的叶子上。
        val dropConfig = Ic2Config.current.worldgen.rubberTree.normalized()
        if (dropConfig.saplingGuaranteeEnabled && leafPositions.isNotEmpty()) {
            RubberTreeSaplingDrop.bindLeaves(world, leafPositions, random)
        }

        // 放完所有方块后手动设置树叶 DISTANCE（对齐 TreeFeature 的 BFS 距离计算）。
        // 不走 vanilla 邻居更新链——世界生成期间 scheduledTick 不保证及时执行，
        // 默认 DISTANCE=7 的叶子会被 randomTick 立即判定为"无支撑"而枯萎。
        updateLeafDistances(world, logPositions, leafPositions)

       return true
   }

   companion object {
        // 对齐 Forge 1.12 WorldGenRubTree.maxHeight
        private const val MAX_TREE_HEIGHT = 8
        // 当前橡胶树树冠半径为 2，这里额外留 1 格缓冲，避免与其他树冠/树干重叠。
        private const val CLEARANCE_RADIUS = 3
        private const val CLEARANCE_HEIGHT = 12
    }
}

/**
 * 橡胶树苗生长判定的共享逻辑，供世界生成 feature 与 Jade 提示复用，避免两处逻辑漂移。
 * 仅依赖方块视图，客户端 world 已同步周围方块，可直接调用。
 */
object RubberSaplingGrowth {

    /**
     * 树苗生长时净空检查的水平半径。
     * Forge 1.12 树叶半径 2（5×5），这里额外留 1 格缓冲 = 3。
     */
    fun clearanceRadius(config: RubberTreeWorldgenConfig): Int = 3

    /**
     * 树苗生长时净空检查的垂直高度。
     * Forge 1.12 最大树高 8 + 顶部叶子 2 = 10。
     */
    fun clearanceHeight(config: RubberTreeWorldgenConfig): Int = 10

    /**
     * 树苗上方 [clearanceRadius]×[clearanceHeight] 的盒子内是否全部可被树木替换。
     * 只有空气和 BlockTags#REPLACEABLE_BY_TREES 方块不会阻挡；任何固体方块
     * （原木、树叶、石头等）都会阻止树苗长成大树，返回 false。
     */
    fun hasClearArea(world: BlockView, origin: BlockPos): Boolean {
        val config = Ic2Config.current.worldgen.rubberTree.normalized()
        val radius = clearanceRadius(config)
        val height = clearanceHeight(config)

        for (x in -radius..radius) {
            for (z in -radius..radius) {
                for (y in 1..height) {
                    val state = world.getBlockState(origin.add(x, y, z))
                    if (!state.isAir && !state.isIn(BlockTags.REPLACEABLE_BY_TREES)) {
                        return false
                    }
                }
            }
        }

        return true
    }
}
