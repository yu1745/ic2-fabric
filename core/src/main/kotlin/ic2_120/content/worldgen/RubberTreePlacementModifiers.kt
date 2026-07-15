package ic2_120.content.worldgen

import com.mojang.serialization.Codec
import ic2_120.config.Ic2Config
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.random.Random
import net.minecraft.world.Heightmap
import net.minecraft.world.biome.Biome
import net.minecraft.world.gen.feature.FeaturePlacementContext
import net.minecraft.world.gen.placementmodifier.AbstractConditionalPlacementModifier
import net.minecraft.world.gen.placementmodifier.PlacementModifier
import net.minecraft.world.gen.placementmodifier.PlacementModifierType
import java.util.stream.Stream

/**
 * 橡胶树世界生成使用的 biome tag。
 *
 * 入口层 [RUBBER_TREE_FOREST_GENERATES] 控制哪些群系允许生成橡胶树（替代旧的 config biome 白名单）；
 * 概率层 [RUBBER_TREE_FOREST] / [RUBBER_TREE_SWAMP] 区分森林与沼泽做不同密度加权，
 * 对齐 Forge 1.12 BiomeDictionary.Type.FOREST / SWAMP 的分类。
 *
 * tag JSON 在 `data/ic2_120/tags/worldgen/biome/` 下，概率分类只引用 vanilla `#is_forest`/`#is_jungle`，
 * 对应 JADX 中的 `BiomeDictionary.Type.FOREST` / `Type.JUNGLE`。
 */
object RubberTreeBiomeTags {
    /** 入口层：所有允许生成橡胶树的群系（= forest ∪ swamp）。 */
    val GENERATES_RUBBER_TREES: TagKey<Biome> =
        TagKey.of(RegistryKeys.BIOME, Identifier("ic2_120", "generates_rubber_trees"))

    /** 概率层：森林/丛林类（Forge FOREST + JUNGLE）。 */
    val FOREST: TagKey<Biome> =
        TagKey.of(RegistryKeys.BIOME, Identifier("ic2_120", "rubber_tree_forest"))

    /** 概率层：沼泽类（Forge SWAMP）。 */
    val SWAMP: TagKey<Biome> =
        TagKey.of(RegistryKeys.BIOME, Identifier("ic2_120", "rubber_tree_swamp"))
}

/**
 * 每区块的橡胶树放置数量，完全对齐 Forge 1.12 Ic2WorldDecorator.genRubberTree 的概率模型。
 *
 * Forge 做法：
 * - 在区块内均匀采样 4 个 biome 点（每点取 chunk 内坐标 8 或 23）
 * - 每个采样点按 biome 类型加权累加 rubberTrees：
 *     SWAMP              → +randInt(10)+5   (5~14)
 *     FOREST 或 JUNGLE   → +randInt(5)+1    (1~5)
 *     其他               → +0
 * - rubberTrees2 = round(rubberTrees * treeDensityFactor) / 2
 * - 概率门：randInt(100) < rubberTrees2 才生成
 * - 生成棵数 = rubberTrees2（每次失败 -3 惩罚）
 */
class RubberTreeConfigPlacementModifier : PlacementModifier() {

    override fun getPositions(
        context: FeaturePlacementContext,
        random: Random,
        pos: BlockPos
    ): Stream<BlockPos> {
        val config = Ic2Config.current.worldgen.rubberTree.normalized()
        if (!config.enabled || config.treeDensityFactor <= 0f) {
            return Stream.empty()
        }

        val world = context.world

        // 对齐 Forge：在 chunk 内 4 个点采样 biome。
        // pos 是区块原点 (chunkX*16, 0, chunkZ*16)，采样偏移与 Forge 一致。
        val chunkX = pos.x
        val chunkZ = pos.z
        var rubberTrees = 0
        for (i in 0 until 4) {
            val sampleX = chunkX + 8 + ((i and 1) * 15)
            val sampleZ = chunkZ + 8 + (((i and 2) ushr 1) * 15)
            // JADX 原版使用 world.func_181545_F()，即世界海平面，而不是 Y=0。
            val biome = world.getBiome(BlockPos(sampleX, world.seaLevel, sampleZ))
            // 与 Forge 顺序一致：先查 SWAMP，再查 FOREST/JUNGLE（两个独立 if，非 else）
            if (biome.isIn(RubberTreeBiomeTags.SWAMP)) {
                rubberTrees += random.nextInt(10) + 5  // 5~14
            }
            if (biome.isIn(RubberTreeBiomeTags.FOREST)) {
                rubberTrees += random.nextInt(5) + 1   // 1~5
            }
        }

        // baseScale = treeDensityFactor（Forge general.ini 默认 1.0）
        val rubberTrees2 = Math.round(rubberTrees * config.treeDensityFactor) / 2
        if (rubberTrees2 <= 0) {
            return Stream.empty()
        }

        // 概率门：randInt(100) < rubberTrees2 才进入放置
        if (random.nextInt(100) >= rubberTrees2) {
            return Stream.empty()
        }

        // 生成 rubberTrees2 棵；失败惩罚 -3 在 feature 层面（feature.generate 返回 false）无法直接回传，
        // 但 vanilla 会对返回的每个 pos 独立调用 feature，所以这里返回 rubberTrees2 个 pos 即可。
        return Stream.generate { pos }.limit(rubberTrees2.toLong())
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
