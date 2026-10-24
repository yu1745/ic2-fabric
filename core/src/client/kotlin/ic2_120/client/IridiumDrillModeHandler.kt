package ic2_120.client

import ic2_120.content.item.IridiumDrill
import ic2_120.content.network.ToggleIridiumSilkTouchPayload
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.util.ActionResult
import net.minecraft.util.TypedActionResult

/**
 * 铱钻头模式切换：
 * 按住 Alt 并右键（空气/方块）时切换精准采集。
 */
@Environment(EnvType.CLIENT)
object IridiumDrillModeHandler {
    private var lastToggleSentOnPlayerAge: Int = Int.MIN_VALUE

    fun register() {
        UseItemCallback.EVENT.register { player, world, hand ->
            val stack = player.getStackInHand(hand)
            if (!world.isClient || stack.item !is IridiumDrill || !ModeKeybinds.isAltDown()) {
                return@register TypedActionResult.pass(stack)
            }
<<<<<<< HEAD
            sendToggleOnceThisTick(player.age)
=======
            ClientPlayNetworking.send(ToggleIridiumSilkTouchPayload)
>>>>>>> backup/1.21.1
            TypedActionResult.success(stack, true)
        }

        UseBlockCallback.EVENT.register { player, world, hand, _ ->
            val stack = player.getStackInHand(hand)
            if (!world.isClient || stack.item !is IridiumDrill || !ModeKeybinds.isAltDown()) {
                return@register ActionResult.PASS
            }
<<<<<<< HEAD
            sendToggleOnceThisTick(player.age)
=======
            ClientPlayNetworking.send(ToggleIridiumSilkTouchPayload)
>>>>>>> backup/1.21.1
            ActionResult.SUCCESS
        }
    }

    private fun sendToggleOnceThisTick(playerAge: Int) {
        if (playerAge == lastToggleSentOnPlayerAge) return
        lastToggleSentOnPlayerAge = playerAge
        ClientPlayNetworking.send(
            NetworkManager.TOGGLE_IRIDIUM_SILK_TOUCH_PACKET,
            PacketByteBuf(Unpooled.buffer())
        )
    }
}
