package ic2_120.content.screen

import ic2_120.content.block.MatterGeneratorBlock
import ic2_120.content.block.machines.MatterGeneratorBlockEntity
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.sync.MatterGeneratorSync
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

@ModScreenHandler(block = MatterGeneratorBlock::class, inventorySize = MatterGeneratorBlockEntity.INVENTORY_SIZE)
class MatterGeneratorScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate,
    private val itemStorage: RoutedItemStorage? = null
) : ScreenHandler(MatterGeneratorScreenHandler::class.type(), syncId) {

    val sync = MatterGeneratorSync(SyncedDataView(propertyDelegate))

    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    private val upgradeSlotSpec: SlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.get({ world, pos -> world.getBlockEntity(pos) }, null) }
    }

    init {
        checkSize(blockInventory, MatterGeneratorBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        addTrackedSlot(blockInventory, MatterGeneratorBlockEntity.SLOT_SCRAP, 72, 40)
        addTrackedSlot(blockInventory, MatterGeneratorBlockEntity.SLOT_CONTAINER_INPUT, 125, 23)
        addTrackedSlot(blockInventory, MatterGeneratorBlockEntity.SLOT_CONTAINER_OUTPUT, 125, 59)

        val upgradeYs = intArrayOf(8, 26, 44, 62)
        for (i in 0 until UpgradeSlotLayout.SLOT_COUNT) {
            addTrackedSlot(blockInventory, MatterGeneratorBlockEntity.SLOT_UPGRADE_INDICES[i], 152, upgradeYs[i], upgradeSlotSpec)
        }

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 142))
        }
    }

    private fun addTrackedSlot(inventory: Inventory, beSlotIndex: Int, x: Int, y: Int, fallbackSpec: SlotSpec? = null) {
        val spec = itemStorage?.deriveSlotSpec(beSlotIndex) ?: fallbackSpec ?: DEFAULT_SLOT_SPEC
        beSlotToHandlerIndex[beSlotIndex] = slots.size
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
                    val storage = itemStorage ?: return ItemStack.EMPTY
                    val moved = SlotMoveHelper.insertFromRoutes(
                        stackInSlot, storage, storage.insertRoutes, beSlotToHandlerIndex, slots)
                    if (!moved) return ItemStack.EMPTY
                }
                else -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, false)) return ItemStack.EMPTY
                }
            }
            if (stackInSlot.isEmpty) slot.stack = ItemStack.EMPTY else slot.markDirty()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTakeItem(player, stackInSlot)
        }
        return stack
    }

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is MatterGeneratorBlock &&
                player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        private val DEFAULT_SLOT_SPEC = SlotSpec()

        const val SLOT_SCRAP_INDEX = 0
        const val SLOT_CONTAINER_INPUT_INDEX = 1
        const val SLOT_CONTAINER_OUTPUT_INDEX = 2
        const val SLOT_UPGRADE_INDEX_START = 3
        const val SLOT_UPGRADE_INDEX_END = 6
        const val PLAYER_INV_START = 7
        const val HOTBAR_END = 43

    }
}
