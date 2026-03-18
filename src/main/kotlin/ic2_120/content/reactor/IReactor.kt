package ic2_120.content.reactor

import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

/**
 * 核反应堆接口。由 NuclearReactorBlockEntity 实现，供 IReactorComponent 访问。
 */
interface IReactor {

    fun getWorld(): World?
    fun getPos(): BlockPos

    /** 当前堆温（贮存热量） */
    fun getHeat(): Int
    fun setHeat(heat: Int)
    fun addHeat(amount: Int): Int

    /** 热量上限（默认 10000） */
    fun getMaxHeat(): Int
    fun setMaxHeat(maxHeat: Int)

    /**
     * 组件散热时，将热量"凭空蒸发"的部分会通过此方法累加，
     * 在 processChambers 结束后统一加回堆温（实际为负值，即降温）。
     */
    fun addEmitHeat(heat: Int)

    /** 热量效果修正系数（0–1），用于爆炸/环境影响的随机判定 */
    fun getHeatEffectModifier(): Float
    fun setHeatEffectModifier(hem: Float)

    /** 本周期累计的发电输出（float，每脉冲 1.0） */
    fun getReactorEnergyOutput(): Float
    fun addOutput(energy: Float): Float

    /** 槽位布局：列数（3–9） */
    fun getReactorCols(): Int
    /** 槽位布局：行数（固定 9） */
    fun getReactorRows(): Int

    fun getItemAt(x: Int, y: Int): ItemStack?
    fun setItemAt(x: Int, y: Int, stack: ItemStack?)

    /** 是否允许发电（红石等，暂可恒为 true） */
    fun produceEnergy(): Boolean

    /** 计算周期（tick），默认 20（每秒一次） */
    fun getTickRate(): Int

    /** 流体冷却模式（暂不实现，恒为 false） */
    fun isFluidCooled(): Boolean

    /** 本计算周期开始时的堆温快照，用于避免同周期内的槽位顺序依赖。 */
    fun getCycleStartHeat(): Int = getHeat()

    /** 当前可用于 reactorVent 抽取的有效堆温（含本周期待结算热量变更）。 */
    fun getEffectiveHeatForDrain(): Int = getHeat()

    /** 检查是否有足够的冷却液（热模式中散热片耐久修复的条件） */
    fun hasCoolant(): Boolean = false

    /** 爆炸 */
    fun explode()

    /** 报告组件产热 */
    fun addHeatProduced(amount: Int)

    /** 报告组件散热 */
    fun addHeatDissipated(amount: Int)

    /** 报告槽位产热、散热和发电 */
    fun addSlotHeatInfo(slot: Int, heatProduced: Int, heatDissipated: Int, energyOutput: Float = 0f)
}
