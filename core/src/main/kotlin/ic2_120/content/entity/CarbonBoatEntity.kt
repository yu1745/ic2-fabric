package ic2_120.content.entity

import net.minecraft.entity.EntityType
import net.minecraft.world.World

/** 碳纤维轻艇实体 */
class CarbonBoatEntity(
    entityType: EntityType<out CarbonBoatEntity>,
    world: World
) : Ic2BoatEntity(entityType, world) {
    override val maxHorizontalSpeed: Double = 0.50
    override val waterDragMultiplier: Double = 0.99
    override val damageTakenMultiplier: Float = 1.15f
}
