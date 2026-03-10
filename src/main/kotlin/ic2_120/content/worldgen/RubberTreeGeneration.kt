package ic2_120.content.worldgen

import ic2_120.Ic2_120
import net.fabricmc.fabric.api.biome.v1.BiomeModifications
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.Identifier
import net.minecraft.world.gen.GenerationStep

/**
 * 橡胶树世界生成。
 * ConfiguredFeature 和 PlacedFeature 通过 data/ic2_120/worldgen/ 下的 JSON 定义。
 */
object RubberTreeGeneration {

    val RUBBER_TREE_PLACED_KEY: RegistryKey<net.minecraft.world.gen.feature.PlacedFeature> =
        RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier(Ic2_120.MOD_ID, "rubber_tree"))

    fun register() {
        // 确保自定义 FoliagePlacerType 已注册（供 JSON 中 rubber_tree 配置引用）
        ModWorldgen.RUBBER_TREE_FOLIAGE_PLACER_TYPE
        BiomeModifications.addFeature(
            BiomeSelectors.foundInOverworld(),
            GenerationStep.Feature.VEGETAL_DECORATION,
            RUBBER_TREE_PLACED_KEY
        )
    }
}
