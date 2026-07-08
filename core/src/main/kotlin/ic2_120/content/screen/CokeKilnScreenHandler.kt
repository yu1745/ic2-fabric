package ic2_120.content.screen

import ic2_120.content.block.CokeKilnBlock
import ic2_120.content.block.machines.CokeKilnBlockEntity
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.sync.CokeKilnSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot

@ModScreenHandler(block = CokeKilnBlock::class, inventorySize = CokeKilnBlockEntity.INVENTORY_SIZE)
class CokeKilnScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate,
    private val itemStorage: RoutedItemStorage? = null
) : ScreenHandler(CokeKilnScreenHandler::class.type(), syncId) {

    val sync = CokeKilnSync(SyncedDataView(propertyDelegate))

    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    private fun addTrackedSlot(inventory: Inventory, beSlot: Int, x: Int, y: Int, spec: SlotSpec) {
        val handlerIndex = slots.size
        addSlot(PredicateSlot(inventory, beSlot, x, y, spec))
        beSlotToHandlerIndex[beSlot] = handlerIndex
    }

    init {
        checkSize(blockInventory, 2)
        addProperties(propertyDelegate)

        addTrackedSlot(blockInventory, SLOT_INPUT, 80, 42, DEFAULT_SLOT_SPEC)
        addTrackedSlot(blockInventory, SLOT_OUTPUT, 44, 42, OUTPUT_SLOT_SPEC)

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 79 + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 137))
        }
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            val beSlot = (slot as? PredicateSlot)?.index ?: -1
            when {
                beSlot >= 0 -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                index in PLAYER_INV_START until HOTBAR_END -> {
                    val storage = itemStorage ?: return ItemStack.EMPTY
                    val moved = SlotMoveHelper.insertFromRoutes(stackInSlot, storage, storage.insertRoutes, beSlotToHandlerIndex, slots)
                    if (!moved) return ItemStack.EMPTY
                }
                else -> if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, false)) return ItemStack.EMPTY
            }
            if (stackInSlot.isEmpty) slot.stack = ItemStack.EMPTY else slot.markDirty()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTakeItem(player, stackInSlot)
        }
        return stack
    }

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is CokeKilnBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        const val SLOT_INPUT = 0
        const val SLOT_OUTPUT = 1
        const val PLAYER_INV_START = 2
        const val HOTBAR_END = 38
        const val SLOT_SIZE = 18

        private val DEFAULT_SLOT_SPEC = SlotSpec()
        private val OUTPUT_SLOT_SPEC = SlotSpec(canInsert = { false }, canTake = { true })

    }
}
