package ic2_120.client

import ic2_120.content.item.MiningLaserItem
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
 * 采矿镭射枪模式切换：
 * 按住模式键并右键（空气/方块）时循环切换模式。
 */
@Environment(EnvType.CLIENT)
object MiningLaserModeHandler {
    fun register() {
        UseItemCallback.EVENT.register { player, world, hand ->
            val stack = player.getStackInHand(hand)
            if (!world.isClient || stack.item !is MiningLaserItem || !ModeKeybinds.isModeKeyDown()) {
                return@register TypedActionResult.pass(stack)
            }
            ClientPlayNetworking.send(
                NetworkManager.TOGGLE_MINING_LASER_MODE_PACKET,
                PacketByteBuf(Unpooled.buffer())
            )
            TypedActionResult.fail(stack)
        }

        UseBlockCallback.EVENT.register { player, world, hand, _ ->
            val stack = player.getStackInHand(hand)
            if (!world.isClient || stack.item !is MiningLaserItem || !ModeKeybinds.isModeKeyDown()) {
                return@register ActionResult.PASS
            }
            ClientPlayNetworking.send(
                NetworkManager.TOGGLE_MINING_LASER_MODE_PACKET,
                PacketByteBuf(Unpooled.buffer())
            )
            ActionResult.FAIL
        }
    }
}
