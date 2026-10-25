package ic2_120.content.network

import ic2_120.config.Ic2Config
import ic2_120.integration.jei.LiveRecipeSource
import net.fabricmc.loader.api.FabricLoader

/** Receives the server's complete, runtime-generated UU replication table. */
object ReplicationCostsSyncReceiver {
    private val inner = ChunkedConfigReceiver { json ->
        Ic2Config.applyServerReplicationCosts(json)
        if (FabricLoader.getInstance().isModLoaded("jei")) {
            LiveRecipeSource.refreshJeiRecipes?.invoke()
        }
    }

    fun accept(packet: ConfigSyncPacket) {
        inner.accept(packet.totalChunks, packet.chunkIndex, packet.chunkData)
    }
}
