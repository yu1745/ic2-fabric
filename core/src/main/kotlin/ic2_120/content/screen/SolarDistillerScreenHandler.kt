package ic2_120.content.screen

import ic2_120.content.block.SolarDistillerBlock
import ic2_120.content.block.machines.SolarDistillerBlockEntity
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.sync.SolarDistillerSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.annotation.ScreenFactory
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

@ModScreenHandler(block = SolarDistillerBlock::class)
class SolarDistillerScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate,
    private val itemStorage: RoutedItemStorage? = null
) : ScreenHandler(SolarDistillerScreenHandler::class.type(), syncId) {

    val sync = SolarDistillerSync(SyncedDataView(propertyDelegate))

    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    init {
        checkSize(blockInventory, SolarDistillerBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        addTrackedSlot(blockInventory, SolarDistillerBlockEntity.SLOT_INPUT_WATER, 17, 27)
        addTrackedSlot(blockInventory, SolarDistillerBlockEntity.SLOT_OUTPUT_EMPTY, 17, 45)
        addTrackedSlot(blockInventory, SolarDistillerBlockEntity.SLOT_INPUT_CELL, 136, 64)
        addTrackedSlot(blockInventory, SolarDistillerBlockEntity.SLOT_OUTPUT_CELL, 136, 82)
        addTrackedSlot(blockInventory, SolarDistillerBlockEntity.SLOT_UPGRADE_0, 152, 8)
        addTrackedSlot(blockInventory, SolarDistillerBlockEntity.SLOT_UPGRADE_1, 152, 26)

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 102 + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 160))
        }
    }

    private fun addTrackedSlot(inventory: Inventory, beSlotIndex: Int, x: Int, y: Int) {
        val spec = itemStorage?.deriveSlotSpec(beSlotIndex) ?: SlotSpec()
        val slot = PredicateSlot(inventory, beSlotIndex, x, y, spec)
        beSlotToHandlerIndex[beSlotIndex] = slots.size
        addSlot(slot)
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (!slot.hasStack()) return stack
        val inSlot = slot.stack
        stack = inSlot.copy()

        when (index) {
            SLOT_OUTPUT_EMPTY_INDEX, SLOT_OUTPUT_CELL_INDEX -> {
                if (!insertItem(inSlot, PLAYER_INV_START, HOTBAR_END + 1, true)) return ItemStack.EMPTY
            }
            SLOT_UPGRADE_0_INDEX, SLOT_UPGRADE_1_INDEX -> {
                if (!insertItem(inSlot, PLAYER_INV_START, HOTBAR_END + 1, true)) return ItemStack.EMPTY
            }
            in PLAYER_INV_START..HOTBAR_END -> {
                val storage = itemStorage ?: return ItemStack.EMPTY
                val moved = SlotMoveHelper.insertFromRoutes(
                    inSlot,
                    storage,
                    storage.insertRoutes,
                    beSlotToHandlerIndex,
                    slots
                )
                if (!moved) return ItemStack.EMPTY
            }
            else -> if (!insertItem(inSlot, PLAYER_INV_START, HOTBAR_END + 1, false)) return ItemStack.EMPTY
        }

        if (inSlot.isEmpty) slot.stack = ItemStack.EMPTY else slot.markDirty()
        if (inSlot.count == stack.count) return ItemStack.EMPTY
        slot.onTakeItem(player, inSlot)
        return stack
    }

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is SolarDistillerBlock &&
                player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        const val SLOT_SIZE = 18

        const val SLOT_INPUT_WATER_INDEX = 0
        const val SLOT_OUTPUT_EMPTY_INDEX = 1
        const val SLOT_INPUT_CELL_INDEX = 2
        const val SLOT_OUTPUT_CELL_INDEX = 3
        const val SLOT_UPGRADE_0_INDEX = 4
        const val SLOT_UPGRADE_1_INDEX = 5
        const val PLAYER_INV_START = 6
        const val HOTBAR_END = 41

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): SolarDistillerScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(SolarDistillerBlockEntity.INVENTORY_SIZE)
            return SolarDistillerScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}
