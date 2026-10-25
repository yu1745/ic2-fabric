package ic2_120.content.network

import io.netty.buffer.ByteBuf
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

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
) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<*> = ID

    companion object {
        val ID = CustomPacketPayload.Type<ScannerResultPacket>(net.minecraft.resources.Identifier.fromNamespaceAndPath("ic2_120", "scanner_result"))
        val CODEC: StreamCodec<FriendlyByteBuf, ScannerResultPacket> = StreamCodec.of(
            { buf, value -> write(value, buf) },
            { read(it) }
        )

        fun read(buf: FriendlyByteBuf): ScannerResultPacket {
            val energy = buf.readInt()
            val energyCapacity = buf.readInt()
            val usesRemaining = buf.readVarInt()
            val maxUses = buf.readVarInt()
            val count = buf.readVarInt()
            val results = mutableListOf<OreScanEntry>()
            for (i in 0 until count) {
                val blockId = buf.readUtf()
                val oreCount = buf.readVarInt()
                results.add(OreScanEntry(blockId, oreCount))
            }
            return ScannerResultPacket(energy, energyCapacity, usesRemaining, maxUses, results)
        }

        fun write(packet: ScannerResultPacket, buf: FriendlyByteBuf) {
            buf.writeInt(packet.energy)
            buf.writeInt(packet.energyCapacity)
            buf.writeVarInt(packet.usesRemaining)
            buf.writeVarInt(packet.maxUses)
            buf.writeVarInt(packet.results.size)
            for (entry in packet.results) {
                buf.writeUtf(entry.blockId)
                buf.writeVarInt(entry.count)
            }
        }
    }
}
