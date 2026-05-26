package ic2_120.content.screen

import ic2_120.content.block.UuScannerBlock
import ic2_120.content.block.machines.UuScannerBlockEntity
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.sync.UuScannerSync
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

@ModScreenHandler(block = UuScannerBlock::class)
class UuScannerScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    val blockPos: net.minecraft.util.math.BlockPos,
    private val context: ScreenHandlerContext,
    propertyDelegate: PropertyDelegate,
    private val itemStorage: RoutedItemStorage? = null
) : ScreenHandler(UuScannerScreenHandler::class.type(), syncId) {

    val sync = UuScannerSync(
        SyncedDataView(propertyDelegate),
        { null },
        { UuScannerSync.ENERGY_CAPACITY }
    )

    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    init {
        checkSize(blockInventory, UuScannerBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        // 输入槽 (54,34)
        addTrackedSlot(PredicateSlot(blockInventory, UuScannerBlockEntity.SLOT_INPUT, 54, 34,
            deriveSpec(UuScannerBlockEntity.SLOT_INPUT)))
        // 电池槽 (7,42)
        addTrackedSlot(PredicateSlot(blockInventory, UuScannerBlockEntity.SLOT_DISCHARGING, 7, 42,
            deriveSpec(UuScannerBlockEntity.SLOT_DISCHARGING)))
        // 水晶槽 (151,64)
        addTrackedSlot(PredicateSlot(blockInventory, UuScannerBlockEntity.SLOT_CRYSTAL, 151, 64,
            deriveSpec(UuScannerBlockEntity.SLOT_CRYSTAL)))

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 7 + col * 18, 83 + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 7 + col * 18, 141))
        }
    }

    private fun addTrackedSlot(slot: PredicateSlot) {
        beSlotToHandlerIndex[slot.index] = slots.size
        addSlot(slot)
    }

    private fun deriveSpec(beSlot: Int) = itemStorage?.deriveSlotSpec(beSlot) ?: SlotSpec()

    override fun onButtonClick(player: PlayerEntity, id: Int): Boolean {
        context.get({ world, pos ->
            val be = world.getBlockEntity(pos) as? UuScannerBlockEntity ?: return@get
            when (id) {
                BUTTON_DELETE_TEMPLATE -> be.deleteTemplate()
                BUTTON_SAVE_TO_CRYSTAL -> be.saveTemplateToCrystal()
            }
        }, false)
        return id == BUTTON_DELETE_TEMPLATE || id == BUTTON_SAVE_TO_CRYSTAL
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            when {
                index in SLOT_INPUT_INDEX..SLOT_CRYSTAL_INDEX -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                }
                index in PLAYER_INV_START..HOTBAR_END -> {
                    val storage = itemStorage ?: return ItemStack.EMPTY
                    val moved = SlotMoveHelper.insertFromRoutes(
                        stackInSlot, storage, storage.insertRoutes, beSlotToHandlerIndex, slots)
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
            world.getBlockState(pos).block is UuScannerBlock &&
                player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        const val SLOT_INPUT_INDEX = 0
        const val SLOT_BATTERY_INDEX = 1
        const val SLOT_CRYSTAL_INDEX = 2
        const val PLAYER_INV_START = 3
        const val HOTBAR_END = 39

        const val BUTTON_DELETE_TEMPLATE = 1
        const val BUTTON_SAVE_TO_CRYSTAL = 2

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): UuScannerScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            return UuScannerScreenHandler(
                syncId, playerInventory,
                SimpleInventory(UuScannerBlockEntity.INVENTORY_SIZE),
                pos,
                ScreenHandlerContext.create(playerInventory.player.world, pos),
                ArrayPropertyDelegate(propertyCount)
            )
        }
    }
}
