package ic2_120.content.screen.slot

import net.minecraft.world.entity.player.Player
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.inventory.Slot

/**
 * 基于 [SlotSpec] 的通用槽位实现。
 */
open class PredicateSlot(
    inventory: Container,
    index: Int,
    x: Int,
    y: Int,
    private val spec: SlotSpec
) : Slot(inventory, index, x, y) {

    override fun mayPlace(stack: ItemStack): Boolean = spec.canInsert(stack)

    override fun getMaxStackSize(): Int = spec.maxItemCount

    override fun getMaxStackSize(stack: ItemStack): Int = spec.maxItemCount

    override fun mayPickup(playerEntity: Player): Boolean = spec.canTake(playerEntity)
}
