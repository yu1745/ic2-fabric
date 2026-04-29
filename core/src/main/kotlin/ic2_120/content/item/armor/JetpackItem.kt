package ic2_120.content.item.armor

import ic2_120.config.Ic2Config
import ic2_120.content.item.ModArmorMaterials
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.item.ArmorItem
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Formatting

/**
 * 喷气背包 (Jetpack)
 *
 * 使用生物燃料作为动力的飞行装置。
 *
 * ## 核心参数
 *
 * - 燃料容量：30桶生物燃料（30,000 mB）
 * - 燃料消耗：2 mB/tick
 * - 飞行时间：满燃料约12.5分钟
 *
 * ## 飞行模式
 *
 * - **垂直模式**：类似创造飞行，按空格上升、Shift下降
 */
open class JetpackItem : ArmorItem(ModArmorMaterials.JETPACK_ARMOR, ArmorItem.Type.CHESTPLATE, FabricItemSettings().maxCount(1)) {

    companion object {
        private const val FUEL_KEY = "Fuel"
        private const val IS_HOVER_KEY = "IsHover"
        private const val FLIGHT_ENABLED_KEY = "FlightEnabled"

        @JvmStatic
        val maxFuel: Long
            get() = Ic2Config.current.armor.jetpack.maxFuel

        @JvmStatic
        val MAX_FUEL: Long
            get() = Ic2Config.current.armor.jetpack.maxFuel

        @JvmStatic
        val fuelPerTick: Long
            get() = Ic2Config.getJetpackFuelPerTick()

        @JvmStatic
        fun getFuel(stack: ItemStack): Long =
            stack.orCreateNbt.getLong(FUEL_KEY).coerceIn(0L, maxFuel)

        @JvmStatic
        fun setFuel(stack: ItemStack, fuel: Long) {
            stack.orCreateNbt.putLong(FUEL_KEY, fuel.coerceIn(0L, maxFuel))
        }

        @JvmStatic
        fun isHovering(stack: ItemStack): Boolean =
            stack.orCreateNbt.getBoolean(IS_HOVER_KEY)

        @JvmStatic
        fun setHovering(stack: ItemStack, hovering: Boolean) {
            stack.orCreateNbt.putBoolean(IS_HOVER_KEY, hovering)
        }

        @JvmStatic
        fun isFlightEnabled(stack: ItemStack): Boolean =
            stack.orCreateNbt.getBoolean(FLIGHT_ENABLED_KEY)

        @JvmStatic
        fun setFlightEnabled(stack: ItemStack, enabled: Boolean) {
            stack.orCreateNbt.putBoolean(FLIGHT_ENABLED_KEY, enabled)
        }

        @JvmStatic
        fun toggleFlightEnabled(stack: ItemStack): Boolean {
            val enabled = !isFlightEnabled(stack)
            setFlightEnabled(stack, enabled)
            return enabled
        }
    }

    override fun appendTooltip(
        stack: ItemStack,
        world: net.minecraft.world.World?,
        tooltip: MutableList<Text>,
        context: net.minecraft.client.item.TooltipContext
    ) {
        super.appendTooltip(stack, world, tooltip, context)

        val fuel = getFuel(stack)
        val ratio = if (maxFuel > 0) fuel.toDouble() / maxFuel else 0.0
        val flightEnabled = isFlightEnabled(stack)
        val flightStatusText = if (flightEnabled) "开启" else "关闭"

        // 计算剩余飞行时间（秒）
        val remainingSeconds = if (fuel > 0) {
            val ticks = fuel / fuelPerTick
            ticks / 20.0
        } else 0.0

        // 格式化时间
        val timeText = if (remainingSeconds >= 60) {
            val minutes = (remainingSeconds / 60).toInt()
            val seconds = (remainingSeconds % 60).toInt()
            "${minutes}分${seconds}秒"
        } else {
            "${remainingSeconds.toInt()}秒"
        }

        tooltip.add(Text.literal("燃料: %,d / %,d mB (%.1f%%)".format(fuel, maxFuel, ratio * 100)))
        tooltip.add(Text.literal("飞行: $flightStatusText | 剩余飞行: $timeText").formatted(Formatting.GRAY))
        tooltip.add(Text.literal("  Alt+M：切换飞行开关").formatted(Formatting.DARK_GRAY))
        tooltip.add(Text.literal("  创造式飞行：空格上升，Shift下降").formatted(Formatting.DARK_GRAY))
    }

    /**
     * 喷气背包使用燃料系统，不走原版耐久系统。
     * 若不禁用，受伤时会累积 Damage NBT 导致耐久条混乱。
     */
    override fun isDamageable(): Boolean = false

    override fun isItemBarVisible(stack: ItemStack): Boolean = true

    override fun getItemBarStep(stack: ItemStack): Int =
        ((getFuel(stack).toDouble() / maxFuel) * 13).toInt().coerceIn(0, 13)

    override fun getItemBarColor(stack: ItemStack): Int {
        val ratio = getFuel(stack).toDouble() / maxFuel
        return when {
            ratio > 0.5 -> 0x4AFF4A
            ratio > 0.2 -> 0xFFFF4A
            else -> 0xFF4A4A
        }
    }
}