package ic2_120.content.reactor

import ic2_120.content.item.AbstractMoxFuelRodItem
import ic2_120.content.item.AbstractUraniumFuelRodItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import ic2_120.editCustomData
import ic2_120.getCustomData

/**
 * 带脉冲耐久的中子反射板。NBT `use` 为已消耗的脉冲数，枯竭后槽位清空且不再提供脉冲加成。
 */
abstract class AbstractFiniteNeutronReflectorItem(
    settings: Item.Properties,
    private val maxPulses: Int
) : AbstractReactorComponent(settings) {

    private fun getConsumed(stack: ItemStack): Int =
        (stack.getCustomData()?.getIntOr("use", 0) ?: 0).coerceIn(0, maxPulses)

    private fun setConsumed(stack: ItemStack, value: Int) {
        stack.editCustomData { it.putInt("use", value.coerceIn(0, maxPulses)) }
    }

    private fun isDepleted(stack: ItemStack): Boolean = getConsumed(stack) >= maxPulses

    private fun remainingPulses(stack: ItemStack): Int =
        (maxPulses - getConsumed(stack)).coerceAtLeast(0)

    override fun processChamber(stack: ItemStack, reactor: IReactor, x: Int, y: Int, heatRun: Boolean) {
        if (heatRun) return
        if (!reactor.produceEnergy()) return
        if (isDepleted(stack)) return

        var drain = 0
        drain += reactor.getItemAt(x - 1, y)?.let(::neighborFuelDrainWeight) ?: 0
        drain += reactor.getItemAt(x + 1, y)?.let(::neighborFuelDrainWeight) ?: 0
        drain += reactor.getItemAt(x, y - 1)?.let(::neighborFuelDrainWeight) ?: 0
        drain += reactor.getItemAt(x, y + 1)?.let(::neighborFuelDrainWeight) ?: 0
        if (drain == 0) return

        val next = (getConsumed(stack) + drain).coerceAtMost(maxPulses)
        setConsumed(stack, next)
        if (next >= maxPulses) {
            reactor.setItemAt(x, y, ItemStack.EMPTY)
        }
    }

    override fun acceptUraniumPulse(
        stack: ItemStack,
        reactor: IReactor,
        pulsingStack: ItemStack,
        youX: Int,
        youY: Int,
        pulseX: Int,
        pulseY: Int,
        heatRun: Boolean
    ): Boolean = !isDepleted(stack)

    override fun isBarVisible(stack: ItemStack): Boolean = !isDepleted(stack)

    override fun getBarColor(stack: ItemStack): Int {
        val frac = remainingPulses(stack).toDouble() / maxPulses
        return when {
            frac > 0.75 -> 0x00FF00
            frac > 0.5 -> 0xFFDD00
            frac > 0.25 -> 0xFFAA00
            else -> 0xFF0000
        }
    }

    override fun getBarWidth(stack: ItemStack): Int {
        val frac = remainingPulses(stack).toDouble() / maxPulses
        return (13.0 * frac).toInt().coerceIn(0, 13)
    }

    override fun appendHoverText(stack: ItemStack, context: Item.TooltipContext, display: net.minecraft.world.item.component.TooltipDisplay, consumer: java.util.function.Consumer<Component>, type: TooltipFlag) {
        val tooltip = ic2_120.compat.TooltipSinkList(consumer)
        super.appendHoverText(stack, context, display, consumer, type)
        if (!isDepleted(stack)) {
            tooltip.add(
                Component.translatable("tooltip.ic2_120.neutron_reflector_pulses", remainingPulses(stack))
                    .withStyle(ChatFormatting.GRAY)
            )
        }
    }
}

private fun neighborFuelDrainWeight(stack: ItemStack): Int {
    val i = stack.item
    return when (i) {
        is AbstractUraniumFuelRodItem -> if (i.isOperationalFuelRod(stack)) i.numberOfCells else 0
        is AbstractMoxFuelRodItem -> if (i.isOperationalFuelRod(stack)) i.numberOfCells else 0
        else -> 0
    }
}
