package ic2_120.content.screen

import ic2_120.compat.world

import ic2_120.compat.pos

import ic2_120.content.block.pipes.PipeBlockEntity
import ic2_120.content.block.pipes.PumpAttachmentBlock
import ic2_120.content.upgrade.FluidPipeUpgradeComponent
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.Slot
import net.minecraft.world.inventory.ClickType
import ic2_120.registry.annotation.ScreenFactory

@ModScreenHandler(names = ["bronze_pump_attachment", "carbon_pump_attachment"])
class PumpAttachmentScreenHandler(
    syncId: Int,
    private val playerInventory: Inventory,
    private val blockEntity: PipeBlockEntity?,
    private val context: ContainerLevelAccess
) : AbstractContainerMenu(PumpAttachmentScreenHandler::class.type(), syncId) {

    constructor(syncId: Int, playerInventory: Inventory, be: PipeBlockEntity) :
        this(syncId, playerInventory, be, ContainerLevelAccess.create(playerInventory.player.world, be.pos))

    init {
        addSlot(object : Slot(SimpleContainer(1), FILTER_SLOT, 80, 31) {
            override fun canInsert(stack: ItemStack): Boolean = false
            override fun canTakeItems(playerEntity: Player): Boolean = false
        })

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 80 + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 138))
        }
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        return ItemStack.EMPTY
    }

    fun filterFluid(): Fluid? = blockEntity?.pumpFilterFluid()

    /** 设置幽灵流体过滤器；JEI 拖放和真实容器取样共用。 */
    fun setFluidFilter(fluid: Fluid): Boolean {
        val be = blockEntity ?: return false
        be.setPumpFilterFluid(fluid)
        sendContentUpdates()
        return true
    }

    override fun onSlotClick(slotIndex: Int, button: Int, actionType: ClickType, player: Player) {
        if (slotIndex == FILTER_SLOT && actionType == ClickType.PICKUP) {
            val cursor = cursorStack
            if (cursor.isEmpty || button == 1) {
                blockEntity?.clearPumpFilter()
            } else {
                val fluid = FluidPipeUpgradeComponent.readFluidFromItemStack(cursor)
                if (fluid != null && setFluidFilter(fluid)) {
                    returnCursorToInventory()
                }
            }
            return
        }
        super.onSlotClick(slotIndex, button, actionType, player)
    }

    override fun stillValid(player: Player): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is PumpAttachmentBlock &&
                player.distanceToSqr(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    /** 真实容器仅作为过滤样本；设置后放回背包，不占用过滤槽。 */
    private fun returnCursorToInventory() {
        val returning = cursorStack.copy()
        if (returning.isEmpty) return
        playerInventory.safeInsert(returning)
        cursorStack = if (returning.isEmpty) ItemStack.EMPTY else returning
    }

    companion object {
        const val FILTER_SLOT = 0
        const val PLAYER_INV_START = 1

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: Inventory, buf: FriendlyByteBuf): PumpAttachmentScreenHandler {
            val pos = buf.readBlockPos()
            val world = playerInventory.player.world
            val be = world.getBlockEntity(pos) as? PipeBlockEntity
            return PumpAttachmentScreenHandler(syncId, playerInventory, be, ContainerLevelAccess.create(world, pos))
        }
    }
}
