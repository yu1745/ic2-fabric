package ic2_120.content.storage

import net.minecraft.world.WorldlyContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.core.Direction
import org.jetbrains.annotations.Nullable

/**
 * 将 [WorldlyContainer] 的三个方法委托给 [RoutedItemStorage]，
 * 使漏斗等自动化系统只访问 [RoutedItemStorage.visibleSlots] 中声明的槽位，
 * 并且只允许从 [RoutedItemStorage.extractSlots] 中提取、按路由规则插入。
 *
 * 机器 BlockEntity 只要实现本接口并提供 [routedItemStorage]，即可自动获得正确的侧面行为。
 */
interface IRoutedSidedInventory : WorldlyContainer {
    val routedItemStorage: RoutedItemStorage

   override fun getSlotsForFace(side: Direction): IntArray = routedItemStorage.visibleSlots

    override fun canPlaceItemThroughFace(slot: Int, stack: ItemStack, side: Direction?): Boolean =
        routedItemStorage.canInsertFromSide(slot, stack)

    override fun canTakeItemThroughFace(slot: Int, stack: ItemStack, side: Direction): Boolean =
        slot in routedItemStorage.extractSlots
}
