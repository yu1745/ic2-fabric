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
        val hitResult = getPlayerPOVHitResult(world, user, ClipContext.Fluid.ANY)

        if (hitResult.type != HitResult.Type.BLOCK) {
            return net.minecraft.world.InteractionResultHolder.pass(stack)
        }

        val list = world.getEntities(
            user,
            user.boundingBox.expandTowards(user.getViewVector(1.0f).scale(5.0)).inflate(1.0),
            EntitySelector.NO_SPECTATORS
        )
        for (entity in list) {
            val box = entity.boundingBox.inflate(entity.pickRadius.toDouble())
            if (box.contains(user.eyePosition)) {
                return net.minecraft.world.InteractionResultHolder.pass(stack)
            }
        }

        if (hitResult is net.minecraft.world.phys.BlockHitResult) {
            val boat = createEntity(world, hitResult)
            boat.yRot = user.yRot
            // 与原版一致：略微收缩碰撞箱，避免水面边界导致放置失败。
            if (!world.noCollision(boat, boat.boundingBox.inflate(-1.0E-7))) {
                return net.minecraft.world.InteractionResultHolder.fail(stack)
            }
            if (!world.isClient) {
                world.addFreshEntity(boat)
                world.gameEvent(user, GameEvent.ENTITY_PLACE, hitResult.location)
                if (!user.isCreative) {
                    stack.shrink(1)
                }
            }
            return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, world.isClient)
        }
        return net.minecraft.world.InteractionResultHolder.pass(stack)
    }

    private fun createEntity(world: Level, hitResult: net.minecraft.world.phys.BlockHitResult): Boat {
        val vec = hitResult.location

        val boat = entityType.create(world)
            ?: throw IllegalStateException("EntityType $entityType did not create a Boat")
        boat.moveTo(vec.x, vec.y, vec.z)
        boat.xo = vec.x
        boat.yo = vec.y
        boat.zo = vec.z
        return boat
    }
}
