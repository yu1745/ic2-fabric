package ic2_120.content.screen

import ic2_120.content.sync.TransformerSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory

import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.state.property.Properties
import net.minecraft.util.math.Direction
import ic2_120.registry.annotation.ScreenFactory

/**
 * 变压器的 ScreenHandler。
 * 提供模式切换按钮（升压/降压）。
 *
 * 所有电压等级的变压器（LV、MV、HV、EV）共用此 UI。
 */
@ModScreenHandler(names = ["lv_transformer", "mv_transformer", "hv_transformer", "ev_transformer"])
class TransformerScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    private val context: ScreenHandlerContext,
    propertyDelegate: PropertyDelegate,
    private val tier: Int = 1  // 变压器等级
) : ScreenHandler(TransformerScreenHandler::class.type(), syncId) {

    val sync = TransformerSync(
        SyncedDataView(propertyDelegate),
        getFacing = { getFacingDirection() },
        tier = tier  // 传递正确的等级
    )

    private fun getFacingDirection(): Direction {
        return context.get({ world, pos ->
            world.getBlockState(pos).get(Properties.HORIZONTAL_FACING)
        }, Direction.NORTH)
    }

    /**
     * 服务端处理：收到客户端发来的按钮点击包后在此执行。
     * 客户端应发送 [ButtonClickC2SPacket] 指定 syncId 与 [BUTTON_ID_TOGGLE_MODE]。
     */
    override fun onButtonClick(player: PlayerEntity, id: Int): Boolean {
        if (id != BUTTON_ID_TOGGLE_MODE) return false
        context.get({ world, pos ->
            val be = world.getBlockEntity(pos)
            if (be is ic2_120.content.block.machines.TransformerBlockEntity) {
                be.toggleMode()
            }
        }, true)
        return true
    }

    init {
        addProperties(propertyDelegate)
    }

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            val block = world.getBlockState(pos).block
            block is ic2_120.content.block.TransformerBlock &&
                    player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    override fun quickMove(player: PlayerEntity, slot: Int): net.minecraft.item.ItemStack {
        // 变压器没有物品栏，返回空物品栈
        return net.minecraft.item.ItemStack.EMPTY
    }

    companion object {
        /** 客户端发送按钮点击时使用此 id，服务端在 [onButtonClick] 中处理 */
        const val BUTTON_ID_TOGGLE_MODE = 0

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): TransformerScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val tier = buf.readVarInt()  // 读取变压器等级
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            return TransformerScreenHandler(syncId, playerInventory, context, ArrayPropertyDelegate(propertyCount), tier)
        }
    }
}
