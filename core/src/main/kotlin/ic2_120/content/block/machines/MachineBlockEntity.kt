package ic2_120.content.block.machines

import ic2_120.content.block.IOwned
import ic2_120.content.block.ITieredMachine
import ic2_120.content.sound.MachineSoundConfig
import ic2_120.content.upgrade.ITransformerUpgradeSupport
import ic2_120.content.upgrade.TransformerUpgradeComponent
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.nbt.CompoundTag
import net.minecraft.sounds.SoundSource
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import java.util.UUID
import net.minecraft.core.HolderLookup

/**
 * 机器 BlockEntity 基类
 *
 * 提供通用的机器功能：
 * - 能量等级（tier）
 * - 电池槽支持（可以用电池为机器供电）
 * - 电池放电逻辑（遵循能量等级规则）
 * - 统一的 setActiveState（块状态更新 + 服务端声音播放）
 */
abstract class MachineBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), ITieredMachine, IOwned {

    /** Per-instance startup phase prevents all loaded machines dirtying the same tick. */
    private val initialDirtyDelayTicks = kotlin.random.RandomSource.nextLong(1L, DIRTY_INTERVAL_TICKS + 1L)
    private var nextDirtyTick = Long.MIN_VALUE

    /**
     * Compatibility alias retained for existing call sites. All machine
     * dirty marking is throttled centrally by [markDirty].
     */
    protected fun markDirtyThrottled() {
        markDirty()
    }

    companion object {
        /** 槽位索引常量 */
        const val FUEL_SLOT = 0      // 燃料槽（子类可使用）
        const val BATTERY_SLOT = 1   // 电池充电/供电槽
        private const val NBT_OWNER_UUID = "OwnerUUID"
        private const val NBT_VOLTAGE_TIER_BONUS = "VoltageTierBonus"
        private const val DIRTY_INTERVAL_TICKS = 20L
    }

    override var ownerUuid: UUID? = null

    override fun readNbt(nbt: CompoundTag, lookup: HolderLookup.WrapperLookup) {
        super.readNbt(nbt, lookup)
        ownerUuid = if (nbt.containsUuid(NBT_OWNER_UUID)) nbt.getUuid(NBT_OWNER_UUID) else null
        if (this is ITransformerUpgradeSupport) {
            // voltageTierBonus 是瞬态字段，正常由每 tick 的 TransformerUpgradeComponent.apply 写入；
            // 此处立即从存档恢复，消除「BE 已加载但尚未首次 tick（bonus=0）」窗口：
            // 电网超压检测在 END_SERVER_TICK 运行，电网 BFS / 其他 BE tick 可能在机器首次 tick
            // 之前同步强载其区块，导致装了高压升级的机器被误判为耐压不足而爆炸。
            // 子类 inventory 此时尚未载入内存，直接解析 "Items" NBT（与 ContainerHelper 落盘键一致）。
            voltageTierBonus = TransformerUpgradeComponent.countUpgradesInNbt(nbt)
        }
    }

    /**
     * 客户端跳过 markDirty；服务端每台机器最多每 20 tick 标脏一次。
     *
     * Create 等模组在装配 contraption（矿车、轴承等）时会用 [net.minecraft.world.level.Level] 的
     * 虚拟实现（如 VirtualRenderWorld）通过 be.load(nbt) 重建 BlockEntity。这类世界的 chunk
     * 不是原版 [net.minecraft.world.level.chunk.LevelChunk]，基类 markDirty 会强转失败导致客户端 ClassCastException 崩溃。
     * 客户端 markDirty 本就是空操作，跳过既安全又无副作用。
     *
     * 全局限流是明确的性能/崩溃恢复取舍：正常区块卸载与关服仍会序列化当前内存状态；
     * 若进程在两次标脏之间崩溃，机器的库存、配置、能量或进度最多可能回退约 20 tick。
     */
    override fun markDirty() {
        val world = world
        if (world != null && world.isClient) return
        if (world != null) {
            val now = world.time
            if (nextDirtyTick != Long.MIN_VALUE && now < nextDirtyTick) return
            nextDirtyTick = if (nextDirtyTick == Long.MIN_VALUE) {
                now + initialDirtyDelayTicks
            } else {
                now + DIRTY_INTERVAL_TICKS
            }
        }
        super.markDirty()
    }

    override fun writeNbt(nbt: CompoundTag, lookup: HolderLookup.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        ownerUuid?.let { nbt.putUuid(NBT_OWNER_UUID, it) }
        if (this is ITransformerUpgradeSupport) {
            // 持久化高压升级加成，作为 readNbt 扫描之外的纵深防御（升级在区块卸载期间不会变化，
            // 存档值恒等于正确值；加载时仍优先按 "Items" 现扫，避免「插入升级后、下次 tick 前存档」的陈旧值）。
            nbt.putInt(NBT_VOLTAGE_TIER_BONUS, voltageTierBonus)
        }
    }

    /**
     * 机器的能量等级（1-4）
     * 子类必须覆写此属性
     */
    abstract override val tier: Int

    /**
     * 获取 inventory
     *
     * 子类需要实现此方法以提供 inventory 访问。
     * 如果机器没有 inventory，返回 null。
     */
    protected abstract fun getInventory(): net.minecraft.world.Container?

    /**
     * 对应的 Block 的 ACTIVE 属性（BooleanProperty）。
     * 子类必须覆写此属性。
     */
    protected abstract val activeProperty: net.minecraft.world.level.block.state.properties.BooleanProperty

    /**
     * 机器声音配置。默认无声音。
     * 子类可通过覆写覆盖。
     */
    protected open val soundConfig: MachineSoundConfig = MachineSoundConfig.none()

    /**
     * 统一的 setActiveState 实现。
     *
     * 行为：
     * 1. 比较 wasActive = state.get(activeProperty)
     * 2. 状态变化时写回 world.setBlockState(...)
     * 3. 仅服务端播放声音
     * 4. 持续音按 loopIntervalTicks 节流播放
     *
     * 子类可通过覆写此方法并在末尾调用 super.setActiveState() 来扩展行为。
     */
    protected open fun setActiveState(world: Level, pos: BlockPos, state: BlockState, active: Boolean) {
        val wasActive = state.get(activeProperty)
        if (wasActive == active) return
        world.setBlockState(pos, state.with(activeProperty, active))

        if (world.isClient) return

        val config = soundConfig
        if (config.soundType == ic2_120.content.sound.SoundType.NONE) return

        playSound(world, pos, wasActive, active)
    }

    private fun playSound(world: Level, pos: BlockPos, wasActive: Boolean, isActive: Boolean) {
        val config = soundConfig

        when (config.soundType) {
            ic2_120.content.sound.SoundType.START_STOP -> {
                // ElectricFurnace / InductionFurnace 状态机
                when {
                    !wasActive && isActive -> {
                        // false -> true: 播放 start
                        config.startSound?.let {
                            world.playSound(null, pos, it, SoundSource.BLOCKS, config.startVolume, config.startPitch)
                        }
                    }
                    wasActive && !isActive -> {
                        // true -> false: 播放 stop
                        config.stopSound?.let {
                            world.playSound(null, pos, it, SoundSource.BLOCKS, config.stopVolume, config.stopPitch)
                        }
                    }
                    // false -> false: 不播放
                }
            }
            ic2_120.content.sound.SoundType.LOOP,
            ic2_120.content.sound.SoundType.OPERATE -> {
                // LOOP / OPERATE 改为客户端 SoundInstance 控制，服务端不重复触发
            }
            ic2_120.content.sound.SoundType.NONE -> { /* 无声音 */ }
        }
    }
}
