package ic2_120.content.network

import ic2_120.Ic2_120
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos

class SemifluidGeneratorFuelStatePacket(
    val pos: BlockPos,
    val fuelColorArgb: Int
) : CustomPayload {
    override fun getId(): CustomPayload.Id<*> = ID

    companion object {
        val ID = CustomPayload.Id<SemifluidGeneratorFuelStatePacket>(Identifier.of(Ic2_120.MOD_ID, "semifluid_generator_fuel_state"))
        val CODEC: PacketCodec<PacketByteBuf, SemifluidGeneratorFuelStatePacket> = PacketCodec.of(
            { value, buf -> write(value, buf) },
            { read(it) }
        )

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
