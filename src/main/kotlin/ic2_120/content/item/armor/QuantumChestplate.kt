package ic2_120.content.item.armor

import ic2_120.content.item.ModArmorMaterials
import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.type
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.item.ArmorItem
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Formatting

/**
 * 量子胸甲 (Quantum Chestplate)
 *
 * 量子套装的胸甲部件，提供飞行功能。
 *
 * ## 核心参数
 *
 * - 载电量：10,000,000 EU（10 MEU）
 * - 能量等级：4
 * - 减伤比例：44%
 *
 * ## 飞行功能
 *
 * - 耗电：8333 EU/tick（10M EU / 20分钟 = 24000 ticks）
 * - 要求全套量子护甲
 * - 快捷键：Alt + F
 *
 * ## 能量消耗
 *
 * - 每减免 1 点伤害消耗 5000 EU
 * - 能量从所有量子装备均匀扣除
 */
@ModItem(name = "quantum_chestplate", tab = CreativeTab.IC2_MATERIALS, group = "quantum_armor")
class QuantumChestplate : QuantumArmorItem(ModArmorMaterials.QUANTUM_ARMOR, ArmorItem.Type.CHESTPLATE, FabricItemSettings().maxCount(1)) {

    companion object {
        private const val FLIGHT_KEY = "QuantumFlightEnabled"
        private const val FLIGHT_COST = 417L  // 10M EU / 20min = 24000 ticks ≈ 417 EU/tick

        fun toggleFlight(stack: ItemStack): Boolean {
            val nbt = stack.orCreateNbt
            val enabled = !nbt.getBoolean(FLIGHT_KEY)
            nbt.putBoolean(FLIGHT_KEY, enabled)
            return enabled
        }

        fun isFlightEnabled(stack: ItemStack): Boolean =
            stack.orCreateNbt.getBoolean(FLIGHT_KEY)
    }

    override fun appendTooltip(stack: ItemStack, world: net.minecraft.world.World?, tooltip: MutableList<Text>, context: net.minecraft.client.item.TooltipContext) {
        super.appendTooltip(stack, world, tooltip, context)
        val flightEnabled = stack.orCreateNbt.getBoolean(FLIGHT_KEY)
        val energy = getEnergy(stack)

        // 计算飞行剩余时间（分钟）
        val remainingMinutes = if (energy > 0 && flightEnabled) {
            val ticks = energy / FLIGHT_COST
            val seconds = ticks / 20.0
            val minutes = seconds / 60.0
            "%.1f".format(minutes)
        } else "N/A"

        tooltip.add(Text.literal("飞行: ${if (flightEnabled) "§aON" else "§cOFF"} §8[${remainingMinutes}分钟]").formatted(Formatting.GRAY))
        tooltip.add(Text.literal("减伤: 44% | 需全套量子护甲").formatted(Formatting.GRAY))
    }
}
