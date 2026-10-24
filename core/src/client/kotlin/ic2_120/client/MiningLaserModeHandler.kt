package ic2_120.client

import ic2_120.content.item.MiningLaserItem
import ic2_120.content.network.ToggleMiningLaserModePayload
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.client.Minecraft
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder

/**
 * 采矿镭射枪模式切换：
 * 按住 Alt 并右键（空气/方块）时循环切换模式。
 *
 * 与 [IridiumDrillModeHandler] 一致使用 `success` 消费本次使用，避免仍触发 [MiningLaserItem.use]。
 * 同一 tick 内 [UseBlockCallback] 与 [UseItemCallback] 可能各触发一次，故每 tick 最多发一包，防止连切两档。
 */
@Environment(EnvType.CLIENT)
object MiningLaserModeHandler {

    private var lastToggleSentOnPlayerAge: Int = -1

    fun register() {
        UseItemCallback.EVENT.register { player, world, hand ->
            val stack = player.getItemInHand(hand)
            if (!world.isClient || stack.item !is MiningLaserItem || !ModeKeybinds.isAltDown()) {
                return@register InteractionResultHolder.pass(stack)
            }
            sendToggleOnceThisTick()
            return@register InteractionResultHolder.success(stack)
        }

        UseBlockCallback.EVENT.register { player, world, hand, _ ->
            val stack = player.getItemInHand(hand)
            if (!world.isClient || stack.item !is MiningLaserItem || !ModeKeybinds.isAltDown()) {
                return@register InteractionResult.PASS
            }
            sendToggleOnceThisTick()
            return@register InteractionResult.SUCCESS
        }
    }

    private fun sendToggleOnceThisTick() {
        val age = Minecraft.getInstance().player?.age ?: return
        if (age == lastToggleSentOnPlayerAge) return
        lastToggleSentOnPlayerAge = age
        ClientPlayNetworking.send(ToggleMiningLaserModePayload)
    }
}
