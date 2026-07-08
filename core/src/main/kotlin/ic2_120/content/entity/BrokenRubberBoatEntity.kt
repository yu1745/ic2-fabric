package ic2_120.content.entity

import ic2_120.registry.annotation.ModEntity
import net.minecraft.entity.EntityType
import net.minecraft.world.World

/** 破损的橡皮艇实体 */
@ModEntity(name = "broken_rubber_boat", width = 1.375f, height = 0.5625f, maxTrackingRange = 10, dataFixerType = "minecraft:boat")
class BrokenRubberBoatEntity(
    entityType: EntityType<out BrokenRubberBoatEntity>,
    world: World
) : Ic2BoatEntity(entityType, world)
