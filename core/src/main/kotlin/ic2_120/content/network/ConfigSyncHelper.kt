package ic2_120.content.network

import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import kotlin.math.min

object ConfigSyncHelper {
    fun sendToPlayer(player: ServerPlayerEntity, channelId: Identifier, json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        val totalChunks = (bytes.size + ConfigSyncPacket.MAX_CHUNK_BYTES - 1) / ConfigSyncPacket.MAX_CHUNK_BYTES
        var offset = 0
        for (index in 0 until totalChunks) {
            val size = min(ConfigSyncPacket.MAX_CHUNK_BYTES, bytes.size - offset)
            val chunk = bytes.copyOfRange(offset, offset + size)
            val buf = PacketByteBuf(Unpooled.buffer())
            ConfigSyncPacket.write(ConfigSyncPacket(totalChunks, index, chunk), buf)
            ServerPlayNetworking.send(player, channelId, buf)
            offset += size
        }
    }
}
