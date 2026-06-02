package buildcraft_addon.content.screen

import buildcraft_addon.content.block.IronEngineBlock
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import ic2_120.registry.annotation.ScreenFactory
import net.minecraft.item.ItemStack

@ModScreenHandler(block = IronEngineBlock::class)
class IronEngineScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    val context: ScreenHandlerContext,
    val propertyDelegate: PropertyDelegate,
) : ScreenHandler(IronEngineScreenHandler::class.type(), syncId) {

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

        addProperties(propertyDelegate)
    }

    // PropertyDelegate: [0]=fuelAmount, [1]=coolantAmount, [2]=residueAmount, [3]=heat
    fun getFuelAmount(): Int = propertyDelegate.get(0)
    fun getCoolantAmount(): Int = propertyDelegate.get(1)
    fun getResidueAmount(): Int = propertyDelegate.get(2)
    fun getHeat(): Int = propertyDelegate.get(3)

    override fun quickMove(player: PlayerEntity, slotIndex: Int): net.minecraft.item.ItemStack {
        return ItemStack.EMPTY
    }

    override fun canUse(player: PlayerEntity): Boolean {
        return context.get({ world, pos ->
            world.getBlockState(pos).block is IronEngineBlock && player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)
    }

    companion object {
        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): IronEngineScreenHandler {
            buf.readBlockPos()
            buf.readVarInt()
            return IronEngineScreenHandler(syncId, playerInventory, ScreenHandlerContext.EMPTY, ArrayPropertyDelegate(4))
        }
    }
}
