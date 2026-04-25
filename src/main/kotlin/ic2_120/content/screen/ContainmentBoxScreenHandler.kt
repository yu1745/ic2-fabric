package ic2_120.content.screen

import ic2_120.content.item.ContainmentBoxInventory
import ic2_120.content.item.ContainmentBoxItem
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.annotation.ScreenFactory
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
@ModScreenHandler(name = "containment_box")
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
                addSlot(PredicateSlot(boxInventory, index, 0, 0, noNestedBoxSpec))
            }
        }
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 0, 0))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 0, 0))
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

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): ContainmentBoxScreenHandler {
            val hand = buf.readEnumConstant(Hand::class.java)
            return ContainmentBoxScreenHandler(syncId, playerInventory, hand)
        }
    }
}
