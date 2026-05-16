package ic2_120.content.network

import ic2_120.Ic2_120
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.math.BlockPos

class SelectTemplatePayload(val pos: BlockPos, val index: Int) : CustomPayload {
    override fun getId(): CustomPayload.Id<*> = ID

    companion object {
        val ID = CustomPayload.Id<SelectTemplatePayload>(Ic2_120.id("select_template"))
        val CODEC: PacketCodec<PacketByteBuf, SelectTemplatePayload> = PacketCodec.of(
            { value, buf ->
                buf.writeBlockPos(value.pos)
                buf.writeVarInt(value.index)
            },
            { buf ->
                SelectTemplatePayload(buf.readBlockPos(), buf.readVarInt())
            }
        )
    }
}
