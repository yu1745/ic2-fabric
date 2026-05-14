package ic2_120.client.colorprovider

import ic2_120.Ic2_120
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

/**
 * 泥炭矿着色器。
 *
 * 复用锡矿石的纹理（矿石基底+白色斑点），通过 tintindex 染成深褐色，
 * 使其呈现泥炭般的视觉效果。
 */
object PeatOreColorProvider {

    /** 深褐色，用于将锡矿石纹理染成泥炭色 */
    private const val PEAT_COLOR = 0x4A3728

    fun register() {
        val block = Registries.BLOCK.get(Identifier(Ic2_120.MOD_ID, "peat_ore"))
        val item = Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "peat_ore"))

        ColorProviderRegistry.BLOCK.register({ _, _, _, _ ->
            PEAT_COLOR
        }, block)

        ColorProviderRegistry.ITEM.register({ _, _ ->
            PEAT_COLOR
        }, item)
    }
}
