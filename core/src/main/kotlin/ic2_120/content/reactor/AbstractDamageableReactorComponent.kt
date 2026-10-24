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
 *
 * 堆叠语义：vanilla 的 Settings.maxDamage(maxUse) 会把 maxCount 强制为 1，
 * 且 Item.getMaxCount() 是 final 无法在 Item 里覆盖，因此这类器件默认完全不
 * 可堆叠。堆叠能力由 [ic2_120.mixin.ReactorComponentStackMixin] 放开：
 *
 * - getMaxCount() 对本品返回 [MAX_STACK_SIZE]（64）
 * - isStackable() 对本品恒为 true（vanilla 对已损耗物品禁止合并）
 *
 * 合并本身仍由 vanilla ItemStack.canCombine 把关（item 相同 + NBT 完全一致，
 * 含 "use"/"Damage" 标签）：**寿命（热量）相同的组件可以堆叠，寿命不同的
 * 永远不能堆叠**。AE2 模糊卡等按 vanilla 耐久工作的外部 mod 不受影响。
 */
abstract class AbstractDamageableReactorComponent(
    settings: FabricItemSettings,
    protected val maxUse: Int
) : AbstractReactorComponent(settings.maxDamage(maxUse)) {

    companion object {
        /**
         * 反应堆组件的堆叠上限。NBT（"use"/"Damage"）完全一致的组件允许堆叠到该数量，
         * 不一致的组件由 canCombine 拒绝合并。
         */
        const val MAX_STACK_SIZE = 64
    }

    protected fun getUse(stack: ItemStack): Int {
        val nbt = stack.nbt ?: return 0
        return nbt.getInt("use").coerceIn(0, maxUse)
    }

    /**
     * 燃料棒是否尚未枯竭（用于中子反射板等邻接判定）
     */
    fun isOperationalFuelRod(stack: ItemStack): Boolean = getUse(stack) < maxUse - 1

    fun setUse(stack: ItemStack, use: Int) {
        val clamped = use.coerceIn(0, maxUse)
        val nbt = stack.orCreateNbt
        nbt.putInt("use", clamped)
        // AE2 模糊卡按 NBT "Damage" 标签 + Item.getMaxDamage() 匹配耐久（Settings.maxDamage 已设 maxUse）；
        // 同步镜像标签，让 AE2（及其他按 vanilla 耐久工作的 mod）能按热值过滤元件。
        nbt.putInt("Damage", clamped)
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
