package ic2_120_advanced_solar_addon.content.recipe

import ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

class AddonConfigSyncPacket(
    val totalChunks: Int,
    val chunkIndex: Int,
    val chunkData: ByteArray
) : CustomPacketPayload {
    override fun getId(): CustomPacketPayload.Id<*> = ID

    companion object {
        val ID = CustomPacketPayload.Id<AddonConfigSyncPacket>(ResourceLocation.fromNamespaceAndPath(IC2AdvancedSolarAddon.MOD_ID, "config_sync"))
        const val MAX_CHUNK_BYTES = 20000

        val CODEC: StreamCodec<FriendlyByteBuf, AddonConfigSyncPacket> = StreamCodec.of(
            { value, buf -> write(value, buf) },
            { read(it) }
        )

        fun read(buf: FriendlyByteBuf): AddonConfigSyncPacket {
            val totalChunks = buf.readVarInt()
            val chunkIndex = buf.readVarInt()
            val chunkData = buf.readByteArray()
            return AddonConfigSyncPacket(totalChunks, chunkIndex, chunkData)
        }

        fun write(packet: AddonConfigSyncPacket, buf: FriendlyByteBuf) {
            buf.writeVarInt(packet.totalChunks)
            buf.writeVarInt(packet.chunkIndex)
            buf.writeByteArray(packet.chunkData)
        }
    }
}
