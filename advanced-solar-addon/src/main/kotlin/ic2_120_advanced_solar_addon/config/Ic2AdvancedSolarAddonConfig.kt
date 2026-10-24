package ic2_120_advanced_solar_addon.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import java.lang.reflect.Modifier
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class ConfigComment(val value: String, val defaultValue: String = "")

data class MolecularTransformerRecipeConfig(
    @field:ConfigComment("输入物品 ID，例如 minecraft:iron_ingot", "")
    val input: String = "",
    @field:ConfigComment("输出物品 ID，例如 ic2_120_advanced_solar_addon:iridium_ingot", "")
    val output: String = "",
    @field:ConfigComment("所需能量（EU）", "0")
    val energy: Long = 0,
    @field:ConfigComment("输出物品数量", "1")
    val outputCount: Int = 1
)

data class Ic2AdvancedSolarAddonMainConfig(
    @field:ConfigComment("分子重组仪配方配置。")
    val molecularTransformer: MolecularTransformerConfig = MolecularTransformerConfig()
)

data class MolecularTransformerConfig(
    @field:ConfigComment("分子重组仪配方列表。")
    val recipes: List<MolecularTransformerRecipeConfig> = defaultRecipes
)

private val defaultRecipes = listOf(
    // ====== 特殊产物 ======
    MolecularTransformerRecipeConfig("minecraft:wither_skeleton_skull", "minecraft:nether_star", 250_000_000),
    MolecularTransformerRecipeConfig("minecraft:iron_ingot", "ic2_120_advanced_solar_addon:iridium_ingot", 9_000_000),

    // ====== 基础转化 ======
    MolecularTransformerRecipeConfig("minecraft:charcoal", "minecraft:coal", 60_000),
    MolecularTransformerRecipeConfig("minecraft:netherrack", "minecraft:gunpowder", 70_000, outputCount = 2),
    MolecularTransformerRecipeConfig("minecraft:sand", "minecraft:gravel", 50_000),
    MolecularTransformerRecipeConfig("minecraft:dirt", "minecraft:clay", 50_000),

    // ====== 染料 / 矿物块 ======
    MolecularTransformerRecipeConfig("minecraft:yellow_wool", "minecraft:glowstone", 500_000),
    MolecularTransformerRecipeConfig("minecraft:blue_wool", "minecraft:lapis_block", 500_000),
    MolecularTransformerRecipeConfig("minecraft:red_wool", "minecraft:redstone_block", 500_000),

    // ====== 阳光化合物产线 ======
    MolecularTransformerRecipeConfig("minecraft:glowstone_dust", "ic2_120_advanced_solar_addon:sunnarium_part", 1_000_000),
    MolecularTransformerRecipeConfig("minecraft:glowstone", "ic2_120_advanced_solar_addon:sunnarium", 9_000_000),

    // ====== 钻石 ======
    MolecularTransformerRecipeConfig("minecraft:coal", "minecraft:diamond", 1_000_000),

    // ====== 金属嬗变 ======
    MolecularTransformerRecipeConfig("ic2_120:tin_ingot", "ic2_120:silver_ingot", 500_000),
    MolecularTransformerRecipeConfig("ic2_120:silver_ingot", "minecraft:gold_ingot", 500_000),

    // ====== 跨模组配方（依赖其他模组提供物品） ======
    MolecularTransformerRecipeConfig("minecraft:lapis_lazuli", "ic2_120:sapphire", 5_000_000),
    MolecularTransformerRecipeConfig("minecraft:redstone", "ic2_120:ruby", 5_000_000),
    MolecularTransformerRecipeConfig("minecraft:copper_ingot", "ic2_120:nickel_ingot", 300_000),
    MolecularTransformerRecipeConfig("minecraft:gold_ingot", "ic2_120:platinum_ingot", 9_000_000),
    MolecularTransformerRecipeConfig("ic2_120:titanium_dust", "ic2_120:chrome_dust", 500_000),
    MolecularTransformerRecipeConfig("ic2_120:titanium_ingot", "ic2_120:chrome_ingot", 500_000),
    MolecularTransformerRecipeConfig("minecraft:quartz", "ae2:certus_quartz_crystal", 500_000)
)

private val DEFAULT_CONFIG_TEMPLATE = Ic2AdvancedSolarAddonMainConfig()

object Ic2AdvancedSolarAddonConfig {
    private val logger = LoggerFactory.getLogger("${IC2AdvancedSolarAddon.MOD_ID}/config")
    private val mapper: ObjectMapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .enable(SerializationFeature.INDENT_OUTPUT)
    private val configPath: Path by lazy {
        FabricLoader.getInstance().configDir.resolve("${IC2AdvancedSolarAddon.MOD_ID}.json")
    }

    @Volatile
    var current: Ic2AdvancedSolarAddonMainConfig = DEFAULT_CONFIG_TEMPLATE
        private set

    fun loadOrThrow() {
        current = readOrCreateDefault()
        logLoaded("loaded")
    }

    fun reloadOrThrow() {
        current = readOrCreateDefault()
        logLoaded("reloaded")
    }

    fun prettyCurrentConfig(): String {
        return mapper.writeValueAsString(current)
    }

    fun applyServerConfig(json: String) {
        current = mapper.readValue(json)
        logLoaded("applied from server")
    }

    fun getMolecularTransformerRecipes(): List<MolecularTransformerRecipeConfig> {
        return current.molecularTransformer.recipes
    }

    fun getRecipeByInput(inputId: String): MolecularTransformerRecipeConfig? {
        val normalized = inputId.trim()
        if (normalized.isEmpty()) return null
        return current.molecularTransformer.recipes.find { it.input == normalized }
    }

    fun addOrUpdateRecipeEnergy(inputId: String, energy: Long): Boolean {
        val normalizedId = inputId.trim()
        if (normalizedId.isEmpty() || energy <= 0) return false

        val currentRecipes = current.molecularTransformer.recipes.toMutableList()
        val existingIndex = currentRecipes.indexOfFirst { it.input == normalizedId }

        val newRecipe = MolecularTransformerRecipeConfig(
            input = normalizedId,
            output = existingIndex.takeIf { it >= 0 }?.let { current.molecularTransformer.recipes[it].output } ?: "",
            energy = energy
        )

        if (existingIndex >= 0) {
            currentRecipes[existingIndex] = newRecipe
        } else {
            currentRecipes.add(newRecipe)
        }

        current = current.copy(
            molecularTransformer = current.molecularTransformer.copy(
                recipes = currentRecipes
            )
        )

        saveCurrentConfig()
        return true
    }

    /**
     * 供附属 mod 追加一条完整的分子重组仪配方到配置文件。
     *
     * 若配置中已存在相同 input 的条目（玩家已手动配置或上次已写入），则**不覆盖**，直接返回 false，
     * 以保留服主的设置。仅当该 input 不存在时，才追加 [inputId]/[outputId]/[energy]/[outputCount] 并落盘。
     *
     * 与 [addOrUpdateRecipeEnergy] 的区别：本方法总是写入完整的 input/output/energy/outputCount，
     * 适用于附属首次注入配方；[addOrUpdateRecipeEnergy] 仅改 energy，对新条目的 output 留空（会被过滤）。
     *
     * 注意：本方法只更新配置文件与 [current]，不会触发 [MTRecipes.loadFromConfig]。
     * 调用方若需让新配方立即在内存配方表中生效，应额外注册扩展配方或手动重载。
     *
     * @return true 表示新增了一条配方；false 表示已存在（未改动）或参数非法
     */
    fun ensureRecipeExists(inputId: String, outputId: String, energy: Long, outputCount: Int = 1): Boolean {
        val normalizedInput = inputId.trim()
        val normalizedOutput = outputId.trim()
        if (normalizedInput.isEmpty() || normalizedOutput.isEmpty() || energy <= 0) return false

        // 已存在同 input 条目 → 不覆盖（保留服主/已有配置）
        if (getRecipeByInput(normalizedInput) != null) return false

        val currentRecipes = current.molecularTransformer.recipes.toMutableList()
        currentRecipes.add(MolecularTransformerRecipeConfig(
            input = normalizedInput,
            output = normalizedOutput,
            energy = energy,
            outputCount = outputCount.coerceAtLeast(1)
        ))
        current = current.copy(
            molecularTransformer = current.molecularTransformer.copy(
                recipes = currentRecipes
            )
        )
        saveCurrentConfig()
        return true
    }

    /**
     * 确保配置中存在指定 input 的分子重组仪配方（供附属持久化注入）。
     *
     * 附属（如工业升级）在 onInitialize 中调用 [MTRecipes.registerExtensionRecipe] 时会间接调用本方法，
     * 把其配方写入配置文件，使玩家可见、可编辑、配置重载后不丢失。
     *
     * - 若配置中已存在相同 input 的配方（玩家手动添加或历史注入），保留原样（配置优先），返回 false。
     * - 若不存在，追加完整配方（input/output/energy/outputCount）并保存到磁盘，返回 true。
     *
     * 调用时机：必须在 [loadOrThrow] 之后（配置文件已加载/创建）。
     * 附属通过 fabric.mod.json depends 声明依赖本 mod，保证其 onInitialize 在本 mod 之后执行，
     * 因此调用本方法时配置文件必定已存在（既有玩家配置，或由 loadOrThrow 刚创建的默认配置）。
     */
    fun ensureRecipe(inputId: String, outputId: String, energy: Long, outputCount: Int = 1): Boolean {
        val normalizedInput = inputId.trim()
        if (normalizedInput.isEmpty() || outputId.isBlank() || energy <= 0) return false
        if (current.molecularTransformer.recipes.any { it.input == normalizedInput }) return false
        val newRecipe = MolecularTransformerRecipeConfig(normalizedInput, outputId, energy, outputCount)
        current = current.copy(
            molecularTransformer = current.molecularTransformer.copy(
                recipes = current.molecularTransformer.recipes + newRecipe
            )
        )
        saveCurrentConfig()
        logger.info("Injected extension MT recipe into config: {} -> {} ({} EU)", normalizedInput, outputId, energy)
        return true
    }

    fun removeRecipe(inputId: String): Boolean {
        val normalizedId = inputId.trim()
        if (normalizedId.isEmpty()) return false

        val currentRecipes = current.molecularTransformer.recipes.toMutableList()
        val existingIndex = currentRecipes.indexOfFirst { it.input == normalizedId }

        if (existingIndex < 0) return false

        currentRecipes.removeAt(existingIndex)
        current = current.copy(
            molecularTransformer = current.molecularTransformer.copy(
                recipes = currentRecipes
            )
        )

        saveCurrentConfig()
        return true
    }

    private fun saveCurrentConfig() {
        Files.writeString(configPath, encodeConfigWithComments(current), StandardCharsets.UTF_8)
    }

    private fun readOrCreateDefault(): Ic2AdvancedSolarAddonMainConfig {
        if (!Files.exists(configPath)) {
            writeDefaultConfig(configPath)
            return DEFAULT_CONFIG_TEMPLATE
        }

        return try {
            val raw = Files.readString(configPath, StandardCharsets.UTF_8)
            val config = mapper.readValue<Ic2AdvancedSolarAddonMainConfig>(raw)
            val parsedRoot = mapper.readTree(raw)
            if (shouldRewriteConfig(parsedRoot, config)) {
                Files.writeString(configPath, encodeConfigWithComments(config), StandardCharsets.UTF_8)
            }
            config
        } catch (e: Exception) {
            throw IllegalStateException("Failed to parse config: $configPath", e)
        }
    }

    private fun writeDefaultConfig(path: Path) {
        Files.createDirectories(path.parent)
        Files.writeString(path, defaultConfigText(), StandardCharsets.UTF_8)
    }

    private fun defaultConfigText(): String {
        return encodeConfigWithComments(DEFAULT_CONFIG_TEMPLATE)
    }

    private fun shouldRewriteConfig(root: JsonNode, config: Ic2AdvancedSolarAddonMainConfig): Boolean {
        return !containsAllExpectedKeys(root, buildCommentedConfigJson(config))
    }

    private fun encodeConfigWithComments(config: Ic2AdvancedSolarAddonMainConfig): String =
        mapper.writerWithDefaultPrettyPrinter().writeValueAsString(buildCommentedConfigJson(config))

    private fun buildCommentedConfigJson(config: Ic2AdvancedSolarAddonMainConfig): ObjectNode =
        buildCommentedObject(
            instance = config,
            jsonNode = mapper.valueToTree(config) as ObjectNode,
            rootComment = "配置文件允许保留这些 _comment_* 说明字段；程序读取时会自动忽略它们。"
        )

    private fun buildCommentedObject(
        instance: Any,
        jsonNode: ObjectNode,
        rootComment: String? = null
    ): ObjectNode {
        val result = mapper.createObjectNode()

        if (rootComment != null) {
            result.put("_comment", rootComment)
        }

        declaredConfigFields(instance.javaClass).forEach { field ->
            field.isAccessible = true
            val fieldName = field.name
            val valueElement = jsonNode.get(fieldName) ?: return@forEach
            field.getAnnotation(ConfigComment::class.java)?.let { annotation ->
                result.put("_comment_$fieldName", formatComment(annotation))
            }

            val fieldValue = field.get(instance)
            val isNestedConfigObject =
                fieldValue != null &&
                    valueElement.isObject &&
                    !Map::class.java.isAssignableFrom(field.type)

            if (isNestedConfigObject) {
                result.set<JsonNode>(fieldName, buildCommentedObject(fieldValue, valueElement as ObjectNode))
            } else {
                result.set<JsonNode>(fieldName, valueElement)
            }
        }

        return result
    }

    private fun containsAllExpectedKeys(actual: JsonNode, expected: ObjectNode): Boolean =
        expected.fieldNames().asSequence().all { key ->
            val actualValue = actual.get(key) ?: return@all false
            val expectedValue = expected.get(key)
            if (expectedValue != null && expectedValue.isObject && actualValue.isObject) {
                containsAllExpectedKeys(actualValue, expectedValue as ObjectNode)
            } else {
                true
            }
        }

    private fun declaredConfigFields(type: Class<*>): List<java.lang.reflect.Field> =
        type.declaredFields.filterNot { field ->
            field.isSynthetic || Modifier.isStatic(field.modifiers)
        }

    private inline fun <reified T : Any> commentOf(fieldName: String): String =
        T::class.java.getDeclaredField(fieldName).getAnnotation(ConfigComment::class.java)?.let { annotation ->
            if (annotation.defaultValue.isBlank()) {
                annotation.value
            } else {
                "${annotation.value} 默认值: ${annotation.defaultValue}"
            }
        } ?: error("Missing @ConfigComment on ${T::class.java.simpleName}.$fieldName")

    private fun formatComment(annotation: ConfigComment): String =
        if (annotation.defaultValue.isBlank()) {
            annotation.value
        } else {
            "${annotation.value} 默认值: ${annotation.defaultValue}"
        }

    private fun logLoaded(action: String) {
        logger.info(
            "Config {}:\n{}",
            action,
            prettyCurrentConfig()
        )
    }
}
