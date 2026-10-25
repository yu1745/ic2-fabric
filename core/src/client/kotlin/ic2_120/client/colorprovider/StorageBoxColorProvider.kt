package ic2_120.client.colorprovider

import ic2_120.Ic2_120
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry
import net.minecraft.client.color.block.BlockTintSources
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

/** Color the shared storage-box block model by material. Item tints live in item definitions. */
object StorageBoxColorProvider {
    private val logger = LoggerFactory.getLogger("${Ic2_120.MOD_ID}/StorageBoxColorProvider")

    private val colors = mapOf(
        "wooden_storage_box" to 0xC6A669,
        "iron_storage_box" to 0xB4B4B4,
        "bronze_storage_box" to 0xCD7F32,
        "steel_storage_box" to 0xD0D0D0,
        "iridium_storage_box" to 0xFFFFFF,
    )

    fun register() {
        var count = 0
        for ((name, rgb) in colors) {
            val block = BuiltInRegistries.BLOCK.getOptional(
                Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, name)
            ).orElse(null) ?: continue
            BlockColorRegistry.register(listOf(BlockTintSources.constant(rgb or -0x1000000)), block)
            count++
        }
        logger.info("已注册 {} 个储物箱方块着色器", count)
    }
}
