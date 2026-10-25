package ic2_120.content.network

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import net.minecraft.core.BlockPos

class SemifluidGeneratorFuelStatePacket(
    val pos: BlockPos,
    val fuelColorArgb: Int
) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<*> = ID

    companion object {
        val ID: CustomPacketPayload.Type<SemifluidGeneratorFuelStatePacket> =
            CustomPacketPayload.Type(Identifier.fromNamespaceAndPath("ic2_120", "semifluid_generator_fuel_state"))

        val CODEC: StreamCodec<FriendlyByteBuf, SemifluidGeneratorFuelStatePacket> = StreamCodec.of(
            { buf, packet -> write(packet, buf) },
            { buf -> read(buf) }
        )

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
