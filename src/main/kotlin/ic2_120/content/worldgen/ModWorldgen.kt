package ic2_120.content.worldgen

import ic2_120.Ic2_120
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import net.minecraft.world.gen.foliage.FoliagePlacerType

/**
 * 模组世界生成相关注册（自定义 FoliagePlacerType 等）。
 */
object ModWorldgen {

    val RUBBER_TREE_FOLIAGE_PLACER_TYPE: FoliagePlacerType<RubberTreeFoliagePlacer> =
        Registry.register(
            Registries.FOLIAGE_PLACER_TYPE,
            Identifier(Ic2_120.MOD_ID, "rubber_tree_foliage_placer"),
            FoliagePlacerType(RubberTreeFoliagePlacer.CODEC)
        )
}
