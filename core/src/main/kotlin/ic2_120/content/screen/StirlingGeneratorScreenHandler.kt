package ic2_120.content.screen

import ic2_120.compat.stack

import ic2_120.content.block.StirlingGeneratorBlock
import ic2_120.content.block.machines.StirlingGeneratorBlockEntity
import ic2_120.content.sync.StirlingGeneratorSync
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.Slot

@ModScreenHandler(block = StirlingGeneratorBlock::class, inventorySize = StirlingGeneratorBlockEntity.INVENTORY_SIZE)
class StirlingGeneratorScreenHandler(
    syncId: Int,
    playerInventory: Inventory,
    blockInventory: Container,
    private val context: ContainerLevelAccess,
    private val propertyDelegate: ContainerData
) : AbstractContainerMenu(StirlingGeneratorScreenHandler::class.type(), syncId) {

    val sync = StirlingGeneratorSync(
        SyncedDataView(propertyDelegate),
        { net.minecraft.core.Direction.NORTH }
    )

    init {
        checkContainerSize(blockInventory, StirlingGeneratorBlockEntity.INVENTORY_SIZE)
        addDataSlots(propertyDelegate)

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 7 + col * 18, 83 + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 7 + col * 18, 141))
        }
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot .hasItem()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            if (index in 0..26) {
                if (!moveItemStackTo(stackInSlot, 27, 36, false)) return ItemStack.EMPTY
            } else if (!moveItemStackTo(stackInSlot, 0, 27, false)) return ItemStack.EMPTY
            if (stackInSlot.isEmpty) slot.stack = ItemStack.EMPTY else slot.setChanged()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTake(player, stackInSlot)
        }
        return stack
    }

    override fun stillValid(player: Player): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is StirlingGeneratorBlock &&
                player.distanceToSqr(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
    }
}
