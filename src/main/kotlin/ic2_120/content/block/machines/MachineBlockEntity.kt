package ic2_120.content.block.machines

import ic2_120.content.block.ITieredMachine
import ic2_120.content.item.energy.IBatteryItem
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.util.math.BlockPos

/**
 * 机器 BlockEntity 基类
 *
 * 提供通用的机器功能：
 * - 能量等级（tier）
 * - 电池槽支持（可以用电池为机器供电）
 * - 电池放电逻辑（遵循能量等级规则）
 */
abstract class MachineBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), ITieredMachine {

    companion object {
        /** 槽位索引常量 */
        const val FUEL_SLOT = 0      // 燃料槽（子类可使用）
        const val BATTERY_SLOT = 1   // 电池充电/供电槽
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
}
