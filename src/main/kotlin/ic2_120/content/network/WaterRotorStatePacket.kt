package ic2_120.content.network


import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos

class WaterRotorStatePacket(
    val pos: BlockPos,
    val isStuck: Boolean,
    val stuckAngle: Float
) {
    companion object {
        val ID = Identifier.of("ic2_120", "water_rotor_state")

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