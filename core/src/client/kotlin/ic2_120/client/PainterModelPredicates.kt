package ic2_120.client

import ic2_120.Ic2_120
import ic2_120.content.item.PainterItem
import ic2_120.registry.instance
import net.minecraft.client.item.ModelPredicateProviderRegistry
import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier

/** Selects one of IC2's complete painter textures from the stored dye color. */
object PainterModelPredicates {
    private val COLOR_ID = Identifier(Ic2_120.MOD_ID, "painter_color")

    fun register() {
        ModelPredicateProviderRegistry.register(
            PainterItem::class.instance(),
            COLOR_ID
        ) { stack: ItemStack, _, _, _ ->
            PainterItem.getColor(stack)?.id?.plus(1)?.div(16.0f) ?: 0.0f
        }
    }
}
