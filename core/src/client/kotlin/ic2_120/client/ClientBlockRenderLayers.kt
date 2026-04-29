package ic2_120.client

import ic2_120.Ic2_120
import ic2_120.registry.BlockRenderLayerRegistry
import ic2_120.registry.type
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.minecraft.block.Block
import net.minecraft.client.render.RenderLayer
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory

object ClientBlockRenderLayers {
    private val logger = LoggerFactory.getLogger("${Ic2_120.MOD_ID}/ClientBlockRenderLayers")

    fun register() {
        registerLayers(BlockRenderLayerRegistry.entries())
    }

    private fun registerLayers(entries: Set<Map.Entry<Identifier, String>>) {
        val cutout = mutableListOf<Block>()
        val cutoutMipped = mutableListOf<Block>()
        val translucent = mutableListOf<Block>()

        for ((id, layerType) in entries) {
            val block = Registries.BLOCK.getOrEmpty(id)
            if (block.isEmpty) {
                logger.warn("跳过渲染层注册：未找到方块 {}", id)
                continue
            }
            when (layerType) {
                "cutout" -> cutout.add(block.get())
                "cutout_mipped" -> cutoutMipped.add(block.get())
                "translucent" -> translucent.add(block.get())
                else -> logger.warn("未知的渲染层类型 '{}' for 方块 {}", layerType, id)
            }
        }

        if (cutout.isNotEmpty()) {
            BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), *cutout.toTypedArray())
            logger.info("已注册 {} 个方块到 cutout 渲染层", cutout.size)
        }
        if (cutoutMipped.isNotEmpty()) {
            BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutoutMipped(), *cutoutMipped.toTypedArray())
            logger.info("已注册 {} 个方块到 cutout_mipped 渲染层", cutoutMipped.size)
        }
        if (translucent.isNotEmpty()) {
            BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getTranslucent(), *translucent.toTypedArray())
            logger.info("已注册 {} 个方块到 translucent 渲染层", translucent.size)
        }
    }
}
