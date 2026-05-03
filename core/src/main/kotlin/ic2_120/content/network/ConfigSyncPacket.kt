package ic2_120.content.network

import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier

class ConfigSyncPacket(
    val totalChunks: Int,
    val chunkIndex: Int,
    val chunkData: ByteArray
) {
    companion object {
        val ID: Identifier = Identifier("ic2_120", "config_sync")
        const val MAX_CHUNK_BYTES = 20000

        fun read(buf: PacketByteBuf): ConfigSyncPacket {
            val totalChunks = buf.readVarInt()
            val chunkIndex = buf.readVarInt()
            val chunkData = buf.readByteArray()
            return ConfigSyncPacket(totalChunks, chunkIndex, chunkData)
        }

        fun write(packet: ConfigSyncPacket, buf: PacketByteBuf) {
            buf.writeVarInt(packet.totalChunks)
            buf.writeVarInt(packet.chunkIndex)
            buf.writeByteArray(packet.chunkData)
        }
    }
}
