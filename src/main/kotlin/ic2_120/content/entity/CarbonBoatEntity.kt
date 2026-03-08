package ic2_120.content.entity

import net.minecraft.entity.EntityType
import net.minecraft.world.World

/** 碳纤维船实体 */
class CarbonBoatEntity(
    entityType: EntityType<out CarbonBoatEntity>,
    world: World
) : Ic2BoatEntity(entityType, world)
