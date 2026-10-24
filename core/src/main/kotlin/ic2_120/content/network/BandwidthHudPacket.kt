package ic2_120.content.network

import ic2_120.Ic2_120
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

data class BandwidthPlayerStat(
    val name: String,
    val bytesPerSecond: Long,
    val totalBytes: Long
)

class BandwidthHudPacket(
    val serverBytesPerSecond: Long,
    val players: List<BandwidthPlayerStat>
) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<*> = ID

    companion object {
        val ID = CustomPacketPayload.Type<BandwidthHudPacket>(ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "bandwidth_hud"))
        val CODEC: StreamCodec<FriendlyByteBuf, BandwidthHudPacket> = StreamCodec.of(
            { value, buf -> write(value, buf) },
            { read(it) }
        )

        fun read(buf: FriendlyByteBuf): BandwidthHudPacket {
            val serverBytesPerSecond = buf.readVarLong()
            val size = buf.readVarInt()
            val players = ArrayList<BandwidthPlayerStat>(size)
            repeat(size) {
                players += BandwidthPlayerStat(
                    name = buf.readString(64),
                    bytesPerSecond = buf.readVarLong(),
                    totalBytes = buf.readVarLong()
                )
            }
            return BandwidthHudPacket(serverBytesPerSecond, players)
        }

        fun write(packet: BandwidthHudPacket, buf: FriendlyByteBuf) {
            buf.writeVarLong(packet.serverBytesPerSecond)
            buf.writeVarInt(packet.players.size)
            for (player in packet.players) {
                buf.writeString(player.name, 64)
                buf.writeVarLong(player.bytesPerSecond)
                buf.writeVarLong(player.totalBytes)
            }
        }
    }
}
