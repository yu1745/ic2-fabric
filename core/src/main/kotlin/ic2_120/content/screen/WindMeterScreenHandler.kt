package ic2_120.content.screen

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

@ModScreenHandler(name = "wind_meter")
class WindMeterScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(WindMeterScreenHandler::class.type(), syncId) {

    init {
        addProperties(propertyDelegate)
    }

    fun getValue(index: Int): Int = propertyDelegate.get(index)

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack = ItemStack.EMPTY

    override fun canUse(player: PlayerEntity): Boolean = true

    companion object {
        const val IDX_MEAN_PERMILLE = 0
        const val IDX_WEATHER_PERMILLE = 1
        const val IDX_GUST_PERMILLE = 2
        const val IDX_EFFECTIVE_PERMILLE = 3
        const val IDX_WOOD_KU = 4
        const val IDX_IRON_KU = 5
        const val IDX_STEEL_KU = 6
        const val IDX_CARBON_KU = 7
        const val IDX_WOOD_START_Y = 8
        const val IDX_IRON_START_Y = 9
        const val IDX_STEEL_START_Y = 10
        const val IDX_CARBON_START_Y = 11
        const val PROP_COUNT = 12
        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): WindMeterScreenHandler {
            val delegate = ArrayPropertyDelegate(PROP_COUNT)
            for (i in 0 until PROP_COUNT) {
                delegate[i] = buf.readVarInt()
            }
            return WindMeterScreenHandler(syncId, playerInventory, delegate)
        }
    }
}
