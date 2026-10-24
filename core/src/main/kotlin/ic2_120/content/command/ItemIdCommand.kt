package ic2_120.content.command

import com.mojang.brigadier.Command
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.commands.Commands.literal
import net.minecraft.network.chat.Component

object ItemIdCommand {
    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                literal("ic2itemid")
                    .executes { context ->
                        val player = context.source.getPlayerOrException()
                        val stack = player.mainHandItem
                        if (stack.isEmpty) {
                            context.source.sendFailure(Component.literal("主手没有物品"))
                            return@executes 0
                        }
                        val id = BuiltInRegistries.ITEM.getKey(stack.item)
                        context.source.sendSuccess({ Component.literal(id.toString()) }, false)
                        Command.SINGLE_SUCCESS
                    }
            )
        }
    }
}
