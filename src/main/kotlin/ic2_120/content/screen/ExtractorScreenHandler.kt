package ic2_120.content.screen

import ic2_120.content.block.ExtractorBlock
import ic2_120.content.sync.ExtractorSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
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

@ModScreenHandler(block = ExtractorBlock::class)
class ExtractorScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(ModScreenHandlers.getType(ExtractorScreenHandler::class), syncId) {

    val sync = ExtractorSync(SyncedDataView(propertyDelegate))

    init {
        checkSize(blockInventory, 2)
        addProperties(propertyDelegate)
        addSlot(Slot(blockInventory, 0, INPUT_SLOT_X, BLOCK_SLOTS_Y))
        addSlot(Slot(blockInventory, 1, OUTPUT_SLOT_X, BLOCK_SLOTS_Y))
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
                index == 1 -> {
                    if (!insertItem(stackInSlot, 2, 38, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                index in 2..37 -> {
                    if (!insertItem(stackInSlot, 0, 1, false)) return ItemStack.EMPTY
                    if (!insertItem(stackInSlot, 1, 2, false)) return ItemStack.EMPTY
                }
                else -> if (!insertItem(stackInSlot, 2, 38, false)) return ItemStack.EMPTY
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
            world.getBlockState(pos).block is ExtractorBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        const val INPUT_SLOT_X = 56
        const val OUTPUT_SLOT_X = 116
        const val BLOCK_SLOTS_Y = 54
        const val PLAYER_INV_Y = 84
        const val HOTBAR_Y = 142
        const val SLOT_SIZE = 18

        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): ExtractorScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(2)
            return ExtractorScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}
