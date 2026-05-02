package ic2_120.content.command

import com.mojang.brigadier.Command
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.registry.Registries
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.text.Text

object ItemIdCommand {
    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                literal("ic2itemid")
                    .executes { context ->
                        val player = context.source.playerOrThrow
                        val stack = player.mainHandStack
                        if (stack.isEmpty) {
                            context.source.sendError(Text.literal("主手没有物品"))
                            return@executes 0
                        }
                        val id = Registries.ITEM.getId(stack.item)
                        context.source.sendFeedback({ Text.literal(id.toString()) }, false)
                        Command.SINGLE_SUCCESS
                    }
            )
        }
    }
}
