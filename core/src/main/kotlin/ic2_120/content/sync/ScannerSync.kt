package ic2_120.content.sync

import ic2_120.content.syncs.SyncSchema

/**
 * 扫描仪共享状态（能量、剩余使用次数）。
 * 由 ScreenHandler 通过 PropertyDelegate 同步到客户端。
 *
 * 注：早期版本曾支持「可配置 XYZ 扫描范围」，对应的 rangeX/Y/Z 同步属性已移除——
 * X/Z 范围始终由 [ScannerType.scanRadius] 固定，仅 Y 维度根据玩家高度动态计算（见
 * [ScannerScreenHandler.computeEnergyCost]）。同步 index 由 [SyncSchema] 按声明顺序自动分配，
 * 不依赖手写常量。
 */
class ScannerSync(schema: SyncSchema) {

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
