package ic2_120.client

import ic2_120.content.item.Chainsaw
import ic2_120.content.network.NetworkManager
import io.netty.buffer.Unpooled
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.ActionResult
import net.minecraft.util.TypedActionResult

/** 链锯模式切换：按住通用模式键并右键切换剪刀功能。 */
@Environment(EnvType.CLIENT)
object ChainsawModeHandler {
    fun register() {
        UseItemCallback.EVENT.register { player, world, hand ->
            val stack = player.getStackInHand(hand)
            if (!world.isClient || stack.item !is Chainsaw || !ModeKeybinds.isAltDown()) {
                return@register TypedActionResult.pass(stack)
            }
            ClientPlayNetworking.send(
                NetworkManager.TOGGLE_CHAINSAW_SHEAR_PACKET,
                PacketByteBuf(Unpooled.buffer())
            )
            TypedActionResult.success(stack, true)
        }

        UseBlockCallback.EVENT.register { player, world, hand, _ ->
            val stack = player.getStackInHand(hand)
            if (!world.isClient || stack.item !is Chainsaw || !ModeKeybinds.isAltDown()) {
                return@register ActionResult.PASS
            }
            ClientPlayNetworking.send(
                NetworkManager.TOGGLE_CHAINSAW_SHEAR_PACKET,
                PacketByteBuf(Unpooled.buffer())
            )
            ActionResult.SUCCESS
        }
    }
}
