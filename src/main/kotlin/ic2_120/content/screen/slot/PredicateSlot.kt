package ic2_120.content.screen.slot

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.slot.Slot

/**
 * 基于 [SlotSpec] 的通用槽位实现。
 */
class PredicateSlot(
    inventory: Inventory,
    index: Int,
    x: Int,
    y: Int,
    private val spec: SlotSpec
) : Slot(inventory, index, x, y) {

    override fun canInsert(stack: ItemStack): Boolean = spec.canInsert(stack)

    override fun getMaxItemCount(stack: ItemStack): Int = spec.maxItemCount

    override fun canTakeItems(playerEntity: PlayerEntity): Boolean = spec.canTake(playerEntity)
}
