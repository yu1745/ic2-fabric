package ic2_120.content.screen

import ic2_120.content.block.CokeKilnBlock
import ic2_120.content.block.machines.CokeKilnBlockEntity
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.sync.CokeKilnSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.annotation.ScreenFactory
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack

import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot

@ModScreenHandler(block = CokeKilnBlock::class)
class CokeKilnScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(CokeKilnScreenHandler::class.type(), syncId) {

    val sync = CokeKilnSync(SyncedDataView(propertyDelegate))

    init {
        checkSize(blockInventory, 2)
        addProperties(propertyDelegate)

        addSlot(PredicateSlot(blockInventory, SLOT_INPUT, 0, 0, INPUT_SLOT_SPEC))
        addSlot(PredicateSlot(blockInventory, SLOT_OUTPUT, 0, 0, OUTPUT_SLOT_SPEC))

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 0, 0))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 0, 0))
        }
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            when {
                index == SLOT_OUTPUT -> if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END + 1, true)) return ItemStack.EMPTY
                index == SLOT_INPUT -> if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END + 1, true)) return ItemStack.EMPTY
                index in PLAYER_INV_START..HOTBAR_END -> {
                    val moved = SlotMoveHelper.insertIntoTargets(
                        stackInSlot,
                        listOf(SlotTarget(slots[SLOT_INPUT], INPUT_SLOT_SPEC))
                    )
                    if (!moved) return ItemStack.EMPTY
                }
                else -> if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END + 1, false)) return ItemStack.EMPTY
            }
            if (stackInSlot.isEmpty) slot.stack = ItemStack.EMPTY else slot.markDirty()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTakeItem(player, stackInSlot)
        }
        return stack
    }

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is CokeKilnBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        const val SLOT_INPUT = 0
        const val SLOT_OUTPUT = 1
        const val PLAYER_INV_START = 2
        const val HOTBAR_END = 37
        const val SLOT_SIZE = 18

        private val INPUT_SLOT_SPEC = SlotSpec(canInsert = { !it.isEmpty })
        private val OUTPUT_SLOT_SPEC = SlotSpec(canInsert = { false })

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): CokeKilnScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(2)
            return CokeKilnScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}
