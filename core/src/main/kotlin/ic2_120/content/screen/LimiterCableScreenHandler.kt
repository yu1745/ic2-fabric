package ic2_120.content.screen

import ic2_120.content.block.cables.CableBlockEntity
import ic2_120.content.block.energy.EnergyNetworkManager
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
import net.minecraft.screen.slot.Slot
import net.minecraft.util.math.BlockPos

@ModScreenHandler(name = "limiter_cable")
class LimiterCableScreenHandler(
    syncId: Int,
    private val playerInventory: PlayerInventory,
    private val pos: BlockPos,
    private val propertyDelegate: PropertyDelegate = ArrayPropertyDelegate(PROPERTY_COUNT)
) : ScreenHandler(LimiterCableScreenHandler::class.type(), syncId) {

    companion object {
        const val PROPERTY_COUNT = 2
        private const val PROP_LOW = 0
        private const val PROP_HIGH = 1

        private const val PLAYER_INV_START = 0
        const val PLAYER_INV_Y = 84
        const val HOTBAR_Y = 142

        // Button IDs for adjustments
        const val BUTTON_MINUS_1000 = 0
        const val BUTTON_MINUS_100  = 1
        const val BUTTON_MINUS_10   = 2
        const val BUTTON_MINUS_1    = 3
        const val BUTTON_PLUS_1     = 4
        const val BUTTON_PLUS_10    = 5
        const val BUTTON_PLUS_100   = 6
        const val BUTTON_PLUS_1000  = 7

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): LimiterCableScreenHandler {
            val pos = buf.readBlockPos()
            val initialLimit = buf.readLong()
            val handler = LimiterCableScreenHandler(syncId, playerInventory, pos)
            handler.limit = initialLimit
            return handler
        }
    }

    /** 32-bit limit value split across two 16-bit PropertyDelegate entries. */
    var limit: Long
        get() {
            val low = propertyDelegate.get(PROP_LOW).toLong() and 0xFFFFL
            val high = propertyDelegate.get(PROP_HIGH).toLong() and 0xFFFFL
            return (high shl 16) or low
        }
        set(value) {
            propertyDelegate.set(PROP_LOW, (value and 0xFFFFL).toInt())
            propertyDelegate.set(PROP_HIGH, ((value shr 16) and 0xFFFFL).toInt())
        }

    init {
        addProperties(propertyDelegate)

        // Player inventory
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, PLAYER_INV_Y + row * 18))
            }
        }
        // Hotbar
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, HOTBAR_Y))
        }
    }

    override fun onButtonClick(player: PlayerEntity, id: Int): Boolean {
        val world = player.world
        if (world.isClient) return true
        val be = world.getBlockEntity(pos) as? CableBlockEntity ?: return false

        val current = be.configuredLimit.coerceAtLeast(0)
        val step = when (id) {
            BUTTON_MINUS_1000 -> -1000L
            BUTTON_MINUS_100  -> -100L
            BUTTON_MINUS_10   -> -10L
            BUTTON_MINUS_1    -> -1L
            BUTTON_PLUS_1     -> 1L
            BUTTON_PLUS_10    -> 10L
            BUTTON_PLUS_100   -> 100L
            BUTTON_PLUS_1000  -> 1000L
            else -> return false
        }
        val newLimit = (current + step).coerceIn(0, 8192)
        if (newLimit != current) {
            be.configuredLimit = newLimit
            be.markDirty()
            EnergyNetworkManager.invalidateAt(world, pos)
            this.limit = newLimit
        }
        return true
    }

    override fun canUse(player: PlayerEntity): Boolean = true

    override fun quickMove(player: PlayerEntity, slotIndex: Int): ItemStack {
        var moved = ItemStack.EMPTY
        val slot = slots[slotIndex]
        if (!slot.hasStack()) return ItemStack.EMPTY
        val stack = slot.stack
        moved = stack.copy()

        if (slotIndex < 27) {
            // Player inventory → hotbar
            if (!insertItem(stack, 27, 36, false)) return ItemStack.EMPTY
        } else {
            // Hotbar → player inventory
            if (!insertItem(stack, 0, 27, false)) return ItemStack.EMPTY
        }

        if (stack.isEmpty) slot.stack = ItemStack.EMPTY
        else slot.markDirty()
        return moved
    }
}
