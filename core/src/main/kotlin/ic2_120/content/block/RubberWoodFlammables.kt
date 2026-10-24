package ic2_120.content.block

import ic2_120.registry.instance
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry
import net.minecraft.world.level.block.Blocks

/**
 * 橡胶木系列可燃注册。
 *
 * 数值与原版橡木系列对齐：
 * - log / wood（带皮/去皮）       燃烧 5、蔓延 5
 * - planks / slab / stairs / fence / fence_gate  燃烧 5、蔓延 20
 * - leaves（橡胶树叶）         燃烧 30、蔓延 60
 *
 * 见 [Blocks.OAK_LOG] / [Blocks.OAK_PLANKS] / [Blocks.OAK_LEAVES] 在 FireBlock 里的注册值。
 */
object RubberWoodFlammables {

    fun register() {
        val registry = FlammableBlockRegistry.getDefaultInstance()

        // 原木 / 木材（带皮）
        registry.add(RubberLogBlock::class.instance(), 5, 5)
        registry.add(RubberWood::class.instance(), 5, 5)
        // 去皮
        registry.add(StrippedRubberLogBlock::class.instance(), 5, 5)
        registry.add(StrippedRubberWoodBlock::class.instance(), 5, 5)
        // 板材 + 衍生
        registry.add(RubberPlanksBlock::class.instance(), 5, 20)
        registry.add(RubberSlabBlock::class.instance(), 5, 20)
        registry.add(RubberStairsBlock::class.instance(), 5, 20)
        registry.add(RubberFenceBlock::class.instance(), 5, 20)
        registry.add(RubberFenceGateBlock::class.instance(), 5, 20)
        // 树叶
        registry.add(RubberLeavesBlock::class.instance(), 30, 60)
    }
}
