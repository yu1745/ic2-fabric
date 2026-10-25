package ic2_120.content.entity

import net.minecraft.world.entity.EntityType
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.vehicle.boat.Boat
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.Level
import net.minecraft.server.level.ServerLevel
import net.minecraft.core.registries.BuiltInRegistries
import kotlin.math.sqrt

/**
 * IC2 船实体基类，用于破损橡皮艇与碳纤维轻艇。
 * 行为与原版船一致，渲染由客户端根据实体类型区分贴图。
 */
abstract class Ic2BoatEntity(
    entityType: EntityType<out Ic2BoatEntity>,
    world: Level
) : Boat(entityType, world, { BuiltInRegistries.ITEM.getValue(BuiltInRegistries.ENTITY_TYPE.getKey(entityType)) }) {
    protected open val maxHorizontalSpeed: Double = 0.40
    protected open val waterDragMultiplier: Double = 1.0
    protected open val damageTakenMultiplier: Float = 1.0f

    override fun tick() {
        super.tick()
        if (level().isClientSide || !isInWater()) return

        val velocity = deltaMovement
        val adjusted = Vec3(velocity.x * waterDragMultiplier, velocity.y, velocity.z * waterDragMultiplier)
        val horizontalSpeed = sqrt(adjusted.x * adjusted.x + adjusted.z * adjusted.z)
        if (horizontalSpeed <= maxHorizontalSpeed || horizontalSpeed <= 1.0E-6) {
            this.deltaMovement = adjusted
            return
        }

        val scale = maxHorizontalSpeed / horizontalSpeed
        this.deltaMovement = Vec3(adjusted.x * scale, adjusted.y, adjusted.z * scale)
    }

    override fun hurtServer(world: ServerLevel, source: DamageSource, amount: Float): Boolean {
        return super.hurtServer(world, source, amount * damageTakenMultiplier)
    }
}
