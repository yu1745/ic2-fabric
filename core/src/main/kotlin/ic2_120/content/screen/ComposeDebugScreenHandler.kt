package ic2_120.content.screen

import ic2_120.content.block.ComposeDebugBlock
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory

import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot
import net.minecraft.util.math.BlockPos
import net.minecraft.network.PacketByteBuf
import ic2_120.registry.annotation.ScreenFactory

/**
 * ComposeDebugScreen 的服务端 ScreenHandler。
 * 不需要任何槽位或同步数据，纯展示用。
 */
@ModScreenHandler(block = ComposeDebugBlock::class)
class ComposeDebugScreenHandler(
    syncId: Int,
    val playerInventory: PlayerInventory
) : ScreenHandler(ComposeDebugScreenHandler::class.type(), syncId) {

    init {
        // 绑定到玩家背包的两个演示槽位；坐标由客户端每帧通过 Compose 锚点驱动。
        addSlot(Slot(playerInventory, 0, 0, 0))
        addSlot(Slot(playerInventory, 1, 0, 0))
    }

    override fun quickMove(player: PlayerEntity, index: Int): net.minecraft.item.ItemStack =
        net.minecraft.item.ItemStack.EMPTY

    override fun canUse(player: PlayerEntity): Boolean = true

    companion object {
        const val GUI_WIDTH = 240
        const val GUI_HEIGHT = 200
        const val SLOT_LEFT_INDEX = 0
        const val SLOT_RIGHT_INDEX = 1

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf?): ComposeDebugScreenHandler {
            return ComposeDebugScreenHandler(syncId, playerInventory)
        }
    }
}
