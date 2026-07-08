package ic2_120.content.screen

import ic2_120.content.sync.InductionFurnaceSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.content.block.InductionFurnaceBlock
import ic2_120.content.block.machines.InductionFurnaceBlockEntity
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.storage.RoutedItemStorage
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

@ModScreenHandler(block = InductionFurnaceBlock::class, inventorySize = InductionFurnaceBlockEntity.INVENTORY_SIZE)
class InductionFurnaceScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate,
    private val itemStorage: RoutedItemStorage? = null
) : ScreenHandler(InductionFurnaceScreenHandler::class.type(), syncId) {

    val sync = InductionFurnaceSync(SyncedDataView(propertyDelegate))

    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    init {
        checkSize(blockInventory, InductionFurnaceBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        addTrackedSlot(blockInventory, InductionFurnaceBlockEntity.SLOT_INPUT_0, 27, 17)
        addTrackedSlot(blockInventory, InductionFurnaceBlockEntity.SLOT_INPUT_1, 45, 17)
        addTrackedSlot(blockInventory, InductionFurnaceBlockEntity.SLOT_OUTPUT_0, 97, 34)
        addTrackedSlot(blockInventory, InductionFurnaceBlockEntity.SLOT_OUTPUT_1, 121, 34)
        addTrackedSlot(blockInventory, InductionFurnaceBlockEntity.SLOT_DISCHARGING, 36, 53)

        // 2 个升级槽 (152,26) (152,44)
        addTrackedSlot(blockInventory, InductionFurnaceBlockEntity.SLOT_UPGRADE_INDICES[0], 152, 26)
        addTrackedSlot(blockInventory, InductionFurnaceBlockEntity.SLOT_UPGRADE_INDICES[1], 152, 44)

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 142))
        }
    }

    private fun addTrackedSlot(inventory: Inventory, beSlot: Int, x: Int, y: Int, spec: SlotSpec = DEFAULT_SLOT_SPEC) {
        val handlerIndex = slots.size
        addSlot(PredicateSlot(inventory, beSlot, x, y, spec))
        beSlotToHandlerIndex[beSlot] = handlerIndex
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
                    val storage = itemStorage ?: return ItemStack.EMPTY
                    val moved = SlotMoveHelper.insertFromRoutes(stackInSlot, storage, storage.insertRoutes, beSlotToHandlerIndex, slots)
                    if (!moved) return ItemStack.EMPTY
                }
                else -> if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, false)) return ItemStack.EMPTY
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
            world.getBlockState(pos).block is InductionFurnaceBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        const val SLOT_SIZE = 18

        private val DEFAULT_SLOT_SPEC = SlotSpec()
        private val OUTPUT_SLOT_SPEC = SlotSpec(canInsert = { false }, canTake = { true })

        const val SLOT_INPUT_0_INDEX = 0
        const val SLOT_INPUT_1_INDEX = 1
        const val SLOT_OUTPUT_0_INDEX = 2
        const val SLOT_OUTPUT_1_INDEX = 3
        const val SLOT_DISCHARGING_INDEX = 4
        const val SLOT_UPGRADE_0_INDEX = 5
        const val SLOT_UPGRADE_1_INDEX = 6
        const val PLAYER_INV_START = 7
        const val HOTBAR_END = 42

    }
}
