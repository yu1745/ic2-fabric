package ic2_120.client

import com.mojang.serialization.MapCodec
import ic2_120.Ic2_120
import ic2_120.content.item.PainterItem
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.ItemOwner
import net.minecraft.world.item.ItemStack

/** Selects one of IC2's complete painter textures from the stored dye color. */
object PainterModelPredicates {
    private val COLOR_ID = Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "painter_color")

    fun register() {
        RangeSelectItemModelProperties.ID_MAPPER.put(COLOR_ID, PainterColorProperty.CODEC)
    }

    private object PainterColorProperty : RangeSelectItemModelProperty {
        val CODEC: MapCodec<PainterColorProperty> = MapCodec.unit(this)
        override fun get(stack: ItemStack, level: ClientLevel?, owner: ItemOwner?, seed: Int): Float =
            PainterItem.getColor(stack)?.id?.plus(1)?.div(16.0f) ?: 0f
        override fun type(): MapCodec<PainterColorProperty> = CODEC
    }
}
