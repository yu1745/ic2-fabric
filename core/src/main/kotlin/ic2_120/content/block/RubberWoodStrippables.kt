package ic2_120.content.block

import ic2_120.registry.instance
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry

/**
 * 橡胶木系列斧头去皮行为注册。
 *
 * 与原版一致：
 * - [RubberLogBlock] → [StrippedRubberLogBlock]
 * - [RubberWood]     → [StrippedRubberWoodBlock]
 *
 * StrippableBlockRegistry 要求方块带 `BlockStateProperties.AXIS`（RotatedPillarBlock 默认满足），
 * 剥皮时自动保留 AXIS，行为对齐 vanilla 橡木。
 */
object RubberWoodStrippables {

    fun register() {
        StrippableBlockRegistry.register(RubberLogBlock::class.instance(), StrippedRubberLogBlock::class.instance())
        StrippableBlockRegistry.register(RubberWood::class.instance(), StrippedRubberWoodBlock::class.instance())
    }
}
