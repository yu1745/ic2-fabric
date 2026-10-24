package ic2_120.content.screen

import ic2_120.compat.stack

import ic2_120.compat.isClient

import ic2_120.compat.cachedState
import ic2_120.compat.world

import ic2_120.content.block.cables.CableBlockEntity
import ic2_120.content.block.cables.SplitterCableBlock
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.annotation.ScreenFactory
import ic2_120.registry.type
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.core.BlockPos

@ModScreenHandler(name = "splitter_cable")
class SplitterCableScreenHandler(
    syncId: Int,
    private val playerInventory: Inventory,
    private val pos: BlockPos,
    private val propertyDelegate: ContainerData = SimpleContainerData(PROPERTY_COUNT)
) : AbstractContainerMenu(SplitterCableScreenHandler::class.type(), syncId) {

    companion object {
        private const val PROPERTY_COUNT = 2
        private const val PROP_THRESHOLD = 0
        private const val PROP_INVERTED = 1

        const val PLAYER_INV_Y = 84
        const val HOTBAR_Y = 142

        const val BUTTON_THRESHOLD_MINUS = 0
        const val BUTTON_THRESHOLD_PLUS = 1
        const val BUTTON_TOGGLE_INVERTED = 2

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: Inventory, buf: FriendlyByteBuf): SplitterCableScreenHandler {
            val pos = buf.readBlockPos()
            val handler = SplitterCableScreenHandler(syncId, playerInventory, pos)
            handler.threshold = buf.readVarInt()
            handler.inverted = buf.readBoolean()
            return handler
        }
    }

    var threshold: Int
        get() = propertyDelegate.getValue(PROP_THRESHOLD)
        set(value) = propertyDelegate.set(
            PROP_THRESHOLD,
            value.coerceIn(CableBlockEntity.MIN_SPLITTER_THRESHOLD, CableBlockEntity.MAX_SPLITTER_THRESHOLD)
        )

    var inverted: Boolean
        get() = propertyDelegate.getValue(PROP_INVERTED) != 0
        set(value) = propertyDelegate.set(PROP_INVERTED, if (value) 1 else 0)

    init {
        addDataSlots(propertyDelegate)
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, PLAYER_INV_Y + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, HOTBAR_Y))
        }
    }

    override fun clickMenuButton(player: Player, id: Int): Boolean {
        if (player.world.isClient) return true
        val be = player.world.getBlockEntity(pos) as? CableBlockEntity ?: return false
        if (be.cachedState.block !is SplitterCableBlock) return false

        when (id) {
            BUTTON_THRESHOLD_MINUS -> be.splitterThreshold =
                (be.splitterThreshold - 1).coerceAtLeast(CableBlockEntity.MIN_SPLITTER_THRESHOLD)
            BUTTON_THRESHOLD_PLUS -> be.splitterThreshold =
                (be.splitterThreshold + 1).coerceAtMost(CableBlockEntity.MAX_SPLITTER_THRESHOLD)
            BUTTON_TOGGLE_INVERTED -> be.splitterInverted = !be.splitterInverted
            else -> return false
        }

        threshold = be.splitterThreshold
        inverted = be.splitterInverted
        be.setChanged()
        (be.cachedState.block as SplitterCableBlock).refreshControlState(player.world, pos, be.cachedState)
        return true
    }

    override fun stillValid(player: Player): Boolean =
        player.world.getBlockState(pos).block is SplitterCableBlock &&
            player.distanceToSqr(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0

    override fun quickMoveStack(player: Player, slotIndex: Int): ItemStack {
        val slot = slots.getOrNull(slotIndex) ?: return ItemStack.EMPTY
        if (!slot .hasItem()) return ItemStack.EMPTY
        val stack = slot.stack
        val moved = stack.copy()
        val inserted = if (slotIndex < 27) {
            moveItemStackTo(stack, 27, 36, false)
        } else {
            moveItemStackTo(stack, 0, 27, false)
        }
        if (!inserted) return ItemStack.EMPTY
        if (stack.isEmpty) slot.stack = ItemStack.EMPTY else slot.setChanged()
        return moved
    }
}
