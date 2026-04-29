package ic2_120.content.block.transmission

import net.minecraft.util.math.Direction

/**
 * 动能机器端口/存储接口（类 Storage 语义）。
 *
 * 设计目标：
 * - 不再依赖“模拟插入”来探测可插入量。
 * - 显式提供缓存剩余量和当前最大可插入量。
 */
interface IKineticMachinePort {
    fun canOutputKuTo(side: Direction): Boolean = false
    fun canInputKuFrom(side: Direction): Boolean = false

    /**
     * 当前已缓存 KU（若机器无缓存可返回 0）。
     */
    fun getStoredKu(side: Direction): Int = 0

    /**
     * 最大 KU 缓存容量（若机器无缓存可返回 0）。
     */
    fun getKuCapacity(side: Direction): Int = 0

    /**
     * 当前剩余可缓存 KU（容量 - 已缓存，最小为 0）。
     */
    fun getRemainingKuBuffer(side: Direction): Int =
        (getKuCapacity(side) - getStoredKu(side)).coerceAtLeast(0)

    /**
     * 本 tick/当前状态下最多还能插入多少 KU。
     * 可包含“端口朝向”和“单 tick 速率上限”等限制。
     */
    fun getMaxInsertableKu(side: Direction): Int =
        if (canInputKuFrom(side)) getRemainingKuBuffer(side) else 0

    /**
     * 本 tick/当前状态下最多还能提取多少 KU。
     * 可包含“端口朝向”和“单 tick 输出上限”等限制。
     */
    fun getMaxExtractableKu(side: Direction): Int =
        if (canOutputKuTo(side)) getStoredKu(side).coerceAtLeast(0) else 0

    /**
     * 向端口插入 KU，返回实际接受量。
     *
     * @param side 从机器视角的交互侧
     * @param amount 请求插入 KU
     * @param simulate true 仅模拟，不改变状态
     */
    fun insertKu(side: Direction, amount: Int, simulate: Boolean): Int = 0

    /**
     * 从端口提取 KU，返回实际提取量。
     *
     * @param side 从机器视角的交互侧
     * @param amount 请求提取 KU
     * @param simulate true 仅模拟，不改变状态
     */
    fun extractKu(side: Direction, amount: Int, simulate: Boolean): Int = 0
}
