package ic2_120.content.screen

import ic2_120.content.item.ContainmentBoxInventory
import ic2_120.content.item.ContainmentBoxItem
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.annotation.ScreenHandlerMode
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot
import net.minecraft.util.Hand

/**
 * 防辐射容纳盒 GUI：12 格 + 玩家背包，数据在物品 NBT。
 */
@ModScreenHandler(name = "containment_box", mode = ScreenHandlerMode.HANDHELD)
class ContainmentBoxScreenHandler(
    syncId: Int,
    private val playerInventory: PlayerInventory,
    private val hand: Hand,
    val boxInventory: ContainmentBoxInventory = ContainmentBoxInventory(playerInventory.player, hand)
) : ScreenHandler(ContainmentBoxScreenHandler::class.type(), syncId) {

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
        if (hand != Hand.MAIN_HAND || inventorySlot != playerInventory.selectedSlot) {
            return Slot(playerInventory, inventorySlot, x, y)
        }
        return object : Slot(playerInventory, inventorySlot, x, y) {
            override fun canInsert(stack: ItemStack): Boolean = false
            override fun canTakeItems(playerEntity: PlayerEntity): Boolean = false
        }
    }

    override fun quickMove(player: PlayerEntity, slotIndex: Int): ItemStack {
        var moved = ItemStack.EMPTY
        val slot = slots[slotIndex]
        if (slot.hasStack()) {
            val stack = slot.stack
            moved = stack.copy()
            val boxSize = ContainmentBoxInventory.SIZE
            if (slotIndex < boxSize) {
                if (!insertItem(stack, boxSize, slots.size, true)) return ItemStack.EMPTY
            } else if (!insertItem(stack, 0, boxSize, false)) {
                return ItemStack.EMPTY
            }
            if (stack.isEmpty) slot.markDirty()
            else slot.markDirty()
        }
        return moved
    }

    override fun canUse(player: PlayerEntity): Boolean = boxInventory.canPlayerUse(player)

    override fun onClosed(player: PlayerEntity) {
        super.onClosed(player)
        boxInventory.onClose(player)
    }

    companion object {
        const val PLAYER_INV_START = ContainmentBoxInventory.SIZE

    }
}
