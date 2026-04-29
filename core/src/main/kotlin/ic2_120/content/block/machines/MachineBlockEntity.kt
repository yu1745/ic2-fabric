package ic2_120.content.block.machines

import ic2_120.content.block.IOwned
import ic2_120.content.block.ITieredMachine
import ic2_120.content.sound.MachineSoundConfig
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.nbt.NbtCompound
import net.minecraft.sound.SoundCategory
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.UUID

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

    companion object {
        /** 槽位索引常量 */
        const val FUEL_SLOT = 0      // 燃料槽（子类可使用）
        const val BATTERY_SLOT = 1   // 电池充电/供电槽
        private const val NBT_OWNER_UUID = "OwnerUUID"
    }

    override var ownerUuid: UUID? = null

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        ownerUuid = if (nbt.containsUuid(NBT_OWNER_UUID)) nbt.getUuid(NBT_OWNER_UUID) else null
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        ownerUuid?.let { nbt.putUuid(NBT_OWNER_UUID, it) }
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
    protected abstract fun getInventory(): net.minecraft.inventory.Inventory?

    /**
     * 对应的 Block 的 ACTIVE 属性（BooleanProperty）。
     * 子类必须覆写此属性。
     */
    protected abstract val activeProperty: net.minecraft.state.property.BooleanProperty

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
    protected open fun setActiveState(world: World, pos: BlockPos, state: BlockState, active: Boolean) {
        val wasActive = state.get(activeProperty)
        if (wasActive != active) {
            world.setBlockState(pos, state.with(activeProperty, active))
        }

        if (world.isClient) return

        val config = soundConfig
        if (config.soundType == ic2_120.content.sound.SoundType.NONE) return

        playSound(world, pos, wasActive, active)
    }

    private fun playSound(world: World, pos: BlockPos, wasActive: Boolean, isActive: Boolean) {
        val config = soundConfig

        when (config.soundType) {
            ic2_120.content.sound.SoundType.START_STOP -> {
                // ElectricFurnace / InductionFurnace 状态机
                when {
                    !wasActive && isActive -> {
                        // false -> true: 播放 start
                        config.startSound?.let {
                            world.playSound(null, pos, it, SoundCategory.BLOCKS, config.startVolume, config.startPitch)
                        }
                    }
                    wasActive && !isActive -> {
                        // true -> false: 播放 stop
                        config.stopSound?.let {
                            world.playSound(null, pos, it, SoundCategory.BLOCKS, config.stopVolume, config.stopPitch)
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
