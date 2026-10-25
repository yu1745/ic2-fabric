package ic2_120.client

import ic2_120.compat.displayClientMessage

import ic2_120.Ic2_120
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import ic2_120.client.compat.*
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import org.slf4j.LoggerFactory

object SodiumCompatibilityWarning {
    private const val SODIUM_MOD_ID = "sodium"
    private const val INDIUM_MOD_ID = "indium"
    private const val WARNING_DURATION_MS = 6000L
    private const val WARNING_SCALE = 2.0f
    private const val WARNING_COLOR = -0xAB

    private val logger = LoggerFactory.getLogger("${Ic2_120.MOD_ID}/SodiumCompatibility")
    private var warningUntilMs: Long = 0L

    fun register() {
        ClientPlayConnectionEvents.JOIN.register(ClientPlayConnectionEvents.Join { _, _, client ->
            if (!shouldWarn()) return@Join
            client.execute {
                warnPlayer(client)
            }
        })
        HudElementRegistry.addLast(net.minecraft.resources.Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "sodium_warning")) { drawContext, _ ->
            renderWarning(drawContext, Minecraft.getInstance())
        }
    }

    private fun shouldWarn(): Boolean {
        val loader = FabricLoader.getInstance()
        if (!loader.isModLoaded(SODIUM_MOD_ID) || loader.isModLoaded(INDIUM_MOD_ID)) {
            return false
        }

        val sodiumVersion = loader.getModContainer(SODIUM_MOD_ID)
            .map { it.metadata.version.friendlyString }
            .orElse("")

        return sodiumRequiresIndium(sodiumVersion)
    }

    private fun sodiumRequiresIndium(version: String): Boolean {
        val match = Regex("""(\d+)\.(\d+)""").find(version) ?: return true
        val major = match.groupValues[1].toIntOrNull() ?: return true
        val minor = match.groupValues[2].toIntOrNull() ?: return true
        return major == 0 && minor < 6
    }

    private fun warnPlayer(client: Minecraft) {
        val player = client.player ?: return
        val warningText = Component.translatable("message.ic2_120.sodium_without_indium")
        logger.warn(
            "Detected Sodium without Indium. Rubber log rendering may be broken on this client. " +
                "Install Indium for Sodium versions below 0.6."
        )
        warningUntilMs = System.currentTimeMillis() + WARNING_DURATION_MS
        player.displayClientMessage(
            warningText,
            false
        )
    }

    private fun renderWarning(drawContext: GuiGraphics, client: Minecraft) {
        if (System.currentTimeMillis() >= warningUntilMs) return
        if (client.player == null) return

        val textRenderer = client.font
        val warningText = Component.translatable("message.ic2_120.sodium_without_indium")
        val warningLines = warningText.string.split('\n')
        val matrices = drawContext.pose()
        val scaledWidth = client.getWindow().guiScaledWidth
        val scaledHeight = client.getWindow().guiScaledHeight
        val scaledCenterX = scaledWidth / (2.0f * WARNING_SCALE)
        val scaledCenterY = scaledHeight / (2.0f * WARNING_SCALE)
        val drawX = scaledCenterX.toInt()
        val totalTextHeight = warningLines.size * textRenderer.lineHeight
        val startY = (scaledCenterY - totalTextHeight / 2.0f).toInt()

        matrices.pushPose()
        matrices.scale(WARNING_SCALE, WARNING_SCALE, 1.0f)
        warningLines.forEachIndexed { index, line ->
            drawContext.drawCenteredString(
                textRenderer,
                line,
                drawX,
                startY + index * textRenderer.lineHeight,
                WARNING_COLOR
            )
        }
        matrices.popPose()
    }
}
