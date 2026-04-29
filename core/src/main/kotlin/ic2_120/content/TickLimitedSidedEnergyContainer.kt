package ic2_120.content

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant
import net.minecraft.util.math.Direction
import team.reborn.energy.api.EnergyStorage

/**
 * 分面能量容器，输入/输出均为**整机每 tick 总上限**（非每面独立上限）。
 *
 * - [maxInsertPerTick] / [maxExtractPerTick]：整个方块每 tick 的总输入/总输出上限，多面共享；
 * - 子类通过 [getSideMaxInsert]/[getSideMaxExtract] 指定各面是否可输入/输出及该面的"意愿上限"；
 * - 基类按 tick 累计实际输入/输出，保证整机不超限；
 * - 提供 [syncCommittedAmount] 以兼容外部直接改 amount（如读 NBT）。
 *
 * 三个能量速度指标：
 * - 输入速度：外部 → 机器（通过 Transfer API 插入）
 * - 输出速度：机器 → 外部（通过 Transfer API 提取）
 * - 耗能速度：机器内部消耗（通过 [consumeEnergy] 方法）
 */
open class TickLimitedSidedEnergyContainer(
    private val baseCapacity: Long,
    protected val maxInsertPerTick: Long,
    protected val maxExtractPerTick: Long,
    private val currentTickProvider: () -> Long? = { null }
) {

    /** 当前存储的能量 */
    var amount: Long = 0L

    /** 容量上限 */
    open val capacity: Long
        get() = baseCapacity

    private val sideStorages = mutableMapOf<Direction?, EnergyStorage>()

    init {
        // 为所有方向创建 SideStorage
        sideStorages[null] = createSideStorage(null)
        Direction.entries.forEach { dir ->
            sideStorages[dir] = createSideStorage(dir)
        }
    }

    /**
     * 获取指定方向的 EnergyStorage 实现。
     * 用于 ClassScanner 注册时按面注册。
     */
    fun getSideStorage(side: Direction?): EnergyStorage {
        return sideStorages[side]!!
    }

    private fun createSideStorage(side: Direction?): EnergyStorage {
        return object : SnapshotParticipant<Long>(), EnergyStorage {
            //SnapshotParticipant传入的tx可以通过createSnapshot创建快照，然后事务的实现类可以用readSnapshot回滚
            override fun createSnapshot(): Long {
//                println("createSnapshot($amount)")
                return this@TickLimitedSidedEnergyContainer.amount
            }

            override fun readSnapshot(snapshot: Long) {
//                println("readSnapshot($snapshot)")
                // 回滚时恢复 amount
                this@TickLimitedSidedEnergyContainer.amount = snapshot
            }

            override fun onFinalCommit() {
                // 最外层事务提交后调用，更新 tick 预算
                this@TickLimitedSidedEnergyContainer.onFinalCommit()
            }

            override fun supportsInsertion(): Boolean {
                return this@TickLimitedSidedEnergyContainer.getMaxInsert(side) > 0
            }

            override fun insert(maxAmount: Long, transaction: TransactionContext): Long {
                val maxAllowed = this@TickLimitedSidedEnergyContainer.getMaxInsert(side)
//                println("maxAllowed:$maxAllowed")
                val space = (capacity - this@TickLimitedSidedEnergyContainer.amount).coerceAtLeast(0)
                val toInsert = minOf(maxAmount, maxAllowed, space)

                if (toInsert > 0) {
                    updateSnapshots(transaction)
                    this@TickLimitedSidedEnergyContainer.amount += toInsert
//                    println("insert:$toInsert")
                    return toInsert
                }
                return 0
            }

            override fun supportsExtraction(): Boolean {
                return this@TickLimitedSidedEnergyContainer.getMaxExtract(side) > 0
            }

            override fun extract(maxAmount: Long, transaction: TransactionContext): Long {
                val maxAllowed = this@TickLimitedSidedEnergyContainer.getMaxExtract(side)
                val toExtract = minOf(maxAmount, this@TickLimitedSidedEnergyContainer.amount, maxAllowed)
//                println("toExtract: $toExtract")
                if (toExtract > 0) {
                    updateSnapshots(transaction)
                    this@TickLimitedSidedEnergyContainer.amount -= toExtract
                    return toExtract
                }
                return 0
            }

            override fun getAmount(): Long = this@TickLimitedSidedEnergyContainer.amount

            override fun getCapacity(): Long = this@TickLimitedSidedEnergyContainer.capacity
        }
    }

    // ========== 容量与限制 ==========

    /**
     * 获取指定方向的最大输入量（包含 tick 限制）。
     * 子类通常不需要覆写此方法，而是覆写 [getSideMaxInsert]。
     */
    open fun getMaxInsert(side: Direction?): Long {
        normalizeTickBudget()
        val sideRaw = getSideMaxInsert(side)
        val sideLimit = sideRaw.coerceAtLeast(0L)
        val remainingThisTick = (sideRaw - insertedThisTick).coerceAtLeast(0L)
        return minOf(sideLimit, remainingThisTick)
    }

    /**
     * 获取指定方向的最大输出量（包含 tick 限制）。
     * 子类通常不需要覆写此方法，而是覆写 [getSideMaxExtract]。
     */
    open fun getMaxExtract(side: Direction?): Long {
        normalizeTickBudget()
        val sideLimit = getSideMaxExtract(side).coerceAtLeast(0L)
        val remainingThisTick = (maxExtractPerTick - extractedThisTick).coerceAtLeast(0L)
        return minOf(sideLimit, remainingThisTick)
    }

    // ========== Tick 预算管理 ==========

    private var budgetTrackedTick: Long = Long.MIN_VALUE
    private var insertedThisTick: Long = 0L
    private var extractedThisTick: Long = 0L
    private var consumedThisTick: Long = 0L
    private var generatedThisTick: Long = 0L
    private var lastInsertedAmount: Long = 0L
    private var lastExtractedAmount: Long = 0L
    private var lastConsumedAmount: Long = 0L
    private var lastGeneratedAmount: Long = 0L
    private var lastCommittedAmount: Long = amount

    /**
     * 提交后的回调，用于 tick 预算管理。
     * 子类如果覆写，必须调用 super.onFinalCommit()。
     */
    open fun onFinalCommit() {
        normalizeTickBudget()
        val delta = amount - lastCommittedAmount
        if (delta > 0L) {
            insertedThisTick += delta
        } else if (delta < 0L) {
            extractedThisTick -= delta
        }
        lastCommittedAmount = amount
        onEnergyCommitted()
    }

    private fun normalizeTickBudget() {
        val now = currentTickProvider() ?: return
        if (budgetTrackedTick != now) {
            // 保存上一次 tick 的实际输入/输出/耗能
            lastInsertedAmount = insertedThisTick
            lastExtractedAmount = extractedThisTick
            lastConsumedAmount = consumedThisTick
            lastGeneratedAmount = generatedThisTick
            // 重置当前 tick 的累计值
            budgetTrackedTick = now
            insertedThisTick = 0L
            extractedThisTick = 0L
            consumedThisTick = 0L
            generatedThisTick = 0L
        }
    }

    // ========== 能量速度指标 ==========

    /** 获取上一次 tick 的实际输入量（EU/t） */
    fun getLastInsertedAmount(): Long = lastInsertedAmount

    /** 获取上一次 tick 的实际输出量（EU/t） */
    fun getLastExtractedAmount(): Long = lastExtractedAmount

    /** 获取上一次 tick 的实际耗能量（EU/t） */
    fun getLastConsumedAmount(): Long = lastConsumedAmount

    fun getLastGeneratedAmount(): Long = lastGeneratedAmount

    fun finalizeFlowSnapshot() {
        normalizeTickBudget()
    }

    /** 获取当前 tick 的累计输入量（子类用于同步） */
    protected fun getCurrentTickInserted(): Long = insertedThisTick

    /** 获取当前 tick 的累计输出量（子类用于同步） */
    protected fun getCurrentTickExtracted(): Long = extractedThisTick

    /** 获取当前 tick 的累计耗能量（子类用于同步） */
    protected fun getCurrentTickConsumed(): Long = consumedThisTick

    /** 获取当前 tick 的累计发电量（子类用于同步） */
    protected fun getCurrentTickGenerated(): Long = generatedThisTick

    /** 对外暴露：当前 tick 的累计输入量（用于同步层复用） */
    fun getCurrentTickInsertedAmount(): Long = insertedThisTick

    /** 对外暴露：当前 tick 的累计输出量（用于同步层复用） */
    fun getCurrentTickExtractedAmount(): Long = extractedThisTick

    /** 对外暴露：当前 tick 的累计耗能量（用于同步层复用） */
    fun getCurrentTickConsumedAmount(): Long = consumedThisTick

    /** 对外暴露：当前 tick 的累计发电量（用于同步层复用） */
    fun getCurrentTickGeneratedAmount(): Long = generatedThisTick

    /**
     * 统一内部发电入口：发电机等应使用此方法。
     * 返回实际增加量（会受容量限制）。
     */
    fun generateEnergy(requested: Long): Long {
        if (requested <= 0L) return 0L
        val space = (capacity - amount).coerceAtLeast(0L)
        val actual = minOf(requested, space)
        if (actual <= 0L) return 0L
        amount += actual
        trackInternalMutation {
            generatedThisTick += actual
        }
        return actual
    }

    /**
     * 统一内部注能入口（非事务路径，如放电槽回充）。
     * 返回实际增加量（会受容量限制）。
     */
    fun insertEnergy(requested: Long): Long {
        if (requested <= 0L) return 0L
        val space = (capacity - amount).coerceAtLeast(0L)
        val actual = minOf(requested, space)
        if (actual <= 0L) return 0L
        amount += actual
        trackInternalMutation {
            insertedThisTick += actual
        }
        return actual
    }

    /**
     * 统一内部取能入口（机器给电池/电动工具充电等）。
     * 返回实际取出量。
     */
    fun extractEnergy(requested: Long): Long {
        if (requested <= 0L) return 0L
        val actual = minOf(requested, amount)
        if (actual <= 0L) return 0L
        amount -= actual
        trackInternalMutation {
            extractedThisTick += actual
        }
        return actual
    }

    /**
     * 统一内部耗能入口（加工、攻击等机器自身运行消耗）。
     * 行为保持为“全有全无”：能量不足时返回 0。
     */
    fun consumeEnergy(requested: Long): Long {
        if (requested <= 0L || amount < requested) return 0L
        amount -= requested
        trackInternalMutation {
            consumedThisTick += requested
        }
        return requested
    }

    /** 从存档恢复能量并同步提交基线，避免首个事务误记账。 */
    fun restoreEnergy(storedAmount: Long) {
        amount = storedAmount.coerceIn(0L, capacity)
        syncCommittedAmount()
        onEnergyCommitted()
    }

    private inline fun trackInternalMutation(record: () -> Unit) {
        val now = currentTickProvider()
        if (now != null) {
            normalizeTickBudget()
            record()
        }
        // 关键：内部路径必须推进基线，避免后续事务 delta 误判。
        lastCommittedAmount = amount
        onEnergyCommitted()
    }

    // ========== 子类覆写点 ==========

    /**
     * 子类定义该面是否可输入及该面的意愿上限；整机总输入由 [maxInsertPerTick] 限制。
     * 默认返回 [maxInsertPerTick]，表示所有方向都可以输入。
     */
    protected open fun getSideMaxInsert(side: Direction?): Long = maxInsertPerTick

    /**
     * 子类定义该面是否可输出及该面的意愿上限；整机总输出由 [maxExtractPerTick] 限制。
     * 默认返回 [maxExtractPerTick]，表示所有方向都可以输出。
     */
    protected open fun getSideMaxExtract(side: Direction?): Long = maxExtractPerTick

    /** 子类可在提交后同步 GUI/NBT 镜像字段。 */
    protected open fun onEnergyCommitted() = Unit

    // ========== 工具方法 ==========

    /** 外部直接改 amount（例如读 NBT）后调用，重置提交基线，避免首个事务误判为大额插入。 */
    fun syncCommittedAmount() {
        lastCommittedAmount = amount
    }
}

/**
 * 支持储能升级、高压升级的分面能量容器。
 *
 * - [baseCapacity]：基础容量
 * - [capacityBonusProvider]：储能升级带来的额外容量
 * - [maxInsertPerTick]：基础每 tick 输入上限
 * - [maxInsertPerTickProvider]：高压升级带来的每 tick 输入上限（可选）
 */
open class UpgradeableTickLimitedSidedEnergyContainer(
    private val baseCapacity: Long,
    private val capacityBonusProvider: () -> Long,
    maxInsertPerTick: Long,
    maxExtractPerTick: Long,
    currentTickProvider: () -> Long? = { null },
    private val maxInsertPerTickProvider: (() -> Long)? = null
) : TickLimitedSidedEnergyContainer(
    baseCapacity + 256 * 10_000L,  // 容量需足够大以容纳最大升级数
    maxInsertPerTick,
    maxExtractPerTick,
    currentTickProvider
) {

    /** 当前有效容量（基础 + 升级加成） */
    fun getEffectiveCapacity(): Long = baseCapacity + capacityBonusProvider().coerceAtLeast(0L)

    /** 对外暴露动态容量，供 Energy API 查询及外部模组识别。 */
    override val capacity: Long
        get() = getEffectiveCapacity()

    /** 当前 tick 有效的 maxInsert 上限（用于高压升级等）。 */
    open fun getEffectiveMaxInsertPerTick(): Long =
        maxInsertPerTickProvider?.invoke() ?: maxInsertPerTick

    /**
     * 高压升级不可使输入超过容量限制。
     * 有效输入上限 = min(高压增益, 剩余容量)。
     */
    override fun getSideMaxInsert(side: Direction?): Long {
        val transformerMax = getEffectiveMaxInsertPerTick()
        val space = (getEffectiveCapacity() - amount).coerceAtLeast(0L)
        return minOf(transformerMax, space)
    }
}

