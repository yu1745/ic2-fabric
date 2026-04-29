package ic2_120.client

import ic2_120.content.item.FoamSprayerItem
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.minecraft.text.Text
import net.minecraft.util.Formatting

/**
 * 建筑泡沫喷枪：容量、剩余喷涂格数、单格/多格模式与 Alt+绑定键提示（与 [ModeKeybinds] 一致）。
 */
@Environment(EnvType.CLIENT)
object FoamSprayerTooltipHandler {

    fun register() {
        ItemTooltipCallback.EVENT.register { stack, _, type, lines ->
            if (stack.item !is FoamSprayerItem) return@register
            val amt = FoamSprayerItem.getFluidAmount(stack)
            val capBuckets = FoamSprayerItem.CAPACITY_DROPLETS / FluidConstants.BUCKET
            val curBuckets = "%.1f".format(amt.toDouble() / FluidConstants.BUCKET.toDouble())
            lines.add(
                Text.translatable("tooltip.ic2_120.foam_sprayer.fluid", curBuckets, capBuckets)
                    .formatted(Formatting.GRAY)
            )
            val spraysLeft = (amt / FoamSprayerItem.DROPLETS_PER_BLOCK).toInt()
            lines.add(
                Text.translatable("tooltip.ic2_120.foam_sprayer.blocks_left", spraysLeft)
                    .formatted(Formatting.GRAY)
            )
            val modeKey = ModeKeybinds.getModeKey()
            val keyName = modeKey.boundKeyLocalizedText.string
            val keyLine = Text.literal("Alt + ").formatted(Formatting.YELLOW)
                .append(Text.literal(keyName).formatted(Formatting.YELLOW))
            if (FoamSprayerItem.isMultiMode(stack)) {
                lines.add(Text.translatable("tooltip.ic2_120.foam_sprayer.mode_multi", keyLine).formatted(Formatting.DARK_AQUA))
            } else {
                lines.add(Text.translatable("tooltip.ic2_120.foam_sprayer.mode_single", keyLine).formatted(Formatting.DARK_AQUA))
            }
        }
    }
}
