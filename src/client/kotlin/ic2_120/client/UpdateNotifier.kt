package ic2_120.client

import ic2_120.Ic2_120
import ic2_120.config.Ic2Config
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.slf4j.LoggerFactory

@Serializable
private data class BuildInfo(
    val ciRunNumber: Int = 0,
    val repositoryOwner: String = "",
    val repositoryName: String = ""
)

@Serializable
private data class LatestReleaseResponse(
    @SerialName("tag_name")
    val tagName: String,
    @SerialName("html_url")
    val htmlUrl: String? = null
)

private data class RemoteRelease(
    val tagName: String,
    val runNumber: Int,
    val htmlUrl: String?
)

object UpdateNotifier {
    private val logger = LoggerFactory.getLogger("${Ic2_120.MOD_ID}/update")
    private val hasScheduledCheck = AtomicBoolean(false)
    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()
    private val localBuildInfo: BuildInfo by lazy(::loadBuildInfo)
    private val modVersion: String by lazy {
        FabricLoader.getInstance()
            .getModContainer(Ic2_120.MOD_ID)
            .map { it.metadata.version.friendlyString }
            .orElse("dev")
    }
    private val releaseTagRegex = Regex("^release-(\\d+)$")
    private const val RETRY_DELAY_SECONDS = 30L

    fun register() {
        ClientPlayConnectionEvents.JOIN.register(ClientPlayConnectionEvents.Join { _, _, client ->
            if (!Ic2Config.current.general.checkForUpdates) return@Join
            val buildInfo = localBuildInfo
            if (buildInfo.ciRunNumber <= 0) return@Join
            if (buildInfo.repositoryOwner.isBlank() || buildInfo.repositoryName.isBlank()) return@Join
            if (!hasScheduledCheck.compareAndSet(false, true)) return@Join
            checkForUpdatesAsync(client, buildInfo)
        })
    }

    private fun checkForUpdatesAsync(client: MinecraftClient, buildInfo: BuildInfo) {
        CompletableFuture
            .supplyAsync { fetchLatestRelease(buildInfo) }
            .thenAccept { remoteRelease ->
                if (remoteRelease == null || remoteRelease.runNumber <= buildInfo.ciRunNumber) {
                    return@thenAccept
                }
                client.execute {
                    showUpdateMessage(client, remoteRelease, buildInfo.ciRunNumber)
                }
            }
            .exceptionally { throwable ->
                logger.warn(
                    "Failed to check updates from GitHub, retrying in {} seconds",
                    RETRY_DELAY_SECONDS,
                    throwable
                )
                scheduleRetry(client, buildInfo)
                null
            }
    }

    private fun scheduleRetry(client: MinecraftClient, buildInfo: BuildInfo) {
        CompletableFuture.delayedExecutor(RETRY_DELAY_SECONDS, TimeUnit.SECONDS).execute {
            checkForUpdatesAsync(client, buildInfo)
        }
    }

    private fun fetchLatestRelease(buildInfo: BuildInfo): RemoteRelease? {
        val repository = "${buildInfo.repositoryOwner}/${buildInfo.repositoryName}"
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.github.com/repos/$repository/releases/latest"))
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "${Ic2_120.MOD_ID}/$modVersion")
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
        if (response.statusCode() != 200) {
            logger.debug("Skipping update prompt, GitHub API returned {}", response.statusCode())
            return null
        }

        val payload = json.decodeFromString<LatestReleaseResponse>(response.body())
        val match = releaseTagRegex.matchEntire(payload.tagName)
        if (match == null) {
            logger.debug("Skipping update prompt, unsupported release tag: {}", payload.tagName)
            return null
        }

        return RemoteRelease(
            tagName = payload.tagName,
            runNumber = match.groupValues[1].toInt(),
            htmlUrl = payload.htmlUrl
        )
    }

    private fun showUpdateMessage(client: MinecraftClient, remoteRelease: RemoteRelease, localRunNumber: Int) {
        val localTag = "release-$localRunNumber"
        client.inGameHud.setOverlayMessage(
            Text.translatable("message.ic2_120.update_available.overlay", remoteRelease.tagName),
            false
        )

        val player = client.player ?: return
        if (!remoteRelease.htmlUrl.isNullOrBlank()) {
            val clickableUrl = Text.literal(remoteRelease.htmlUrl)
                .styled {
                    it.withColor(Formatting.AQUA)
                        .withUnderline(true)
                        .withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, remoteRelease.htmlUrl))
                        .withHoverEvent(
                            HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Text.translatable("message.ic2_120.update_available.link_hover")
                            )
                        )
                }
            player.sendMessage(
                Text.translatable("message.ic2_120.update_available.chat", remoteRelease.tagName, localTag)
                    .append(Text.literal(" "))
                    .append(clickableUrl),
                false
            )
            return
        }

        player.sendMessage(
            Text.translatable(
                "message.ic2_120.update_available.chat",
                remoteRelease.tagName,
                localTag
            ),
            false
        )
    }

    private fun loadBuildInfo(): BuildInfo {
        val resourcePath = "/assets/${Ic2_120.MOD_ID}/build_info.json"
        val stream = UpdateNotifier::class.java.getResourceAsStream(resourcePath) ?: return BuildInfo()
        return try {
            stream.bufferedReader(StandardCharsets.UTF_8).use { reader ->
                json.decodeFromString<BuildInfo>(reader.readText())
            }
        } catch (e: Exception) {
            logger.warn("Failed to read {}", resourcePath, e)
            BuildInfo()
        }
    }
}
