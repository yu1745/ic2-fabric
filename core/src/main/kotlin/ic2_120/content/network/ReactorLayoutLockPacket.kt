package ic2_120.content.network

import net.minecraft.item.Item
import net.minecraft.network.PacketByteBuf
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos

class ReactorLayoutLockPacket(
    val pos: BlockPos,
    val lockedSlots: Map<Int, Item>
) {
    companion object {
        val ID: Identifier = Identifier("ic2_120", "reactor_layout_lock")

        fun read(buf: PacketByteBuf): ReactorLayoutLockPacket {
            val pos = buf.readBlockPos()
            val size = buf.readVarInt()
            val lockedSlots = mutableMapOf<Int, Item>()
            for (i in 0 until size) {
                val slot = buf.readVarInt()
                val item = Registries.ITEM.get(buf.readIdentifier())
                lockedSlots[slot] = item
            }
            return ReactorLayoutLockPacket(pos, lockedSlots)
        }

        fun write(packet: ReactorLayoutLockPacket, buf: PacketByteBuf) {
            buf.writeBlockPos(packet.pos)
            buf.writeVarInt(packet.lockedSlots.size)
            for ((slot, item) in packet.lockedSlots) {
                buf.writeVarInt(slot)
                buf.writeIdentifier(Registries.ITEM.getId(item))
            }
        }
    }
}
