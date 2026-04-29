package ic2_120.content.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.IntegerArgumentType
import ic2_120.config.Ic2Config
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.text.Text
import net.minecraft.util.Formatting

object UuReplicationCommand {
    fun register() {
        CommandRegistrationCallback.EVENT.register(CommandRegistrationCallback { dispatcher, _, _ ->
            dispatcher.register(
                literal("ic2uu")
                    .requires { source -> source.hasPermissionLevel(2) }
                    // 设置命令: /ic2uu set <uu_cost>
                    .then(
                        literal("set")
                            .then(
                                argument("uu_cost", IntegerArgumentType.integer(1))
                                    .executes { context ->
                                        val source = context.source
                                        val player = source.player
                                            ?: return@executes run {
                                                source.sendError(Text.literal("此命令只能由玩家执行"))
                                                0
                                            }

                                        val uuCost = IntegerArgumentType.getInteger(context, "uu_cost")
                                        val mainHandStack = player.mainHandStack

                                        if (mainHandStack.isEmpty) {
                                            source.sendError(Text.literal("主手必须持有物品"))
                                            return@executes 0
                                        }

                                        val item = mainHandStack.item
                                        val itemId = item.toString()

                                        // 添加或更新白名单并保存
                                        val success = Ic2Config.addOrUpdateReplicationCost(itemId, uuCost)

                                        if (success) {
                                            source.sendFeedback(
                                                {
                                                    Text.literal("")
                                                        .append(Text.literal("成功设置 ").formatted(Formatting.GREEN))
                                                        .append(Text.literal(itemId).formatted(Formatting.YELLOW))
                                                        .append(Text.literal(" 的UU复制成本为 ").formatted(Formatting.GREEN))
                                                        .append(Text.literal("$uuCost uB").formatted(Formatting.AQUA))
                                                        .append(Text.literal("（已写入配置文件）").formatted(Formatting.GRAY))
                                                },
                                                true
                                            )
                                            Command.SINGLE_SUCCESS
                                        } else {
                                            source.sendError(Text.literal("保存配置失败，请查看服务器日志"))
                                            0
                                        }
                                    }
                            )
                    )
                    // 移除命令: /ic2uu remove
                    .then(
                        literal("remove")
                            .executes { context ->
                                val source = context.source
                                val player = source.player
                                    ?: return@executes run {
                                        source.sendError(Text.literal("此命令只能由玩家执行"))
                                        0
                                    }

                                val mainHandStack = player.mainHandStack

                                if (mainHandStack.isEmpty) {
                                    source.sendError(Text.literal("主手必须持有物品"))
                                    return@executes 0
                                }

                                val item = mainHandStack.item
                                val itemId = item.toString()

                                // 从白名单中移除并保存
                                val success = Ic2Config.removeReplicationCost(itemId)

                                if (success) {
                                    source.sendFeedback(
                                        {
                                            Text.literal("")
                                                .append(Text.literal("成功移除 ").formatted(Formatting.GREEN))
                                                .append(Text.literal(itemId).formatted(Formatting.YELLOW))
                                                .append(Text.literal(" 的UU复制配置").formatted(Formatting.GREEN))
                                        },
                                        true
                                    )
                                    Command.SINGLE_SUCCESS
                                } else {
                                    source.sendError(Text.literal("该物品未配置UU复制成本或移除失败"))
                                    0
                                }
                            }
                    )
                    // 查询命令: /ic2uu get
                    .then(
                        literal("get")
                            .executes { context ->
                                val source = context.source
                                val player = source.player
                                    ?: return@executes run {
                                        source.sendError(Text.literal("此命令只能由玩家执行"))
                                        0
                                    }

                                val mainHandStack = player.mainHandStack

                                if (mainHandStack.isEmpty) {
                                    source.sendError(Text.literal("主手必须持有物品"))
                                    return@executes 0
                                }

                                val item = mainHandStack.item
                                val itemId = item.toString()
                                val currentCost = Ic2Config.getReplicationCostUb(itemId)

                                if (currentCost != null) {
                                    source.sendFeedback(
                                        {
                                            Text.literal("")
                                                .append(Text.literal("物品 ").formatted(Formatting.WHITE))
                                                .append(Text.literal(itemId).formatted(Formatting.YELLOW))
                                                .append(Text.literal(" 的UU复制成本: ").formatted(Formatting.WHITE))
                                                .append(Text.literal("$currentCost uB").formatted(Formatting.AQUA))
                                        },
                                        false
                                    )
                                } else {
                                    source.sendFeedback(
                                        {
                                            Text.literal("")
                                                .append(Text.literal("物品 ").formatted(Formatting.WHITE))
                                                .append(Text.literal(itemId).formatted(Formatting.YELLOW))
                                                .append(Text.literal(" 未配置UU复制成本").formatted(Formatting.RED))
                                        },
                                        false
                                    )
                                }
                                Command.SINGLE_SUCCESS
                            }
                    )
                    // 列出命令: /ic2uu list [page]
                    .then(
                        literal("list")
                            .executes { context ->
                                showReplicationList(context.source, 1)
                            }
                            .then(
                                argument("page", IntegerArgumentType.integer(1))
                                    .executes { context ->
                                        val page = IntegerArgumentType.getInteger(context, "page")
                                        showReplicationList(context.source, page)
                                    }
                            )
                    )
            )
        })
    }

    private fun showReplicationList(source: net.minecraft.server.command.ServerCommandSource, page: Int): Int {
        val allCosts = Ic2Config.getAllReplicationCosts().toList().sortedBy { it.first }

        if (allCosts.isEmpty()) {
            source.sendFeedback({ Text.literal("UU复制白名单为空").formatted(Formatting.YELLOW) }, false)
            return Command.SINGLE_SUCCESS
        }

        val itemsPerPage = 10
        val totalPages = (allCosts.size + itemsPerPage - 1) / itemsPerPage
        val actualPage = page.coerceIn(1, totalPages)
        val startIndex = (actualPage - 1) * itemsPerPage
        val endIndex = minOf(startIndex + itemsPerPage, allCosts.size)

        source.sendFeedback(
            { Text.literal("=== UU复制白名单 (第 $actualPage/$totalPages 页) ===").formatted(Formatting.GOLD) },
            false
        )

        for (i in startIndex until endIndex) {
            val (itemId, cost) = allCosts[i]
            source.sendFeedback(
                {
                    Text.literal("")
                        .append(Text.literal("• ").formatted(Formatting.GRAY))
                        .append(Text.literal(itemId).formatted(Formatting.WHITE))
                        .append(Text.literal(": ").formatted(Formatting.GRAY))
                        .append(Text.literal("$cost uB").formatted(Formatting.AQUA))
                },
                false
            )
        }

        if (actualPage < totalPages) {
            source.sendFeedback(
                { Text.literal("使用 /ic2uu list ${actualPage + 1} 查看下一页").formatted(Formatting.GRAY) },
                false
            )
        }

        return Command.SINGLE_SUCCESS
    }
}
