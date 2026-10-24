package ic2_120.content.network

import io.netty.buffer.ByteBuf
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload

/**
 * 扫描结果条目
 */
data class OreScanEntry(
    val blockId: String,
    val count: Int
)

/**
 * 扫描结果数据包（S2C）
 * 服务端扫描完成后，将结果编码到此包发给客户端。
 */
class ScannerResultPacket(
    val energy: Int,
    val energyCapacity: Int,
    val usesRemaining: Int,
    val maxUses: Int,
    val results: List<OreScanEntry>
) : CustomPayload {
    override fun getId(): CustomPayload.Id<*> = ID

    companion object {
        val ID = CustomPayload.Id<ScannerResultPacket>(net.minecraft.util.Identifier.of("ic2_120", "scanner_result"))
        val CODEC: PacketCodec<PacketByteBuf, ScannerResultPacket> = PacketCodec.of(
            { value, buf -> write(value, buf) },
            { read(it) }
        )

        fun read(buf: PacketByteBuf): ScannerResultPacket {
            val energy = buf.readInt()
            val energyCapacity = buf.readInt()
            val usesRemaining = buf.readVarInt()
            val maxUses = buf.readVarInt()
            val count = buf.readVarInt()
            val results = mutableListOf<OreScanEntry>()
            for (i in 0 until count) {
                val blockId = buf.readString()
                val oreCount = buf.readVarInt()
                results.add(OreScanEntry(blockId, oreCount))
            }
            return ScannerResultPacket(energy, energyCapacity, usesRemaining, maxUses, results)
        }

        fun write(packet: ScannerResultPacket, buf: PacketByteBuf) {
            buf.writeInt(packet.energy)
            buf.writeInt(packet.energyCapacity)
            buf.writeVarInt(packet.usesRemaining)
            buf.writeVarInt(packet.maxUses)
            buf.writeVarInt(packet.results.size)
            for (entry in packet.results) {
                buf.writeString(entry.blockId)
                buf.writeVarInt(entry.count)
            }
        }
    }
}
