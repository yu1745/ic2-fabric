package ic2_120.client

import ic2_120.content.block.BaseCableBlock
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.item.BlockItem
import net.minecraft.text.Text
import net.minecraft.util.Formatting

/**
 * 为导线类方块的物品注册 tooltip，鼠标悬停时显示可传输的 EU/t 与每格损耗。
 */
object CableTooltip {

    fun register() {
        ItemTooltipCallback.EVENT.register { stack, context, lines ->
            val item = stack.item
            if (item !is BlockItem) return@register
            val block = item.block
            if (block !is BaseCableBlock) return@register
            val euPerTick = block.getTransferRate()
            lines.add(Text.translatable("tooltip.ic2_120.cable_eu_t", euPerTick).formatted(Formatting.GRAY))
            val lossEu = block.getEnergyLoss() / 1000.0  // getEnergyLoss() 为 milliEU/格
            val lossStr = if (lossEu == lossEu.toLong().toDouble()) "${lossEu.toLong()}" else "%.3f".format(lossEu).trimEnd('0').trimEnd('.')
            lines.add(Text.translatable("tooltip.ic2_120.cable_loss", lossStr).formatted(Formatting.GRAY))
        }
    }
}
