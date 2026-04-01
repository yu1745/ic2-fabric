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
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

object SeedCommand {
    private const val PERMISSION_LEVEL = 2

    private val seedTypeSuggestionProvider = SuggestionProvider<ServerCommandSource> { _, builder ->
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
                CommandManager.literal("ic2seed")
                    .requires { source ->
                        source.hasPermissionLevel(PERMISSION_LEVEL) ||
                        source.player?.isCreative() == true
                    }
                    .then(
                        CommandManager.literal("list")
                            .executes { ctx -> listExecute(ctx) }
                    )
                    .then(
                        CommandManager.argument("seed_type", StringArgumentType.string())
                            .suggests(seedTypeSuggestionProvider)
                            .then(
                                CommandManager.argument("growth", IntegerArgumentType.integer(0, 31))
                                    .then(
                                        CommandManager.argument("gain", IntegerArgumentType.integer(0, 31))
                                            .then(
                                                CommandManager.argument("resistance", IntegerArgumentType.integer(0, 31))
                                                    .executes { ctx -> execute(ctx) }
                                            )
                                    )
                            )
                    )
            )
        }
    }

    private fun listExecute(ctx: CommandContext<ServerCommandSource>): Int {
        val source = ctx.source
        val types = CropType.entries
        val maxKeyWidth = types.maxOf { it.asString().length }
        val lines = buildString {
            appendLine("=== 作物种子类型 (${types.size} 种) ===")
            types.forEach { type ->
                val displayName = CropSeedData.displayName(type).string
                appendLine("  ${type.asString().padEnd(maxKeyWidth)} - $displayName")
            }
        }
        source.sendFeedback({ Text.literal(lines) }, false)
        return types.size
    }

    private fun execute(ctx: CommandContext<ServerCommandSource>): Int {
        val source = ctx.source
        val player = source.player ?: return 0

        val seedTypeStr = StringArgumentType.getString(ctx, "seed_type")
        val seedType = CropType.entries.firstOrNull { it.asString().equals(seedTypeStr, ignoreCase = true) }
            ?: run {
                source.sendError(Text.literal("未知种子类型: $seedTypeStr，可用类型见 Tab 补全"))
                return 0
            }

        val growth = IntegerArgumentType.getInteger(ctx, "growth")
        val gain = IntegerArgumentType.getInteger(ctx, "gain")
        val resistance = IntegerArgumentType.getInteger(ctx, "resistance")

        val itemStack = CropSeedBagItem.createStack(seedType, CropStats(growth, gain, resistance), scanLevel = 4)

        if (!player.inventory.insertStack(itemStack)) {
            player.dropItem(itemStack, true)
            source.sendFeedback(
                { Text.literal("已给予种子（背包已满，丢在地上）") },
                false
            )
        } else {
            source.sendFeedback(
                {
                    Text.literal("已给予 ")
                        .append(CropSeedData.displayName(seedType))
                        .append(Text.literal(" G:$growth Ga:$gain R:$resistance"))
                },
                false
            )
        }

        return Command.SINGLE_SUCCESS
    }
}
