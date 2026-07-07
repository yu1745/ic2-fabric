package ic2_120.content.upgrade

import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

/**
 * 通用红石控制组件。
 *
 * 只要机器实现 [IRedstoneControlSupport]，即可通过该组件统一判断当前是否允许运行。
 */
object RedstoneControlComponent {

    /**
    * 返回机器当前是否允许运行。
    * - 未实现 [IRedstoneControlSupport]：默认允许（兼容旧机器）
     * - 默认模式（无反转升级）：无红石信号时运行，有信号时停机
     * - 反转模式（有红石反转升级）：有红石信号时运行，无信号时停机
    */
   fun canRun(world: World, pos: BlockPos, machine: Any): Boolean {
       if (machine !is IRedstoneControlSupport) return true
       val powered = world.isReceivingRedstonePower(pos)
        return if (machine.redstoneInverted) powered else !powered
   }
}
