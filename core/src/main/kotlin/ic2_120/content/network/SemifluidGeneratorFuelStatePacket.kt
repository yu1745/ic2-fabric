package ic2_120.content.network

<<<<<<< HEAD
import net.minecraft.network.PacketByteBuf
=======
import ic2_120.Ic2_120
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
>>>>>>> backup/1.21.1
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos

class SemifluidGeneratorFuelStatePacket(
    val pos: BlockPos,
    val fuelColorArgb: Int
<<<<<<< HEAD
) {
    companion object {
        val ID: Identifier = Identifier("ic2_120", "semifluid_generator_fuel_state")
=======
) : CustomPayload {
    override fun getId(): CustomPayload.Id<*> = ID

    companion object {
        val ID = CustomPayload.Id<SemifluidGeneratorFuelStatePacket>(Identifier.of(Ic2_120.MOD_ID, "semifluid_generator_fuel_state"))
        val CODEC: PacketCodec<PacketByteBuf, SemifluidGeneratorFuelStatePacket> = PacketCodec.of(
            { value, buf -> write(value, buf) },
            { read(it) }
        )
>>>>>>> backup/1.21.1

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
