package ic2_120.content.screen

import ic2_120.content.block.LeashKineticGeneratorBlock
import ic2_120.content.sync.LeashKineticGeneratorSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.annotation.ScreenFactory
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

@ModScreenHandler(block = LeashKineticGeneratorBlock::class)
class LeashKineticGeneratorScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(LeashKineticGeneratorScreenHandler::class.type(), syncId) {

    val sync = LeashKineticGeneratorSync(SyncedDataView(propertyDelegate))

    init {
        addProperties(propertyDelegate)

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 0, 0))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 0, 0))
        }
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack = ItemStack.EMPTY

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is LeashKineticGeneratorBlock &&
                player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        const val PLAYER_INV_START = 0
        const val SLOT_SIZE = 18

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): LeashKineticGeneratorScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            return LeashKineticGeneratorScreenHandler(
                syncId,
                playerInventory,
                context,
                ArrayPropertyDelegate(propertyCount)
            )
        }
    }
}
