package ic2_120.content.network

import ic2_120.Ic2_120
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier

/**
 * 配置/复制成本分块同步包。1.20.5+ fabric 移除了原始字节包 API，
 * 旧的两个通道 id（config_sync / replication_costs_sync）通过 [channelId] 字段复用同一个 payload type。
 */
class ConfigSyncChunkPayload(
    val channelId: Identifier,
    val totalChunks: Int,
    val chunkIndex: Int,
    val chunkData: ByteArray,
) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<ConfigSyncChunkPayload> = ID

    companion object {
        val ID = CustomPacketPayload.Type<ConfigSyncChunkPayload>(Ic2_120.id("config_sync_chunk"))
        val CODEC: StreamCodec<FriendlyByteBuf, ConfigSyncChunkPayload> = StreamCodec.of(
            { buf, p ->
                buf.writeIdentifier(p.channelId)
                buf.writeVarInt(p.totalChunks)
                buf.writeVarInt(p.chunkIndex)
                buf.writeByteArray(p.chunkData)
            },
            { buf ->
                ConfigSyncChunkPayload(buf.readIdentifier(), buf.readVarInt(), buf.readVarInt(), buf.readByteArray())
            },
        )
    }
}
