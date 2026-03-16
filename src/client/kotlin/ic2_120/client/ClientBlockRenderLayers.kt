package ic2_120.client

import ic2_120.Ic2_120
import ic2_120.registry.TransparentBlockRegistry
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
        registerTransparent(TransparentBlockRegistry.ids())
    }

    private fun registerTransparent(blockIds: Iterable<Identifier>) {
        val blocks = mutableListOf<Block>()
        for (id in blockIds) {
            val block = Registries.BLOCK.getOrEmpty(id)
            if (block.isPresent) {
                blocks.add(block.get())
            } else {
                logger.warn("跳过渲染层注册：未找到方块 {}", id)
            }
        }

        if (blocks.isNotEmpty()) {
            // translucent 层会关闭背面剔除，透明材质可看到方块背向面的几何
            BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getTranslucent(), *blocks.toTypedArray())
            logger.info("已注册 {} 个透明方块到 translucent 渲染层", blocks.size)
        }
    }
}
