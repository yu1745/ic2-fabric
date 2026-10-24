package ic2_120_advanced_solar_addon.content.recipe

import ic2_120_advanced_solar_addon.config.Ic2AdvancedSolarAddonConfig
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.util.Identifier
import net.minecraft.registry.Registries
import org.slf4j.LoggerFactory

object MTRecipes {
    private val LOGGER = LoggerFactory.getLogger("ic2_120_advanced_solar_addon/MTRecipes")
    private val recipes = mutableListOf<MTRecipe>()

    /**
     * 附属 mod 注册的扩展配方（默认值）。
     *
     * 这些配方在 [loadFromConfig] / [loadFromSync] 之后会被重新注入，因此不会被配置重载冲掉。
     * 若 ASA 配置文件中存在相同 input id 的条目，则以配置文件中的 energy 为准（玩家可覆盖耗电量）。
     */
    private val extensionRecipes = mutableListOf<ExtensionRecipe>()

    data class MTRecipe(
        val input: ItemStack,
        val output: ItemStack,
        val energy: Long
    )

    /**
     * 附属扩展配方的默认值（input/output 用物品 id 字符串表示，便于配置覆盖查找）。
     */
    data class ExtensionRecipe(
        val inputId: String,
        val outputId: String,
        val defaultEnergy: Long,
        val outputCount: Int = 1
    )

    fun init() {
        loadFromConfig()
    }

    fun loadFromConfig() {
        recipes.clear()
        for (configRecipe in Ic2AdvancedSolarAddonConfig.getMolecularTransformerRecipes()) {
            addRecipe(configRecipe.input, configRecipe.output, configRecipe.energy, configRecipe.outputCount)
        }
        reinjectExtensionRecipes()
    }

    fun loadFromSync(entries: List<MTRecipeEntry>) {
        recipes.clear()
        for (entry in entries) {
            addRecipe(entry.inputId, entry.outputId, entry.energy, entry.outputCount)
        }
        reinjectExtensionRecipes()
    }

    /**
     * 重新注入附属注册的扩展配方。
     *
     * 对每条扩展配方：若当前 recipes 中已存在相同 input 的条目（通常来自配置文件），跳过（配置优先）；
     * 否则用默认 energy 注入。这保证了：
     * - 玩家未在配置文件写该配方时，使用附属提供的默认耗电量；
     * - 玩家在配置文件写了该配方时，使用玩家指定的耗电量；
     * - 配置重载（reload/sync）后附属配方始终存在，不会被冲掉。
     */
    private fun reinjectExtensionRecipes() {
        for (ext in extensionRecipes) {
            val alreadyPresent = recipes.any { existing ->
                val existingInputId = Registries.ITEM.getId(existing.input.item).toString()
                existingInputId == ext.inputId
            }
            if (alreadyPresent) continue
            addRecipe(ext.inputId, ext.outputId, ext.defaultEnergy, ext.outputCount)
        }
    }

    /**
     * 供附属 mod 注册分子重组仪扩展配方（可配置、配置重载后不丢失）。
     *
     * 与 [addRecipe]（运行时硬编码、会被重载冲掉）不同，本方法注册的配方：
     * - 存入 [extensionRecipes]，每次 [loadFromConfig]/[loadFromSync] 后自动重新注入；
     * - 默认耗电量为 [defaultEnergy]，玩家可在 ASA 配置文件中加入同 input 的条目来覆盖。
     *
     * 建议附属在 `onInitialize` 中调用。ASA 的 `MTRecipes.init()` 延后到
     * SERVER_STARTING（所有 mod 物品注册完成后），附属调用本方法时配方表可能尚未
     * 从配置加载；首次注入发生在附属调用本方法后的下一次 reinject，或
     * SERVER_STARTING 时 `loadFromConfig` 末尾的 reinject，二者均可保证不丢。
     *
     * @param inputId 输入物品 id（如 "ic2_120_advanced_solar_addon:iridium_ingot"）
     * @param outputId 输出物品 id
     * @param defaultEnergy 默认所需能量（EU），玩家可在配置文件覆盖
     * @param outputCount 输出数量
     */
    fun registerExtensionRecipe(inputId: String, outputId: String, defaultEnergy: Long, outputCount: Int = 1) {
        extensionRecipes.add(ExtensionRecipe(inputId, outputId, defaultEnergy, outputCount))
        // 持久化到 ASA 配置文件：若配置中尚无该 input 的配方则追加（玩家已有则保留，配置优先）。
        // 前提：ASA onInitialize 已执行 loadOrThrow（创建/加载配置文件）；附属通过 fabric.mod.json
        // depends 声明依赖 ASA，Fabric loader 保证附属 onInitialize 在 ASA 之后执行。
        Ic2AdvancedSolarAddonConfig.ensureRecipe(inputId, outputId, defaultEnergy, outputCount)
        reinjectExtensionRecipes()
    }

    /**
     * 供附属 mod 运行时注册分子重组仪配方（按 [ItemStack] 直接匹配）。
     *
     * 与从配置文件加载的 [loadFromConfig] 不同，本方法允许附属在初始化时
     * 直接添加硬编码配方（例如工业升级附属的光子产出配方）。
     *
     * 注意：通过本方法添加的配方在 [loadFromConfig]/[loadFromSync] 后会丢失。
     * 若需要配置可覆盖且重载不丢失，请使用 [registerExtensionRecipe]。
     *
     * @param input 输入物品栈（数量决定每次消耗）
     * @param output 输出物品栈
     * @param energy 所需能量（EU）
     * @return 是否添加成功（输入/输出为空或能量 ≤0 时返回 false）
     */
    fun addRecipe(input: ItemStack, output: ItemStack, energy: Long): Boolean {
        val inItem = input.item
        val outItem = output.item
        if (inItem == Items.AIR || outItem == Items.AIR || energy <= 0) return false
        recipes.add(MTRecipe(input.copy(), output.copy(), energy))
        return true
    }

    private fun addRecipe(inputId: String, outputId: String, energy: Long, outputCount: Int = 1) {
        val inId = Identifier.tryParse(inputId)
        val outId = Identifier.tryParse(outputId)
        if (inId == null || outId == null) {
            LOGGER.warn("MT recipe dropped (invalid id): {} -> {}", inputId, outputId)
            return
        }
        val inputItem = Registries.ITEM.get(inId)
        val outputItem = Registries.ITEM.get(outId)
        if (inputItem == Items.AIR || outputItem == Items.AIR || energy <= 0) {
            // 静默丢弃会表现为「物品无法放入分子重组仪」，必须留痕：
            // 常见诱因是加载时机早于物品注册（如 Connector 服务器上 core 初始化晚于附属）。
            LOGGER.warn(
                "MT recipe dropped: {} -> {} (input={}, output={}, energy={}) — 物品未注册或能量非法",
                inputId, outputId,
                if (inputItem === Items.AIR) "AIR" else "ok",
                if (outputItem === Items.AIR) "AIR" else "ok",
                energy
            )
            return
        }
        recipes.add(MTRecipe(
            input = ItemStack(inputItem),
            output = ItemStack(outputItem, outputCount.coerceIn(1, 64)),
            energy = energy
        ))
    }

    fun findRecipe(input: ItemStack): MTRecipe? {
        return recipes.find { recipe ->
            ItemStack.canCombine(recipe.input, input) && input.count >= recipe.input.count
        }
    }

    fun getRecipes(): List<MTRecipe> = recipes.toList()
}
