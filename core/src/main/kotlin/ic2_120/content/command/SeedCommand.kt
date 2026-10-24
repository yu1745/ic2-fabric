package ic2_120.content.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import ic2_120.content.crop.CropStats
import ic2_120.content.crop.CropType
import ic2_120.content.item.CropSeedBagItem
import ic2_120.content.item.CropSeedData
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.Commands
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component

object SeedCommand {
    private const val PERMISSION_LEVEL = 2

    private val seedTypeSuggestionProvider = SuggestionProvider<CommandSourceStack> { _, builder ->
        val remaining = builder.remaining.lowercase()
        for (type in CropType.entries) {
            if (type.asString().lowercase().startsWith(remaining)) {
                builder.suggest(type.asString())
            }
        }
        builder.buildFuture()
    }

    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                Commands.literal("ic2seed")
                    .requires { source ->
                        source.hasPermission(PERMISSION_LEVEL) ||
                        source.player?.isCreative() == true
                    }
                    .then(
                        Commands.literal("list")
                            .executes { ctx -> listExecute(ctx) }
                    )
                    .then(
                        Commands.argument("seed_type", StringArgumentType.string())
                            .suggests(seedTypeSuggestionProvider)
                            .then(
                                Commands.argument("growth", IntegerArgumentType.integer(0, 31))
                                    .then(
                                        Commands.argument("gain", IntegerArgumentType.integer(0, 31))
                                            .then(
                                                Commands.argument("resistance", IntegerArgumentType.integer(0, 31))
                                                    .executes { ctx -> execute(ctx) }
                                            )
                                    )
                            )
                    )
            )
        }
    }

    private fun listExecute(ctx: CommandContext<CommandSourceStack>): Int {
        val source = ctx.source
        val types = CropType.entries
        val maxKeyWidth = types.maxOf { it.asString().length }
        val msg = Component.literal("=== 作物种子类型 (${types.size} 种) ===\n")
        types.forEachIndexed { idx, type ->
            if (idx > 0) msg.append(Component.literal("\n"))
            msg.append(Component.literal("  ${type.asString().padEnd(maxKeyWidth)} - "))
            msg.append(CropSeedData.displayName(type))
        }
        source.sendSuccess({ msg }, false)
        return types.size
    }

    private fun execute(ctx: CommandContext<CommandSourceStack>): Int {
        val source = ctx.source
        val player = source.player ?: return 0

        val seedTypeStr = StringArgumentType.getString(ctx, "seed_type")
        val seedType = CropType.entries.firstOrNull { it.asString().equals(seedTypeStr, ignoreCase = true) }
            ?: run {
                source.sendFailure(Component.literal("未知种子类型: $seedTypeStr，可用类型见 Tab 补全"))
                return 0
            }

        val growth = IntegerArgumentType.getInteger(ctx, "growth")
        val gain = IntegerArgumentType.getInteger(ctx, "gain")
        val resistance = IntegerArgumentType.getInteger(ctx, "resistance")

        val itemStack = CropSeedBagItem.createStack(seedType, CropStats(growth, gain, resistance), scanLevel = 4)

        if (!player.inventory.add(itemStack)) {
            player.drop(itemStack, true)
            source.sendSuccess(
                { Component.literal("已给予种子（背包已满，丢在地上）") },
                false
            )
        } else {
            source.sendSuccess(
                {
                    Component.literal("已给予 ")
                        .append(CropSeedData.displayName(seedType))
                        .append(Component.literal(" G:$growth Ga:$gain R:$resistance"))
                },
                false
            )
        }

        return Command.SINGLE_SUCCESS
    }
}
