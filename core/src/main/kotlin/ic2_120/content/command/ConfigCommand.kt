package ic2_120.content.command

import com.mojang.brigadier.Command
import ic2_120.config.Ic2Config
import ic2_120.content.network.ConfigSyncHelper
import ic2_120.content.network.ConfigSyncPacket
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
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
                                    val configJson = Ic2Config.prettyCurrentConfig()
                                    val replicationCostsJson = Ic2Config.prettyAllReplicationCosts()
                                    for (player in source.server.playerManager.playerList) {
                                        ConfigSyncHelper.sendToPlayer(player, ConfigSyncPacket.ID, configJson)
                                        ConfigSyncHelper.sendToPlayer(
                                            player,
                                            ConfigSyncPacket.REPLICATION_COSTS_ID,
                                            replicationCostsJson
                                        )
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
