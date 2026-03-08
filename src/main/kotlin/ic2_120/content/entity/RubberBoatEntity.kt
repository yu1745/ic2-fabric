package ic2_120.content.entity

import net.minecraft.entity.EntityType
import net.minecraft.world.World

/** 橡皮艇实体 */
class RubberBoatEntity(
    entityType: EntityType<out RubberBoatEntity>,
    world: World
) : Ic2BoatEntity(entityType, world)
