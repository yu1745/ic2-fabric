package ic2_120.content.screen

import ic2_120.content.block.CondenserBlock
import ic2_120.content.block.machines.CondenserBlockEntity
import ic2_120.content.item.HeatVentItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.sync.CondenserSync
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
import net.minecraft.util.math.Direction

@ModScreenHandler(block = CondenserBlock::class)
class CondenserScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate,
    private val itemStorage: RoutedItemStorage? = null
) : ScreenHandler(CondenserScreenHandler::class.type(), syncId) {

    val sync = CondenserSync(SyncedDataView(propertyDelegate), { Direction.NORTH })

    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    init {
        checkSize(blockInventory, CondenserBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        addTrackedSlot(blockInventory, 0, VENT_0_X, VENT_0_Y)
        addTrackedSlot(blockInventory, 1, VENT_1_X, VENT_1_Y)
        addTrackedSlot(blockInventory, 2, VENT_2_X, VENT_2_Y)
        addTrackedSlot(blockInventory, 3, VENT_3_X, VENT_3_Y)
        addTrackedSlot(blockInventory, CondenserBlockEntity.SLOT_UPGRADE, UPGRADE_X, UPGRADE_Y)
        addTrackedSlot(blockInventory, CondenserBlockEntity.SLOT_DISCHARGE, DISCHARGE_X, DISCHARGE_Y)
        addTrackedSlot(blockInventory, CondenserBlockEntity.SLOT_WATER_INPUT, WATER_IN_X, WATER_IN_Y)
        addTrackedSlot(blockInventory, CondenserBlockEntity.SLOT_WATER_OUTPUT, WATER_OUT_X, WATER_OUT_Y)

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, PLAYER_INV_X + col * 18, HOTBAR_Y))
        }
    }

    private fun addTrackedSlot(inventory: Inventory, beSlotIndex: Int, x: Int, y: Int) {
        val spec = itemStorage?.deriveSlotSpec(beSlotIndex) ?: FALLBACK_SPEC
        beSlotToHandlerIndex[beSlotIndex] = slots.size
        addSlot(PredicateSlot(inventory, beSlotIndex, x, y, spec))
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (!slot.hasStack()) return ItemStack.EMPTY
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
        return stack
    }

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is CondenserBlock &&
                player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        const val VENT_0_X = 26; const val VENT_0_Y = 25
        const val VENT_1_X = 134; const val VENT_1_Y = 25
        const val VENT_2_X = 26; const val VENT_2_Y = 43
        const val VENT_3_X = 134; const val VENT_3_Y = 43
        const val UPGRADE_X = 152; const val UPGRADE_Y = 72
        const val DISCHARGE_X = 8; const val DISCHARGE_Y = 43
        const val WATER_IN_X = 26; const val WATER_IN_Y = 72
        const val WATER_OUT_X = 134; const val WATER_OUT_Y = 72
        const val PLAYER_INV_START = 8
        const val HOTBAR_END = 43
        const val PLAYER_INV_X = 8
        const val PLAYER_INV_Y = 101
        const val HOTBAR_Y = 159

        private val FALLBACK_SPEC = SlotSpec()

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): CondenserScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(CondenserBlockEntity.INVENTORY_SIZE)
            return CondenserScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}
