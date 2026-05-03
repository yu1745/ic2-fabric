package ic2_120.content.network

import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier

class ConfigSyncPacket(
    val totalChunks: Int,
    val chunkIndex: Int,
    val chunkData: ByteArray
) : CustomPayload {
    override fun getId(): CustomPayload.Id<*> = ID

    companion object {
        val ID = CustomPayload.Id<ConfigSyncPacket>(Identifier.of("ic2_120", "config_sync"))
        const val MAX_CHUNK_BYTES = 20000

        val CODEC: PacketCodec<PacketByteBuf, ConfigSyncPacket> = PacketCodec.of(
            { value, buf -> write(value, buf) },
            { read(it) }
        )

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
