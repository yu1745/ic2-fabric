package ic2_120.content.screen

import ic2_120.content.block.TeslaCoilBlock
import ic2_120.content.sync.TeslaCoilSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext

/**
 * 特斯拉线圈 GUI 的 ScreenHandler。无机器槽位，仅玩家背包 + 能量同步。
 */
@ModScreenHandler(block = TeslaCoilBlock::class)
class TeslaCoilScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(TeslaCoilScreenHandler::class.type(), syncId) {

    val sync = TeslaCoilSync(
        schema = SyncedDataView(propertyDelegate),
        currentTickProvider = { null }
    )

    init {
        addProperties(propertyDelegate)
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(net.minecraft.screen.slot.Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(net.minecraft.screen.slot.Slot(playerInventory, col, 8 + col * 18, 142))
        }
    }

    override fun quickMove(player: PlayerEntity, index: Int): net.minecraft.item.ItemStack =
        net.minecraft.item.ItemStack.EMPTY

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is TeslaCoilBlock && player.squaredDistanceTo(
                pos.x + 0.5,
                pos.y + 0.5,
                pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): TeslaCoilScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val ctx = ScreenHandlerContext.create(playerInventory.player.world, pos)
            return TeslaCoilScreenHandler(syncId, playerInventory, ctx, ArrayPropertyDelegate(propertyCount))
        }
    }
}
