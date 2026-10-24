package buildcraft_addon.content.screen

import buildcraft_addon.content.block.IronEngineBlock
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.Slot
import ic2_120.registry.annotation.ScreenFactory
import net.minecraft.world.item.ItemStack

@ModScreenHandler(block = IronEngineBlock::class)
class IronEngineScreenHandler(
    syncId: Int,
    playerInventory: Inventory,
    val context: ContainerLevelAccess,
    val propertyDelegate: ContainerData,
) : AbstractContainerMenu(IronEngineScreenHandler::class.type(), syncId) {

    init {
        checkDataCount(propertyDelegate, 4)
        // No item slots — only fluid tanks
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

    // ContainerData: [0]=fuelAmount, [1]=coolantAmount, [2]=residueAmount, [3]=heat
    fun getFuelAmount(): Int = propertyDelegate.get(0)
    fun getCoolantAmount(): Int = propertyDelegate.get(1)
    fun getResidueAmount(): Int = propertyDelegate.get(2)
    fun getHeat(): Int = propertyDelegate.get(3)

    override fun quickMoveStack(player: Player, slotIndex: Int): net.minecraft.world.item.ItemStack {
        return ItemStack.EMPTY
    }

    override fun stillValid(player: Player): Boolean {
        return context.get({ world, pos ->
            world.getBlockState(pos).block is IronEngineBlock && player.distanceToSqr(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)
    }

    companion object {
        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: Inventory, buf: FriendlyByteBuf): IronEngineScreenHandler {
            buf.readBlockPos()
            buf.readVarInt()
            return IronEngineScreenHandler(syncId, playerInventory, ContainerLevelAccess.EMPTY, SimpleContainerData(4))
        }
    }
}
