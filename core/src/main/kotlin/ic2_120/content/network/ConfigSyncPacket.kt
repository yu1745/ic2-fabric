package ic2_120.content.network

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation

class ConfigSyncPacket(
    val totalChunks: Int,
    val chunkIndex: Int,
    val chunkData: ByteArray
) {
    companion object {
        val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath("ic2_120", "config_sync")
        val REPLICATION_COSTS_ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath("ic2_120", "replication_costs_sync")
        const val MAX_CHUNK_BYTES = 20000

        fun read(buf: FriendlyByteBuf): ConfigSyncPacket {
            val totalChunks = buf.readVarInt()
            val chunkIndex = buf.readVarInt()
            val chunkData = buf.readByteArray()
            return ConfigSyncPacket(totalChunks, chunkIndex, chunkData)
        }

        fun write(packet: ConfigSyncPacket, buf: FriendlyByteBuf) {
            buf.writeVarInt(packet.totalChunks)
            buf.writeVarInt(packet.chunkIndex)
            buf.writeByteArray(packet.chunkData)
        }
    }
}
