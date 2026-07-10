package ic2_120.content.worldgen

import ic2_120.Ic2_120
import ic2_120.config.Ic2Config
import net.fabricmc.fabric.api.biome.v1.BiomeModifications
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.Identifier
import net.minecraft.world.gen.GenerationStep
import org.slf4j.LoggerFactory

/**
 * 橡胶树世界生成。
 *
 * 群系选择由 biome tag `#ic2_120:generates_rubber_trees` 控制（引用 `rubber_tree_forest` ∪ `rubber_tree_swamp`），
 * tag JSON 里引用 vanilla `#is_forest`/`#is_taiga`/`#is_jungle` 自动覆盖高版本新增的等价群系。
 * 放置次数/稀有度/水深仍由 Ic2Config 控制。
 */
object RubberTreeGeneration {

    private val logger = LoggerFactory.getLogger("ic2_120.rubber_tree")

    val RUBBER_TREE_PLACED_KEY: RegistryKey<net.minecraft.world.gen.feature.PlacedFeature> =
        RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier(Ic2_120.MOD_ID, "rubber_tree"))

    fun register() {
        val config = Ic2Config.current.worldgen.rubberTree.normalized()
        // 确保自定义 FoliagePlacerType 已注册（供 JSON 中 rubber_tree 配置引用）
        ModWorldgen.RUBBER_TREE_FEATURE
        ModWorldgen.RUBBER_TREE_FOLIAGE_PLACER_TYPE
        ModWorldgen.RUBBER_HOLE_TREE_DECORATOR_TYPE
        ModWorldgen.RUBBER_TREE_CONFIG_PLACEMENT_MODIFIER_TYPE
        ModWorldgen.RUBBER_TREE_CONFIG_WATER_DEPTH_FILTER_TYPE

        if (!config.enabled) {
            logger.info("Rubber tree worldgen disabled by config")
            return
        }

        BiomeModifications.addFeature(
            BiomeSelectors.tag(RubberTreeBiomeTags.GENERATES_RUBBER_TREES),
            GenerationStep.Feature.VEGETAL_DECORATION,
            RUBBER_TREE_PLACED_KEY
        )
    }
}
