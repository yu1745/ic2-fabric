package ic2_120.content.screen

import ic2_120.compat.world

import ic2_120.content.sync.TransformerSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.SimpleContainer
import net.minecraft.network.FriendlyByteBuf

import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.Slot
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.core.Direction
import ic2_120.registry.annotation.ScreenFactory

/**
 * 变压器的 AbstractContainerMenu。
 * 提供固定升压 / 固定降压两个按钮。
 *
 * 所有电压等级的变压器（LV、MV、HV、EV）共用此 UI。
 */
@ModScreenHandler(names = ["lv_transformer", "mv_transformer", "hv_transformer", "ev_transformer"])
class TransformerScreenHandler(
    syncId: Int,
    playerInventory: Inventory,
    private val context: ContainerLevelAccess,
    propertyDelegate: ContainerData,
    private val tier: Int = 1
) : AbstractContainerMenu(TransformerScreenHandler::class.type(), syncId) {

    val sync = TransformerSync(
        SyncedDataView(propertyDelegate),
        getFacing = { getFacingDirection() },
        tier = tier
    )

    private fun getFacingDirection(): Direction {
        return context.evaluate({ world, pos ->
            world.getBlockState(pos).getValue(BlockStateProperties.FACING)
        }, Direction.NORTH)
    }

    override fun clickMenuButton(player: Player, id: Int): Boolean {
        context.evaluate({ world, pos ->
            val be = world.getBlockEntity(pos)
            if (be is ic2_120.content.block.machines.TransformerBlockEntity) {
                when (id) {
                    BUTTON_ID_STEP_UP -> be.setModeDirect(TransformerSync.Mode.STEP_UP)
                    BUTTON_ID_STEP_DOWN -> be.setModeDirect(TransformerSync.Mode.STEP_DOWN)
                    else -> return@evaluate true
                }
            }
        }, true)
        return true
    }

    init {
        addDataSlots(propertyDelegate)

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

    override fun stillValid(player: Player): Boolean =
        context.evaluate({ world, pos ->
            val block = world.getBlockState(pos).block
            block is ic2_120.content.block.TransformerBlock &&
                    player.distanceToSqr(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    override fun quickMoveStack(player: Player, slot: Int): net.minecraft.world.item.ItemStack =
        net.minecraft.world.item.ItemStack.EMPTY

    companion object {
        const val BUTTON_ID_STEP_UP = 0
        const val BUTTON_ID_STEP_DOWN = 1

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: Inventory, buf: FriendlyByteBuf): TransformerScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val tier = buf.readVarInt()
            val context = ContainerLevelAccess.create(playerInventory.player.level(), pos)
            return TransformerScreenHandler(syncId, playerInventory, context, SimpleContainerData(propertyCount), tier)
        }
    }
}
