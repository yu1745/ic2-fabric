package ic2_120.content.entity

<<<<<<< HEAD
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
=======
import ic2_120.Ic2_120
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnGroup
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

object ModEntities {

    val BROKEN_RUBBER_BOAT: EntityType<BrokenRubberBoatEntity> = Registry.register(
        Registries.ENTITY_TYPE,
        Identifier.of(Ic2_120.MOD_ID, "broken_rubber_boat"),
        EntityType.Builder.create<BrokenRubberBoatEntity>({ type, world -> BrokenRubberBoatEntity(type, world) }, SpawnGroup.MISC)
            .dimensions(1.375f, 0.5625f)
            .maxTrackingRange(10)
            .build("minecraft:boat")
    )

    val CARBON_BOAT: EntityType<CarbonBoatEntity> = Registry.register(
        Registries.ENTITY_TYPE,
        Identifier.of(Ic2_120.MOD_ID, "carbon_boat"),
        EntityType.Builder.create<CarbonBoatEntity>({ type, world -> CarbonBoatEntity(type, world) }, SpawnGroup.MISC)
            .dimensions(1.375f, 0.5625f)
            .maxTrackingRange(10)
            .build("minecraft:boat")
    )

    val RUBBER_BOAT: EntityType<RubberBoatEntity> = Registry.register(
        Registries.ENTITY_TYPE,
        Identifier.of(Ic2_120.MOD_ID, "rubber_boat"),
        EntityType.Builder.create<RubberBoatEntity>({ type, world -> RubberBoatEntity(type, world) }, SpawnGroup.MISC)
            .dimensions(1.375f, 0.5625f)
            .maxTrackingRange(10)
            .build("minecraft:boat")
    )

    val ELECTRIC_BOAT: EntityType<ElectricBoatEntity> = Registry.register(
        Registries.ENTITY_TYPE,
        Identifier.of(Ic2_120.MOD_ID, "electric_boat"),
        EntityType.Builder.create<ElectricBoatEntity>({ type, world -> ElectricBoatEntity(type, world) }, SpawnGroup.MISC)
            .dimensions(1.375f, 0.5625f)
            .maxTrackingRange(10)
            .build("minecraft:boat")
    )

    val LASER_PROJECTILE: EntityType<LaserProjectileEntity> = Registry.register(
        Registries.ENTITY_TYPE,
        Identifier.of(Ic2_120.MOD_ID, "laser_projectile"),
        EntityType.Builder.create<LaserProjectileEntity>({ type, world -> LaserProjectileEntity(type, world) }, SpawnGroup.MISC)
            .dimensions(0.1f, 0.1f)
            .maxTrackingRange(256)
            .trackingTickInterval(1)
            .build("minecraft:arrow")
    )

    /** 在注解驱动扫描物品前调用，确保所有实体类型已注册（供 @ModItem 物品类引用） */
    fun register() {
        listOf(BROKEN_RUBBER_BOAT, CARBON_BOAT, RUBBER_BOAT, ELECTRIC_BOAT, LASER_PROJECTILE)
    }
>>>>>>> backup/1.21.1
}
