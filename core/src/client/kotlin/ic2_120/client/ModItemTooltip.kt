package ic2_120.client

import ic2_120.Ic2_120
import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.KineticGeneratorBlock
import ic2_120.content.block.WindKineticGeneratorBlock
import ic2_120.content.block.WaterKineticGeneratorBlock
import ic2_120.content.block.ManualKineticGeneratorBlock
import ic2_120.content.block.cables.BaseCableBlock
import ic2_120.content.block.cables.IInsulatedCable
import ic2_120.content.block.machines.TransformerUtils
import ic2_120.content.block.transmission.BevelGearBlock
import ic2_120.content.block.transmission.CarbonTransmissionShaftBlock
import ic2_120.content.block.transmission.IronTransmissionShaftBlock
import ic2_120.content.block.transmission.SteelTransmissionShaftBlock
import ic2_120.content.block.transmission.WoodTransmissionShaftBlock
import ic2_120.content.item.energy.ITiered
import ic2_120.content.item.getFluidCellVariant
import ic2_120.registry.ClassScanner
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.world.item.BlockItem
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.BlockPos

/**
 * 模组物品 tooltip 注册：导线显示 EU/t 与损耗，流体单元显示所含流体名称。
 * 变压器显示能量等级转换信息。
 * 所有实现 ITiered 接口的物品显示电压等级。
 */
object ModItemTooltip {

    fun register() {
        ItemTooltipCallback.EVENT.register { stack, context, type, lines ->
            // 导线 tooltip
            val item = stack.item
            if (item is BlockItem) {
                val block = item.block
                if (block is BaseCableBlock && block is ITiered) {
                    val euPerTick = block.getTransferRate()
                    lines.add(Component.translatable("tooltip.ic2_120.cable_eu_t", euPerTick).formatted(ChatFormatting.GRAY))
                    val lossEu = block.getEnergyLoss() / 1000.0  // getEnergyLoss() 为 milliEU/格
                    val lossStr = if (lossEu == lossEu.toLong().toDouble()) "${lossEu.toLong()}" else "%.3f".format(lossEu).trimEnd('0').trimEnd('.')
                    lines.add(Component.translatable("tooltip.ic2_120.cable_loss", lossStr).formatted(ChatFormatting.GRAY))
                    addVoltageTierTooltip(lines, block.tier)
                    // 绝缘导线显示耐压等级
                    if (block is IInsulatedCable) {
                        val maxSafeEu = TransformerUtils.getEuForTier(block.insulationLevel)
                        if (block.insulationLevel >= 5) {
                            lines.add(Component.translatable("tooltip.ic2_120.cable_insulation_max").formatted(ChatFormatting.GRAY))
                        } else {
                            lines.add(Component.translatable("tooltip.ic2_120.cable_insulation", maxSafeEu).formatted(ChatFormatting.GRAY))
                        }
                    }
                    return@register
                }
                when (block) {
                    is WoodTransmissionShaftBlock -> addKineticTransmissionTooltip(lines, 128, 0)
                    is IronTransmissionShaftBlock -> addKineticTransmissionTooltip(lines, 512, 2)
                    is SteelTransmissionShaftBlock -> addKineticTransmissionTooltip(lines, 2048, 1)
                    is CarbonTransmissionShaftBlock -> addKineticTransmissionTooltip(lines, 8192, 0)
                    is BevelGearBlock -> addKineticTransmissionTooltip(lines, 2048, 3)
                    is WindKineticGeneratorBlock -> {
                        lines.add(Component.translatable("tooltip.ic2_120.kinetic_source").formatted(ChatFormatting.GRAY))
                    }
                    is WaterKineticGeneratorBlock -> {
                        lines.add(Component.translatable("tooltip.ic2_120.kinetic_source").formatted(ChatFormatting.GRAY))
                    }
                    is ManualKineticGeneratorBlock -> {
                        lines.add(Component.translatable("tooltip.ic2_120.kinetic_source").formatted(ChatFormatting.GRAY))
                        lines.add(Component.translatable("tooltip.ic2_120.manual_kinetic_insert_crank").formatted(ChatFormatting.DARK_GRAY))
                    }
                    is KineticGeneratorBlock -> {
                        lines.add(Component.translatable("tooltip.ic2_120.kinetic_convert_rate").formatted(ChatFormatting.GRAY))
                        lines.add(Component.translatable("tooltip.ic2_120.kinetic_max_input").formatted(ChatFormatting.GRAY))
                    }
                }

                // 变压器 tooltip：显示能量等级转换
                when (block) {
                    is ic2_120.content.block.LvTransformerBlock -> addTransformerTooltip(lines, 1, 2)
                    is ic2_120.content.block.MvTransformerBlock -> addTransformerTooltip(lines, 2, 3)
                    is ic2_120.content.block.HvTransformerBlock -> addTransformerTooltip(lines, 3, 4)
                    is ic2_120.content.block.EvTransformerBlock -> addTransformerTooltip(lines, 4, 5)
                }
            }
            // 流体单元 tooltip：显示所含流体
            if (item == BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "fluid_cell"))) {
                val variant = stack.getFluidCellVariant()
                if (variant != null && !variant.isBlank) {
                    val fluidId = BuiltInRegistries.FLUID.getId(variant.fluid)
                    lines.add(Component.translatable("fluid.${fluidId.namespace}.${fluidId.path}").formatted(ChatFormatting.GRAY))
                }
            }

            if (isWrenchItem(item)) {
                addWrenchTooltip(item, lines)
            }

            // 反应堆组件 tooltip：显示产热和散热信息
            if (item is ic2_120.content.reactor.IReactorComponent) {
                // 这里可以添加默认的产热和散热信息
                // 实际的产热和散热信息需要从反应堆方块实体中获取
                // lines.add(Component.literal("产热: 0").formatted(ChatFormatting.GRAY))
                // lines.add(Component.literal("散热: 0").formatted(ChatFormatting.GRAY))
            }

            // 所有实现 ITiered 的物品显示电压等级（导线已在上面处理）
            // 机器方块通过 @ModBlockEntity 映射获取 BlockEntity，再取 tier
            val tier = when {
                item is ITiered -> (item as ITiered).tier
                item is BlockItem && item.block is ITiered -> (item.block as ITiered).tier
                item is BlockItem -> {
                    val beType = ClassScanner.getBlockEntityTypeForBlock(item.block)
                    val be = beType?.instantiate(BlockPos.ORIGIN, item.block.defaultState)
                    (be as? ITieredMachine)?.tier
                }
                else -> null
            }
            if (tier != null) {
                addVoltageTierTooltip(lines, tier)
            }
        }
    }

    private fun addVoltageTierTooltip(lines: MutableList<Component>, tier: Int) {
        val tierName = "${tier}"
        lines.add(Component.translatable("tooltip.ic2_120.voltage_tier", tierName).formatted(ChatFormatting.GRAY))
    }

    private fun isWrenchItem(item: net.minecraft.world.item.Item): Boolean {
        val id = BuiltInRegistries.ITEM.getId(item)
        return id.namespace == Ic2_120.MOD_ID && (id.path == "wrench" || id.path == "electric_wrench")
    }

    private fun addWrenchTooltip(item: net.minecraft.world.item.Item, lines: MutableList<Component>) {
        lines.add(Component.translatable("tooltip.ic2_120.wrench.rotate_machine").formatted(ChatFormatting.GRAY))
        lines.add(Component.translatable("tooltip.ic2_120.wrench.pipe_toggle").formatted(ChatFormatting.GRAY))
        lines.add(Component.translatable("tooltip.ic2_120.wrench.break_machine").formatted(ChatFormatting.GRAY))
        lines.add(Component.translatable("tooltip.ic2_120.wrench.tank_retain").formatted(ChatFormatting.DARK_GRAY))

        val id = BuiltInRegistries.ITEM.getId(item)
        if (id.path == "electric_wrench") {
            lines.add(Component.translatable("tooltip.ic2_120.wrench.cost_energy").formatted(ChatFormatting.DARK_GRAY))
            lines.add(Component.translatable("tooltip.ic2_120.wrench.energy_required").formatted(ChatFormatting.DARK_GRAY))
        } else {
            lines.add(Component.translatable("tooltip.ic2_120.wrench.cost_durability").formatted(ChatFormatting.DARK_GRAY))
        }
    }

    /**
     * 添加变压器的 tooltip 信息
     * 格式：一个点的面 xxx EU/t | 五个点的面 xxx EU/t
     */
    private fun addTransformerTooltip(lines: MutableList<Component>, tier: Int, nextTier: Int) {
        val dotEu = ic2_120.content.block.machines.TransformerUtils.getEuForTier(nextTier)
        val sideEu = ic2_120.content.block.machines.TransformerUtils.getEuForTier(tier)
        lines.add(Component.translatable("tooltip.ic2_120.transformer.format", dotEu, sideEu).formatted(ChatFormatting.GRAY))
    }

    private fun addKineticTransmissionTooltip(lines: MutableList<Component>, capacityKu: Int, lossKu: Int) {
        lines.add(Component.translatable("tooltip.ic2_120.kinetic_capacity", capacityKu).formatted(ChatFormatting.GRAY))
        lines.add(Component.translatable("tooltip.ic2_120.kinetic_loss", lossKu).formatted(ChatFormatting.GRAY))
    }
}
