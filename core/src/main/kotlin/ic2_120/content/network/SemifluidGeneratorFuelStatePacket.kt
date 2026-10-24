package ic2_120.content.network

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.BlockPos

class SemifluidGeneratorFuelStatePacket(
    val pos: BlockPos,
    val fuelColorArgb: Int
) {
    companion object {
        val ID: ResourceLocation = ResourceLocation.of("ic2_120", "semifluid_generator_fuel_state")

        fun read(buf: FriendlyByteBuf): SemifluidGeneratorFuelStatePacket {
            val pos = buf.readBlockPos()
            val color = buf.readVarInt()
            return SemifluidGeneratorFuelStatePacket(pos, color)
        }

        fun write(packet: SemifluidGeneratorFuelStatePacket, buf: FriendlyByteBuf) {
            buf.writeBlockPos(packet.pos)
            buf.writeVarInt(packet.fuelColorArgb)
        }
    }
}
