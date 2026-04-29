package ic2_120.content.sync

import ic2_120.content.syncs.SyncSchema

/**
 * 扫描仪共享状态（能量、剩余使用次数、扫描半径）。
 * 由 ScreenHandler 通过 PropertyDelegate 同步到客户端。
 */
class ScannerSync(schema: SyncSchema) {
    companion object {
        const val PROPERTY_ENERGY = 0
        const val PROPERTY_ENERGY_CAPACITY = 1
        const val PROPERTY_USES_REMAINING = 2
        const val PROPERTY_MAX_USES = 3
        const val PROPERTY_COUNT = 4
    }

    var energy by schema.int("Energy")
    var energyCapacity by schema.int("EnergyCapacity")
    var usesRemaining by schema.int("UsesRemaining")
    var maxUses by schema.int("MaxUses")

    fun init(energy: Int, energyCapacity: Int, usesRemaining: Int, maxUses: Int) {
        this.energy = energy
        this.energyCapacity = energyCapacity
        this.usesRemaining = usesRemaining
        this.maxUses = maxUses
    }

    fun consumeEnergy(amount: Int): Boolean {
        if (energy < amount) return false
        energy -= amount
        return true
    }

    fun consumeUse(): Boolean {
        if (usesRemaining <= 0) return false
        usesRemaining--
        return true
    }
}
