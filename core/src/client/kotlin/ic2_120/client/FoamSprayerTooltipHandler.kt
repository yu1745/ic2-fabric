package ic2_120.client

import ic2_120.content.item.FoamSprayerItem
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting

/**
 * 建筑泡沫喷枪：容量、剩余喷涂格数、单格/多格模式与 Alt+右键提示。
 */
@Environment(EnvType.CLIENT)
object FoamSprayerTooltipHandler {

    fun register() {
        ItemTooltipCallback.EVENT.register { stack, _, lines ->
            if (stack.item !is FoamSprayerItem) return@register
            val amt = FoamSprayerItem.getFluidAmount(stack)
            val capBuckets = FoamSprayerItem.CAPACITY_DROPLETS / FluidConstants.BUCKET
            val curBuckets = "%.1f".format(amt.toDouble() / FluidConstants.BUCKET.toDouble())
            lines.add(
                Component.translatable("tooltip.ic2_120.foam_sprayer.fluid", curBuckets, capBuckets)
                    .withStyle(ChatFormatting.GRAY)
            )
            val spraysLeft = (amt / FoamSprayerItem.DROPLETS_PER_BLOCK).toInt()
            lines.add(
                Component.translatable("tooltip.ic2_120.foam_sprayer.blocks_left", spraysLeft)
                    .withStyle(ChatFormatting.GRAY)
            )
            val keyLine = Component.translatable("tooltip.ic2_120.foam_sprayer.alt_right_click")
                .withStyle(ChatFormatting.YELLOW)
            if (FoamSprayerItem.isMultiMode(stack)) {
                lines.add(Component.translatable("tooltip.ic2_120.foam_sprayer.mode_multi", keyLine).withStyle(ChatFormatting.DARK_AQUA))
            } else {
                lines.add(Component.translatable("tooltip.ic2_120.foam_sprayer.mode_single", keyLine).withStyle(ChatFormatting.DARK_AQUA))
            }
        }
    }
}
