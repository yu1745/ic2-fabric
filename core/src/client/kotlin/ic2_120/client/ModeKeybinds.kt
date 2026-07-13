package ic2_120.client

import ic2_120.content.network.NetworkManager
import io.netty.buffer.Unpooled
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.client.util.InputUtil
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.ActionResult
import net.minecraft.util.TypedActionResult
import org.lwjgl.glfw.GLFW

/**
 * 手持工具模式切换：按住 Alt 并右键。
 *
 * - 泡沫喷枪：按住 **Alt** 并**右键**切换模式。
 * - 采矿镭射枪、铱钻头、链锯：按住 Alt 并右键切换模式。
 *
 * 飞行（量子胸甲/喷气背包）复用原版创造飞行的双击空格触发。
 * 量子套夜视(Alt+N) 使用 ArmorKeybinds。
 */
@Environment(EnvType.CLIENT)
object ModeKeybinds {
    fun register() {
        UseBlockCallback.EVENT.register { player, world, hand, _ ->
            if (world.isClient && isAltDown() && player.getStackInHand(hand).item is ic2_120.content.item.FoamSprayerItem) {
                toggleFoamMode()
                ActionResult.FAIL
            } else {
                ActionResult.PASS
            }
        }

        UseItemCallback.EVENT.register { player, world, hand ->
            if (world.isClient && isAltDown() && player.getStackInHand(hand).item is ic2_120.content.item.FoamSprayerItem) {
                toggleFoamMode()
                TypedActionResult.fail(player.getStackInHand(hand))
            } else {
                TypedActionResult.pass(player.getStackInHand(hand))
            }
        }
    }

    fun isAltDown(): Boolean {
        val client = net.minecraft.client.MinecraftClient.getInstance()
        val window = client.window.handle
        return InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_ALT) ||
               InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_ALT)
    }

    private fun toggleFoamMode() {
        ClientPlayNetworking.send(
            NetworkManager.TOGGLE_FOAM_SPRAYER_MODE_PACKET,
            PacketByteBuf(Unpooled.buffer())
        )
    }

}
