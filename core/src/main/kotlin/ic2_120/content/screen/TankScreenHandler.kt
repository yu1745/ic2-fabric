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
import ic2_120.registry.annotation.ScreenFactory

@ModScreenHandler(name = "tank")
class TankScreenHandler(
    syncId: Int,
    propertyDelegate: PropertyDelegate,
    private val context: ScreenHandlerContext
) : ScreenHandler(TankScreenHandler::class.type(), syncId) {

    val sync = TankSync(SyncedDataView(propertyDelegate)) { 0 }

    init {
        addProperties(propertyDelegate)
    }

    override fun quickMove(player: PlayerEntity?, index: Int) = ItemStack.EMPTY
    override fun canUse(player: PlayerEntity) = true

    companion object {
        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): TankScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            return TankScreenHandler(syncId, ArrayPropertyDelegate(propertyCount), context)
        }
    }
}
