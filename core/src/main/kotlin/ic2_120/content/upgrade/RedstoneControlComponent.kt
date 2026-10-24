package ic2_120.content.upgrade

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import java.util.WeakHashMap

/**
 * 红石门控（P3，2026-08-26 TTL 修订）。
 *
 * 旧实现缓存条件 tick == now 只防「同 tick 多次调用」，跨 tick 必重查
 * isReceivingRedstonePower（6 面信号扫描，spark 实测 SignalGetter 全局 0.98%）。
 *
 * 修订：TTL 缓存，默认 6 tick（0.3s）内直接复用上次查询结果。
 * 语义代价有界：红石变化后最多延迟 0.3s 生效（开启与关断双向对称）。
 * 不做纯事件失效（审查结论：Arclight+Connector 栈下插件静默写方块、
 * 强充能拓扑、跨区块边界三类漏网，纯事件不可靠，TTL 有界且简单）。
 *
 * WeakHashMap 键为机器对象（IRedstoneControlSupport 实现者即各机器 BE），
 * BE 被 GC 后条目自动消失；缓存条目极小（1 long + 1 boolean）。
 */
object RedstoneControlComponent {

    /** 红石查询结果的有效 tick 数。6t = 0.3s，人手操作红石无感知。 */
    private const val REDSTONE_TTL_TICKS = 6L

    private class RedstoneCacheEntry(
        val world: Level,
        val tick: Long,
        val inverted: Boolean,
        val powered: Boolean
    )

    private val tickCache = WeakHashMap<Any, RedstoneCacheEntry>()

    /**
     * 机器是否允许运行（红石门控）：
     * - 无红石控制配置（未实现 IRedstoneControlSupport 或未装升级）：恒 true
     * - 正常模式：有红石信号时运行
     * - 反转模式（有红石反转升级）：无红石信号时运行
     *
     * TTL 内的重复调用不重查世界；inverted 配置变化立即绕过缓存。
    */
    fun canRun(world: Level, pos: BlockPos, machine: Any): Boolean {
        if (machine !is IRedstoneControlSupport) return true
        val inverted = machine.redstoneInverted
        val now = world.time
        val cached = tickCache[machine]
        val powered = if (cached != null && cached.world === world
            && now - cached.tick <= REDSTONE_TTL_TICKS && cached.inverted == inverted
        ) {
            cached.powered
        } else {
            world.isReceivingRedstonePower(pos).also {
                tickCache[machine] = RedstoneCacheEntry(world, now, inverted, it)
            }
        }
        return if (inverted) !powered else powered
    }
}
