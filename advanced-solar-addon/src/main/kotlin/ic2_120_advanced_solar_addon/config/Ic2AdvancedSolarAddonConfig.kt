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
    val energy: Long = 0
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
    MolecularTransformerRecipeConfig(
        input = "ic2_120_advanced_solar_addon:fluorite",
        output = "ic2_120_advanced_solar_addon:sunnarium",
        energy = 9000000
    ),
    MolecularTransformerRecipeConfig(
        input = "ic2_120_advanced_solar_addon:fluorite_dust",
        output = "ic2_120_advanced_solar_addon:sunnarium_part",
        energy = 1000000
    ),
    MolecularTransformerRecipeConfig(
        input = "minecraft:iron_ingot",
        output = "ic2_120_advanced_solar_addon:iridium_ingot",
        energy = 9000000
    )
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
