package ic2_120.content.item.energy

import ic2_120.content.energy.EnergyTier
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack

/**
 * 给玩家背包中的可充电物品充电，返回实际充入量（EU）。
 *
 * 充电优先级：主手 > 装备栏 > 物品栏(快捷栏) > 背包(主背包)
 * 对可堆叠物品：若堆叠数量 > 1，且有空位则拆成单个后充电；无空位则跳过。
 */
fun chargePlayerInventory(player: PlayerEntity, eu: Long): Long {
    var remaining = eu
    var charged = 0L
    val handled = mutableListOf<ItemStack>()

    fun chargeStack(target: ItemStack) {
        if (remaining <= 0L || target.isEmpty) return
        if (handled.any { it === target }) return
        handled += target

        val chargeTarget = when {
            target.count <= 1 -> target
            else -> {
                val emptySlot = player.inventory.emptySlot
                if (emptySlot < 0) return
                val single = target.copy()
                single.count = 1
                target.decrement(1)
                player.inventory.setStack(emptySlot, single)
                single
            }
        }

        when (val item = chargeTarget.item) {
            is IBatteryItem -> {
                val accepted = item.charge(chargeTarget, remaining)
                charged += accepted
                remaining -= accepted
            }

            is IElectricTool -> {
                val current = item.getEnergy(chargeTarget)
                val canAccept = (item.maxCapacity - current).coerceAtLeast(0L)
                if (canAccept <= 0L) return
                val accepted = minOf(remaining, canAccept)
                item.setEnergy(chargeTarget, current + accepted)
                charged += accepted
                remaining -= accepted
            }
        }
    }

    chargeStack(player.mainHandStack)
    for (target in player.inventory.armor) chargeStack(target)
    for (i in 0..8) chargeStack(player.inventory.getStack(i))
    for (i in 9 until player.inventory.main.size) chargeStack(player.inventory.getStack(i))

    return charged
}

/**
 * 以“每件物品独立限速”的方式给玩家物品充电，返回实际消耗的 EU。
 *
 * 规则：
 * - 仅充电实现了 [IBatteryItem] / [IElectricTool] 的物品
 * - 仅当物品 tier <= machineTier
 * - 每件物品每 tick 上限：EnergyTier.euPerTickFromTier(min(machineTier, item.tier))
 * - 每件物品独立计算上限，不共享单一总限额
 */
fun chargePlayerInventoryPerItemLimit(
    player: PlayerEntity,
    machineTier: Int,
    machineEnergyProvider: () -> Long,
    extractEnergy: (Long) -> Long
): Long {
    var charged = 0L
    val handled = mutableListOf<ItemStack>()

    fun chargeStack(target: ItemStack) {
        if (target.isEmpty) return
        if (handled.any { it === target }) return
        handled += target

        val item = target.item
        when (item) {
            is IBatteryItem -> {
                if (!item.canCharge || item.tier > machineTier || item.isFullyCharged(target)) return
                val perItemLimit = EnergyTier.euPerTickFromTier(minOf(machineTier, item.tier))
                val remaining = (item.maxCapacity - item.getCurrentCharge(target)).coerceAtLeast(0L)
                val machineAvailable = machineEnergyProvider().coerceAtLeast(0L)
                val requested = minOf(perItemLimit, remaining, machineAvailable)
                if (requested <= 0L) return

                val extracted = extractEnergy(requested).coerceIn(0L, requested)
                if (extracted <= 0L) return

                val accepted = item.charge(target, extracted).coerceIn(0L, extracted)
                if (accepted < extracted) {
                    item.discharge(target, extracted - accepted)
                }
                charged += accepted
            }

            is IElectricTool -> {
                if (item.tier > machineTier || item.isFullyCharged(target)) return
                val perItemLimit = EnergyTier.euPerTickFromTier(minOf(machineTier, item.tier))
                val current = item.getEnergy(target)
                val remaining = (item.maxCapacity - current).coerceAtLeast(0L)
                val machineAvailable = machineEnergyProvider().coerceAtLeast(0L)
                val requested = minOf(perItemLimit, remaining, machineAvailable)
                if (requested <= 0L) return

                val extracted = extractEnergy(requested).coerceIn(0L, requested)
                if (extracted <= 0L) return

                item.setEnergy(target, current + extracted)
                charged += extracted
            }
        }
    }

    chargeStack(player.mainHandStack)
    chargeStack(player.offHandStack)
    for (target in player.inventory.armor) chargeStack(target)
    for (i in 0 until player.inventory.main.size) chargeStack(player.inventory.getStack(i))

    return charged
}
