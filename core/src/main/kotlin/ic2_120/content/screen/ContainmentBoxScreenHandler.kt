package ic2_120.content.screen

import ic2_120.compat.stack

import ic2_120.content.item.ContainmentBoxInventory
import ic2_120.content.item.ContainmentBoxItem
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.annotation.ScreenHandlerMode
import ic2_120.registry.type
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.InteractionHand

/**
 * 防辐射容纳盒 GUI：12 格 + 玩家背包，数据在物品 NBT。
 */
@ModScreenHandler(name = "containment_box", mode = ScreenHandlerMode.HANDHELD)
class ContainmentBoxScreenHandler(
    syncId: Int,
    private val playerInventory: Inventory,
    private val hand: InteractionHand,
    val boxInventory: ContainmentBoxInventory = ContainmentBoxInventory(playerInventory.player, hand)
) : AbstractContainerMenu(ContainmentBoxScreenHandler::class.type(), syncId) {

    private val noNestedBoxSpec = SlotSpec(canInsert = { it.item !is ContainmentBoxItem })

    init {
        val boxCols = 4
        for (row in 0 until 3) {
            for (col in 0 until boxCols) {
                val index = row * boxCols + col
                addSlot(PredicateSlot(boxInventory, index, 53 + col * 18, 20 + row * 18, noNestedBoxSpec))
            }
        }
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                val inventorySlot = col + row * 9 + 9
                addSlot(createPlayerSlot(inventorySlot, 8 + col * 18, 84 + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(createPlayerSlot(col, 8 + col * 18, 142))
        }
    }

    private fun createPlayerSlot(inventorySlot: Int, x: Int, y: Int): Slot {
        if (hand != InteractionHand.MAIN_HAND || inventorySlot != playerInventory.selectedSlot) {
            return Slot(playerInventory, inventorySlot, x, y)
        }
        return object : Slot(playerInventory, inventorySlot, x, y) {
            override fun canInsert(stack: ItemStack): Boolean = false
            override fun canTakeItems(playerEntity: Player): Boolean = false
        }
    }

    override fun quickMoveStack(player: Player, slotIndex: Int): ItemStack {
        var moved = ItemStack.EMPTY
        val slot = slots[slotIndex]
        if (slot .hasItem()) {
            val stack = slot.stack
            moved = stack.copy()
            val boxSize = ContainmentBoxInventory.SIZE
            if (slotIndex < boxSize) {
                if (!moveItemStackTo(stack, boxSize, slots.size, true)) return ItemStack.EMPTY
            } else if (!moveItemStackTo(stack, 0, boxSize, false)) {
                return ItemStack.EMPTY
            }
            if (stack.isEmpty) slot.setChanged()
            else slot.setChanged()
        }
        return moved
    }

    override fun stillValid(player: Player): Boolean = boxInventory.canPlayerUse(player)

    override fun onClosed(player: Player) {
        super.onClosed(player)
        boxInventory.onClose(player)
    }

    companion object {
        const val PLAYER_INV_START = ContainmentBoxInventory.SIZE

    }
}
