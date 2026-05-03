package ic2_120.content.item.armor

import ic2_120.config.Ic2Config
import ic2_120.content.item.ModArmorMaterials
import net.minecraft.item.ArmorItem
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import ic2_120.editCustomData
import ic2_120.getCustomData
import net.minecraft.item.Item
import net.minecraft.item.tooltip.TooltipType

/**
 * 喷气背包 (Jetpack)
 *
 * 使用生物燃料作为动力的飞行装置，类似创造飞行，按空格上升、Shift下降。
 */
open class JetpackItem : ArmorItem(
    ModArmorMaterials.JETPACK_ARMOR,
    ArmorItem.Type.CHESTPLATE,
    Item.Settings().maxCount(1).maxDamage(ArmorItem.Type.CHESTPLATE.getMaxDamage(ModArmorMaterials.JETPACK_DURABILITY_MULTIPLIER))
) {

    companion object {
        private const val FUEL_KEY = "Fuel"
        private const val IS_HOVER_KEY = "IsHover"
        private const val FLIGHT_ENABLED_KEY = "FlightEnabled"
        private const val FUEL_REMAINDER_KEY = "FuelRemainder"

        @JvmStatic
        val maxFuel: Long
            get() = Ic2Config.current.armor.jetpack.maxFuel

        @JvmStatic
        val MAX_FUEL: Long
            get() = Ic2Config.current.armor.jetpack.maxFuel

        @JvmStatic
        val fuelPerTick: Double
            get() = Ic2Config.getJetpackFuelPerTick()

        @JvmStatic
        val flightDurationSeconds: Int
            get() = Ic2Config.current.armor.jetpack.flightDurationSeconds

        @JvmStatic
        fun getFuel(stack: ItemStack): Long =
            (stack.getCustomData()?.getLong(FUEL_KEY) ?: 0L).coerceIn(0L, maxFuel)

        @JvmStatic
        fun setFuel(stack: ItemStack, fuel: Long) {
            stack.editCustomData { it.putLong(FUEL_KEY, fuel.coerceIn(0L, maxFuel)) }
        }

        @JvmStatic
        fun isHovering(stack: ItemStack): Boolean =
            stack.getCustomData()?.getBoolean(IS_HOVER_KEY) ?: false

        @JvmStatic
        fun setHovering(stack: ItemStack, hovering: Boolean) {
            stack.editCustomData { it.putBoolean(IS_HOVER_KEY, hovering) }
        }

        @JvmStatic
        fun isFlightEnabled(stack: ItemStack): Boolean =
            stack.getCustomData()?.getBoolean(FLIGHT_ENABLED_KEY) ?: false

        @JvmStatic
        fun setFlightEnabled(stack: ItemStack, enabled: Boolean) {
            stack.editCustomData { it.putBoolean(FLIGHT_ENABLED_KEY, enabled) }
        }

        /**
         * 使用余数累加器精确消耗燃料（支持小数消耗速率）
         * @return true 如果消耗成功，false 如果燃料不足
         */
        @JvmStatic
        fun consumeFuelPerTick(stack: ItemStack): Boolean {
            val fuel = getFuel(stack)
            if (fuel <= 0) return false
            val nbt = stack.orCreateNbt
            var remainder = nbt.getDouble(FUEL_REMAINDER_KEY)
            val cost = fuelPerTick
            remainder += cost
            val toConsume = remainder.toLong()
            if (toConsume <= 0) {
                nbt.putDouble(FUEL_REMAINDER_KEY, remainder)
                return true
            }
            if (fuel < toConsume) return false
            setFuel(stack, fuel - toConsume)
            nbt.putDouble(FUEL_REMAINDER_KEY, remainder - toConsume)
            return true
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
        context: Item.TooltipContext,
        tooltip: MutableList<Text>,
        type: TooltipType
    ) {
        super.appendTooltip(stack, context, tooltip, type)

        val fuel = getFuel(stack)
        val ratio = if (maxFuel > 0) fuel.toDouble() / maxFuel else 0.0
        val flightEnabled = isFlightEnabled(stack)

        // 计算剩余飞行时间（秒）
        val remainingSeconds = if (fuel > 0 && maxFuel > 0) {
            fuel.toDouble() / maxFuel * flightDurationSeconds
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
        tooltip.add(Text.literal("飞行: ").append(
            Text.translatable(if (flightEnabled) "tooltip.ic2_120.status.on" else "tooltip.ic2_120.status.off")
        ).append(Text.literal(" | 剩余: $timeText")).formatted(Formatting.GRAY))
    }

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
