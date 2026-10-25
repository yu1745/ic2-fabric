package ic2_120.content.network

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.core.BlockPos

/**
 * 风力发电机转子状态同步包
 */
class WindRotorStatePacket(
    val pos: BlockPos,
    val isStuck: Boolean,
    val stuckAngle: Float
) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<*> = ID

    companion object {
        val ID = CustomPacketPayload.Type<WindRotorStatePacket>(net.minecraft.resources.Identifier.fromNamespaceAndPath("ic2_120", "wind_rotor_state"))
        val CODEC: StreamCodec<FriendlyByteBuf, WindRotorStatePacket> = StreamCodec.of(
            { buf, value -> write(value, buf) },
            { read(it) }
        )

        fun read(buf: FriendlyByteBuf): WindRotorStatePacket {
            val pos = buf.readBlockPos()
            val isStuck = buf.readBoolean()
            val stuckAngle = buf.readFloat()
            return WindRotorStatePacket(pos, isStuck, stuckAngle)
        }

        fun write(packet: WindRotorStatePacket, buf: FriendlyByteBuf) {
            buf.writeBlockPos(packet.pos)
            buf.writeBoolean(packet.isStuck)
            buf.writeFloat(packet.stuckAngle)
        }
    }
}
