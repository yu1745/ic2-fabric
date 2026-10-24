package ic2_120.content.item.energy

import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.world.World

/**
 * 无线充电电池基类
 *
 * 继承自 BatteryItemBase，增加无线充电功能。
 * 容量为同等级普通电池的4倍，速度不变。
 *
 * @param name 物品 ID
 * @param tier 能量等级
 * @param baseMaxCapacity 同等级普通电池的最大容量
 */
abstract class WirelessBatteryItemBase(
    name: String,
    tier: Int,
    baseMaxCapacity: Long
) : BatteryItemBase(
    name = name,
    tier = tier,
    maxCapacity = baseMaxCapacity * 4, // 容量是普通版本的4倍
    canChargeWireless = true // 支持无线充电
) {
    override fun inventoryTick(
        stack: ItemStack,
        world: World,
        entity: Entity,
        slot: Int,
        selected: Boolean
    ) {
        super.inventoryTick(stack, world, entity, slot, selected)
        if (world.isClient) return
        val player = entity as? PlayerEntity ?: return
        autoChargeEquipment(stack, player)
    }

    /**
     * 给玩家物品栏中的电动工具与能量护甲（[IElectricTool]）充电。
     *
     * - 不充 [IBatteryItem] 电池，避免多枚无线电池互充死循环。
     * - 仅当目标 [IElectricTool.tier] 小于等于本电池 [tier] 时才会充入。
     *
     * @param player 玩家
     * @param amount 尝试充电的电量（EU）
     * @return 实际充入所有目标的电量总和（EU）
     */
    fun chargeEquipment(
        player: net.minecraft.entity.player.PlayerEntity,
        amount: Long
    ): Long {
        var remaining = amount
        var charged = 0L

        val inventory = player.inventory
        for (i in 0 until inventory.size()) {
            if (remaining <= 0) break

            val stack = inventory.getStack(i)
            if (stack.isEmpty) continue
            if (stack.item === this) continue

            val item = stack.item
            if (item is IBatteryItem) continue
            val tool = item as? IElectricTool ?: continue
            if (tool.tier > tier) continue

            val current = tool.getEnergy(stack)
            val maxCap = tool.maxCapacity
            val canAccept = (maxCap - current).coerceAtLeast(0L)
            if (canAccept <= 0L) continue

            val toAdd = minOf(remaining, canAccept)
            tool.setEnergy(stack, current + toAdd)
            charged += toAdd
            remaining -= toAdd
        }

        return charged
    }

    /**
     * 自动给背包内符合条件的 [IElectricTool] 充电（每 tick 使用 [nominalEuPerTick] 作为本帧上限）。
     */
    fun autoChargeEquipment(stack: net.minecraft.item.ItemStack, player: net.minecraft.entity.player.PlayerEntity) {
        if (!canChargeWireless) return
        // 堆叠的无线电池不能触发充电，避免多耗电（N 倍于堆叠数）
        if (stack.count > 1) return

        val energy = getCurrentCharge(stack)
        if (energy <= 0) return

        val charged = chargeEquipment(player, nominalEuPerTick())
        if (charged > 0) {
            discharge(stack, charged)
        }
    }
}
