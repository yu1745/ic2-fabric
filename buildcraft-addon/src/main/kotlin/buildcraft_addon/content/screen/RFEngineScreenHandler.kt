package buildcraft_addon.content.screen

import buildcraft_addon.content.block.RFEngineBlock
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.Slot
import ic2_120.registry.annotation.ScreenFactory
import net.minecraft.world.item.ItemStack
import ic2_120.compat.*

@ModScreenHandler(block = RFEngineBlock::class)
class RFEngineScreenHandler(
    syncId: Int,
    playerInventory: Inventory,
    val blockInventory: Container,
    val context: ContainerLevelAccess,
    val propertyDelegate: ContainerData,
) : AbstractContainerMenu(RFEngineScreenHandler::class.type(), syncId) {

    init {
        checkContainerSize(blockInventory, 4)

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

        addDataSlots(propertyDelegate)
    }

    // ContainerData: [0]=currentRF, [1]=heat
    fun getCurrentRF(): Int = propertyDelegate.get(0)
    fun getHeat(): Int = propertyDelegate.get(1)

    override fun quickMoveStack(player: Player, slotIndex: Int): net.minecraft.world.item.ItemStack {
        val slot = slots.getOrNull(slotIndex) ?: return ItemStack.EMPTY
        if (!slot .hasItem()) return ItemStack.EMPTY
        val original = slot.item
        val newStack = original.copy()

        if (slotIndex in 0..3) {
            if (!moveItemStackTo(original, 4, 40, true)) return ItemStack.EMPTY
        } else if (slotIndex in 4 until 40) {
            if (!moveItemStackTo(original, 0, 4, false)) return ItemStack.EMPTY
        }

        if (original.isEmpty) {
            slot.set(ItemStack.EMPTY)
        } else {
            slot.setChanged()
        }
        return newStack
    }

    override fun stillValid(player: Player): Boolean {
        return context.evaluate({ world, pos ->
            world.getBlockState(pos).block is RFEngineBlock && player.distanceToSqr(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)
    }

    companion object {
        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: Inventory, buf: FriendlyByteBuf): RFEngineScreenHandler {
            buf.readBlockPos()
            buf.readVarInt()
            return RFEngineScreenHandler(syncId, playerInventory, SimpleContainer(4), ContainerLevelAccess.NULL, SimpleContainerData(2))
        }
    }
}
