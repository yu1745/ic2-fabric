package ic2_120.content.screen

import ic2_120.content.block.MagnetizerBlock
import ic2_120.content.block.machines.MagnetizerBlockEntity
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.sync.MagnetizerSync
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

@ModScreenHandler(block = MagnetizerBlock::class, inventorySize = MagnetizerBlockEntity.INVENTORY_SIZE)
class MagnetizerScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    propertyDelegate: PropertyDelegate,
    private val itemStorage: RoutedItemStorage? = null
) : ScreenHandler(MagnetizerScreenHandler::class.type(), syncId) {

    val sync = MagnetizerSync(SyncedDataView(propertyDelegate))

    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    private val upgradeSlotSpec: SlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.get({ world, pos -> world.getBlockEntity(pos) }, null) }
    }

    init {
        checkSize(blockInventory, MagnetizerBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        addTrackedSlot(blockInventory, MagnetizerBlockEntity.SLOT_DISCHARGING, 8, 46)

        val bootsSpec = itemStorage?.deriveSlotSpec(MagnetizerBlockEntity.SLOT_BOOTS) ?: SlotSpec(maxItemCount = 1)
        addTrackedSlot(blockInventory, MagnetizerBlockEntity.SLOT_BOOTS, 80, 32, bootsSpec)

        for (i in 0 until UpgradeSlotLayout.SLOT_COUNT) {
            addTrackedSlot(blockInventory, MagnetizerBlockEntity.SLOT_UPGRADE_INDICES[i], 152, 5 + i * 18, upgradeSlotSpec)
        }

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 79 + row * 18))
            }
        }

        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 137))
        }
    }

    private fun addTrackedSlot(inventory: Inventory, beSlotIndex: Int, x: Int, y: Int, fallbackSpec: SlotSpec? = null) {
        val spec = itemStorage?.deriveSlotSpec(beSlotIndex) ?: fallbackSpec ?: DEFAULT_SLOT_SPEC
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
                index in PLAYER_INV_START until HOTBAR_END -> {
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
            if (stackInSlot.isEmpty) slot.stack = ItemStack.EMPTY else slot.markDirty()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTakeItem(player, stackInSlot)
        }
        return stack
    }

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is MagnetizerBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        const val SLOT_SIZE = 18
        private val DEFAULT_SLOT_SPEC = SlotSpec()

        const val SLOT_DISCHARGING_INDEX = 0
        const val SLOT_BOOTS_INDEX = 1
        const val SLOT_UPGRADE_INDEX_START = 2
        const val SLOT_UPGRADE_INDEX_END = 5
        const val PLAYER_INV_START = 6
        const val HOTBAR_END = 42

    }
}
