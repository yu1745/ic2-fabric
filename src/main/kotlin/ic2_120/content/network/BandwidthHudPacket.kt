package ic2_120.content.network

import ic2_120.Ic2_120
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier

data class BandwidthPlayerStat(
    val name: String,
    val bytesPerSecond: Long,
    val totalBytes: Long
)

data class BandwidthHudPacket(
    val serverBytesPerSecond: Long,
    val players: List<BandwidthPlayerStat>
) {
    companion object {
        val ID: Identifier = Identifier(Ic2_120.MOD_ID, "bandwidth_hud")

        fun read(buf: PacketByteBuf): BandwidthHudPacket {
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

        fun write(packet: BandwidthHudPacket, buf: PacketByteBuf) {
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
