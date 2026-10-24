package ic2_120.client.colorprovider

import ic2_120.Ic2_120
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry
import net.minecraft.client.color.block.BlockColor
import net.minecraft.client.color.item.ItemColor
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import org.slf4j.LoggerFactory

/**
 * 管道着色器
 *
 * 为不同材质的管道提供不同的颜色：
 * - 青铜：青铜色 (RGB 133, 66, 0)
 * - 碳纤维：深灰色 (RGB 25, 25, 25)
 *
 * 支持方块着色（放置在世界中的方块）和物品着色（手持、物品栏、掉落物）
 */
object PipeColorProvider {
    private val logger = LoggerFactory.getLogger("${Ic2_120.MOD_ID}/PipeColorProvider")

    /**
     * 青铜管道颜色 - RGB 133, 66, 0
     */
    private const val BRONZE_COLOR = 0x854200

    /**
     * 碳纤维管道颜色 - RGB 25, 25, 25
     */
    private const val CARBON_COLOR = 0x191919

    fun register() {
        val blockProvider = BlockColor { state, world, pos, tintIndex ->
            if (tintIndex != 0) return@BlockColor 0xFFFFFF
            val blockId = BuiltInRegistries.BLOCK.getKey(state.block)
            when {
                blockId.path.startsWith("bronze_pipe") || blockId.path == "bronze_pump_attachment" -> BRONZE_COLOR
                blockId.path.startsWith("carbon_pipe") || blockId.path == "carbon_pump_attachment" -> CARBON_COLOR
                else -> 0xFFFFFF
            }
        }

        val itemProvider = ItemColor { stack, tintIndex ->
            if (tintIndex != 0) return@ItemColor 0xFFFFFF
            val item = stack.item
            val itemId = BuiltInRegistries.ITEM.getKey(item)
            when {
                itemId.path.startsWith("bronze_pipe") || itemId.path == "bronze_pump_attachment" -> BRONZE_COLOR
                itemId.path.startsWith("carbon_pipe") || itemId.path == "carbon_pump_attachment" -> CARBON_COLOR
                else -> 0xFFFFFF
            }
        }

        // 所有管道方块 ID
        val pipeIds = listOf(
            "bronze_pipe_tiny",
            "bronze_pipe_small",
            "bronze_pipe_medium",
            "bronze_pipe_large",
            "carbon_pipe_tiny",
            "carbon_pipe_small",
            "carbon_pipe_medium",
            "carbon_pipe_large",
            "bronze_pump_attachment",
            "carbon_pump_attachment"
        )

        var registeredBlockCount = 0
        var registeredItemCount = 0
        for (pipeId in pipeIds) {
            val id = ResourceLocation.fromNamespaceAndPath("ic2_120", pipeId)

            val block = BuiltInRegistries.BLOCK.getOptional(id)
            if (block.isPresent) {
                ColorProviderRegistry.BLOCK.register(blockProvider, block.get())
                registeredBlockCount++
            }

            val item = BuiltInRegistries.ITEM.getOptional(id)
            if (item.isPresent) {
                ColorProviderRegistry.ITEM.register(itemProvider, item.get())
                registeredItemCount++
            }
        }

        logger.info("已注册 {} 个管道方块着色器和 {} 个物品着色器", registeredBlockCount, registeredItemCount)
    }
}
