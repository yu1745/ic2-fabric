package ic2_120.content.network

import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos

class SemifluidGeneratorFuelStatePacket(
    val pos: BlockPos,
    val fuelColorArgb: Int
) {
    companion object {
        val ID: Identifier = Identifier("ic2_120", "semifluid_generator_fuel_state")

        fun read(buf: PacketByteBuf): SemifluidGeneratorFuelStatePacket {
            val pos = buf.readBlockPos()
            val color = buf.readVarInt()
            return SemifluidGeneratorFuelStatePacket(pos, color)
        }

        fun write(packet: SemifluidGeneratorFuelStatePacket, buf: PacketByteBuf) {
            buf.writeBlockPos(packet.pos)
            buf.writeVarInt(packet.fuelColorArgb)
        }
    }
}
