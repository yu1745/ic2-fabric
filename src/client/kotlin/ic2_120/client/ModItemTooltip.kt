package ic2_120.client

import ic2_120.Ic2_120
import ic2_120.content.block.cables.BaseCableBlock
import ic2_120.content.block.machines.NuclearReactorBlockEntity
import ic2_120.content.item.getFluidCellVariant
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.item.BlockItem
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier

/**
 * 模组物品 tooltip 注册：导线显示 EU/t 与损耗，流体单元显示所含流体名称。
 */
object ModItemTooltip {

    fun register() {
        ItemTooltipCallback.EVENT.register { stack, context, lines ->
            // 导线 tooltip
            val item = stack.item
            if (item is BlockItem) {
                val block = item.block
                if (block is BaseCableBlock) {
                    val euPerTick = block.getTransferRate()
                    lines.add(Text.translatable("tooltip.ic2_120.cable_eu_t", euPerTick).formatted(Formatting.GRAY))
                    val lossEu = block.getEnergyLoss() / 1000.0  // getEnergyLoss() 为 milliEU/格
                    val lossStr = if (lossEu == lossEu.toLong().toDouble()) "${lossEu.toLong()}" else "%.3f".format(lossEu).trimEnd('0').trimEnd('.')
                    lines.add(Text.translatable("tooltip.ic2_120.cable_loss", lossStr).formatted(Formatting.GRAY))
                    return@register
                }
            }
            // 流体单元 tooltip：显示所含流体
            if (item == Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "fluid_cell"))) {
                val variant = stack.getFluidCellVariant()
                if (variant != null && !variant.isBlank) {
                    val fluidId = Registries.FLUID.getId(variant.fluid)
                    lines.add(Text.translatable("fluid.${fluidId.namespace}.${fluidId.path}").formatted(Formatting.GRAY))
                }
            }
            
            // 反应堆组件 tooltip：显示产热和散热信息
            if (item is ic2_120.content.reactor.IReactorComponent) {
                // 这里可以添加默认的产热和散热信息
                // 实际的产热和散热信息需要从反应堆方块实体中获取
                // lines.add(Text.literal("产热: 0").formatted(Formatting.GRAY))
                // lines.add(Text.literal("散热: 0").formatted(Formatting.GRAY))
            }
        }
    }
}
