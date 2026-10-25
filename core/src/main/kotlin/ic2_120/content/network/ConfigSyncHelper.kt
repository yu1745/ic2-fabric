package ic2_120.content.network

import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import kotlin.math.min

object ConfigSyncHelper {
    fun sendToPlayer(player: ServerPlayer, channelId: Identifier, json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        val totalChunks = (bytes.size + ConfigSyncPacket.MAX_CHUNK_BYTES - 1) / ConfigSyncPacket.MAX_CHUNK_BYTES
        var offset = 0
        for (index in 0 until totalChunks) {
            val size = min(ConfigSyncPacket.MAX_CHUNK_BYTES, bytes.size - offset)
            val chunk = bytes.copyOfRange(offset, offset + size)
            ServerPlayNetworking.send(
                player,
                ConfigSyncChunkPayload(channelId, totalChunks, index, chunk)
            )
            offset += size
        }
    }
}
