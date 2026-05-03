package ic2_120_advanced_solar_addon.content.recipe

import ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier

class AddonConfigSyncPacket(
    val totalChunks: Int,
    val chunkIndex: Int,
    val chunkData: ByteArray
) : CustomPayload {
    override fun getId(): CustomPayload.Id<*> = ID

    companion object {
        val ID = CustomPayload.Id<AddonConfigSyncPacket>(Identifier.of(IC2AdvancedSolarAddon.MOD_ID, "config_sync"))
        const val MAX_CHUNK_BYTES = 20000

        val CODEC: PacketCodec<PacketByteBuf, AddonConfigSyncPacket> = PacketCodec.of(
            { value, buf -> write(value, buf) },
            { read(it) }
        )

        fun read(buf: PacketByteBuf): AddonConfigSyncPacket {
            val totalChunks = buf.readVarInt()
            val chunkIndex = buf.readVarInt()
            val chunkData = buf.readByteArray()
            return AddonConfigSyncPacket(totalChunks, chunkIndex, chunkData)
        }

        fun write(packet: AddonConfigSyncPacket, buf: PacketByteBuf) {
            buf.writeVarInt(packet.totalChunks)
            buf.writeVarInt(packet.chunkIndex)
            buf.writeByteArray(packet.chunkData)
        }
    }
}
