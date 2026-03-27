package ic2_120.client

import ic2_120.content.item.IridiumDrill
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

/**
 * 铱钻头模式切换：
 * 按住“模式键”并右键（空气/方块）时切换精准采集。
 */
@Environment(EnvType.CLIENT)
object IridiumDrillModeHandler {
    fun register() {
        UseItemCallback.EVENT.register { player, world, hand ->
            val stack = player.getStackInHand(hand)
            if (!world.isClient || stack.item !is IridiumDrill || !ModeKeybinds.isModeKeyDown()) {
                return@register TypedActionResult.pass(stack)
            }
            ClientPlayNetworking.send(
                NetworkManager.TOGGLE_IRIDIUM_SILK_TOUCH_PACKET,
                PacketByteBuf(Unpooled.buffer())
            )
            TypedActionResult.success(stack, true)
        }

        UseBlockCallback.EVENT.register { player, world, hand, _ ->
            val stack = player.getStackInHand(hand)
            if (!world.isClient || stack.item !is IridiumDrill || !ModeKeybinds.isModeKeyDown()) {
                return@register ActionResult.PASS
            }
            ClientPlayNetworking.send(
                NetworkManager.TOGGLE_IRIDIUM_SILK_TOUCH_PACKET,
                PacketByteBuf(Unpooled.buffer())
            )
            ActionResult.SUCCESS
        }
    }
}
