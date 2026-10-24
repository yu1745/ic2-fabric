package ic2_120.content.screen

import ic2_120.content.sync.TankSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.Slot

@ModScreenHandler(name = "tank")
class TankScreenHandler(
    syncId: Int,
    playerInventory: Inventory,
    private val context: ContainerLevelAccess,
    propertyDelegate: ContainerData,
) : AbstractContainerMenu(TankScreenHandler::class.type(), syncId) {

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

    override fun quickMove(player: Player, index: Int): ItemStack {
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

    override fun canUse(player: Player): Boolean = true

    companion object {
    }
}
