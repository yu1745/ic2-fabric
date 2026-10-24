package ic2_120.client

import ic2_120.content.item.Chainsaw
import ic2_120.content.network.NetworkManager
import io.netty.buffer.Unpooled
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import ic2_120.content.network.ToggleChainsawShearPayload

/** 链锯模式切换：按住 Alt 并右键切换剪刀功能。 */
@Environment(EnvType.CLIENT)
object ChainsawModeHandler {
    private var lastToggleSentOnPlayerAge: Int = Int.MIN_VALUE

    fun register() {
        UseItemCallback.EVENT.register { player, world, hand ->
            val stack = player.getItemInHand(hand)
            if (!world.isClientSide || stack.item !is Chainsaw || !ModeKeybinds.isAltDown()) {
                return@register InteractionResultHolder.pass(stack)
            }
            sendToggleOnceThisTick(player.tickCount)
            InteractionResultHolder.success(stack)
        }

        UseBlockCallback.EVENT.register { player, world, hand, _ ->
            val stack = player.getItemInHand(hand)
            if (!world.isClientSide || stack.item !is Chainsaw || !ModeKeybinds.isAltDown()) {
                return@register InteractionResult.PASS
            }
            sendToggleOnceThisTick(player.tickCount)
            InteractionResult.SUCCESS
        }
    }

    private fun sendToggleOnceThisTick(playerAge: Int) {
        if (playerAge == lastToggleSentOnPlayerAge) return
        lastToggleSentOnPlayerAge = playerAge
        ClientPlayNetworking.send(ToggleChainsawShearPayload)
    }
}
