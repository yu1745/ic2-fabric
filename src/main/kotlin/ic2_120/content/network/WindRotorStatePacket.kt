package ic2_120.content.network


import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos

/**
 * 风力发电机转子状态同步包
 * @param pos 方块位置
 * @param isStuck 转子是否被卡住
 * @param stuckAngle 卡住时的角度（0-359）
 */
class WindRotorStatePacket(
    val pos: BlockPos,
    val isStuck: Boolean,
    val stuckAngle: Float
) {
    companion object {
        val ID = Identifier.of("ic2_120", "wind_rotor_state")

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
