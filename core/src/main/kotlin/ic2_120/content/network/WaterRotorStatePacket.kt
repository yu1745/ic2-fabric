package ic2_120.content.network

import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.math.BlockPos

class WaterRotorStatePacket(
    val pos: BlockPos,
    val isStuck: Boolean,
    val stuckAngle: Float
) : CustomPayload {
    override fun getId(): CustomPayload.Id<*> = ID

    companion object {
        val ID = CustomPayload.Id<WaterRotorStatePacket>(net.minecraft.util.Identifier.of("ic2_120", "water_rotor_state"))
        val CODEC: PacketCodec<PacketByteBuf, WaterRotorStatePacket> = PacketCodec.of(
            { value, buf -> write(value, buf) },
            { read(it) }
        )

        fun read(buf: PacketByteBuf): WaterRotorStatePacket {
            val pos = buf.readBlockPos()
            val isStuck = buf.readBoolean()
            val stuckAngle = buf.readFloat()
            return WaterRotorStatePacket(pos, isStuck, stuckAngle)
        }

        fun write(packet: WaterRotorStatePacket, buf: PacketByteBuf) {
            buf.writeBlockPos(packet.pos)
            buf.writeBoolean(packet.isStuck)
            buf.writeFloat(packet.stuckAngle)
        }
    }
}