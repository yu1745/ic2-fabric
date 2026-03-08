package ic2_120.content

import net.minecraft.util.math.Direction
import team.reborn.energy.api.base.SimpleSidedEnergyContainer

/**
 * 分面能量容器，输入/输出均为**整机每 tick 总上限**（非每面独立上限）。
 *
 * - [maxInsertPerTick] / [maxExtractPerTick]：整个方块每 tick 的总输入/总输出上限，多面共享；
 * - 子类通过 [getSideMaxInsert]/[getSideMaxExtract] 指定各面是否可输入/输出及该面的“意愿上限”；
 * - 基类按 tick 累计实际输入/输出，保证整机不超限；
 * - 提供 [syncCommittedAmount] 以兼容外部直接改 amount（如读 NBT）。
 */
open class TickLimitedSidedEnergyContainer(
    private val capacity: Long,
    private val maxInsertPerTick: Long,
    private val maxExtractPerTick: Long,
    private val currentTickProvider: () -> Long? = { null }
) : SimpleSidedEnergyContainer() {

    private var budgetTrackedTick: Long = Long.MIN_VALUE
    private var insertedThisTick: Long = 0L
    private var extractedThisTick: Long = 0L
    private var lastCommittedAmount: Long = 0L

    override fun getCapacity(): Long = capacity

    final override fun getMaxInsert(side: Direction?): Long {
        normalizeTickBudget()
        val sideLimit = getSideMaxInsert(side).coerceAtLeast(0L).coerceAtMost(maxInsertPerTick)
        val remainingThisTick = (maxInsertPerTick - insertedThisTick).coerceAtLeast(0L)
        return minOf(sideLimit, remainingThisTick)
    }

    final override fun getMaxExtract(side: Direction?): Long {
        normalizeTickBudget()
        val sideLimit = getSideMaxExtract(side).coerceAtLeast(0L)
        val remainingThisTick = (maxExtractPerTick - extractedThisTick).coerceAtLeast(0L)
        return minOf(sideLimit, remainingThisTick)
    }

    final override fun onFinalCommit() {
        normalizeTickBudget()
        val delta = amount - lastCommittedAmount
        if (delta > 0L) {
            insertedThisTick = (insertedThisTick + delta).coerceAtMost(maxInsertPerTick)
        } else if (delta < 0L) {
            extractedThisTick = (extractedThisTick - delta).coerceAtMost(maxExtractPerTick)
        }
        lastCommittedAmount = amount
        onEnergyCommitted()
    }

    /** 外部直接改 amount（例如读 NBT）后调用，重置提交基线，避免首个事务误判为大额插入。 */
    fun syncCommittedAmount() {
        lastCommittedAmount = amount
    }

    /** 子类定义该面是否可输入及该面的意愿上限；整机总输入由 [maxInsertPerTick] 限制。 */
    protected open fun getSideMaxInsert(side: Direction?): Long = maxInsertPerTick

    /** 子类定义该面是否可输出及该面的意愿上限；整机总输出由 [maxExtractPerTick] 限制。 */
    protected open fun getSideMaxExtract(side: Direction?): Long = maxExtractPerTick

    /** 子类可在提交后同步 GUI/NBT 镜像字段。 */
    protected open fun onEnergyCommitted() = Unit

    private fun normalizeTickBudget() {
        val now = currentTickProvider() ?: return
        if (budgetTrackedTick != now) {
            budgetTrackedTick = now
            insertedThisTick = 0L
            extractedThisTick = 0L
        }
    }
}
