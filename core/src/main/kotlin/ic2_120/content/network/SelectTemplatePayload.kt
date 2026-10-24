package ic2_120.content.network

import ic2_120.Ic2_120
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.core.BlockPos

class SelectTemplatePayload(val pos: BlockPos, val index: Int) : CustomPacketPayload {
    override fun getId(): CustomPacketPayload.Id<*> = ID

    companion object {
        val ID = CustomPacketPayload.Id<SelectTemplatePayload>(Ic2_120.id("select_template"))
        val CODEC: StreamCodec<FriendlyByteBuf, SelectTemplatePayload> = StreamCodec.of(
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
