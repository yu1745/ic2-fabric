package ic2_120_advanced_solar_addon.content.screen

import ic2_120_advanced_solar_addon.content.block.MolecularTransformerBlock
import ic2_120_advanced_solar_addon.content.block.MolecularTransformerBlockEntity
import ic2_120_advanced_solar_addon.content.sync.MolecularTransformerSync
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.annotation.ScreenFactory
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import net.minecraft.util.math.Direction

@ModScreenHandler(block = MolecularTransformerBlock::class)
class MolecularTransformerScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    private val blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate,
    private val itemStorage: RoutedItemStorage? = null
) : ScreenHandler(MolecularTransformerScreenHandler::class.type(), syncId) {

    val sync = MolecularTransformerSync(
        schema = SyncedDataView(propertyDelegate),
        tier = MolecularTransformerBlockEntity.TIER,
        getFacing = { Direction.NORTH },
        currentTickProvider = { null }
    )

    /**
     * BlockEntity slot index -> ScreenHandler slot index mapping.
     */
    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    init {
        addProperties(propertyDelegate)

        // Machine slots
        addTrackedSlot(blockInventory, MolecularTransformerBlock.INPUT_SLOT)
        addTrackedSlot(blockInventory, MolecularTransformerBlock.OUTPUT_SLOT)

        // Player inventory
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 0, 0))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 0, 0))
        }
    }

    /**
     * Adds a PredicateSlot and records BE slot index -> handler slot index mapping.
     * SlotSpec is derived from [itemStorage] when available; falls back to default on client.
     */
    private fun addTrackedSlot(inventory: Inventory, beSlotIndex: Int) {
        val spec = itemStorage?.deriveSlotSpec(beSlotIndex) ?: fallbackSpec(beSlotIndex)
        val handlerIndex = slots.size
        beSlotToHandlerIndex[beSlotIndex] = handlerIndex
        addSlot(PredicateSlot(inventory, beSlotIndex, 0, 0, spec))
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()

            // PredicateSlot index is the BE slot index; player slots are not PredicateSlot, return -1
            val beSlot = (slot as? PredicateSlot)?.index ?: -1

            when {
                // Machine slots (input/output) -> player inventory
                beSlot >= 0 -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                // Player inventory -> machine (via RoutedItemStorage routes)
                index in PLAYER_INV_START..HOTBAR_END -> {
                    val storage = itemStorage
                    if (storage == null) return ItemStack.EMPTY
                    val moved = SlotMoveHelper.insertFromRoutes(
                        stackInSlot,
                        storage,
                        storage.insertRoutes,
                        beSlotToHandlerIndex,
                        slots
                    )
                    if (!moved) return ItemStack.EMPTY
                }
                // Hotbar <-> main inventory swap
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
            player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        const val SLOT_SIZE = 18
        const val SLOT_INPUT_INDEX = 0
        const val SLOT_OUTPUT_INDEX = 1
        const val PLAYER_INV_START = 2
        const val HOTBAR_END = 38

        private val DEFAULT_INPUT_SPEC = SlotSpec(canInsert = { true })
        private val DEFAULT_OUTPUT_SPEC = SlotSpec(canInsert = { false })

        private fun fallbackSpec(beSlotIndex: Int): SlotSpec = when (beSlotIndex) {
            MolecularTransformerBlock.INPUT_SLOT -> DEFAULT_INPUT_SPEC
            MolecularTransformerBlock.OUTPUT_SLOT -> DEFAULT_OUTPUT_SPEC
            else -> SlotSpec()
        }

        @ScreenFactory
        @JvmStatic
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): MolecularTransformerScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInventory = net.minecraft.inventory.SimpleInventory(MolecularTransformerBlock.INVENTORY_SIZE)
            val propertyDelegate = net.minecraft.screen.ArrayPropertyDelegate(propertyCount)
            return MolecularTransformerScreenHandler(syncId, playerInventory, blockInventory, context, propertyDelegate)
        }
    }
}
