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
import java.util.ArrayList
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
            return Stream.empty()
        }

        val positions = ArrayList<BlockPos>(config.countPerChunk)
        repeat(config.countPerChunk) {
            if (random.nextFloat() < 1.0f / config.rarityChance.toFloat()) {
                positions += pos
            }
        }
        return positions.stream()
    }

    override fun getType(): PlacementModifierType<*> =
        ModWorldgen.RUBBER_TREE_CONFIG_PLACEMENT_MODIFIER_TYPE

    companion object {
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
            return false
        }

        val oceanFloorY = context.getTopY(Heightmap.Type.OCEAN_FLOOR, pos.x, pos.z)
        val worldSurfaceY = context.getTopY(Heightmap.Type.WORLD_SURFACE, pos.x, pos.z)
        return worldSurfaceY - oceanFloorY <= config.maxWaterDepth
    }

    override fun getType(): PlacementModifierType<*> =
        ModWorldgen.RUBBER_TREE_CONFIG_WATER_DEPTH_FILTER_TYPE

    companion object {
        val CODEC: Codec<RubberTreeConfigWaterDepthFilterPlacementModifier> =
            Codec.unit(::RubberTreeConfigWaterDepthFilterPlacementModifier)
    }
}
