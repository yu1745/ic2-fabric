package ic2_120.content.command

import com.mojang.brigadier.Command
import ic2_120.config.Ic2Config
import ic2_120.content.network.ConfigSyncPacket
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.text.Text

object ConfigCommand {
    fun register() {
        CommandRegistrationCallback.EVENT.register(CommandRegistrationCallback { dispatcher, _, _ ->
            dispatcher.register(
                literal("ic2config")
                    .requires { source -> source.hasPermissionLevel(2) }
                    .then(
                        literal("reload")
                            .executes { context ->
                                val source = context.source
                                try {
                                    Ic2Config.reloadOrThrow()
                                    val json = Ic2Config.prettyCurrentConfig()
                                    val bytes = json.toByteArray(Charsets.UTF_8)
                                    val totalChunks = (bytes.size + ConfigSyncPacket.MAX_CHUNK_BYTES - 1) / ConfigSyncPacket.MAX_CHUNK_BYTES
                                    var offset = 0
                                    for (index in 0 until totalChunks) {
                                        val size = minOf(ConfigSyncPacket.MAX_CHUNK_BYTES, bytes.size - offset)
                                        val chunk = bytes.copyOfRange(offset, offset + size)
                                        for (player in source.server.playerManager.playerList) {
                                            val buf = PacketByteBuf(Unpooled.buffer())
                                            ConfigSyncPacket.write(ConfigSyncPacket(totalChunks, index, chunk), buf)
                                            ServerPlayNetworking.send(player, ConfigSyncPacket.ID, buf)
                                        }
                                        offset += size
                                    }
                                    source.sendFeedback(
                                        {
                                            Text.literal("ic2_120 config reloaded and synced to all players")
                                        },
                                        true
                                    )
                                    Command.SINGLE_SUCCESS
                                } catch (e: Exception) {
                                    source.sendError(Text.literal("Failed to reload ic2_120 config: ${e.message}"))
                                    0
                                }
                            }
                    )
            )
        })
    }
}
