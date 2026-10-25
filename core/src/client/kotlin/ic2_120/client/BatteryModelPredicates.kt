package ic2_120.client

import com.mojang.serialization.MapCodec
import ic2_120.Ic2_120
import ic2_120.compat.nbt
import ic2_120.content.item.NanoSaber
import ic2_120.content.item.energy.IBatteryItem
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.ItemOwner
import net.minecraft.world.item.ItemStack

/** Dynamic values used by the 26.2 item definition range dispatch models. */
object BatteryModelPredicates {
    private val CHARGE_ID = Identifier.fromNamespaceAndPath("ic2", "charge")
    private val NANO_SABER_ACTIVE_ID = Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "nano_saber_active")
    private val SCROLL_BOUND_ID = Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "bound")

    fun register() {
        RangeSelectItemModelProperties.ID_MAPPER.put(CHARGE_ID, ChargeProperty.CODEC)
        RangeSelectItemModelProperties.ID_MAPPER.put(NANO_SABER_ACTIVE_ID, NanoSaberProperty.CODEC)
        RangeSelectItemModelProperties.ID_MAPPER.put(SCROLL_BOUND_ID, ScrollBoundProperty.CODEC)
    }

    private object ChargeProperty : RangeSelectItemModelProperty {
        val CODEC: MapCodec<ChargeProperty> = MapCodec.unit(this)
        override fun get(stack: ItemStack, level: ClientLevel?, owner: ItemOwner?, seed: Int): Float =
            (stack.item as? IBatteryItem)?.getChargeRatio(stack)?.toFloat() ?: 0f
        override fun type(): MapCodec<ChargeProperty> = CODEC
    }

    private object NanoSaberProperty : RangeSelectItemModelProperty {
        val CODEC: MapCodec<NanoSaberProperty> = MapCodec.unit(this)
        override fun get(stack: ItemStack, level: ClientLevel?, owner: ItemOwner?, seed: Int): Float =
            NanoSaber.getActiveData()
        override fun type(): MapCodec<NanoSaberProperty> = CODEC
    }

    private object ScrollBoundProperty : RangeSelectItemModelProperty {
        val CODEC: MapCodec<ScrollBoundProperty> = MapCodec.unit(this)
        override fun get(stack: ItemStack, level: ClientLevel?, owner: ItemOwner?, seed: Int): Float =
            if (stack.nbt?.getBoolean("HasBind")?.orElse(false) == true) 1f else 0f
        override fun type(): MapCodec<ScrollBoundProperty> = CODEC
    }
}
