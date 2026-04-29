package ic2_120.content.screen

import ic2_120.content.block.pipes.PipeBlockEntity
import ic2_120.content.block.pipes.PumpAttachmentBlock
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack

import net.minecraft.screen.ScreenHandler
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType
import ic2_120.registry.annotation.ScreenFactory

@ModScreenHandler(names = ["bronze_pump_attachment", "carbon_pump_attachment"])
class PumpAttachmentScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    private val blockEntity: PipeBlockEntity?,
    private val context: ScreenHandlerContext
) : ScreenHandler(PumpAttachmentScreenHandler::class.type(), syncId) {

    constructor(syncId: Int, playerInventory: PlayerInventory, be: PipeBlockEntity) :
        this(syncId, playerInventory, be, ScreenHandlerContext.create(playerInventory.player.world, be.pos))

    init {
        addSlot(object : Slot(object : net.minecraft.inventory.SimpleInventory(1) {}, 0, 0, 0) {
            override fun canInsert(stack: ItemStack): Boolean = false
            override fun canTakeItems(playerEntity: PlayerEntity): Boolean = false
            override fun getStack(): ItemStack = blockEntity?.pumpFilterGhostStack() ?: ItemStack.EMPTY
            override fun hasStack(): Boolean = !(blockEntity?.pumpFilterGhostStack()?.isEmpty ?: true)
        })

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 0, 0))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 0, 0))
        }
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        return ItemStack.EMPTY
    }

    override fun onSlotClick(slotIndex: Int, button: Int, actionType: SlotActionType, player: PlayerEntity) {
        if (slotIndex == 0 && actionType == SlotActionType.PICKUP) {
            val cursor = cursorStack
            if (cursor.isEmpty || button == 1) {
                blockEntity?.clearPumpFilter()
            } else {
                blockEntity?.setPumpFilterFromStack(cursor)
            }
            return
        }
        super.onSlotClick(slotIndex, button, actionType, player)
    }

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is PumpAttachmentBlock &&
                player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        const val PLAYER_INV_START = 1

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): PumpAttachmentScreenHandler {
            val pos = buf.readBlockPos()
            val world = playerInventory.player.world
            val be = world.getBlockEntity(pos) as? PipeBlockEntity
            return PumpAttachmentScreenHandler(syncId, playerInventory, be, ScreenHandlerContext.create(world, pos))
        }
    }
}
