package ic2_120.content.entity

import net.minecraft.entity.EntityType
import net.minecraft.world.World

/** 橡皮艇实体 */
class RubberBoatEntity(
    entityType: EntityType<out RubberBoatEntity>,
    world: World
) : Ic2BoatEntity(entityType, world) {
    override val maxHorizontalSpeed: Double = 0.36
    override val waterDragMultiplier: Double = 1.02
    override val damageTakenMultiplier: Float = 0.8f
}
