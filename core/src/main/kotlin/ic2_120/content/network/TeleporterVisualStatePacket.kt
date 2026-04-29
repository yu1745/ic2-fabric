package ic2_120.content.network

import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos

class TeleporterVisualStatePacket(
    val pos: BlockPos,
    val charging: Boolean,
    val chargeProgress: Int,
    val chargeMax: Int,
    val teleportRange: Int,
    val chargingEntityId: Int
) {
    companion object {
        val ID: Identifier = Identifier("ic2_120", "teleporter_visual_state")

        fun read(buf: PacketByteBuf): TeleporterVisualStatePacket {
            val pos = buf.readBlockPos()
            val charging = buf.readBoolean()
            val progress = buf.readVarInt()
            val max = buf.readVarInt()
            val range = buf.readVarInt()
            val entityId = buf.readVarInt()
            return TeleporterVisualStatePacket(pos, charging, progress, max, range, entityId)
        }

        fun write(packet: TeleporterVisualStatePacket, buf: PacketByteBuf) {
            buf.writeBlockPos(packet.pos)
            buf.writeBoolean(packet.charging)
            buf.writeVarInt(packet.chargeProgress)
            buf.writeVarInt(packet.chargeMax)
            buf.writeVarInt(packet.teleportRange)
            buf.writeVarInt(packet.chargingEntityId)
        }
    }
}
