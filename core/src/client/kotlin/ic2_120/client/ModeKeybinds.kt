package ic2_120.client

import ic2_120.content.item.Chainsaw
import ic2_120.content.item.FoamSprayerItem
import ic2_120.content.item.IridiumDrill
import ic2_120.content.item.MiningLaserItem
import ic2_120.content.network.NetworkManager
import io.netty.buffer.Unpooled
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.client.MinecraftClient
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
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
    @Volatile
    private var leftAltDown = false

    @Volatile
    private var rightAltDown = false

    private var lastFoamToggleAge = Int.MIN_VALUE

    /** One physical use-key press may toggle at most once, even when vanilla repeats held use. */
    private var modeToggleConsumedForCurrentUse = false

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (!client.options.useKey.isPressed) {
                modeToggleConsumedForCurrentUse = false
            }

            // GLFW may retain a modifier as pressed when focus is lost before its release event.
            // Reset our event-driven state while unfocused so Alt+Tab cannot leave mode switching stuck on.
            if (!client.isWindowFocused) resetModifierState()
        }

        UseBlockCallback.EVENT.register { player, world, hand, _ ->
            if (world.isClient && isAltDown() && player.getStackInHand(hand).item is ic2_120.content.item.FoamSprayerItem) {
                toggleFoamModeOnceThisTick(player.age)
                ActionResult.FAIL
            } else {
                ActionResult.PASS
            }
        }

        UseItemCallback.EVENT.register { player, world, hand ->
            if (world.isClient && isAltDown() && player.getStackInHand(hand).item is ic2_120.content.item.FoamSprayerItem) {
                toggleFoamModeOnceThisTick(player.age)
                TypedActionResult.fail(player.getStackInHand(hand))
            } else {
                TypedActionResult.pass(player.getStackInHand(hand))
            }
        }
    }

    /**
     * Updated from the vanilla keyboard callback instead of polling GLFW during a mouse callback.
     * Polling can report a stale Alt state after focus changes in Connector/Forge environments.
     */
    @JvmStatic
    fun onKeyEvent(key: Int, action: Int) {
        when (key) {
            GLFW.GLFW_KEY_LEFT_ALT -> leftAltDown = action != GLFW.GLFW_RELEASE
            GLFW.GLFW_KEY_RIGHT_ALT -> rightAltDown = action != GLFW.GLFW_RELEASE
        }
    }

    @JvmStatic
    fun resetModifierState() {
        leftAltDown = false
        rightAltDown = false
        modeToggleConsumedForCurrentUse = false
    }

    fun isAltDown(): Boolean = leftAltDown || rightAltDown

    /**
     * Handles Alt+use before vanilla chooses the entity/block/item interaction path.
     * Returning true tells the MinecraftClient mixin to cancel the entire use action,
     * which prevents Connector from continuing into useOnBlock after the mode packet.
     */
    @JvmStatic
    fun tryHandleModeSwitchUse(): Boolean {
        if (!isAltDown()) return false
        val player = MinecraftClient.getInstance().player ?: return false

        val packet = Hand.values().firstNotNullOfOrNull { hand ->
            when (player.getStackInHand(hand).item) {
                is FoamSprayerItem -> NetworkManager.TOGGLE_FOAM_SPRAYER_MODE_PACKET
                is MiningLaserItem -> NetworkManager.TOGGLE_MINING_LASER_MODE_PACKET
                is IridiumDrill -> NetworkManager.TOGGLE_IRIDIUM_SILK_TOUCH_PACKET
                is Chainsaw -> NetworkManager.TOGGLE_CHAINSAW_SHEAR_PACKET
                else -> null
            }
        } ?: return false

        // Keep consuming vanilla's repeated doItemUse calls while the same use press is held,
        // but only send the mode packet on the initial press.
        if (modeToggleConsumedForCurrentUse) return true
        modeToggleConsumedForCurrentUse = true
        ClientPlayNetworking.send(packet, PacketByteBuf(Unpooled.buffer()))
        return true
    }

    private fun toggleFoamModeOnceThisTick(playerAge: Int) {
        if (playerAge == lastFoamToggleAge) return
        lastFoamToggleAge = playerAge
        ClientPlayNetworking.send(
            NetworkManager.TOGGLE_FOAM_SPRAYER_MODE_PACKET,
            PacketByteBuf(Unpooled.buffer())
        )
    }

}
