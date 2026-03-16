package ic2_120.content.item.energy

import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.type
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.text.Text

/**
 * 电池物品基类
 *
 * 提供电池的通用功能：
 * - NBT 存储电量
 * - 充放电逻辑
 * - 工具提示显示电量
 *
 * @param name 物品 ID（不含命名空间）
 * @param tier 能量等级（1-4）
 * @param maxCapacity 最大容量（EU）
 * @param transferSpeed 充放电速度（EU/t）
 * @param canChargeWireless 是否可以无线充电
 * @param settings 物品设置（可选）
 */
abstract class BatteryItemBase(
    private val name: String,
    override val tier: Int,
    override val maxCapacity: Long,
    override val transferSpeed: Int,
    override val canChargeWireless: Boolean,
    settings: FabricItemSettings = FabricItemSettings()
) : Item(settings), IBatteryItem {

    companion object {
        /** NBT 键：存储电量 */
        const val ENERGY_KEY = "Energy"

        /**
         * 从物品栈获取电量
         */
        fun getEnergy(stack: ItemStack): Long {
            val nbt = stack.orCreateNbt
            return nbt.getLong(ENERGY_KEY)
        }

        /**
         * 设置物品栈的电量
         * @param stack 物品栈
         * @param energy 电量值（会被限制在有效范围内）
         * @param maxCapacity 最大容量（用于限制上限）
         */
        fun setEnergy(stack: ItemStack, energy: Long, maxCapacity: Long) {
            val nbt = stack.orCreateNbt
            val clampedEnergy = energy.coerceIn(0, maxCapacity)
            nbt.putLong(ENERGY_KEY, clampedEnergy)
        }

        /**
         * 格式化电量显示
         * @param energy 当前电量
         * @param maxCapacity 最大容量
         * @return 格式化后的文本（如 "3,000 / 10,000 EU"）
         */
        fun formatEnergy(energy: Long, maxCapacity: Long): String {
            return "%,d / %,d EU".format(energy, maxCapacity)
        }

        /**
         * 格式化电量百分比
         * @param ratio 电量比例（0.0 - 1.0）
         * @return 格式化后的文本（如 "30.0%"）
         */
        fun formatPercentage(ratio: Double): String {
            return "%.1f%%".format(ratio * 100)
        }
    }

    init {
        // 验证参数
        require(tier in 1..4) { "电池等级必须在 1-4 之间，当前值: $tier" }
        require(maxCapacity > 0) { "最大容量必须大于 0，当前值: $maxCapacity" }
        require(transferSpeed > 0) { "传输速度必须大于 0，当前值: $transferSpeed" }
    }

    override fun getCurrentCharge(stack: ItemStack): Long {
        return getEnergy(stack)
    }

    override fun setCurrentCharge(stack: ItemStack, charge: Long) {
        setEnergy(stack, charge, maxCapacity)
    }

    /**
     * 添加工具提示（电量显示）
     * 子类可以重写此方法添加额外的提示信息
     */
    override fun appendTooltip(
        stack: ItemStack,
        world: net.minecraft.world.World?,
        tooltip: MutableList<Text>,
        context: net.minecraft.client.item.TooltipContext
    ) {
        super.appendTooltip(stack, world, tooltip, context)

        val energy = getCurrentCharge(stack)
        val ratio = getChargeRatio(stack)

        // 电量文本：如 "3,000 / 10,000 EU (30.0%)"
        tooltip.add(
            Text.literal("⚡ ${formatEnergy(energy, maxCapacity)} (${formatPercentage(ratio)})")
        )

        // 等级和速度信息
        tooltip.add(
            Text.literal("等级: $tier | 速度: $transferSpeed EU/t")
                .formatted(net.minecraft.util.Formatting.GRAY)
        )

        // 无线充电提示
        if (canChargeWireless) {
            tooltip.add(
                Text.literal("可无线充电")
                    .formatted(net.minecraft.util.Formatting.AQUA)
            )
        }
    }

    /**
     * 物品是否具有特效（用于显示电量条）
     */
    override fun isItemBarVisible(stack: ItemStack): Boolean {
        return true
    }

    /**
     * 获取物品栏填充度（用于显示电量条）
     * @return 0.0（空）到 1.0（满）
     */
    override fun getItemBarStep(stack: ItemStack): Int {
        val ratio = getChargeRatio(stack)
        return (ratio * 13).toInt().coerceIn(0, 13)
    }

    /**
     * 获取物品栏颜色（根据电量变化）
     */
    override fun getItemBarColor(stack: ItemStack): Int {
        val ratio = getChargeRatio(stack)
        return when {
            ratio > 0.5 -> 0x4AFF4A // 绿色（电量充足）
            ratio > 0.2 -> 0xFFFF4A // 黄色（中等电量）
            else -> 0xFF4A4A // 红色（低电量）
        }
    }
}
