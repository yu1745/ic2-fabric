package ic2_120_advanced_solar_addon.content.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.LongArgumentType
import ic2_120_advanced_solar_addon.config.Ic2AdvancedSolarAddonConfig
import ic2_120_advanced_solar_addon.config.MolecularTransformerRecipeConfig
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.world.item.Items
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.commands.Commands.argument
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.resources.ResourceLocation
import net.minecraft.commands.Commands.literal

object MolecularTransformerCommand {
    fun register() {
        CommandRegistrationCallback.EVENT.register(CommandRegistrationCallback { dispatcher, _, _ ->
            dispatcher.register(
                literal("ic2mt")
                    .requires { source -> source.hasPermission(2) }
                    // 设置命令: /ic2mt set <energy>
                    .then(
                        literal("set")
                            .then(
                                argument("energy", LongArgumentType.longArg(1))
                                    .executes { context ->
                                        val source = context.source
                                        val player = source.player
                                            ?: return@executes run {
                                                source.sendFailure(Component.literal("此命令只能由玩家执行"))
                                                0
                                            }

                                        val energy = LongArgumentType.getLong(context, "energy")
                                        val mainHandStack = player.mainHandItem

                                        if (mainHandStack.isEmpty) {
                                            source.sendFailure(Component.literal("主手必须持有物品"))
                                            return@executes 0
                                        }

                                        val itemId = BuiltInRegistries.ITEM.getKey(mainHandStack.item).toString()

                                        // 添加或更新配方配置并保存
                                        val success = Ic2AdvancedSolarAddonConfig.addOrUpdateRecipeEnergy(itemId, energy)

                                        if (success) {
                                            source.sendSuccess(
                                                {
                                                    Component.literal("")
                                                        .append(Component.literal("成功设置 ").withStyle(ChatFormatting.GREEN))
                                                        .append(Component.literal(itemId).withStyle(ChatFormatting.YELLOW))
                                                        .append(Component.literal(" 的分子重组仪所需电量为 ").withStyle(ChatFormatting.GREEN))
                                                        .append(Component.literal("$energy EU").withStyle(ChatFormatting.AQUA))
                                                        .append(Component.literal("（已写入配置文件）").withStyle(ChatFormatting.GRAY))
                                                },
                                                true
                                            )
                                            Command.SINGLE_SUCCESS
                                        } else {
                                            source.sendFailure(Component.literal("保存配置失败，请查看服务器日志"))
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
                                        source.sendFailure(Component.literal("此命令只能由玩家执行"))
                                        0
                                    }

                                val mainHandStack = player.mainHandItem

                                if (mainHandStack.isEmpty) {
                                    source.sendFailure(Component.literal("主手必须持有物品"))
                                    return@executes 0
                                }

                                val itemId = BuiltInRegistries.ITEM.getKey(mainHandStack.item).toString()

                                // 从配置中移除并保存
                                val success = Ic2AdvancedSolarAddonConfig.removeRecipe(itemId)

                                if (success) {
                                    source.sendSuccess(
                                        {
                                            Component.literal("")
                                                .append(Component.literal("成功移除 ").withStyle(ChatFormatting.GREEN))
                                                .append(Component.literal(itemId).withStyle(ChatFormatting.YELLOW))
                                                .append(Component.literal(" 的分子重组仪配置").withStyle(ChatFormatting.GREEN))
                                        },
                                        true
                                    )
                                    Command.SINGLE_SUCCESS
                                } else {
                                    source.sendFailure(Component.literal("该物品未配置分子重组仪配方或移除失败"))
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
                                        source.sendFailure(Component.literal("此命令只能由玩家执行"))
                                        0
                                    }

                                val mainHandStack = player.mainHandItem

                                if (mainHandStack.isEmpty) {
                                    source.sendFailure(Component.literal("主手必须持有物品"))
                                    return@executes 0
                                }

                                val itemId = BuiltInRegistries.ITEM.getKey(mainHandStack.item).toString()
                                val currentRecipe = Ic2AdvancedSolarAddonConfig.getRecipeByInput(itemId)

                                if (currentRecipe != null) {
                                    source.sendSuccess(
                                        {
                                            Component.literal("")
                                                .append(Component.literal("物品 ").withStyle(ChatFormatting.WHITE))
                                                .append(Component.literal(itemId).withStyle(ChatFormatting.YELLOW))
                                                .append(Component.literal(" 的分子重组仪配置:").withStyle(ChatFormatting.WHITE))
                                                .append(Component.literal("\n  - 所需电量: ").withStyle(ChatFormatting.GREEN))
                                                .append(Component.literal("${currentRecipe.energy} EU").withStyle(ChatFormatting.AQUA))
                                                .append(Component.literal("\n  - 输出物品: ").withStyle(ChatFormatting.GREEN))
                                                .append(Component.literal(currentRecipe.output).withStyle(ChatFormatting.YELLOW))
                                        },
                                        false
                                    )
                                } else {
                                    source.sendSuccess(
                                        {
                                            Component.literal("")
                                                .append(Component.literal("物品 ").withStyle(ChatFormatting.WHITE))
                                                .append(Component.literal(itemId).withStyle(ChatFormatting.YELLOW))
                                                .append(Component.literal(" 未配置分子重组仪配方").withStyle(ChatFormatting.RED))
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

    private fun showRecipeList(source: net.minecraft.commands.CommandSourceStack, page: Int): Int {
        val allRecipes = Ic2AdvancedSolarAddonConfig.getMolecularTransformerRecipes()
            .sortedBy { it.input }

        if (allRecipes.isEmpty()) {
            source.sendSuccess({ Component.literal("分子重组仪配方列表为空").withStyle(ChatFormatting.YELLOW) }, false)
            return Command.SINGLE_SUCCESS
        }

        val itemsPerPage = 10
        val totalPages = (allRecipes.size + itemsPerPage - 1) / itemsPerPage
        val actualPage = page.coerceIn(1, totalPages)
        val startIndex = (actualPage - 1) * itemsPerPage
        val endIndex = minOf(startIndex + itemsPerPage, allRecipes.size)

        source.sendSuccess(
            { Component.literal("=== 分子重组仪配方列表 (第 $actualPage/$totalPages 页) ===").withStyle(ChatFormatting.GOLD) },
            false
        )

        for (i in startIndex until endIndex) {
            val recipe = allRecipes[i]
            fun itemDisplay(id: String): Component {
                val item = ResourceLocation.tryParse(id)?.let { BuiltInRegistries.ITEM.get(it) }
                return if (item != null && item !== Items.AIR) {
                    Component.translatable(item.descriptionId)
                        .withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal(" ($id)").withStyle(ChatFormatting.DARK_GRAY))
                } else {
                    Component.literal(id).withStyle(ChatFormatting.WHITE)
                }
            }
            source.sendSuccess(
                {
                    Component.literal("")
                        .append(Component.literal("• ").withStyle(ChatFormatting.GRAY))
                        .append(itemDisplay(recipe.input))
                        .append(Component.literal(" -> ").withStyle(ChatFormatting.GRAY))
                        .append(itemDisplay(recipe.output))
                        .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("${recipe.energy} EU").withStyle(ChatFormatting.AQUA))
                },
                false
            )
        }

        if (actualPage < totalPages) {
            source.sendSuccess(
                { Component.literal("使用 /ic2mt list ${actualPage + 1} 查看下一页").withStyle(ChatFormatting.GRAY) },
                false
            )
        }

        return Command.SINGLE_SUCCESS
    }
}
