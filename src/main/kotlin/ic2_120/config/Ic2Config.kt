package ic2_120.config

import ic2_120.Ic2_120
import ic2_120.content.uu.UuTemplateEntry
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.lang.reflect.Modifier
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class ConfigComment(val value: String, val defaultValue: String = "")

@Serializable
data class Ic2MainConfig(
    @field:ConfigComment("通用配置。")
    val general: GeneralConfig = GeneralConfig(),
    @field:ConfigComment("回收机相关配置。")
    val recycler: RecyclerConfig = RecyclerConfig(),
    @field:ConfigComment("核能系统相关配置。")
    val nuclear: NuclearConfig = NuclearConfig(),
    @field:ConfigComment("UU 复制白名单配置。")
    val uuReplication: UuReplicationConfig = UuReplicationConfig(),
    @field:ConfigComment("采矿机配置。")
    val miner: MinerConfig = MinerConfig(),
    @field:ConfigComment("世界生成配置。")
    val worldgen: WorldgenConfig = WorldgenConfig()
)

@Serializable
data class GeneralConfig(
    @field:ConfigComment("启动/重载时是否把当前配置打印到日志。", "true")
    val logConfigOnLoad: Boolean = true,
    @field:ConfigComment("是否启用更新检查。", "true")
    val checkForUpdates: Boolean = true
)

@Serializable
data class RecyclerConfig(
    // Item id list, e.g. ["minecraft:stick", "ic2_120:scrap"]
    @field:ConfigComment("回收机黑名单。填写物品 id 列表，例如 minecraft:stick。", "[\"minecraft:stick\"]")
    val blacklist: List<String> = listOf("minecraft:stick")
)

@Serializable
data class NuclearConfig(
    /** 是否允许核反应堆在过热时爆炸 */
    @field:ConfigComment("是否允许核反应堆在过热时爆炸。", "true")
    val enableReactorExplosion: Boolean = true
)

@Serializable
data class UuReplicationConfig(
    /**
     * 可复制物品白名单：key 为物品 id，value 为所需 UU 物质，单位 uB。
     * 详见 [UuReplicationDefaults] 了解定价原则。
     */
    @field:ConfigComment("UU 复制白名单。key=物品 id，value=所需 UU 物质（uB）。")
    val replicationWhitelist: Map<String, Int> = UuReplicationDefaults.defaultWhitelist
)

@Serializable
data class MinerConfig(
    @field:ConfigComment(
        "采矿机额外可挖方块 id 列表。默认矿石通过名称自动匹配（含 ore），此列表用于添加特殊方块或者别的mod的方块。",
        "[]"
    )
    val additionalMineableBlocks: List<String> = emptyList()
)

private val DEFAULT_RUBBER_TREE_BIOMES = listOf(
    "minecraft:forest",
    "minecraft:flower_forest",
    "minecraft:birch_forest",
    "minecraft:dark_forest",
    "minecraft:taiga",
    "minecraft:old_growth_pine_taiga",
    "minecraft:old_growth_spruce_taiga",
    "minecraft:jungle",
    "minecraft:sparse_jungle",
    "minecraft:bamboo_jungle",
    "minecraft:swamp"
)

@Serializable
data class WorldgenConfig(
    @field:ConfigComment("橡胶树世界生成配置。enabled/biomes 变更后需要重启；其余多数参数 /ic2config reload 后影响未来生成。")
    val rubberTree: RubberTreeWorldgenConfig = RubberTreeWorldgenConfig()
)

@Serializable
data class RubberTreeWorldgenConfig(
    /** 是否允许自然生成橡胶树。 */
    @field:ConfigComment("是否允许自然生成橡胶树。", "true")
    val enabled: Boolean = true,
    /** 允许生成橡胶树的生物群系列表。 */
    @field:ConfigComment(
        "允许生成橡胶树的生物群系列表。填写 biome id，例如 minecraft:forest。",
        "[\"minecraft:forest\", \"minecraft:flower_forest\", \"minecraft:birch_forest\", \"minecraft:dark_forest\", \"minecraft:taiga\", \"minecraft:old_growth_pine_taiga\", \"minecraft:old_growth_spruce_taiga\", \"minecraft:jungle\", \"minecraft:sparse_jungle\", \"minecraft:bamboo_jungle\", \"minecraft:swamp\"]"
    )
    val biomes: List<String> = DEFAULT_RUBBER_TREE_BIOMES,
    /** 每个区块内尝试几次橡胶树放置。 */
    @field:ConfigComment("每个区块进行几次橡胶树放置尝试。", "1")
    val countPerChunk: Int = 1,
    /** 每次尝试的稀有度，64 表示平均 1/64 概率通过。 */
    @field:ConfigComment("每次尝试的稀有度。64 表示每次尝试平均 1/64 概率通过。", "64")
    val rarityChance: Int = 64,
    /** 允许的地表水深，0 表示不允许生成在水面上。 */
    @field:ConfigComment("允许生成时的最大地表水深。0 表示不允许刷在水面上。", "0")
    val maxWaterDepth: Int = 0,
    /** 树干基础高度。 */
    @field:ConfigComment("树干基础高度。", "6")
    val baseHeight: Int = 6,
    /** 树干第一段随机高度。 */
    @field:ConfigComment("树干第一段随机高度。", "2")
    val heightRandA: Int = 2,
    /** 树干第二段随机高度。 */
    @field:ConfigComment("树干第二段随机高度。", "0")
    val heightRandB: Int = 0,
    /** 树冠半径。 */
    @field:ConfigComment("树冠半径。", "2")
    val foliageRadius: Int = 2,
    /** 树冠偏移。 */
    @field:ConfigComment("树冠相对树干顶端的垂直偏移。", "0")
    val foliageOffset: Int = 0,
    /** 树冠高度。 */
    @field:ConfigComment("树冠高度。", "4")
    val foliageHeight: Int = 4,
    /** 每根自然橡胶原木生成 0 个湿孔的权重。默认 11。 */
    @field:ConfigComment("每根自然橡胶原木生成 0 个湿橡胶孔的权重。", "11")
    val zeroHoleWeight: Int = 11,
    /** 每根自然橡胶原木生成 1 个湿孔的权重。默认 2。 */
    @field:ConfigComment("每根自然橡胶原木生成 1 个湿橡胶孔的权重。", "2")
    val singleHoleWeight: Int = 2,
    /** 每根自然橡胶原木生成 2 个湿孔的权重。默认 1。 */
    @field:ConfigComment("每根自然橡胶原木生成 2 个湿橡胶孔的权重。", "1")
    val doubleHoleWeight: Int = 1,
    /** 是否忽略藤蔓阻挡。 */
    @field:ConfigComment("生成时是否忽略藤蔓阻挡。", "true")
    val ignoreVines: Boolean = true,
    /** 是否强制把树底替换成 dirt provider。 */
    @field:ConfigComment("是否强制把树底替换成 dirt provider。", "false")
    val forceDirt: Boolean = false
) {
    /**
     * 统一在读取侧做约束，避免配置写错后在世界生成阶段抛异常。
     */
    fun normalized(): RubberTreeWorldgenConfig = copy(
        biomes = biomes.map(String::trim).filter(String::isNotEmpty).distinct(),
        countPerChunk = countPerChunk.coerceIn(0, 256),
        rarityChance = rarityChance.coerceAtLeast(1),
        maxWaterDepth = maxWaterDepth.coerceAtLeast(0),
        baseHeight = baseHeight.coerceIn(0, 32),
        heightRandA = heightRandA.coerceIn(0, 24),
        heightRandB = heightRandB.coerceIn(0, 24),
        foliageRadius = foliageRadius.coerceIn(0, 8),
        foliageOffset = foliageOffset.coerceIn(-8, 8),
        foliageHeight = foliageHeight.coerceIn(1, 16),
        zeroHoleWeight = zeroHoleWeight.coerceAtLeast(0),
        singleHoleWeight = singleHoleWeight.coerceAtLeast(0),
        doubleHoleWeight = doubleHoleWeight.coerceAtLeast(0)
    )
}

private val DEFAULT_CONFIG_TEMPLATE = Ic2MainConfig(
    general = GeneralConfig(
        logConfigOnLoad = true,
        checkForUpdates = true
    ),
    recycler = RecyclerConfig(
        blacklist = listOf("minecraft:stick")
    ),
    nuclear = NuclearConfig(
        enableReactorExplosion = true
    ),
    uuReplication = UuReplicationConfig(
        replicationWhitelist = UuReplicationDefaults.defaultWhitelist
    ),
    miner = MinerConfig(),
    worldgen = WorldgenConfig(
        rubberTree = RubberTreeWorldgenConfig()
    )
)

object Ic2Config {
    private val logger = LoggerFactory.getLogger("${Ic2_120.MOD_ID}/config")
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
    private val configPath: Path by lazy {
        FabricLoader.getInstance().configDir.resolve("${Ic2_120.MOD_ID}.json")
    }

    @Volatile
    var current: Ic2MainConfig = DEFAULT_CONFIG_TEMPLATE
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
        return json.encodeToString(current)
    }

    fun getReplicationCostUb(itemId: String): Int? {
        val normalized = itemId.trim()
        if (normalized.isEmpty()) return null
        return current.uuReplication.replicationWhitelist[normalized]
            ?.takeIf { it > 0 }
    }

    fun getReplicationTemplate(itemId: String): UuTemplateEntry? {
        val cost = getReplicationCostUb(itemId) ?: return null
        return UuTemplateEntry(itemId, cost)
    }

    private fun readOrCreateDefault(): Ic2MainConfig {
        if (!Files.exists(configPath)) {
            writeDefaultConfig(configPath)
            return DEFAULT_CONFIG_TEMPLATE
        }

        return try {
            val raw = Files.readString(configPath, StandardCharsets.UTF_8)
            val config = json.decodeFromString<Ic2MainConfig>(raw)
            val parsedRoot = json.parseToJsonElement(raw).jsonObject
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

    private fun shouldRewriteConfig(root: JsonObject, config: Ic2MainConfig): Boolean {
        if (root.containsKey("creative")) return true
        return !containsAllExpectedKeys(root, buildCommentedConfigJson(config))
    }

    private fun encodeConfigWithComments(config: Ic2MainConfig): String =
        json.encodeToString(JsonObject.serializer(), buildCommentedConfigJson(config))

    private fun buildCommentedConfigJson(config: Ic2MainConfig): JsonObject =
        buildCommentedObject(
            instance = config,
            jsonObject = json.encodeToJsonElement(Ic2MainConfig.serializer(), config).jsonObject,
            rootComment = "配置文件允许保留这些 _comment_* 说明字段；程序读取时会自动忽略它们。"
        )

    private fun buildCommentedObject(
        instance: Any,
        jsonObject: JsonObject,
        rootComment: String? = null
    ): JsonObject = buildJsonObject {
        if (rootComment != null) {
            put("_comment", JsonPrimitive(rootComment))
        }

        declaredConfigFields(instance.javaClass).forEach { field ->
            field.isAccessible = true
            val fieldName = field.name
            val valueElement = jsonObject[fieldName] ?: return@forEach
            field.getAnnotation(ConfigComment::class.java)?.let { annotation ->
                put("_comment_$fieldName", JsonPrimitive(formatComment(annotation)))
            }

            val fieldValue = field.get(instance)
            val isNestedConfigObject =
                fieldValue != null &&
                    valueElement is JsonObject &&
                    !Map::class.java.isAssignableFrom(field.type)

            if (isNestedConfigObject) {
                put(fieldName, buildCommentedObject(fieldValue!!, valueElement.jsonObject))
            } else {
                put(fieldName, valueElement)
            }
        }
    }

    private fun containsAllExpectedKeys(actual: JsonObject, expected: JsonObject): Boolean =
        expected.all { (key, expectedValue) ->
            val actualValue = actual[key] ?: return@all false
            if (expectedValue is JsonObject && actualValue is JsonObject) {
                containsAllExpectedKeys(actualValue, expectedValue)
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
        if (!current.general.logConfigOnLoad) return
        logger.info(
            "Config {}:\n{}",
            action,
            prettyCurrentConfig()
        )
    }
}
