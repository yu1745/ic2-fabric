package ic2_120.content.screen

import ic2_120.content.block.AnimalmatronBlock
import ic2_120.content.block.machines.AnimalmatronBlockEntity
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.sync.AnimalmatronSync
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

@ModScreenHandler(block = AnimalmatronBlock::class, inventorySize = AnimalmatronBlockEntity.INVENTORY_SIZE)
class AnimalmatronScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate,
    private val itemStorage: RoutedItemStorage? = null
) : ScreenHandler(AnimalmatronScreenHandler::class.type(), syncId) {

    val sync = AnimalmatronSync(SyncedDataView(propertyDelegate))

    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    init {
        checkSize(blockInventory, AnimalmatronBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        addTrackedSlot(blockInventory, AnimalmatronBlockEntity.SLOT_WATER_INPUT, 57, 55)
        addTrackedSlot(blockInventory, AnimalmatronBlockEntity.SLOT_WATER_OUTPUT, 75, 55)
        addTrackedSlot(blockInventory, AnimalmatronBlockEntity.SLOT_WEED_EX_INPUT, 49, 26)
        addTrackedSlot(blockInventory, AnimalmatronBlockEntity.SLOT_WEED_EX_OUTPUT, 67, 26)

        for ((i, slotIndex) in AnimalmatronBlockEntity.SLOT_FEED_INDICES.withIndex()) {
            addTrackedSlot(blockInventory, slotIndex, 44 + i * 18, 79)
        }

        for (i in AnimalmatronBlockEntity.SLOT_UPGRADE_INDICES.indices) {
            addTrackedSlot(blockInventory, AnimalmatronBlockEntity.SLOT_UPGRADE_INDICES[i], 152, 25 + i * 18)
        }

        addTrackedSlot(blockInventory, AnimalmatronBlockEntity.SLOT_SHEARS, 8, 79)
        addTrackedSlot(blockInventory, AnimalmatronBlockEntity.SLOT_HARVEST_OUTPUT, 26, 79)

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 109 + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 167))
        }
    }

    private fun addTrackedSlot(inventory: Inventory, beSlotIndex: Int, x: Int, y: Int) {
        val spec = itemStorage?.deriveSlotSpec(beSlotIndex) ?: SlotSpec()
        val handlerIndex = slots.size
        beSlotToHandlerIndex[beSlotIndex] = handlerIndex
        addSlot(PredicateSlot(inventory, beSlotIndex, x, y, spec))
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
                index in PLAYER_INV_START..HOTBAR_END -> {
                    val storage = itemStorage
                    if (storage == null) return ItemStack.EMPTY
                    val moved = SlotMoveHelper.insertFromRoutes(
                        stackInSlot, storage, storage.insertRoutes, beSlotToHandlerIndex, slots
                    )
                    if (!moved) return ItemStack.EMPTY
                }
                else -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, false)) return ItemStack.EMPTY
                }
            }
            if (stackInSlot.isEmpty) slot.stack = ItemStack.EMPTY
            else slot.markDirty()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTakeItem(player, stackInSlot)
        }
        return stack
    }

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is AnimalmatronBlock &&
                player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        const val SLOT_SIZE = 18

        const val SLOT_WATER_INPUT_INDEX = 0
        const val SLOT_WATER_OUTPUT_INDEX = 1
        const val SLOT_WEED_EX_INPUT_INDEX = 2
        const val SLOT_WEED_EX_OUTPUT_INDEX = 3

        const val SLOT_FEED_INDEX_START = 4
        const val SLOT_FEED_INDEX_END = 8

        const val SLOT_UPGRADE_INDEX_START = 9
        const val SLOT_UPGRADE_INDEX_END = 12
        const val SLOT_SHEARS_INDEX = 13
        const val SLOT_HARVEST_OUTPUT_INDEX = 14

        const val PLAYER_INV_START = 15
        const val HOTBAR_END = 50

    }
}
