package ic2_120_advanced_solar_addon

import ic2_120_advanced_solar_addon.config.Ic2AdvancedSolarAddonConfig
import ic2_120_advanced_solar_addon.content.command.MolecularTransformerCommand
import ic2_120_advanced_solar_addon.content.recipe.AddonConfigSyncPacket
import ic2_120_advanced_solar_addon.content.recipe.MTRecipes
import ic2_120.registry.ClassScanner
import io.netty.buffer.Unpooled
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory

object IC2AdvancedSolarAddon : ModInitializer {
    const val MOD_ID = "ic2_120_advanced_solar_addon"
    val LOGGER = LoggerFactory.getLogger(MOD_ID)

    fun id(path: String): Identifier = Identifier(MOD_ID, path)

    override fun onInitialize() {
        LOGGER.info("Initializing IC2 Advanced Solar Addon...")

        // 加载配置文件
        Ic2AdvancedSolarAddonConfig.loadOrThrow()

        // 使用本体 mod 的 ClassScanner 注册附属内容
        ClassScanner.scanAndRegister(
            MOD_ID,
            listOf(
                "ic2_120_advanced_solar_addon.content.tab",
                "ic2_120_advanced_solar_addon.content.block",
                "ic2_120_advanced_solar_addon.content.screen",
                "ic2_120_advanced_solar_addon.content.item"
            )
        )

        // 初始化分子重组仪配方（从配置加载）
        MTRecipes.init()

        // 注册命令
        MolecularTransformerCommand.register()

        // 玩家加入时发送完整配置同步（分包）
        ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
            val json = Ic2AdvancedSolarAddonConfig.prettyCurrentConfig()
            val bytes = json.toByteArray(Charsets.UTF_8)
            val totalChunks = (bytes.size + AddonConfigSyncPacket.MAX_CHUNK_BYTES - 1) / AddonConfigSyncPacket.MAX_CHUNK_BYTES
            var offset = 0
            for (index in 0 until totalChunks) {
                val size = minOf(AddonConfigSyncPacket.MAX_CHUNK_BYTES, bytes.size - offset)
                val chunk = bytes.copyOfRange(offset, offset + size)
                val buf = PacketByteBuf(Unpooled.buffer())
                AddonConfigSyncPacket.write(AddonConfigSyncPacket(totalChunks, index, chunk), buf)
                ServerPlayNetworking.send(handler.player, AddonConfigSyncPacket.ID, buf)
                offset += size
            }
        }

        LOGGER.info("IC2 Advanced Solar Addon initialized!")
    }
}
