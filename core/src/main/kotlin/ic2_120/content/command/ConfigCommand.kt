package ic2_120.content.command

import com.mojang.brigadier.Command
import ic2_120.config.Ic2Config
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
                                    source.sendFeedback(
                                        {
                                            Text.literal("ic2_120 config reloaded:\n${Ic2Config.prettyCurrentConfig()}")
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
