package ic2_120.content.upgrade

/**
 * 机器支持能量存储升级的接口。
 *
 * 每个储能升级增加 1 万 EU 电量缓冲，由 [EnergyStorageUpgradeComponent] 统计并写入。
 */
interface IEnergyStorageUpgradeSupport {

    /** 储能升级带来的额外容量（EU），由组件每 tick 写入 */
    var capacityBonus: Long
}
