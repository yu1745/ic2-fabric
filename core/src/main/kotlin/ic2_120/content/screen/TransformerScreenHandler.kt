package ic2_120.content.screen

import ic2_120.content.sync.TransformerSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.network.PacketByteBuf

import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import net.minecraft.state.property.Properties
import net.minecraft.util.math.Direction
import ic2_120.registry.annotation.ScreenFactory

/**
 * 变压器的 ScreenHandler。
 * 提供固定升压 / 固定降压两个按钮。
 *
 * 所有电压等级的变压器（LV、MV、HV、EV）共用此 UI。
 */
@ModScreenHandler(names = ["lv_transformer", "mv_transformer", "hv_transformer", "ev_transformer"])
class TransformerScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    private val context: ScreenHandlerContext,
    propertyDelegate: PropertyDelegate,
    private val tier: Int = 1
) : ScreenHandler(TransformerScreenHandler::class.type(), syncId) {

    val sync = TransformerSync(
        SyncedDataView(propertyDelegate),
        getFacing = { getFacingDirection() },
        tier = tier
    )

    private fun getFacingDirection(): Direction {
        return context.get({ world, pos ->
            world.getBlockState(pos).get(Properties.FACING)
        }, Direction.NORTH)
    }

    override fun onButtonClick(player: PlayerEntity, id: Int): Boolean {
        context.get({ world, pos ->
            val be = world.getBlockEntity(pos)
            if (be is ic2_120.content.block.machines.TransformerBlockEntity) {
                when (id) {
                    BUTTON_ID_STEP_UP -> be.setModeDirect(TransformerSync.Mode.STEP_UP)
                    BUTTON_ID_STEP_DOWN -> be.setModeDirect(TransformerSync.Mode.STEP_DOWN)
                    else -> return@get
                }
            }
        }, true)
        return true
    }

    init {
        addProperties(propertyDelegate)

        // 玩家物品栏
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 137 + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 195))
        }
    }

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            val block = world.getBlockState(pos).block
            block is ic2_120.content.block.TransformerBlock &&
                    player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    override fun quickMove(player: PlayerEntity, slot: Int): net.minecraft.item.ItemStack =
        net.minecraft.item.ItemStack.EMPTY

    companion object {
        const val BUTTON_ID_STEP_UP = 0
        const val BUTTON_ID_STEP_DOWN = 1

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): TransformerScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val tier = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            return TransformerScreenHandler(syncId, playerInventory, context, ArrayPropertyDelegate(propertyCount), tier)
        }
    }
}
