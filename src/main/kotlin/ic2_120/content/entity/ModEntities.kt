package ic2_120.content.entity

import ic2_120.Ic2_120
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnGroup
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

object ModEntities {

    val BROKEN_RUBBER_BOAT: EntityType<BrokenRubberBoatEntity> = Registry.register(
        Registries.ENTITY_TYPE,
        Identifier(Ic2_120.MOD_ID, "broken_rubber_boat"),
        EntityType.Builder.create<BrokenRubberBoatEntity>({ type, world -> BrokenRubberBoatEntity(type, world) }, SpawnGroup.MISC)
            .setDimensions(1.375f, 0.5625f)
            .maxTrackingRange(10)
            .build(Ic2_120.MOD_ID + ":broken_rubber_boat")
    )

    val CARBON_BOAT: EntityType<CarbonBoatEntity> = Registry.register(
        Registries.ENTITY_TYPE,
        Identifier(Ic2_120.MOD_ID, "carbon_boat"),
        EntityType.Builder.create<CarbonBoatEntity>({ type, world -> CarbonBoatEntity(type, world) }, SpawnGroup.MISC)
            .setDimensions(1.375f, 0.5625f)
            .maxTrackingRange(10)
            .build(Ic2_120.MOD_ID + ":carbon_boat")
    )

    val RUBBER_BOAT: EntityType<RubberBoatEntity> = Registry.register(
        Registries.ENTITY_TYPE,
        Identifier(Ic2_120.MOD_ID, "rubber_boat"),
        EntityType.Builder.create<RubberBoatEntity>({ type, world -> RubberBoatEntity(type, world) }, SpawnGroup.MISC)
            .setDimensions(1.375f, 0.5625f)
            .maxTrackingRange(10)
            .build(Ic2_120.MOD_ID + ":rubber_boat")
    )

    val ELECTRIC_BOAT: EntityType<ElectricBoatEntity> = Registry.register(
        Registries.ENTITY_TYPE,
        Identifier(Ic2_120.MOD_ID, "electric_boat"),
        EntityType.Builder.create<ElectricBoatEntity>({ type, world -> ElectricBoatEntity(type, world) }, SpawnGroup.MISC)
            .setDimensions(1.375f, 0.5625f)
            .maxTrackingRange(10)
            .build(Ic2_120.MOD_ID + ":electric_boat")
    )

    /** 在注解驱动扫描物品前调用，确保所有船实体类型已注册（供 @ModItem 船物品类引用） */
    fun register() {
        listOf(BROKEN_RUBBER_BOAT, CARBON_BOAT, RUBBER_BOAT, ELECTRIC_BOAT)
    }
}
