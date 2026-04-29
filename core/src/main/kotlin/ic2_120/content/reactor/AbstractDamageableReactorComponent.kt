package ic2_120.content.reactor

import net.minecraft.client.item.TooltipContext
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.world.World
import net.fabricmc.fabric.api.item.v1.FabricItemSettings

/**
 * 带耐久/热容的反应堆组件。使用 NBT "use" 存储热量，
 * 覆盖 Item 的方法显示耐久度条。
 */
abstract class AbstractDamageableReactorComponent(
    settings: FabricItemSettings,
    protected val maxUse: Int
) : AbstractReactorComponent(settings) {

    protected fun getUse(stack: ItemStack): Int {
        val nbt = stack.nbt ?: return 0
        return nbt.getInt("use").coerceIn(0, maxUse)
    }

    /** 燃料棒是否尚未枯竭（用于中子反射板等邻接判定） */
    fun isOperationalFuelRod(stack: ItemStack): Boolean = getUse(stack) < maxUse - 1

    fun setUse(stack: ItemStack, use: Int) {
        stack.orCreateNbt.putInt("use", use.coerceIn(0, maxUse))
    }

    protected fun incrementUse(stack: ItemStack) {
        setUse(stack, (getUse(stack) + 1).coerceAtMost(maxUse))
    }

    fun getUseFraction(stack: ItemStack): Double =
        (getUse(stack).toDouble() / maxUse).coerceIn(0.0, 1.0)

    private fun getRemainingFraction(stack: ItemStack): Double =
        1.0 - getUseFraction(stack)

    override fun isItemBarVisible(stack: ItemStack): Boolean = true

    override fun getItemBarColor(stack: ItemStack): Int {
        val remaining = getRemainingFraction(stack)
        return when {
            remaining > 0.75 -> 0x00FF00
            remaining > 0.5 -> 0xFFDD00
            remaining > 0.25 -> 0xFFAA00
            else -> 0xFF0000
        }
    }

    override fun getItemBarStep(stack: ItemStack): Int {
        val remaining = getRemainingFraction(stack)
        return (13.0 * remaining).toInt().coerceIn(0, 13)
    }

    override fun appendTooltip(stack: ItemStack, world: World?, tooltip: MutableList<Text>, context: TooltipContext) {
        super.appendTooltip(stack, world, tooltip, context)
        val remaining = maxUse - getUse(stack)
        tooltip.add(Text.translatable("tooltip.ic2_120.reactor_durability", remaining, maxUse).formatted(Formatting.GRAY))
    }
}
