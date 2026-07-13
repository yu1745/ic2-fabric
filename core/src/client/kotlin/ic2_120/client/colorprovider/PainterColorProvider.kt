package ic2_120.client.colorprovider

import ic2_120.content.item.PainterItem
import ic2_120.registry.instance
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry

/** 将涂刷器 NBT 中的颜色应用到物品模型第二层蒙版。 */
object PainterColorProvider {
    fun register() {
        ColorProviderRegistry.ITEM.register({ stack, tintIndex ->
            if (tintIndex != 1) return@register -1
            PainterItem.getColor(stack)?.signColor ?: -1
        }, PainterItem::class.instance())
    }
}
