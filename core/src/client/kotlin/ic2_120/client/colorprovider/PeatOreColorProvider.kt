package ic2_120.client.colorprovider

import ic2_120.Ic2_120
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry
import net.minecraft.client.color.block.BlockTintSources
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier

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
        val block = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "peat_ore"))
        BlockColorRegistry.register(listOf(BlockTintSources.constant(PEAT_COLOR or -0x1000000)), block)
    }
}
