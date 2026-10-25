package ic2_120.client

import ic2_120.Ic2_120
import ic2_120.content.block.TransformerBlock
import ic2_120.content.block.machines.TransformerBlockEntity
import ic2_120.content.sync.TransformerSync
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.phys.BlockHitResult

object TransformerTooltipConfig {
    private var enabled = false

    @JvmStatic fun setEnabled(value: Boolean) { enabled = value }
    @JvmStatic fun isEnabled(): Boolean = enabled
    @JvmStatic fun toggle() { enabled = !enabled }

    fun register() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "transformer_mode")) { graphics, _ ->
            if (!enabled) return@addLast
            val client = Minecraft.getInstance()
            if (client.debugOverlay.showDebugScreen()) return@addLast
            val hit = client.hitResult as? BlockHitResult ?: return@addLast
            val world = client.level ?: return@addLast
            if (world.getBlockState(hit.blockPos).block !is TransformerBlock) return@addLast
            val entity = world.getBlockEntity(hit.blockPos) as? TransformerBlockEntity ?: return@addLast
            val mode = if (entity.getSyncedMode() == TransformerSync.Mode.STEP_UP)
                Component.literal("降压模式 (高→低)").withStyle(ChatFormatting.RED)
            else Component.literal("升压模式 (低→高)").withStyle(ChatFormatting.GREEN)
            val x = (graphics.guiWidth() - client.font.width(mode)) / 2
            graphics.text(client.font, mode, x, graphics.guiHeight() / 2 - 20, -1, true)
        }
    }
}
