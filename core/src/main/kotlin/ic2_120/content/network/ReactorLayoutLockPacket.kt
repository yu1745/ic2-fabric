package ic2_120.content.network

import net.minecraft.world.item.Item
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.BlockPos

class ReactorLayoutLockPacket(
    val pos: BlockPos,
    val lockedSlots: Map<Int, Item>
) {
    companion object {
        val ID: ResourceLocation = ResourceLocation.of("ic2_120", "reactor_layout_lock")

        fun read(buf: FriendlyByteBuf): ReactorLayoutLockPacket {
            val pos = buf.readBlockPos()
            val size = buf.readVarInt()
            val lockedSlots = mutableMapOf<Int, Item>()
            for (i in 0 until size) {
                val slot = buf.readVarInt()
                val item = BuiltInRegistries.ITEM.get(buf.readIdentifier())
                lockedSlots[slot] = item
            }
            return ReactorLayoutLockPacket(pos, lockedSlots)
        }

        fun write(packet: ReactorLayoutLockPacket, buf: FriendlyByteBuf) {
            buf.writeBlockPos(packet.pos)
            buf.writeVarInt(packet.lockedSlots.size)
            for ((slot, item) in packet.lockedSlots) {
                buf.writeVarInt(slot)
                buf.writeIdentifier(BuiltInRegistries.ITEM.getId(item))
            }
        }
    }
}
