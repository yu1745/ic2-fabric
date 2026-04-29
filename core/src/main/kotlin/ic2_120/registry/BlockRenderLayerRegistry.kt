package ic2_120.registry

import net.minecraft.util.Identifier

/**
 * 记录方块 ID 到渲染层类型的映射。
 * 由通用注册阶段填充，客户端初始化阶段读取并注册到对应 RenderLayer。
 *
 * renderLayer 类型：
 *  - "cutout"        → RenderLayer.getCutout()，适合完全透明/镂空纹理
 *  - "cutout_mipped" → RenderLayer.getCutoutMipped()，适合带 mipmap 的玻璃类纹理
 *  - "translucent"   → RenderLayer.getTranslucent()，适合需要半透明混合的方块（冰、染色玻璃容器）
 *
 * 空字符串表示不注册（使用默认 solid 层）。
 */
object BlockRenderLayerRegistry {
    private val blockRenderLayers = linkedMapOf<Identifier, String>()

    fun clear() {
        blockRenderLayers.clear()
    }

    fun put(id: Identifier, renderLayer: String) {
        if (renderLayer.isNotEmpty()) {
            blockRenderLayers[id] = renderLayer
        }
    }

    fun entries(): Set<Map.Entry<Identifier, String>> = blockRenderLayers.entries
}
