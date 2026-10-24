package ic2_120.content.network

import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.math.BlockPos

/**
 * 风力发电机转子状态同步包
 */
class WindRotorStatePacket(
    val pos: BlockPos,
    val isStuck: Boolean,
    val stuckAngle: Float
) : CustomPayload {
    override fun getId(): CustomPayload.Id<*> = ID

    companion object {
        val ID = CustomPayload.Id<WindRotorStatePacket>(net.minecraft.util.Identifier.of("ic2_120", "wind_rotor_state"))
        val CODEC: PacketCodec<PacketByteBuf, WindRotorStatePacket> = PacketCodec.of(
            { value, buf -> write(value, buf) },
            { read(it) }
        )

        fun read(buf: PacketByteBuf): WindRotorStatePacket {
            val pos = buf.readBlockPos()
            val isStuck = buf.readBoolean()
            val stuckAngle = buf.readFloat()
            return WindRotorStatePacket(pos, isStuck, stuckAngle)
        }

        fun write(packet: WindRotorStatePacket, buf: PacketByteBuf) {
            buf.writeBlockPos(packet.pos)
            buf.writeBoolean(packet.isStuck)
            buf.writeFloat(packet.stuckAngle)
        }
    }
}
