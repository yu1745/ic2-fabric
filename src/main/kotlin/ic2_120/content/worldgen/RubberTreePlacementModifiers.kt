package ic2_120.content.worldgen

import com.mojang.serialization.Codec
import ic2_120.config.Ic2Config
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.random.Random
import net.minecraft.world.Heightmap
import net.minecraft.world.gen.feature.FeaturePlacementContext
import net.minecraft.world.gen.placementmodifier.AbstractConditionalPlacementModifier
import net.minecraft.world.gen.placementmodifier.PlacementModifier
import net.minecraft.world.gen.placementmodifier.PlacementModifierType
import org.slf4j.LoggerFactory
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Stream

/**
 * 用配置文件控制每区块的橡胶树尝试次数和稀有度，
 * 这样 placed_feature JSON 只保留 placement 链路入口，具体数值由 Ic2Config 决定。
 */
class RubberTreeConfigPlacementModifier : PlacementModifier() {

    override fun getPositions(
        context: FeaturePlacementContext,
        random: Random,
        pos: BlockPos
    ): Stream<BlockPos> {
        val config = Ic2Config.current.worldgen.rubberTree.normalized()
        if (!config.enabled || config.countPerChunk <= 0) {
            if (PLACEMENT_SKIP_LOG_COUNTER.getAndIncrement() < PLACEMENT_LOG_LIMIT) {
                logger.info(
                    "Rubber tree placement skipped at origin={} because enabled={} countPerChunk={}",
                    pos,
                    config.enabled,
                    config.countPerChunk
                )
            }
            return Stream.empty()
        }

        val positions = ArrayList<BlockPos>(config.countPerChunk)
        val sampledRolls = ArrayList<String>(minOf(config.countPerChunk, MAX_LOGGED_ROLLS))
        val passThreshold = 1.0f / config.rarityChance.toFloat()
        repeat(config.countPerChunk) {
            val roll = random.nextFloat()
            if (sampledRolls.size < MAX_LOGGED_ROLLS) {
                sampledRolls += "%.5f".format(java.util.Locale.ROOT, roll)
            }
            if (roll < passThreshold) {
                positions += pos
            }
        }

        if (PLACEMENT_RESULT_LOG_COUNTER.getAndIncrement() < PLACEMENT_LOG_LIMIT) {
            if (positions.isEmpty()) {
                logger.info(
                    "Rubber tree placement evaluated at origin={}: attempts={}, passed=0, reason=all_random_rolls_failed, rarityChance={}, threshold<{}, sampledRolls=[{}]",
                    pos,
                    config.countPerChunk,
                    config.rarityChance,
                    "%.5f".format(java.util.Locale.ROOT, passThreshold),
                    sampledRolls.joinToString(", ")
                )
            } else {
                logger.info(
                    "Rubber tree placement evaluated at origin={}: attempts={}, passed={}, rarityChance={}, threshold<{}, sampledRolls=[{}]",
                    pos,
                    config.countPerChunk,
                    positions.size,
                    config.rarityChance,
                    "%.5f".format(java.util.Locale.ROOT, passThreshold),
                    sampledRolls.joinToString(", ")
                )
            }
        }
        return positions.stream()
    }

    override fun getType(): PlacementModifierType<*> =
        ModWorldgen.RUBBER_TREE_CONFIG_PLACEMENT_MODIFIER_TYPE

    companion object {
        private val logger = LoggerFactory.getLogger("ic2_120.rubber_tree.placement")
        private const val PLACEMENT_LOG_LIMIT = 64
        private const val MAX_LOGGED_ROLLS = 8
        private val PLACEMENT_SKIP_LOG_COUNTER = AtomicInteger()
        private val PLACEMENT_RESULT_LOG_COUNTER = AtomicInteger()

        val CODEC: Codec<RubberTreeConfigPlacementModifier> =
            Codec.unit(::RubberTreeConfigPlacementModifier)
    }
}

/**
 * 用配置文件控制橡胶树允许生成时的地表水深，避免 placed_feature JSON 写死后无法调整。
 */
class RubberTreeConfigWaterDepthFilterPlacementModifier : AbstractConditionalPlacementModifier() {

    override fun shouldPlace(
        context: FeaturePlacementContext,
        random: Random,
        pos: BlockPos
    ): Boolean {
        val config = Ic2Config.current.worldgen.rubberTree.normalized()
        if (!config.enabled) {
            if (WATER_DEPTH_DISABLED_LOG_COUNTER.getAndIncrement() < WATER_DEPTH_LOG_LIMIT) {
                logger.info("Rubber tree water-depth filter blocked at pos={} because config is disabled", pos)
            }
            return false
        }

        val oceanFloorY = context.getTopY(Heightmap.Type.OCEAN_FLOOR, pos.x, pos.z)
        val worldSurfaceY = context.getTopY(Heightmap.Type.WORLD_SURFACE, pos.x, pos.z)
        val waterDepth = worldSurfaceY - oceanFloorY
        val allowed = waterDepth <= config.maxWaterDepth

        if (WATER_DEPTH_RESULT_LOG_COUNTER.getAndIncrement() < WATER_DEPTH_LOG_LIMIT) {
            logger.info(
                "Rubber tree water-depth filter at pos={}: waterDepth={}, maxWaterDepth={}, allowed={}",
                pos,
                waterDepth,
                config.maxWaterDepth,
                allowed
            )
        }

        return allowed
    }

    override fun getType(): PlacementModifierType<*> =
        ModWorldgen.RUBBER_TREE_CONFIG_WATER_DEPTH_FILTER_TYPE

    companion object {
        private val logger = LoggerFactory.getLogger("ic2_120.rubber_tree.water_depth")
        private const val WATER_DEPTH_LOG_LIMIT = 64
        private val WATER_DEPTH_DISABLED_LOG_COUNTER = AtomicInteger()
        private val WATER_DEPTH_RESULT_LOG_COUNTER = AtomicInteger()

        val CODEC: Codec<RubberTreeConfigWaterDepthFilterPlacementModifier> =
            Codec.unit(::RubberTreeConfigWaterDepthFilterPlacementModifier)
    }
}
