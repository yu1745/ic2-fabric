package ic2_120.client.colorprovider

import ic2_120.Ic2_120
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry
import net.minecraft.block.Block
import net.minecraft.client.color.block.BlockColorProvider
import net.minecraft.client.color.item.ItemColorProvider
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory

/**
 * 储物箱着色器
 *
 * 为不同材质的储物箱提供不同的颜色：
 * - 木质：木头的棕黄色
 * - 铁质：铁灰色
 * - 青铜：青铜色
 * - 钢制：浅钢灰色
 * - 铱：独立的材质（不着色）
 *
 * 支持方块着色（放置在世界中的方块）和物品着色（手持、物品栏、掉落物）
 */
object StorageBoxColorProvider {
    private val logger = LoggerFactory.getLogger("${Ic2_120.MOD_ID}/StorageBoxColorProvider")

    /**
     * 木质储物箱颜色 - 类似橡木的棕黄色
     */
    private const val WOODEN_COLOR = 0xC6A669

    /**
     * 铁质储物箱颜色 - 铁灰色
     */
    private const val IRON_COLOR = 0xB4B4B4

    /**
     * 青铜储物箱颜色 - 青铜色
     */
    private const val BRONZE_COLOR = 0xCD7F32

    /**
     * 钢制储物箱颜色 - 浅钢灰色
     */
    private const val STEEL_COLOR = 0xD0D0D0

    /**
     * 铱储物箱使用独立材质，返回 -1 表示不着色
     */
    private const val IRIDIUM_COLOR = -1

    fun register() {
        val blockProvider = BlockColorProvider { state, world, pos, tintIndex ->
            val blockId = Registries.BLOCK.getId(state.block)
            when (blockId.path) {
                "wooden_storage_box" -> WOODEN_COLOR
                "iron_storage_box" -> IRON_COLOR
                "bronze_storage_box" -> BRONZE_COLOR
                "steel_storage_box" -> STEEL_COLOR
                "iridium_storage_box" -> IRIDIUM_COLOR
                else -> 0xFFFFFF
            }
        }

        val itemProvider = ItemColorProvider { stack, tintIndex ->
            val item = stack.item
            val itemId = Registries.ITEM.getId(item)
            when (itemId.path) {
                "wooden_storage_box" -> WOODEN_COLOR
                "iron_storage_box" -> IRON_COLOR
                "bronze_storage_box" -> BRONZE_COLOR
                "steel_storage_box" -> STEEL_COLOR
                "iridium_storage_box" -> IRIDIUM_COLOR
                else -> 0xFFFFFF
            }
        }

        // 通过 ID 获取所有储物箱方块并注册着色器
        val storageBoxIds = listOf(
            "wooden_storage_box",
            "iron_storage_box",
            "bronze_storage_box",
            "steel_storage_box",
            "iridium_storage_box"
        )

        var registeredBlockCount = 0
        var registeredItemCount = 0
        for (blockId in storageBoxIds) {
            val id = Identifier.of("ic2_120", blockId)
            val block = Registries.BLOCK.getOrEmpty(id)
            if (block.isPresent) {
                ColorProviderRegistry.BLOCK.register(blockProvider, block.get())
                registeredBlockCount++
            }

            val item = Registries.ITEM.getOrEmpty(id)
            if (item.isPresent) {
                ColorProviderRegistry.ITEM.register(itemProvider, item.get())
                registeredItemCount++
            }
        }

        logger.info("已注册 {} 个储物箱方块着色器和 {} 个物品着色器", registeredBlockCount, registeredItemCount)
    }
}
