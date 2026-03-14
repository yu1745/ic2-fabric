package ic2_120.client

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.SpriteContents
import net.minecraft.fluid.Fluid
import org.slf4j.LoggerFactory
import java.lang.reflect.Field

/**
 * 流体工具类：提供流体相关的实用函数
 */
object FluidUtils {

    private val logger = LoggerFactory.getLogger("ic2_120/FluidUtils")

    private val spriteImageField: Field = run {
        SpriteContents::class.java.declaredFields
            .firstOrNull { it.type == NativeImage::class.java }
            ?.apply { isAccessible = true }
            ?: throw NoSuchFieldException("SpriteContents has no NativeImage field (mapping may differ)")
    }

    private val colorCache = mutableMapOf<Fluid, Int>()

    /**
     * 获取流体的颜色（带缓存）
     * @param fluid 目标流体
     * @return ARGB 格式的颜色值，获取失败返回 -1
     */
    fun getFluidColor(fluid: Fluid): Int {
        return colorCache.getOrPut(fluid) {
            sampleColorFromFluidTexture(fluid)
        }
    }

    /**
     * 清除颜色缓存
     */
    fun clearColorCache() {
        colorCache.clear()
    }

    /**
     * 从流体的 still 贴图采样平均颜色
     * @param fluid 目标流体
     * @return ARGB 格式的颜色值，获取失败返回 -1
     */
    private fun sampleColorFromFluidTexture(fluid: Fluid): Int {
        val handler = FluidRenderHandlerRegistry.INSTANCE.get(fluid) ?: return -1
        val sprites = handler.getFluidSprites(null, null, fluid.defaultState)
        if (sprites.isEmpty()) return -1

        val stillSprite = sprites[0]
        @Suppress("UNCHECKED_CAST")
        val image = spriteImageField.get(stillSprite.contents) as? NativeImage ?: return -1
        return sampleAverageColor(image)
    }

    /**
     * 对 NativeImage 非透明像素取平均色
     * @param image 目标图像
     * @return ARGB 格式的颜色值（alpha 固定为 0xFF），获取失败返回 -1
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
                val b = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val r = color and 0xFF

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
