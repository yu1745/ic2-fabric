package ic2_120.content.entity

import net.minecraft.entity.EntityType
import net.minecraft.world.World

/** 电动艇实体 */
class ElectricBoatEntity(
    entityType: EntityType<out ElectricBoatEntity>,
    world: World
) : Ic2BoatEntity(entityType, world)
