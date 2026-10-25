package ic2_120.client.colorprovider

import ic2_120.Ic2_120
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry
import net.minecraft.client.color.block.BlockTintSources
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

/** Tint shared pipe geometry by material; item tints are in item definitions. */
object PipeColorProvider {
    private val logger = LoggerFactory.getLogger("${Ic2_120.MOD_ID}/PipeColorProvider")
    private const val BRONZE_COLOR = 0x854200
    private const val CARBON_COLOR = 0x191919

    private val pipeIds = listOf(
        "bronze_pipe_tiny", "bronze_pipe_small", "bronze_pipe_medium", "bronze_pipe_large",
        "carbon_pipe_tiny", "carbon_pipe_small", "carbon_pipe_medium", "carbon_pipe_large",
        "bronze_pump_attachment", "carbon_pump_attachment",
    )

    fun register() {
        var count = 0
        for (name in pipeIds) {
            val block = BuiltInRegistries.BLOCK.getOptional(
                Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, name)
            ).orElse(null) ?: continue
            val rgb = if (name.startsWith("bronze_")) BRONZE_COLOR else CARBON_COLOR
            BlockColorRegistry.register(listOf(BlockTintSources.constant(rgb or -0x1000000)), block)
            count++
        }
        logger.info("已注册 {} 个管道方块着色器", count)
    }
}
