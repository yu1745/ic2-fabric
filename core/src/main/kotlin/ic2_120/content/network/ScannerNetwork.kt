package ic2_120.content.network

import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier

/**
 * 扫描结果条目
 */
data class OreScanEntry(
    val blockId: String,  // 带命名空间的方块ID，如 "minecraft:coal_ore"
    val count: Int
)

/**
 * 扫描结果数据包（S2C）
 * 服务端扫描完成后，将结果编码到此包发给客户端。
 */
class ScannerResultPacket(
    val energy: Int,          // 当前能量（UI 显示用）
    val energyCapacity: Int,  // 最大能量
    val usesRemaining: Int,   // 剩余使用次数
    val maxUses: Int,         // 最大使用次数
    val results: List<OreScanEntry>
) {
    companion object {
        val ID: Identifier = net.minecraft.util.Identifier("ic2_120", "scanner_result")

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
