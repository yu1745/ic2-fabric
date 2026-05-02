package ic2_120_advanced_solar_addon.content.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.LongArgumentType
import ic2_120_advanced_solar_addon.config.Ic2AdvancedSolarAddonConfig
import ic2_120_advanced_solar_addon.config.MolecularTransformerRecipeConfig
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier

object MolecularTransformerCommand {
    fun register() {
        CommandRegistrationCallback.EVENT.register(CommandRegistrationCallback { dispatcher, _, _ ->
            dispatcher.register(
                literal("ic2mt")
                    .requires { source -> source.hasPermissionLevel(2) }
                    // 设置命令: /ic2mt set <energy>
                    .then(
                        literal("set")
                            .then(
                                argument("energy", LongArgumentType.longArg(1))
                                    .executes { context ->
                                        val source = context.source
                                        val player = source.player
                                            ?: return@executes run {
                                                source.sendError(Text.literal("此命令只能由玩家执行"))
                                                0
                                            }

                                        val energy = LongArgumentType.getLong(context, "energy")
                                        val mainHandStack = player.mainHandStack

                                        if (mainHandStack.isEmpty) {
                                            source.sendError(Text.literal("主手必须持有物品"))
                                            return@executes 0
                                        }

                                        val itemId = mainHandStack.item.registryEntry.registryKey().value.toString()

                                        // 添加或更新配方配置并保存
                                        val success = Ic2AdvancedSolarAddonConfig.addOrUpdateRecipeEnergy(itemId, energy)

                                        if (success) {
                                            source.sendFeedback(
                                                {
                                                    Text.literal("")
                                                        .append(Text.literal("成功设置 ").formatted(Formatting.GREEN))
                                                        .append(Text.literal(itemId).formatted(Formatting.YELLOW))
                                                        .append(Text.literal(" 的分子重组仪所需电量为 ").formatted(Formatting.GREEN))
                                                        .append(Text.literal("$energy EU").formatted(Formatting.AQUA))
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
                    // 移除命令: /ic2mt remove
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

                                val itemId = mainHandStack.item.registryEntry.registryKey().value.toString()

                                // 从配置中移除并保存
                                val success = Ic2AdvancedSolarAddonConfig.removeRecipe(itemId)

                                if (success) {
                                    source.sendFeedback(
                                        {
                                            Text.literal("")
                                                .append(Text.literal("成功移除 ").formatted(Formatting.GREEN))
                                                .append(Text.literal(itemId).formatted(Formatting.YELLOW))
                                                .append(Text.literal(" 的分子重组仪配置").formatted(Formatting.GREEN))
                                        },
                                        true
                                    )
                                    Command.SINGLE_SUCCESS
                                } else {
                                    source.sendError(Text.literal("该物品未配置分子重组仪配方或移除失败"))
                                    0
                                }
                            }
                    )
                    // 查询命令: /ic2mt get
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

                                val itemId = mainHandStack.item.registryEntry.registryKey().value.toString()
                                val currentRecipe = Ic2AdvancedSolarAddonConfig.getRecipeByInput(itemId)

                                if (currentRecipe != null) {
                                    source.sendFeedback(
                                        {
                                            Text.literal("")
                                                .append(Text.literal("物品 ").formatted(Formatting.WHITE))
                                                .append(Text.literal(itemId).formatted(Formatting.YELLOW))
                                                .append(Text.literal(" 的分子重组仪配置:").formatted(Formatting.WHITE))
                                                .append(Text.literal("\n  - 所需电量: ").formatted(Formatting.GREEN))
                                                .append(Text.literal("${currentRecipe.energy} EU").formatted(Formatting.AQUA))
                                                .append(Text.literal("\n  - 输出物品: ").formatted(Formatting.GREEN))
                                                .append(Text.literal(currentRecipe.output).formatted(Formatting.YELLOW))
                                        },
                                        false
                                    )
                                } else {
                                    source.sendFeedback(
                                        {
                                            Text.literal("")
                                                .append(Text.literal("物品 ").formatted(Formatting.WHITE))
                                                .append(Text.literal(itemId).formatted(Formatting.YELLOW))
                                                .append(Text.literal(" 未配置分子重组仪配方").formatted(Formatting.RED))
                                        },
                                        false
                                    )
                                }
                                Command.SINGLE_SUCCESS
                            }
                    )
                    // 列出命令: /ic2mt list [page]
                    .then(
                        literal("list")
                            .executes { context ->
                                showRecipeList(context.source, 1)
                            }
                            .then(
                                argument("page", LongArgumentType.longArg(1))
                                    .executes { context ->
                                        val page = LongArgumentType.getLong(context, "page").toInt()
                                        showRecipeList(context.source, page)
                                    }
                            )
                    )
            )
        })
    }

    private fun showRecipeList(source: net.minecraft.server.command.ServerCommandSource, page: Int): Int {
        val allRecipes = Ic2AdvancedSolarAddonConfig.getMolecularTransformerRecipes()
            .sortedBy { it.input }

        if (allRecipes.isEmpty()) {
            source.sendFeedback({ Text.literal("分子重组仪配方列表为空").formatted(Formatting.YELLOW) }, false)
            return Command.SINGLE_SUCCESS
        }

        val itemsPerPage = 10
        val totalPages = (allRecipes.size + itemsPerPage - 1) / itemsPerPage
        val actualPage = page.coerceIn(1, totalPages)
        val startIndex = (actualPage - 1) * itemsPerPage
        val endIndex = minOf(startIndex + itemsPerPage, allRecipes.size)

        source.sendFeedback(
            { Text.literal("=== 分子重组仪配方列表 (第 $actualPage/$totalPages 页) ===").formatted(Formatting.GOLD) },
            false
        )

        for (i in startIndex until endIndex) {
            val recipe = allRecipes[i]
            fun itemDisplay(id: String): Text {
                val item = Identifier.tryParse(id)?.let { Registries.ITEM.get(it) }
                return if (item != null && item !== Items.AIR) {
                    Text.translatable(item.translationKey)
                        .formatted(Formatting.YELLOW)
                        .append(Text.literal(" ($id)").formatted(Formatting.DARK_GRAY))
                } else {
                    Text.literal(id).formatted(Formatting.WHITE)
                }
            }
            source.sendFeedback(
                {
                    Text.literal("")
                        .append(Text.literal("• ").formatted(Formatting.GRAY))
                        .append(itemDisplay(recipe.input))
                        .append(Text.literal(" -> ").formatted(Formatting.GRAY))
                        .append(itemDisplay(recipe.output))
                        .append(Text.literal(": ").formatted(Formatting.GRAY))
                        .append(Text.literal("${recipe.energy} EU").formatted(Formatting.AQUA))
                },
                false
            )
        }

        if (actualPage < totalPages) {
            source.sendFeedback(
                { Text.literal("使用 /ic2mt list ${actualPage + 1} 查看下一页").formatted(Formatting.GRAY) },
                false
            )
        }

        return Command.SINGLE_SUCCESS
    }
}
