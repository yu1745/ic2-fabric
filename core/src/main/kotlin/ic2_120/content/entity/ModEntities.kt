package ic2_120.content.entity

import net.minecraft.entity.EntityType
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

/**
 * 实体类型查找。
 * 实际注册由 @ModEntity 注解驱动，在 ClassScanner 中完成。
 * 这里仅在物品类首次访问时从注册表查找已注册的类型。
 */
object ModEntities {

    @Suppress("UNCHECKED_CAST")
    val BROKEN_RUBBER_BOAT: EntityType<BrokenRubberBoatEntity>
        get() = Registries.ENTITY_TYPE.get(Identifier("ic2_120", "broken_rubber_boat")) as EntityType<BrokenRubberBoatEntity>

    @Suppress("UNCHECKED_CAST")
    val CARBON_BOAT: EntityType<CarbonBoatEntity>
        get() = Registries.ENTITY_TYPE.get(Identifier("ic2_120", "carbon_boat")) as EntityType<CarbonBoatEntity>

    @Suppress("UNCHECKED_CAST")
    val RUBBER_BOAT: EntityType<RubberBoatEntity>
        get() = Registries.ENTITY_TYPE.get(Identifier("ic2_120", "rubber_boat")) as EntityType<RubberBoatEntity>

    @Suppress("UNCHECKED_CAST")
    val ELECTRIC_BOAT: EntityType<ElectricBoatEntity>
        get() = Registries.ENTITY_TYPE.get(Identifier("ic2_120", "electric_boat")) as EntityType<ElectricBoatEntity>

    @Suppress("UNCHECKED_CAST")
    val LASER_PROJECTILE: EntityType<LaserProjectileEntity>
        get() = Registries.ENTITY_TYPE.get(Identifier("ic2_120", "laser_projectile")) as EntityType<LaserProjectileEntity>
}
