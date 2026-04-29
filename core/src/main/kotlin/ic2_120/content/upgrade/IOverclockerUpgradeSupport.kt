package ic2_120.content.upgrade

/**
 * 机器支持加速升级的接口。
 *
 * 实现此接口的机器可接收 [OverclockerUpgradeComponent] 计算的速度与耗能倍率。
 * 倍率由升级槽中加速升级数量累乘得出（如 1 个 = 2x，2 个 = 4x）。
 */
interface IOverclockerUpgradeSupport {

    /** 速度倍率（≥1），加工进度每 tick 增量按此倍率放大 */
    var speedMultiplier: Float

    /** 耗能倍率（≥1），每 tick 能量消耗按此倍率放大 */
    var energyMultiplier: Float
}
