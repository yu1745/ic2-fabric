package buildcraft_addon.content.screen

import buildcraft_addon.content.block.StoneEngineBlock
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
import net.minecraft.item.Items

@ModScreenHandler(block = StoneEngineBlock::class)
class StoneEngineScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    val blockInventory: Inventory,
    val context: ScreenHandlerContext,
    val propertyDelegate: PropertyDelegate,
) : ScreenHandler(StoneEngineScreenHandler::class.type(), syncId) {

    init {
        checkSize(blockInventory, 1)
        checkDataCount(propertyDelegate, 2)

        // Fuel slot (BC8 pos: 80, 41)
        addSlot(object : Slot(blockInventory, 0, 80, 41) {
            override fun canInsert(stack: net.minecraft.item.ItemStack): Boolean {
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

        addProperties(propertyDelegate)
    }

    fun getBurnProgress(): Float {
        val total = propertyDelegate.get(1)
        if (total <= 0) return 0f
        return propertyDelegate.get(0).toFloat() / total.toFloat()
    }

    override fun quickMove(player: PlayerEntity, slotIndex: Int): net.minecraft.item.ItemStack {
        val slot = slots[slotIndex] ?: return ItemStack.EMPTY
        if (!slot.hasStack()) return ItemStack.EMPTY
        val original = slot.stack
        val newStack = original.copy()

        if (slotIndex == 0) {
            if (!insertItem(original, 1, 37, true)) return ItemStack.EMPTY
        } else if (slotIndex in 1..36) {
            if (!insertItem(original, 0, 1, false)) return ItemStack.EMPTY
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
            world.getBlockState(pos).block is StoneEngineBlock && player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)
    }

    companion object {
        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): StoneEngineScreenHandler {
            buf.readBlockPos()
            buf.readVarInt()
            return StoneEngineScreenHandler(syncId, playerInventory, SimpleInventory(1), ScreenHandlerContext.EMPTY, ArrayPropertyDelegate(2))
        }
    }
}
