package ic2_120.content.network

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import kotlin.math.min

object ConfigSyncHelper {
    fun sendToPlayer(player: ServerPlayer, channelId: ResourceLocation, json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        val totalChunks = (bytes.size + ConfigSyncPacket.MAX_CHUNK_BYTES - 1) / ConfigSyncPacket.MAX_CHUNK_BYTES
        var offset = 0
        for (index in 0 until totalChunks) {
            val size = min(ConfigSyncPacket.MAX_CHUNK_BYTES, bytes.size - offset)
            val chunk = bytes.copyOfRange(offset, offset + size)
            // TODO(1.21.1): fabric 1.20.5+ 移除了 send(player, id, FriendlyByteBuf) 原始字节包 API，
            // ConfigSyncPacket 尚未迁移为注册了 streamCodec 的 CustomPacketPayload（NetworkManager 迁移中）。
            // 暂以匿名 payload 占位保证编译通过，运行时同步链路需在 ConfigSyncPacket 完成 payload 化后接回
            //（客户端接收侧 ConfigSyncReceiver 同样待迁移）。
            val payload: CustomPacketPayload = object : CustomPacketPayload {
                override fun type(): CustomPacketPayload.Type<*> = CustomPacketPayload.Type<CustomPacketPayload>(channelId)
            }
            ServerPlayNetworking.send(player, payload)
            offset += size
        }
    }
}
