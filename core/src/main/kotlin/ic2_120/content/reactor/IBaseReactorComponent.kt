package ic2_120.content.reactor

import net.minecraft.item.ItemStack

/**
 * 反应堆组件基础接口。可放入反应堆槽位的物品需实现此接口。
 */
interface IBaseReactorComponent {
    /**
     * 该物品是否可放入反应堆。
     * @param stack 物品堆
     * @param reactor 目标反应堆
     */
    fun canBePlacedIn(stack: ItemStack, reactor: IReactor): Boolean
}
