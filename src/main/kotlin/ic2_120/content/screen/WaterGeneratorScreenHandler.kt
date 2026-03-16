package ic2_120.content.screen

import ic2_120.content.block.WaterGeneratorBlock
import ic2_120.content.block.machines.WaterGeneratorBlockEntity
import ic2_120.content.item.energy.IElectricTool
import ic2_120.content.item.energy.IBatteryItem
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
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.item.isWaterFuel
import ic2_120.content.sync.WaterGeneratorSync
import ic2_120.content.syncs.SyncedDataView

@ModScreenHandler(block = WaterGeneratorBlock::class)
class WaterGeneratorScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(WaterGeneratorScreenHandler::class.type(), syncId) {

    val sync = WaterGeneratorSync(
        SyncedDataView(propertyDelegate),
        getFacing = { net.minecraft.util.math.Direction.NORTH },
        currentTickProvider = { null }
    )

    init {
        checkSize(blockInventory, 3)
        addProperties(propertyDelegate)

        addSlot(PredicateSlot(blockInventory, WaterGeneratorBlockEntity.FUEL_SLOT, FUEL_SLOT_X, BLOCK_SLOTS_Y, FUEL_SLOT_SPEC))
        addSlot(PredicateSlot(blockInventory, WaterGeneratorBlockEntity.EMPTY_CONTAINER_SLOT, EMPTY_CONTAINER_SLOT_X, BLOCK_SLOTS_Y, EMPTY_CONTAINER_SLOT_SPEC))
        addSlot(PredicateSlot(blockInventory, WaterGeneratorBlockEntity.BATTERY_SLOT, BATTERY_SLOT_X, BLOCK_SLOTS_Y, BATTERY_SLOT_SPEC))

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, PLAYER_INV_Y + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, HOTBAR_Y))
        }
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            when {
                index == WaterGeneratorBlockEntity.FUEL_SLOT -> if (!insertItem(stackInSlot, 3, 39, true)) return ItemStack.EMPTY
                index == WaterGeneratorBlockEntity.EMPTY_CONTAINER_SLOT -> if (!insertItem(stackInSlot, 3, 39, true)) return ItemStack.EMPTY
                index == WaterGeneratorBlockEntity.BATTERY_SLOT -> if (!insertItem(stackInSlot, 3, 39, true)) return ItemStack.EMPTY
                index in 3..38 -> {
                    val moved = SlotMoveHelper.insertIntoTargets(
                        stackInSlot,
                        listOf(
                            SlotTarget(slots[WaterGeneratorBlockEntity.FUEL_SLOT], FUEL_SLOT_SPEC),
                            SlotTarget(slots[WaterGeneratorBlockEntity.BATTERY_SLOT], BATTERY_SLOT_SPEC)
                        )
                    )
                    if (!moved) return ItemStack.EMPTY
                }
                else -> if (!insertItem(stackInSlot, 3, 39, false)) return ItemStack.EMPTY
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
            world.getBlockState(pos).block is WaterGeneratorBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        const val FUEL_SLOT_X = 56
        const val EMPTY_CONTAINER_SLOT_X = 86
        const val BATTERY_SLOT_X = 116
        const val BLOCK_SLOTS_Y = 54
        const val PLAYER_INV_Y = 84
        const val HOTBAR_Y = 142
        const val SLOT_SIZE = 18
        private val FUEL_SLOT_SPEC = SlotSpec(
            maxItemCount = 64,
            canInsert = { stack -> stack.isWaterFuel() }
        )
        private val EMPTY_CONTAINER_SLOT_SPEC = SlotSpec(
            maxItemCount = 64,
            canInsert = { false }
        )
        private val BATTERY_SLOT_SPEC = SlotSpec(
            maxItemCount = 1,
            canInsert = { stack -> stack.item is IBatteryItem || stack.item is IElectricTool }
        )

        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): WaterGeneratorScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(3)
            return WaterGeneratorScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}
