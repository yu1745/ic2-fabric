package ic2_120.config

import ic2_120.Ic2_120
import ic2_120.content.uu.UuTemplateEntry
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory

@Serializable
data class Ic2MainConfig(
    val general: GeneralConfig = GeneralConfig(),
    val recycler: RecyclerConfig = RecyclerConfig(),
    val nuclear: NuclearConfig = NuclearConfig(),
    val uuReplication: UuReplicationConfig = UuReplicationConfig()
)

@Serializable
data class GeneralConfig(
    val logConfigOnLoad: Boolean = true,
    val checkForUpdates: Boolean = true
)

@Serializable
data class RecyclerConfig(
    // Item id list, e.g. ["minecraft:stick", "ic2_120:scrap"]
    val blacklist: List<String> = listOf("minecraft:stick")
)

@Serializable
data class NuclearConfig(
    /** 是否允许核反应堆在过热时爆炸 */
    val enableReactorExplosion: Boolean = true
)

@Serializable
data class UuReplicationConfig(
    /**
     * 可复制物品白名单：key 为物品 id，value 为所需 UU 物质，单位 uB。
     * 详见 [UuReplicationDefaults] 了解定价原则。
     */
    val replicationWhitelist: Map<String, Int> = UuReplicationDefaults.defaultWhitelist
)

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
            val hasLegacyCreativeSection = json.parseToJsonElement(raw).jsonObject.containsKey("creative")
            if (hasLegacyCreativeSection) {
                Files.writeString(configPath, json.encodeToString(config), StandardCharsets.UTF_8)
            }
            config
        } catch (e: Exception) {
            throw IllegalStateException("Failed to parse config: $configPath", e)
        }
    }

    private fun writeDefaultConfig(path: Path) {
        Files.createDirectories(path.parent)
        Files.writeString(path, json.encodeToString(DEFAULT_CONFIG_TEMPLATE), StandardCharsets.UTF_8)
    }

    private fun defaultConfigText(): String {
        return json.encodeToString(DEFAULT_CONFIG_TEMPLATE)
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
