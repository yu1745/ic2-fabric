package ic2_120.content.reactor

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.fabricmc.fabric.api.item.v1.FabricItemSettings

/**
 * 反应堆组件抽象基类。所有方法默认空实现。
 */
abstract class AbstractReactorComponent(settings: FabricItemSettings) : Item(settings), IReactorComponent {

    override fun processChamber(stack: ItemStack, reactor: IReactor, x: Int, y: Int, heatRun: Boolean) {}

    override fun acceptUraniumPulse(
        stack: ItemStack,
        reactor: IReactor,
        pulsingStack: ItemStack,
        youX: Int,
        youY: Int,
        pulseX: Int,
        pulseY: Int,
        heatRun: Boolean
    ): Boolean = false

    override fun canStoreHeat(stack: ItemStack, reactor: IReactor, x: Int, y: Int): Boolean = false

    override fun getMaxHeat(stack: ItemStack, reactor: IReactor, x: Int, y: Int): Int = 0

    override fun getCurrentHeat(stack: ItemStack, reactor: IReactor, x: Int, y: Int): Int = 0

    override fun alterHeat(stack: ItemStack, reactor: IReactor, x: Int, y: Int, heat: Int): Int = heat

    override fun influenceExplosion(stack: ItemStack, reactor: IReactor): Float = 0f

    override fun canBePlacedIn(stack: ItemStack, reactor: IReactor): Boolean = true
}
