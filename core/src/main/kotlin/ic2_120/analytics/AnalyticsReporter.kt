package ic2_120.analytics

import ic2_120.Ic2_120
import ic2_120.config.Ic2Config
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory

/**
 * 匿名使用统计上报。
 *
 * 仅上报：随机 installId（持久化的 UUID）+ mod 版本 + Minecraft 版本 + 来源（server/client）。
 * 不含用户名、世界名、IP、坐标、硬件等任何可追溯信息。后端（Cloudflare Worker）也不存储 IP。
 *
 * 触发点：
 *   - 服务端启动时 [Ic2_120.onInitialize] 注册的 SERVER_STARTED → report("server")
 *   - 客户端加入世界时 [Ic2_120Client.onInitializeClient] 注册的 JOIN → report("client")
 *
 * 用户可在 config/ic2_120.json 中将 enableAnonymousStatistics 设为 false 关闭。
 * 所有网络/解析异常都被静默吞掉，绝不影响游戏。
 */
object AnalyticsReporter {
    private val logger = LoggerFactory.getLogger("${Ic2_120.MOD_ID}/analytics")

    /**
     * 内置默认上报地址（部署后可在 config 的 analyticsEndpoint 覆盖）。
     * 不可达时上报自动失败并被吞掉，不影响游戏。
     */
    private const val DEFAULT_ENDPOINT = "https://ic2-analytics.wangyu174551226.workers.dev"

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    /** 最大重试次数（含首次，共 3 次）。国内网络常不稳定。 */
    private const val MAX_ATTEMPTS = 3

    /** 重试间隔（秒）。 */
    private const val RETRY_DELAY_SECONDS = 30L

    private val idFile: Path by lazy {
        FabricLoader.getInstance().configDir.resolve(Ic2_120.MOD_ID).resolve("analytics_id.txt")
    }

    private val modVersion: String by lazy {
        FabricLoader.getInstance()
            .getModContainer(Ic2_120.MOD_ID)
            .map { it.metadata.version.friendlyString }
            .orElse("dev")
    }

    private val mcVersion: String by lazy {
        FabricLoader.getInstance()
            .getModContainer("minecraft")
            .map { it.metadata.version.friendlyString }
            .orElse("unknown")
    }

    /** 缓存的 installId，避免每次上报都读盘。 */
    @Volatile
    private var cachedId: String? = null

    /**
     * 上报一次使用。source 为 "server" 或 "client"。
     * 若配置关闭、或 endpoint 解析失败，直接 return。
     * 异步执行，绝不阻塞调用方。
     */
    fun report(source: String) {
        val config = Ic2Config.current.general
        if (!config.enableAnonymousStatistics) return

        val endpoint = config.analyticsEndpoint.ifBlank { DEFAULT_ENDPOINT }
        val parsed = runCatching { URI.create(endpoint) }.getOrNull() ?: return
        if (parsed.scheme == null || parsed.host == null) return

        val installId = getOrCreateInstallId() ?: return

        // 异步发起，带最多 MAX_ATTEMPTS 次重试（国内网络常不稳定）
        sendPingWithRetry(parsed, installId, source, attempt = 1)
    }

    /**
     * 异步发送一次 ping，失败时延迟 [RETRY_DELAY_SECONDS] 秒重试，最多 [MAX_ATTEMPTS] 次。
     * 全程在后台线程池执行，绝不阻塞游戏主线程，也不向调用方抛异常。
     */
    private fun sendPingWithRetry(endpoint: URI, installId: String, source: String, attempt: Int) {
        CompletableFuture
            .supplyAsync { sendPingOnce(endpoint, installId, source) }
            .whenComplete { _, throwable ->
                if (throwable == null) return@whenComplete // 成功
                if (attempt >= MAX_ATTEMPTS) {
                    logger.debug("Anonymous analytics ping gave up after {} attempts", MAX_ATTEMPTS, throwable)
                    return@whenComplete
                }
                logger.debug(
                    "Anonymous analytics ping attempt {}/{} failed, retrying in {}s",
                    attempt, MAX_ATTEMPTS, RETRY_DELAY_SECONDS, throwable
                )
                CompletableFuture.delayedExecutor(RETRY_DELAY_SECONDS, TimeUnit.SECONDS).execute {
                    sendPingWithRetry(endpoint, installId, source, attempt + 1)
                }
            }
    }

    private fun sendPingOnce(endpoint: URI, installId: String, source: String) {
        val body = """{"installId":"$installId","modVersion":"$modVersion","mcVersion":"$mcVersion","source":"$source"}"""
        val request = HttpRequest.newBuilder()
            .uri(endpoint.resolve("/ping"))
            .header("Content-Type", "application/json; charset=utf-8")
            .header("User-Agent", "${Ic2_120.MOD_ID}/$modVersion")
            .timeout(Duration.ofSeconds(5))
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .build()
        // 不关心响应内容，只要不抛异常即可
        httpClient.send(request, HttpResponse.BodyHandlers.discarding())
    }

    /**
     * 读取或生成持久化的匿名 installId。
     * 文件带中文说明，让用户用记事本打开就能看懂用途。
     * 读取失败时返回临时内存 UUID 兜底（不阻断上报，也不覆盖原文件）。
     */
    private fun getOrCreateInstallId(): String? {
        cachedId?.let { return it }

        val id = readIdFromFile() ?: createIdFile()
        cachedId = id
        return id
    }

    private fun readIdFromFile(): String? {
        return try {
            if (!Files.exists(idFile)) return null
            // 取最后一行非空、非注释行作为 UUID
            val lines = Files.readAllLines(idFile, StandardCharsets.UTF_8)
            val id = lines.asReversed()
                .firstOrNull { it.isNotBlank() && !it.trimStart().startsWith("#") }
                ?.trim()
                ?: return null
            // 校验是合法 UUID
            UUID.fromString(id).toString()
        } catch (e: Exception) {
            logger.debug("Failed to read analytics_id.txt, falling back to in-memory id", e)
            // 解析失败用临时 UUID 兜底，不覆盖原文件
            UUID.randomUUID().toString()
        }
    }

    private fun createIdFile(): String {
        val id = UUID.randomUUID().toString()
        try {
            Files.createDirectories(idFile.parent)
            val content = buildString {
                appendLine("# 本文件由 IC2_120 自动生成。")
                appendLine("# 这个 ID 的唯一作用：统计有多少玩家安装了本 mod。")
                appendLine("# 它是一个随机生成的匿名编号，不含任何个人信息，也无法定位到你。")
                appendLine("# 如不希望参与统计，可在 config/ic2_120.json 中将 enableAnonymousStatistics 设为 false。")
                append(id)
            }
            Files.writeString(idFile, content, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            logger.debug("Failed to write analytics_id.txt (will use in-memory id)", e)
        }
        return id
    }
}
