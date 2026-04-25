package ic2_120.content.network

import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos

/**
 * 槽位热能与发电信息
 */
data class SlotHeatEnergyInfo(
    val heatProduced: Int,
    val heatDissipated: Int,
    val energyOutput: Float
)

class ReactorHeatInfoPacket(val pos: BlockPos, val slotHeatInfo: Map<Int, SlotHeatEnergyInfo>) : CustomPayload {
    override fun getId(): CustomPayload.Id<*> = ID

    companion object {
        val ID = CustomPayload.Id<ReactorHeatInfoPacket>(Identifier.of("ic2_120", "reactor_heat_info"))
        val CODEC: PacketCodec<PacketByteBuf, ReactorHeatInfoPacket> = PacketCodec.of(
            { value, buf -> write(value, buf) },
            { read(it) }
        )

        fun read(buf: PacketByteBuf): ReactorHeatInfoPacket {
            val pos = buf.readBlockPos()
            val size = buf.readVarInt()
            val slotHeatInfo = mutableMapOf<Int, SlotHeatEnergyInfo>()
            for (i in 0 until size) {
                val slot = buf.readVarInt()
                val produced = buf.readVarInt()
                val dissipated = buf.readVarInt()
                val output = buf.readFloat()
                slotHeatInfo[slot] = SlotHeatEnergyInfo(produced, dissipated, output)
            }
            return ReactorHeatInfoPacket(pos, slotHeatInfo)
        }

        fun write(packet: ReactorHeatInfoPacket, buf: PacketByteBuf) {
            buf.writeBlockPos(packet.pos)
            buf.writeVarInt(packet.slotHeatInfo.size)
            for ((slot, info) in packet.slotHeatInfo) {
                buf.writeVarInt(slot)
                buf.writeVarInt(info.heatProduced)
                buf.writeVarInt(info.heatDissipated)
                buf.writeFloat(info.energyOutput)
            }
        }
    }
}