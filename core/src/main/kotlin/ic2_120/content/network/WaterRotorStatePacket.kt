package ic2_120.content.network

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.core.BlockPos

class WaterRotorStatePacket(
    val pos: BlockPos,
    val isStuck: Boolean,
    val stuckAngle: Float
) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<*> = ID

    companion object {
        val ID = CustomPacketPayload.Type<WaterRotorStatePacket>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ic2_120", "water_rotor_state"))
        val CODEC: StreamCodec<FriendlyByteBuf, WaterRotorStatePacket> = StreamCodec.of(
            { buf, value -> write(value, buf) },
            { read(it) }
        )

        fun read(buf: FriendlyByteBuf): WaterRotorStatePacket {
            val pos = buf.readBlockPos()
            val isStuck = buf.readBoolean()
            val stuckAngle = buf.readFloat()
            return WaterRotorStatePacket(pos, isStuck, stuckAngle)
        }

        fun write(packet: WaterRotorStatePacket, buf: FriendlyByteBuf) {
            buf.writeBlockPos(packet.pos)
            buf.writeBoolean(packet.isStuck)
            buf.writeFloat(packet.stuckAngle)
        }
    }
}