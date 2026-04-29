package ic2_120.content.worldgen

import ic2_120.Ic2_120
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import net.minecraft.world.gen.feature.Feature
import net.minecraft.world.gen.feature.TreeFeatureConfig
import net.minecraft.world.gen.foliage.FoliagePlacerType
import net.minecraft.world.gen.placementmodifier.PlacementModifierType
import net.minecraft.world.gen.treedecorator.TreeDecoratorType

/**
 * 模组世界生成相关注册（自定义 FoliagePlacerType 等）。
 */
object ModWorldgen {

    val RUBBER_TREE_FEATURE: Feature<TreeFeatureConfig> =
        Registry.register(
            Registries.FEATURE,
            Identifier(Ic2_120.MOD_ID, "rubber_tree_feature"),
            RubberTreeFeature()
        )

    val RUBBER_TREE_FOLIAGE_PLACER_TYPE: FoliagePlacerType<RubberTreeFoliagePlacer> =
        Registry.register(
            Registries.FOLIAGE_PLACER_TYPE,
            Identifier(Ic2_120.MOD_ID, "rubber_tree_foliage_placer"),
            FoliagePlacerType(RubberTreeFoliagePlacer.CODEC)
        )

    val RUBBER_HOLE_TREE_DECORATOR_TYPE: TreeDecoratorType<RubberHoleTreeDecorator> =
        Registry.register(
            Registries.TREE_DECORATOR_TYPE,
            Identifier(Ic2_120.MOD_ID, "rubber_hole_tree_decorator"),
            TreeDecoratorType(RubberHoleTreeDecorator.CODEC)
        )

    val RUBBER_TREE_CONFIG_PLACEMENT_MODIFIER_TYPE: PlacementModifierType<RubberTreeConfigPlacementModifier> =
        Registry.register(
            Registries.PLACEMENT_MODIFIER_TYPE,
            Identifier(Ic2_120.MOD_ID, "rubber_tree_config_placement"),
            PlacementModifierType { RubberTreeConfigPlacementModifier.CODEC }
        )

    val RUBBER_TREE_CONFIG_WATER_DEPTH_FILTER_TYPE: PlacementModifierType<RubberTreeConfigWaterDepthFilterPlacementModifier> =
        Registry.register(
            Registries.PLACEMENT_MODIFIER_TYPE,
            Identifier(Ic2_120.MOD_ID, "rubber_tree_config_water_depth_filter"),
            PlacementModifierType { RubberTreeConfigWaterDepthFilterPlacementModifier.CODEC }
        )
}
