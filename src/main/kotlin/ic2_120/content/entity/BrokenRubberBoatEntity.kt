package ic2_120.content.entity

import net.minecraft.entity.EntityType
import net.minecraft.world.World

/** 破损的橡胶船实体 */
class BrokenRubberBoatEntity(
    entityType: EntityType<out BrokenRubberBoatEntity>,
    world: World
) : Ic2BoatEntity(entityType, world)
