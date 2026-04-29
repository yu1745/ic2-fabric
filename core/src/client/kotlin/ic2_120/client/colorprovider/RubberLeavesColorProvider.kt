package ic2_120.client.colorprovider

import ic2_120.Ic2_120
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry
import net.minecraft.client.color.world.BiomeColors
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

//todo 目前橡胶树叶还是不够显眼，这是有问题的，不好找

/**
 * 橡胶树叶生物群系着色器
 *
 * 根据生物群系对橡胶树叶应用颜色：
 * - 亮度参考白桦树叶 (foliage color)
 * - 色调比白桦树叶更黄，呈现独特的金黄色/橄榄黄风格
 * - 在世界中随群系变化，物品栏使用默认金黄绿色
 */
object RubberLeavesColorProvider {

    /** 物品栏/手持时的默认颜色 (金黄绿色，比白桦树叶更黄) */
    private const val DEFAULT_RUBBER_LEAVES_COLOR = 0xc4b848

    fun register() {
        val block = Registries.BLOCK.get(Identifier(Ic2_120.MOD_ID, "rubber_leaves"))
        val item = Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "rubber_leaves"))

        ColorProviderRegistry.BLOCK.register({ _, world, pos, _ ->
            if (world != null && pos != null) {
                // 获取群系的白桦树风格 foliage 亮度
                val baseBrightness = BiomeColors.getFoliageColor(world, pos)
                // 将橡胶树叶的基础黄色 (0xc4b848) 根据群系亮度进行调整
                applyBiomeBrightness(DEFAULT_RUBBER_LEAVES_COLOR, baseBrightness)
            } else {
                DEFAULT_RUBBER_LEAVES_COLOR
            }
        }, block)

        ColorProviderRegistry.ITEM.register({ _, _ ->
            DEFAULT_RUBBER_LEAVES_COLOR
        }, item)
    }

    /**
     * 根据目标亮度调整颜色的亮度，保持色调不变
     * @param baseColor 基础颜色 (橡胶树叶的黄色调)
     * @param targetBrightness 目标亮度 (来自群系的 foliage 颜色)
     * @return 调整后的颜色
     */
    private fun applyBiomeBrightness(baseColor: Int, targetBrightness: Int): Int {
        val targetRGB = targetBrightness and 0xFFFFFF
        val currentRGB = baseColor and 0xFFFFFF

        // 计算亮度的比率
        val currentBrightness = getLuminance(currentRGB)
        val targetLuminance = getLuminance(targetRGB)

        return when {
            currentBrightness == 0 -> baseColor // 避免除以 0
            else -> {
                // 按比例调整 RGB 分量
                val r = ((currentRGB ushr 16) * targetLuminance / currentBrightness).coerceAtMost(255)
                val g = ((currentRGB ushr 8 and 0xFF) * targetLuminance / currentBrightness).coerceAtMost(255)
                val b = ((currentRGB and 0xFF) * targetLuminance / currentBrightness).coerceAtMost(255)
                (r shl 16) or (g shl 8) or b
            }
        }
    }

    /** 计算 RGB 颜色的亮度 ( luminance) */
    private fun getLuminance(rgb: Int): Int {
        val r = (rgb ushr 16) and 0xFF
        val g = (rgb ushr 8) and 0xFF
        val b = rgb and 0xFF
        // 使用加权平均计算感知亮度
        return (r * 299 + g * 587 + b * 114) / 1000
    }
}
