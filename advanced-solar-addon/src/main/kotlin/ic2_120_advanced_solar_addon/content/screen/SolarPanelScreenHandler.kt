package ic2_120_advanced_solar_addon.content.screen

import ic2_120_advanced_solar_addon.content.sync.SolarPanelSync
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import net.minecraft.util.math.Direction
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.annotation.ScreenFactory
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.type

@ModScreenHandler(names = ["advanced_solar_panel", "hybrid_solar_panel", "ultimate_solar_panel", "quantum_solar_panel"])
class SolarPanelScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(SolarPanelScreenHandler::class.type(), syncId) {

    val sync = SolarPanelSync(
        schema = SyncedDataView(propertyDelegate),
        capacity = 1L,
        tier = 1,
        getFacing = { Direction.NORTH },
        currentTickProvider = { null }
    )

    init {
        addProperties(propertyDelegate)

        // Player inventory
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 0, 0))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 0, 0))
        }
    }

    override fun quickMove(player: PlayerEntity, index: Int): net.minecraft.item.ItemStack {
        return net.minecraft.item.ItemStack.EMPTY
    }

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        const val SLOT_SIZE = 18
        const val PLAYER_INV_START = 0

        @ScreenFactory
        @JvmStatic
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): SolarPanelScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val propertyDelegate = net.minecraft.screen.ArrayPropertyDelegate(propertyCount)
            return SolarPanelScreenHandler(syncId, playerInventory, context, propertyDelegate)
        }
    }
}
