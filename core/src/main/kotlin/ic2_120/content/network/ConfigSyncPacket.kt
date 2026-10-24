package ic2_120.content.network

import net.minecraft.network.PacketByteBuf
<<<<<<< HEAD
=======
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
>>>>>>> backup/1.21.1
import net.minecraft.util.Identifier

class ConfigSyncPacket(
    val totalChunks: Int,
    val chunkIndex: Int,
    val chunkData: ByteArray
<<<<<<< HEAD
) {
    companion object {
        val ID: Identifier = Identifier("ic2_120", "config_sync")
        val REPLICATION_COSTS_ID: Identifier = Identifier("ic2_120", "replication_costs_sync")
        const val MAX_CHUNK_BYTES = 20000

=======
) : CustomPayload {
    override fun getId(): CustomPayload.Id<*> = ID

    companion object {
        val ID = CustomPayload.Id<ConfigSyncPacket>(Identifier.of("ic2_120", "config_sync"))
        const val MAX_CHUNK_BYTES = 20000

        val CODEC: PacketCodec<PacketByteBuf, ConfigSyncPacket> = PacketCodec.of(
            { value, buf -> write(value, buf) },
            { read(it) }
        )

>>>>>>> backup/1.21.1
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
