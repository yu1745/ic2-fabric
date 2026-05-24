package ic2_120.content.screen

import ic2_120.content.block.GeoGeneratorBlock
import ic2_120.content.block.machines.GeoGeneratorBlockEntity
import ic2_120.content.item.isLavaFuel
import ic2_120.content.item.energy.canBeCharged
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.sync.GeoGeneratorSync
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

@ModScreenHandler(block = GeoGeneratorBlock::class)
class GeoGeneratorScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate,
    private val itemStorage: RoutedItemStorage? = null
) : ScreenHandler(GeoGeneratorScreenHandler::class.type(), syncId) {

    val sync = GeoGeneratorSync(
        SyncedDataView(propertyDelegate),
        getFacing = { net.minecraft.util.math.Direction.NORTH },
        currentTickProvider = { null }
    )

    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    private fun addTrackedSlot(slot: Slot, beSlotIndex: Int) {
        beSlotToHandlerIndex[beSlotIndex] = slots.size
        addSlot(slot)
    }

    init {
        checkSize(blockInventory, GeoGeneratorBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        // 机器槽位（规则由 RoutedItemStorage 路由推导）
        val fuelSlotSpec = itemStorage?.deriveSlotSpec(GeoGeneratorBlockEntity.FUEL_SLOT)
            ?: SLOT_SPEC_FALLBACK_FUEL
        val emptyContainerSlotSpec = itemStorage?.deriveSlotSpec(GeoGeneratorBlockEntity.EMPTY_CONTAINER_SLOT)
            ?: SLOT_SPEC_FALLBACK_EMPTY_CONTAINER
        val batterySlotSpec = itemStorage?.deriveSlotSpec(GeoGeneratorBlockEntity.BATTERY_SLOT)
            ?: SLOT_SPEC_FALLBACK_BATTERY

        addTrackedSlot(PredicateSlot(blockInventory, GeoGeneratorBlockEntity.FUEL_SLOT, 38, 18, fuelSlotSpec), GeoGeneratorBlockEntity.FUEL_SLOT)
        addTrackedSlot(PredicateSlot(blockInventory, GeoGeneratorBlockEntity.EMPTY_CONTAINER_SLOT, 38, 57, emptyContainerSlotSpec), GeoGeneratorBlockEntity.EMPTY_CONTAINER_SLOT)
        addTrackedSlot(PredicateSlot(blockInventory, GeoGeneratorBlockEntity.BATTERY_SLOT, 124, 43, batterySlotSpec), GeoGeneratorBlockEntity.BATTERY_SLOT)

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
            when {
                index == SLOT_FUEL_INDEX -> if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                index == SLOT_EMPTY_CONTAINER_INDEX -> if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                index == SLOT_BATTERY_INDEX -> if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
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
            world.getBlockState(pos).block is GeoGeneratorBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        const val SLOT_SIZE = 18
        const val SLOT_FUEL_INDEX = 0
        const val SLOT_EMPTY_CONTAINER_INDEX = 1
        const val SLOT_BATTERY_INDEX = 2
        const val PLAYER_INV_START = 3
        const val HOTBAR_END = 39

        private val SLOT_SPEC_FALLBACK_FUEL = SlotSpec(
            maxItemCount = 64,
            canInsert = { stack -> stack.isLavaFuel() }
        )
        private val SLOT_SPEC_FALLBACK_EMPTY_CONTAINER = SlotSpec(
            maxItemCount = 64,
            canInsert = { false }
        )
        private val SLOT_SPEC_FALLBACK_BATTERY = SlotSpec(
            maxItemCount = 1,
            canInsert = { stack -> stack.canBeCharged() }
        )

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): GeoGeneratorScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(GeoGeneratorBlockEntity.INVENTORY_SIZE)
            return GeoGeneratorScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}
