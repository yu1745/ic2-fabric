package ic2_120.content.screen

import ic2_120.content.block.CropmatronBlock
import ic2_120.content.block.machines.CropmatronBlockEntity
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.sync.CropmatronSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import ic2_120.registry.annotation.ScreenFactory

@ModScreenHandler(block = CropmatronBlock::class)
class CropmatronScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate,
    private val itemStorage: RoutedItemStorage? = null
) : ScreenHandler(CropmatronScreenHandler::class.type(), syncId) {

    val sync = CropmatronSync(SyncedDataView(propertyDelegate))

    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    init {
        checkSize(blockInventory, CropmatronBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        addTrackedSlot(blockInventory, CropmatronBlockEntity.SLOT_WATER_INPUT, 57, 55)
        addTrackedSlot(blockInventory, CropmatronBlockEntity.SLOT_WATER_OUTPUT, 75, 55)
        addTrackedSlot(blockInventory, CropmatronBlockEntity.SLOT_WEED_EX_INPUT, 49, 26)
        addTrackedSlot(blockInventory, CropmatronBlockEntity.SLOT_WEED_EX_OUTPUT, 67, 26)

        for ((i, slotIndex) in CropmatronBlockEntity.SLOT_FERTILIZER_INDICES.withIndex()) {
            addTrackedSlot(blockInventory, slotIndex, 8 + i * 18, 79)
        }

        for (i in CropmatronBlockEntity.SLOT_UPGRADE_INDICES.indices) {
            addTrackedSlot(blockInventory, CropmatronBlockEntity.SLOT_UPGRADE_INDICES[i], 152, 25 + i * 18)
        }

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
            world.getBlockState(pos).block is CropmatronBlock &&
                player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        const val SLOT_SIZE = 18

        const val SLOT_WATER_INPUT_INDEX = 0
        const val SLOT_WATER_OUTPUT_INDEX = 1
        const val SLOT_WEED_EX_INPUT_INDEX = 2
        const val SLOT_WEED_EX_OUTPUT_INDEX = 3

        const val SLOT_FERTILIZER_INDEX_START = 4
        const val SLOT_FERTILIZER_INDEX_END = 10

        const val SLOT_UPGRADE_INDEX_START = 11
        const val SLOT_UPGRADE_INDEX_END = 14

        const val PLAYER_INV_START = 15
        const val HOTBAR_END = 50

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): CropmatronScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(CropmatronBlockEntity.INVENTORY_SIZE)
            return CropmatronScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}
