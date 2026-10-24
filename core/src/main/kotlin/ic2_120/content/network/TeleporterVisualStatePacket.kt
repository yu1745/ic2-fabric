package ic2_120.content.network

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.core.BlockPos

class TeleporterVisualStatePacket(
    val pos: BlockPos,
    val charging: Boolean,
    val chargeProgress: Int,
    val chargeMax: Int,
    val teleportRange: Int,
    val chargingEntityId: Int
) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<*> = ID

    companion object {
        val ID = CustomPacketPayload.Type<TeleporterVisualStatePacket>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ic2_120", "teleporter_visual_state"))
        val CODEC: StreamCodec<FriendlyByteBuf, TeleporterVisualStatePacket> = StreamCodec.of(
            { buf, value -> write(value, buf) },
            { read(it) }
        )

        fun read(buf: FriendlyByteBuf): TeleporterVisualStatePacket {
            val pos = buf.readBlockPos()
            val charging = buf.readBoolean()
            val progress = buf.readVarInt()
            val max = buf.readVarInt()
            val range = buf.readVarInt()
            val entityId = buf.readVarInt()
            return TeleporterVisualStatePacket(pos, charging, progress, max, range, entityId)
        }

        fun write(packet: TeleporterVisualStatePacket, buf: FriendlyByteBuf) {
            buf.writeBlockPos(packet.pos)
            buf.writeBoolean(packet.charging)
            buf.writeVarInt(packet.chargeProgress)
            buf.writeVarInt(packet.chargeMax)
            buf.writeVarInt(packet.teleportRange)
            buf.writeVarInt(packet.chargingEntityId)
        }
    }
}
