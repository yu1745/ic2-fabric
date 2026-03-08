package ic2_120.content.entity

import net.minecraft.entity.EntityType
import net.minecraft.entity.vehicle.BoatEntity
import net.minecraft.world.World

/**
 * IC2 船实体基类，用于破损橡胶船与碳纤维船。
 * 行为与原版船一致，渲染由客户端根据实体类型区分贴图。
 */
abstract class Ic2BoatEntity(
    entityType: EntityType<out Ic2BoatEntity>,
    world: World
) : BoatEntity(entityType, world)
