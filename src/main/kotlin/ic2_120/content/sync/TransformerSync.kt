package ic2_120.content.sync

import ic2_120.content.TickLimitedSidedEnergyContainer
import ic2_120.content.syncs.SyncSchema
import net.minecraft.util.math.Direction

/**
 * 变压器的同步属性与分面能量存储。
 *
 * 变压器只改变电压等级（EU/t 速率），不改变 EU 总量。
 *
 * 变压器模式：
 * - STEP_UP (升压): 正面接收低级能量，其他五面输出高级能量
 *   - 例：LV变压器接收 32 EU/t，累积后以 128 EU/t 输出
 *   - 能量守恒：4 tick × 32 EU = 1 tick × 128 EU = 128 EU
 * - STEP_DOWN (降压): 其他五面接收高级能量，正面输出低级能量
 *   - 例：LV变压器接收 128 EU/t，以 32 EU/t 输出
 *   - 能量守恒：1 tick × 128 EU = 4 tick × 32 EU = 128 EU
 *
 * 能量比例：
 * - LV变压器 (1级): 低级 32 EU/t <-> 高级 128 EU/t
 * - MV变压器 (2级): 低级 128 EU/t <-> 高级 512 EU/t
 * - HV变压器 (3级): 低级 512 EU/t <-> 高级 2048 EU/t
 * - EV变压器 (4级): 低级 2048 EU/t <-> 高级 8192 EU/t
 */
class TransformerSync(
    schema: SyncSchema,
    private val getFacing: () -> Direction,
    currentTickProvider: () -> Long? = { null },
    private val tier: Int = 1
) : TickLimitedSidedEnergyContainer(
    baseCapacity = getCapacityForTier(tier),
    maxInsertPerTick = getMaxInsertForTier(tier),
    maxExtractPerTick = getMaxExtractForTier(tier),
    currentTickProvider = currentTickProvider
) {

    /**
     * 变压器工作模式
     */
    enum class Mode(val id: Int, val translationKey: String) {
        STEP_UP(0, "mode.step_up"),      // 升压模式：正面输入低级能量，其他面输出高级能量
        STEP_DOWN(1, "mode.step_down");  // 降压模式：其他面输入高级能量，正面输出低级能量

        companion object {
            fun fromId(id: Int): Mode = entries.firstOrNull { it.id == id } ?: STEP_UP
        }
    }

    companion object {
        // 根据变压器等级获取容量
        // 容量基于高级能量的速率，确保能存储足够的能量进行转换
        fun getCapacityForTier(tier: Int): Long {
            return when (tier) {
                1 -> 512L    // LV: 128 * 4 (存储4个tick的高级能量)
                2 -> 2048L   // MV: 512 * 4
                3 -> 8192L   // HV: 2048 * 4
                4 -> 32768L  // EV: 8192 * 4
                else -> 512L
            }
        }

        // 获取最大输入速率（基于高级能量等级，因为无论哪种模式都可能接收高级能量）
        fun getMaxInsertForTier(tier: Int): Long {
            return when (tier) {
                1 -> 128L    // LV: 最大接收 128 EU/t (2级)
                2 -> 512L    // MV: 最大接收 512 EU/t (3级)
                3 -> 2048L   // HV: 最大接收 2048 EU/t (4级)
                4 -> 8192L   // EV: 最大接收 8192 EU/t (5级)
                else -> 128L
            }
        }

        // 获取最大输出速率（基于高级能量等级，因为无论哪种模式都可能输出高级能量）
        fun getMaxExtractForTier(tier: Int): Long {
            return when (tier) {
                1 -> 128L    // LV: 最大输出 128 EU/t (2级)
                2 -> 512L    // MV: 最大输出 512 EU/t (3级)
                3 -> 2048L   // HV: 最大输出 2048 EU/t (4级)
                4 -> 8192L   // EV: 最大输出 8192 EU/t (5级)
                else -> 128L
            }
        }

        const val NBT_ENERGY_STORED = "EnergyStored"
        const val NBT_MODE = "Mode"
    }

    var energy by schema.int("Energy")
    var mode by schema.int("Mode")
    private val flow = EnergyFlowSync(schema, this)

    private val facing: Direction
        get() = getFacing()

    /** 低级能量等级（输入侧或输出侧） */
    val lowTier: Int
        get() = tier

    /** 高级能量等级（输出侧或输入侧） */
    val highTier: Int
        get() = tier + 1

    /** 获取输入侧的能量等级（低级） */
    fun getInputTierValue(): Int = lowTier

    /** 获取输出侧的能量等级（高级） */
    fun getOutputTierValue(): Int = highTier

    /** 获取低级能量的每 tick 速率 */
    fun getLowEuPerTick(): Long = ITieredMachine.euPerTickFromTier(lowTier)

    /** 获取高级能量的每 tick 速率 */
    fun getHighEuPerTick(): Long = ITieredMachine.euPerTickFromTier(highTier)

    fun getMode(): Mode = Mode.fromId(mode)

    fun setMode(newMode: Mode) {
        mode = newMode.id
    }

    /** 切换模式（升压 <-> 降压） */
    fun toggleMode() {
        val currentMode = getMode()
        val nextMode = if (currentMode == Mode.STEP_UP) Mode.STEP_DOWN else Mode.STEP_UP
        setMode(nextMode)
    }

    /**
     * 根据当前模式和方向决定该面是否可输入
     * - 升压模式：只有正面可以输入（低级能量）
     * - 降压模式：除正面外的其他面可以输入（高级能量）
     */
    override fun getSideMaxInsert(side: Direction?): Long {
        if (side == null) return 0L

        val currentMode = getMode()
        val isFront = (side == facing)

        return when (currentMode) {
            Mode.STEP_UP -> {
                // 升压：只有正面接收低级能量
                if (isFront) getLowEuPerTick() else 0L
            }
            Mode.STEP_DOWN -> {
                // 降压：其他面接收高级能量
                if (!isFront) getHighEuPerTick() else 0L
            }
        }
    }

    /**
     * 根据当前模式和方向决定该面是否可输出
     * - 升压模式：除正面外的其他面可以输出（高级能量）
     * - 降压模式：只有正面可以输出（低级能量）
     */
    override fun getSideMaxExtract(side: Direction?): Long {
        if (side == null) return 0L

        val currentMode = getMode()
        val isFront = (side == facing)

        return when (currentMode) {
            Mode.STEP_UP -> {
                // 升压：其他面输出高级能量
                if (!isFront) getHighEuPerTick() else 0L
            }
            Mode.STEP_DOWN -> {
                // 降压：只有正面输出低级能量
                if (isFront) getLowEuPerTick() else 0L
            }
        }
    }

    override fun onEnergyCommitted() {
        energy = amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    /**
     * 在 tick 结束时调用，同步当前 tick 的实际输入/输出
     * 必须在 tick 方法结束时调用，此时 tick 即将变化
     */
    fun syncCurrentTickFlow() {
        // 更新滑动窗口平均值
        flow.syncCurrentTickFlow()
    }

    /** 获取同步的滤波后输入量（EU/t） */
    fun getSyncedInsertedAmount(): Long = flow.getSyncedInsertedAmount()

    /** 获取同步的滤波后输出量（EU/t） */
    fun getSyncedExtractedAmount(): Long = flow.getSyncedExtractedAmount()
}

/**
 * ITieredMachine 伴生对象的引用
 * 为了避免循环依赖，在这里复制必要的函数
 */
private object ITieredMachine {
    fun euPerTickFromTier(tier: Int): Long {
        if (tier <= 1) return 32L
        var m = 32L
        repeat((tier - 1).coerceAtMost(8)) { m *= 4 }
        return m
    }
}

