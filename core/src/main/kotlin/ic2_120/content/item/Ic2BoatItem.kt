package ic2_120.content.item

import ic2_120.compat.isClient
import ic2_120.compat.pos

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.vehicle.Boat
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.entity.EntitySelector
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.HitResult
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.gameevent.GameEvent

/**
 * 用于放置 IC2 船实体的物品。使用方式与原版船一致：对水面或地面使用即可放置可乘坐的船。
 * 子类配合 @ModItem 由 ClassScanner 注解驱动注册。
 */
open class Ic2BoatItem(
    private val entityType: EntityType<out Boat>,
    settings: Item.Properties
) : Item(settings) {

    override fun use(world: Level, user: net.minecraft.world.entity.player.Player, hand: InteractionHand): net.minecraft.world.InteractionResultHolder<ItemStack> {
        val stack = user.getItemInHand(hand)
        val hitResult = raycast(world, user, ClipContext.FluidHandling.ANY)

        if (hitResult.type != HitResult.Type.BLOCK) {
            return net.minecraft.world.InteractionResultHolder.pass(stack)
        }

        val list = world.getOtherEntities(
            user,
            user.boundingBox.stretch(user.rotationVector.multiply(5.0)).expand(1.0),
            EntitySelector.EXCEPT_SPECTATOR
        )
        for (entity in list) {
            val box = entity.boundingBox.expand(entity.targetingMargin.toDouble())
            if (box.contains(user.eyePos)) {
                return net.minecraft.world.InteractionResultHolder.pass(stack)
            }
        }

        if (hitResult is net.minecraft.world.phys.BlockHitResult) {
            val boat = createEntity(world, hitResult)
            boat.yaw = user.yaw
            // 与原版一致：略微收缩碰撞箱，避免水面边界导致放置失败。
            if (!world.isSpaceEmpty(boat, boat.boundingBox.expand(-1.0E-7))) {
                return net.minecraft.world.InteractionResultHolder.fail(stack)
            }
            if (!world.isClient) {
                world.addFreshEntity(boat)
                world.emitGameEvent(user, GameEvent.ENTITY_PLACE, boat.pos)
                if (!user.isCreative) {
                    stack.shrink(1)
                }
            }
            return net.minecraft.world.InteractionResultHolder.success(stack, world.isClient)
        }
        return net.minecraft.world.InteractionResultHolder.pass(stack)
    }

    private fun createEntity(world: Level, hitResult: net.minecraft.world.phys.BlockHitResult): Boat {
        val vec = hitResult.pos

        val boat = entityType.create(world)
            ?: throw IllegalStateException("EntityType $entityType did not create a Boat")
        boat.setPosition(vec.x, vec.y, vec.z)
        boat.prevX = vec.x
        boat.prevY = vec.y
        boat.prevZ = vec.z
        return boat
    }
}
