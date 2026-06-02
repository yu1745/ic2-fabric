package buildcraft_addon.content.screen

import buildcraft_addon.content.block.RFEngineBlock
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import ic2_120.registry.annotation.ScreenFactory
import net.minecraft.item.ItemStack

@ModScreenHandler(block = RFEngineBlock::class)
class RFEngineScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    val blockInventory: Inventory,
    val context: ScreenHandlerContext,
    val propertyDelegate: PropertyDelegate,
) : ScreenHandler(RFEngineScreenHandler::class.type(), syncId) {

    init {
        checkSize(blockInventory, 4)
        checkDataCount(propertyDelegate, 2)

        // 4 upgrade slots (BC8 pos: 62,44 / 80,44 / 98,44 / 116,44)
        for (i in 0..3) {
            addSlot(Slot(blockInventory, i, 62 + i * 18, 44))
        }

        // Player inventory (y offset = 95)
        for (row in 0..2) {
            for (col in 0..8) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 95 + row * 18))
            }
        }
        // Player hotbar (y = 95 + 58 = 153)
        for (col in 0..8) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 153))
        }

        addProperties(propertyDelegate)
    }

    // PropertyDelegate: [0]=currentRF, [1]=heat
    fun getCurrentRF(): Int = propertyDelegate.get(0)
    fun getHeat(): Int = propertyDelegate.get(1)

    override fun quickMove(player: PlayerEntity, slotIndex: Int): net.minecraft.item.ItemStack {
        val slot = slots.getOrNull(slotIndex) ?: return ItemStack.EMPTY
        if (!slot.hasStack()) return ItemStack.EMPTY
        val original = slot.stack
        val newStack = original.copy()

        if (slotIndex in 0..3) {
            if (!insertItem(original, 4, 40, true)) return ItemStack.EMPTY
        } else if (slotIndex in 4..40) {
            if (!insertItem(original, 0, 4, false)) return ItemStack.EMPTY
        }

        if (original.isEmpty) {
            slot.stack = ItemStack.EMPTY
        } else {
            slot.markDirty()
        }
        return newStack
    }

    override fun canUse(player: PlayerEntity): Boolean {
        return context.get({ world, pos ->
            world.getBlockState(pos).block is RFEngineBlock && player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)
    }

    companion object {
        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): RFEngineScreenHandler {
            buf.readBlockPos()
            buf.readVarInt()
            return RFEngineScreenHandler(syncId, playerInventory, SimpleInventory(4), ScreenHandlerContext.EMPTY, ArrayPropertyDelegate(2))
        }
    }
}
