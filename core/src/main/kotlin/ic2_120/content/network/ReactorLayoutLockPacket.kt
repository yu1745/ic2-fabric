package ic2_120.content.network

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.world.item.Item
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.core.BlockPos

class ReactorLayoutLockPacket(
    val pos: BlockPos,
    val lockedSlots: Map<Int, Item>
) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<*> = ID

    companion object {
        val ID: CustomPacketPayload.Type<ReactorLayoutLockPacket> =
            CustomPacketPayload.Type(Identifier.fromNamespaceAndPath("ic2_120", "reactor_layout_lock"))

        val CODEC: StreamCodec<FriendlyByteBuf, ReactorLayoutLockPacket> = StreamCodec.of(
            { buf, packet -> write(packet, buf) },
            { buf -> read(buf) }
        )

        fun read(buf: FriendlyByteBuf): ReactorLayoutLockPacket {
            val pos = buf.readBlockPos()
            val size = buf.readVarInt()
            val lockedSlots = mutableMapOf<Int, Item>()
            for (i in 0 until size) {
                val slot = buf.readVarInt()
                val item = BuiltInRegistries.ITEM.getValue(buf.readIdentifier())
                lockedSlots[slot] = item
            }
            return ReactorLayoutLockPacket(pos, lockedSlots)
        }

        fun write(packet: ReactorLayoutLockPacket, buf: FriendlyByteBuf) {
            buf.writeBlockPos(packet.pos)
            buf.writeVarInt(packet.lockedSlots.size)
            for ((slot, item) in packet.lockedSlots) {
                buf.writeVarInt(slot)
                buf.writeIdentifier(BuiltInRegistries.ITEM.getKey(item))
            }
        }
    }
}
