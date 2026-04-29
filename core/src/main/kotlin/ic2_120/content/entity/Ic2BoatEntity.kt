package ic2_120.content.entity

import net.minecraft.entity.EntityType
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.vehicle.BoatEntity
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import kotlin.math.sqrt

/**
 * IC2 船实体基类，用于破损橡皮艇与碳纤维轻艇。
 * 行为与原版船一致，渲染由客户端根据实体类型区分贴图。
 */
abstract class Ic2BoatEntity(
    entityType: EntityType<out Ic2BoatEntity>,
    world: World
) : BoatEntity(entityType, world) {
    protected open val maxHorizontalSpeed: Double = 0.40
    protected open val waterDragMultiplier: Double = 1.0
    protected open val damageTakenMultiplier: Float = 1.0f

    override fun tick() {
        super.tick()
        if (world.isClient || !isTouchingWater) return

        val velocity = velocity
        val adjusted = Vec3d(velocity.x * waterDragMultiplier, velocity.y, velocity.z * waterDragMultiplier)
        val horizontalSpeed = sqrt(adjusted.x * adjusted.x + adjusted.z * adjusted.z)
        if (horizontalSpeed <= maxHorizontalSpeed || horizontalSpeed <= 1.0E-6) {
            this.velocity = adjusted
            return
        }

        val scale = maxHorizontalSpeed / horizontalSpeed
        this.velocity = Vec3d(adjusted.x * scale, adjusted.y, adjusted.z * scale)
    }

    override fun damage(source: DamageSource, amount: Float): Boolean {
        return super.damage(source, amount * damageTakenMultiplier)
    }
}
