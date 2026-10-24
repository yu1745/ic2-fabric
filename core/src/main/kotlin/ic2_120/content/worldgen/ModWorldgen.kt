package ic2_120.content.worldgen

import ic2_120.Ic2_120
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType
import net.minecraft.world.level.levelgen.placement.PlacementModifierType
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType

/**
 * 模组世界生成相关注册（自定义 FoliagePlacerType 等）。
 */
object ModWorldgen {

    val RUBBER_TREE_FEATURE: Feature<TreeConfiguration> =
        Registry.register(
            BuiltInRegistries.FEATURE,
            ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "rubber_tree_feature"),
            RubberTreeFeature()
        )

    val RUBBER_TREE_FOLIAGE_PLACER_TYPE: FoliagePlacerType<RubberTreeFoliagePlacer> =
        Registry.register(
            BuiltInRegistries.FOLIAGE_PLACER_TYPE,
            ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "rubber_tree_foliage_placer"),
            FoliagePlacerType(RubberTreeFoliagePlacer.CODEC)
        )

    val RUBBER_HOLE_TREE_DECORATOR_TYPE: TreeDecoratorType<RubberHoleTreeDecorator> =
        Registry.register(
            BuiltInRegistries.TREE_DECORATOR_TYPE,
            ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "rubber_hole_tree_decorator"),
            TreeDecoratorType(RubberHoleTreeDecorator.CODEC)
        )

    val RUBBER_TREE_CONFIG_PLACEMENT_MODIFIER_TYPE: PlacementModifierType<RubberTreeConfigPlacementModifier> =
        Registry.register(
            BuiltInRegistries.PLACEMENT_MODIFIER_TYPE,
            ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "rubber_tree_config_placement"),
            // MapCodec 内联构造：不依赖 RubberTreePlacementModifiers.CODEC 的具体类型（Codec/MapCodec 迁移中）
            PlacementModifierType { com.mojang.serialization.MapCodec.unit { RubberTreeConfigPlacementModifier() } }
        )

    val RUBBER_TREE_CONFIG_WATER_DEPTH_FILTER_TYPE: PlacementModifierType<RubberTreeConfigWaterDepthFilterPlacementModifier> =
        Registry.register(
            BuiltInRegistries.PLACEMENT_MODIFIER_TYPE,
            ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "rubber_tree_config_water_depth_filter"),
            PlacementModifierType { com.mojang.serialization.MapCodec.unit { RubberTreeConfigWaterDepthFilterPlacementModifier() } }
        )
}
