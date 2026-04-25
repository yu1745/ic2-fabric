package ic2_120.content.worldgen

import ic2_120.Ic2_120
import ic2_120.config.Ic2Config
import net.fabricmc.fabric.api.biome.v1.BiomeModifications
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.Identifier
import net.minecraft.world.biome.Biome
import net.minecraft.world.gen.GenerationStep
import org.slf4j.LoggerFactory

/**
 * 橡胶树世界生成。
 * ConfiguredFeature 仍通过 data/ic2_120/worldgen/ 下的 JSON 提供基底，
 * 但放置次数/稀有度/水深与生物群系选择改为运行时读取 Ic2Config。
 */
object RubberTreeGeneration {

    private val logger = LoggerFactory.getLogger("ic2_120.rubber_tree")

    val RUBBER_TREE_PLACED_KEY: RegistryKey<net.minecraft.world.gen.feature.PlacedFeature> =
        RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(Ic2_120.MOD_ID, "rubber_tree"))

    fun register() {
        val config = Ic2Config.current.worldgen.rubberTree.normalized()
        ModWorldgen.RUBBER_TREE_FEATURE
        // 确保自定义 FoliagePlacerType 已注册（供 JSON 中 rubber_tree 配置引用）
        ModWorldgen.RUBBER_TREE_FOLIAGE_PLACER_TYPE
        ModWorldgen.RUBBER_HOLE_TREE_DECORATOR_TYPE
        ModWorldgen.RUBBER_TREE_CONFIG_PLACEMENT_MODIFIER_TYPE
        ModWorldgen.RUBBER_TREE_CONFIG_WATER_DEPTH_FILTER_TYPE

        if (!config.enabled) {
            logger.info("Rubber tree worldgen disabled by config")
            return
        }

        val biomeKeys = config.biomes.mapNotNull(::toBiomeKey)
        if (biomeKeys.isEmpty()) {
            logger.warn("Rubber tree worldgen skipped because no valid biomes were configured")
            return
        }

        BiomeModifications.addFeature(
            BiomeSelectors.includeByKey(*biomeKeys.toTypedArray()),
            GenerationStep.Feature.VEGETAL_DECORATION,
            RUBBER_TREE_PLACED_KEY
        )
    }

    private fun toBiomeKey(rawId: String): RegistryKey<Biome>? {
        val biomeId = Identifier.tryParse(rawId)
        if (biomeId == null) {
            logger.warn("Ignoring invalid rubber tree biome id in config: {}", rawId)
            return null
        }
        return RegistryKey.of(RegistryKeys.BIOME, biomeId)
    }
}
