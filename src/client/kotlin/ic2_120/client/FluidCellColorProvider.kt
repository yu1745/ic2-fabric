package ic2_120.client

import ic2_120.Ic2_120
import ic2_120.content.item.getFluidCellVariant
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.SpriteContents
import net.minecraft.fluid.Fluid
import net.minecraft.fluid.Fluids
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory

/**
 * 通用流体单元着色器：从流体贴图采样颜色，渲染到物品中心（tintindex 1），
 * 便于手持时识别所含流体。水和岩浆使用原版硬编码颜色。
 * 大部分流体颜色直接来自贴图，因此从已加载的 Sprite 解析贴图取平均色。
 */
object FluidCellColorProvider {

    private const val WATER_COLOR = 0x3F76E4
    private const val LAVA_COLOR = 0xFF4400

    private val colorCache = mutableMapOf<Fluid, Int>()

    private val logger = LoggerFactory.getLogger("ic2_120/FluidCellColorProvider")

    private val spriteImageField = run {
        SpriteContents::class.java.getDeclaredField("image").apply { isAccessible = true }
    }

    fun register() {
        val fluidCell = Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "fluid_cell"))
        ColorProviderRegistry.ITEM.register({ stack, tintIndex ->
            if (tintIndex != 1) return@register -1
            val fluid = stack.getFluidCellVariant()?.fluid ?: return@register -1
            when (fluid) {
                // Fluids.WATER -> WATER_COLOR
                // Fluids.LAVA -> LAVA_COLOR
                else -> colorCache.getOrPut(fluid) { sampleColorFromFluidTexture(fluid) }
            }
        }, fluidCell)
    }

    /**
     * 从流体 still 贴图（已加载到 Sprite）采样平均颜色。
     * 通过反射访问 SpriteContents.image，无需重新加载纹理。
     */
    private fun sampleColorFromFluidTexture(fluid: Fluid): Int {
        val handler = FluidRenderHandlerRegistry.INSTANCE.get(fluid) ?: return -1
        val sprites = handler.getFluidSprites(null, null, fluid.defaultState)
        if (sprites.isEmpty()) return -1
        val stillSprite = sprites[0]
        // println("stillSprite: $stillSprite")
        val image = spriteImageField.get(stillSprite.contents) as? NativeImage ?: return -1
        return sampleAverageColor(image)
    }

    /**
     * 对贴图非透明像素取平均色，返回 0xAARRGGBB（alpha 固定为 0xFF）。
     * 使用全局采样，排除边缘像素以提高准确性。
     *
     * 注意：NativeImage.getColor() 返回 ABGR 格式（不是 ARGB）：
     * - bits 24-31: Alpha
     * - bits 16-23: Blue
     * - bits 8-15: Green
     * - bits 0-7: Red
     */
    private fun sampleAverageColor(image: NativeImage): Int {
        val w = image.width
        val h = image.height
        if (w <= 0 || h <= 0) return -1

        // 排除边缘像素（流体纹理边缘通常有渐变/透明）
        val edgeMargin = minOf(w, h) / 8

        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        var count = 0

        // 全局采样：遍历所有非边缘像素
        for (y in edgeMargin until (h - edgeMargin)) {
            for (x in edgeMargin until (w - edgeMargin)) {
                val color = image.getColor(x, y)

                // ABGR 格式解析
                val a = (color shr 24) and 0xFF
                val b = (color shr 16) and 0xFF  // 注意：bits 16-23 是 Blue
                val g = (color shr 8) and 0xFF   // bits 8-15 是 Green
                val r = color and 0xFF          // bits 0-7 是 Red

                // 只采样不透明像素
                if (a >= 64) {
                    rSum += r
                    gSum += g
                    bSum += b
                    count++
                }
            }
        }

        if (count == 0) {
            logger.warn("No valid pixels found in fluid texture")
            return -1
        }

        val r = (rSum / count).toInt().coerceIn(0, 255)
        val g = (gSum / count).toInt().coerceIn(0, 255)
        val b = (bSum / count).toInt().coerceIn(0, 255)

        // 返回 ARGB 格式（Minecraft 着色器使用的格式）
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}
