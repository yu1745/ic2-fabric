package ic2_120.content.reactor

import net.minecraft.item.ItemStack

/**
 * 反应堆组件接口。实现 processChamber、热量相关方法等。
 */
interface IReactorComponent : IBaseReactorComponent {

    /**
     * 每周期调用两次：pass 0 为发电/脉冲阶段，pass 1 为热量分配阶段。
     */
    fun processChamber(stack: ItemStack, reactor: IReactor, x: Int, y: Int, heatRun: Boolean)

    /**
     * 是否接受来自邻接燃料棒的中子脉冲。反射器返回 true 以增加脉冲数。
     */
    fun acceptUraniumPulse(
        stack: ItemStack,
        reactor: IReactor,
        pulsingStack: ItemStack,
        youX: Int,
        youY: Int,
        pulseX: Int,
        pulseY: Int,
        heatRun: Boolean
    ): Boolean

    /** 该组件是否可储存热量（用于燃料棒热量分配、散热片邻接冷却等） */
    fun canStoreHeat(stack: ItemStack, reactor: IReactor, x: Int, y: Int): Boolean

    /** 该组件的最大热容量 */
    fun getMaxHeat(stack: ItemStack, reactor: IReactor, x: Int, y: Int): Int

    /** 该组件当前储存的热量 */
    fun getCurrentHeat(stack: ItemStack, reactor: IReactor, x: Int, y: Int): Int

    /**
     * 改变该组件的热量。返回未能吸收的热量（正数表示溢出，会传回堆温）。
     * 若组件被销毁（超热容），返回剩余热量。
     */
    fun alterHeat(stack: ItemStack, reactor: IReactor, x: Int, y: Int, heat: Int): Int

    /**
     * 爆炸时的影响系数。>0 且 <1 时与 boomMod 相乘；>=1 时加到 boomPower。
     */
    fun influenceExplosion(stack: ItemStack, reactor: IReactor): Float
}
