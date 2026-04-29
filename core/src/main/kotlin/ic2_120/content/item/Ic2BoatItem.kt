package ic2_120.content.item

import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.vehicle.BoatEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.predicate.entity.EntityPredicates
import net.minecraft.util.Hand
import net.minecraft.util.hit.HitResult
import net.minecraft.world.RaycastContext
import net.minecraft.world.World
import net.minecraft.world.event.GameEvent

/**
 * 用于放置 IC2 船实体的物品。使用方式与原版船一致：对水面或地面使用即可放置可乘坐的船。
 * 子类配合 @ModItem 由 ClassScanner 注解驱动注册。
 */
open class Ic2BoatItem(
    private val entityType: EntityType<out BoatEntity>,
    settings: net.fabricmc.fabric.api.item.v1.FabricItemSettings
) : Item(settings) {

    override fun use(world: World, user: net.minecraft.entity.player.PlayerEntity, hand: Hand): net.minecraft.util.TypedActionResult<ItemStack> {
        val stack = user.getStackInHand(hand)
        val hitResult = raycast(world, user, RaycastContext.FluidHandling.ANY)

        if (hitResult.type != HitResult.Type.BLOCK) {
            return net.minecraft.util.TypedActionResult.pass(stack)
        }

        val list = world.getOtherEntities(
            user,
            user.boundingBox.stretch(user.rotationVector.multiply(5.0)).expand(1.0),
            EntityPredicates.EXCEPT_SPECTATOR
        )
        for (entity in list) {
            val box = entity.boundingBox.expand(entity.targetingMargin.toDouble())
            if (box.contains(user.eyePos)) {
                return net.minecraft.util.TypedActionResult.pass(stack)
            }
        }

        if (hitResult is net.minecraft.util.hit.BlockHitResult) {
            val boat = createEntity(world, hitResult)
            boat.yaw = user.yaw
            // 与原版一致：略微收缩碰撞箱，避免水面边界导致放置失败。
            if (!world.isSpaceEmpty(boat, boat.boundingBox.expand(-1.0E-7))) {
                return net.minecraft.util.TypedActionResult.fail(stack)
            }
            if (!world.isClient) {
                world.spawnEntity(boat)
                world.emitGameEvent(user, GameEvent.ENTITY_PLACE, boat.pos)
                if (!user.isCreative) {
                    stack.decrement(1)
                }
            }
            return net.minecraft.util.TypedActionResult.success(stack, world.isClient)
        }
        return net.minecraft.util.TypedActionResult.pass(stack)
    }

    private fun createEntity(world: World, hitResult: net.minecraft.util.hit.BlockHitResult): BoatEntity {
        val vec = hitResult.pos

        val boat = entityType.create(world)
            ?: throw IllegalStateException("EntityType $entityType did not create a BoatEntity")
        boat.setPosition(vec.x, vec.y, vec.z)
        boat.prevX = vec.x
        boat.prevY = vec.y
        boat.prevZ = vec.z
        return boat
    }
}
