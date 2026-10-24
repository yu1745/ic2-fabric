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
import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import org.lwjgl.glfw.GLFW
import ic2_120.content.network.ToggleChainsawShearPayload
import ic2_120.content.network.ToggleIridiumSilkTouchPayload
import ic2_120.content.network.ToggleMiningLaserModePayload
import ic2_120.content.network.ToggleFoamSprayerModePayload

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
            if (!client.options.keyUse.isDown) {
                modeToggleConsumedForCurrentUse = false
            }

            // GLFW may retain a modifier as pressed when focus is lost before its release event.
            // Reset our event-driven state while unfocused so Alt+Tab cannot leave mode switching stuck on.
            if (!client.isWindowActive()) resetModifierState()
        }

        UseBlockCallback.EVENT.register { player, world, hand, _ ->
            if (world.isClientSide && isAltDown() && player.getItemInHand(hand).item is ic2_120.content.item.FoamSprayerItem) {
                toggleFoamModeOnceThisTick(player.tickCount)
                InteractionResult.FAIL
            } else {
                InteractionResult.PASS
            }
        }

        UseItemCallback.EVENT.register { player, world, hand ->
            if (world.isClientSide && isAltDown() && player.getItemInHand(hand).item is ic2_120.content.item.FoamSprayerItem) {
                toggleFoamModeOnceThisTick(player.tickCount)
                InteractionResultHolder.fail(player.getItemInHand(hand))
            } else {
                InteractionResultHolder.pass(player.getItemInHand(hand))
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
     * Returning true tells the Minecraft mixin to cancel the entire use action,
     * which prevents Connector from continuing into useOnBlock after the mode packet.
     */
    @JvmStatic
    fun tryHandleModeSwitchUse(): Boolean {
        if (!isAltDown()) return false
        val player = Minecraft.getInstance().player ?: return false

        val packet = InteractionHand.values().firstNotNullOfOrNull { hand ->
            when (player.getItemInHand(hand).item) {
                is FoamSprayerItem -> ToggleFoamSprayerModePayload
                is MiningLaserItem -> ToggleMiningLaserModePayload
                is IridiumDrill -> ToggleIridiumSilkTouchPayload
                is Chainsaw -> ToggleChainsawShearPayload
                else -> null
            }
        } ?: return false

        // Keep consuming vanilla's repeated doItemUse calls while the same use press is held,
        // but only send the mode packet on the initial press.
        if (modeToggleConsumedForCurrentUse) return true
        modeToggleConsumedForCurrentUse = true
        ClientPlayNetworking.send(packet)
        return true
    }

    private fun toggleFoamModeOnceThisTick(playerAge: Int) {
        if (playerAge == lastFoamToggleAge) return
        lastFoamToggleAge = playerAge
        ClientPlayNetworking.send(ToggleFoamSprayerModePayload)
    }

}
