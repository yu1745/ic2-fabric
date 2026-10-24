package buildcraft_addon.content.screen

import buildcraft_addon.content.block.StoneEngineBlock
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
import net.minecraft.world.item.Items

@ModScreenHandler(block = StoneEngineBlock::class)
class StoneEngineScreenHandler(
    syncId: Int,
    playerInventory: Inventory,
    val blockInventory: Container,
    val context: ContainerLevelAccess,
    val propertyDelegate: ContainerData,
) : AbstractContainerMenu(StoneEngineScreenHandler::class.type(), syncId) {

    init {
        checkContainerSize(blockInventory, 1)
        checkDataCount(propertyDelegate, 2)

        // Fuel slot (BC8 pos: 80, 41)
        addSlot(object : Slot(blockInventory, 0, 80, 41) {
            override fun canInsert(stack: net.minecraft.world.item.ItemStack): Boolean {
                return stack.item.recipeRemainder !== stack.item || stack.item.toString().contains("coal")
            }
        })

        // Player inventory (y offset = 84)
        for (row in 0..2) {
            for (col in 0..8) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18))
            }
        }
        // Player hotbar (y = 84 + 58 = 142)
        for (col in 0..8) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 142))
        }

        addDataSlots(propertyDelegate)
    }

    fun getBurnProgress(): Float {
        val total = propertyDelegate.get(1)
        if (total <= 0) return 0f
        return propertyDelegate.get(0).toFloat() / total.toFloat()
    }

    override fun quickMoveStack(player: Player, slotIndex: Int): net.minecraft.world.item.ItemStack {
        val slot = slots[slotIndex] ?: return ItemStack.EMPTY
        if (!slot .hasItem()) return ItemStack.EMPTY
        val original = slot.stack
        val newStack = original.copy()

        if (slotIndex == 0) {
            if (!moveItemStackTo(original, 1, 37, true)) return ItemStack.EMPTY
        } else if (slotIndex in 1..36) {
            if (!moveItemStackTo(original, 0, 1, false)) return ItemStack.EMPTY
        }

        if (original.isEmpty) {
            slot.stack = ItemStack.EMPTY
        } else {
            slot.setChanged()
        }
        return newStack
    }

    override fun stillValid(player: Player): Boolean {
        return context.get({ world, pos ->
            world.getBlockState(pos).block is StoneEngineBlock && player.distanceToSqr(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)
    }

    companion object {
        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: Inventory, buf: FriendlyByteBuf): StoneEngineScreenHandler {
            buf.readBlockPos()
            buf.readVarInt()
            return StoneEngineScreenHandler(syncId, playerInventory, SimpleContainer(1), ContainerLevelAccess.EMPTY, SimpleContainerData(2))
        }
    }
}
