package ic2_120.content.screen

import ic2_120.content.sync.TankSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import ic2_120.registry.annotation.ScreenFactory

@ModScreenHandler(name = "tank")
class TankScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    propertyDelegate: PropertyDelegate,
    private val context: ScreenHandlerContext
) : ScreenHandler(TankScreenHandler::class.type(), syncId) {

    val sync = TankSync(SyncedDataView(propertyDelegate)) { 0 }

    init {
        addProperties(propertyDelegate)

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
        var moved = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stack = slot.stack
            moved = stack.copy()
            if (index < 27) {
                if (!insertItem(stack, 27, 36, false)) return ItemStack.EMPTY
            } else if (!insertItem(stack, 0, 27, false)) {
                return ItemStack.EMPTY
            }
            if (stack.isEmpty) slot.stack = ItemStack.EMPTY
            else slot.markDirty()
        }
        return moved
    }

    override fun canUse(player: PlayerEntity): Boolean = true

    companion object {
        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): TankScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            return TankScreenHandler(syncId, playerInventory, ArrayPropertyDelegate(propertyCount), context)
        }
    }
}
