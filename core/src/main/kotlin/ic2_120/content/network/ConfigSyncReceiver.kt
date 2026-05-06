package ic2_120.content.network

import ic2_120.config.Ic2Config
import ic2_120.integration.jei.Ic2JeiPlugin
import net.fabricmc.loader.api.FabricLoader

object ConfigSyncReceiver {
    private val inner = ChunkedConfigReceiver { json ->
        Ic2Config.applyServerConfig(json)
        if (FabricLoader.getInstance().isModLoaded("jei")) {
            Ic2JeiPlugin.refreshReplicatorRecipes()
        }
    }

    fun accept(packet: ConfigSyncPacket) {
        inner.accept(packet.totalChunks, packet.chunkIndex, packet.chunkData)
    }
}
