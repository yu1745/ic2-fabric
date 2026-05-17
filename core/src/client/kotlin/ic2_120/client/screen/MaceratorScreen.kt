package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.t
import ic2_120.content.block.MaceratorBlock
import ic2_120.content.screen.MaceratorScreenHandler
import ic2_120.content.sync.MaceratorSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = MaceratorBlock::class)
class MaceratorScreen(
    handler: MaceratorScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<MaceratorScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = 176
        backgroundHeight = 166
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 1f, 1f, 176, 166, 256, 256)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)

        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1)
        val energyFrac = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val progressFrac = if (MaceratorSync.PROGRESS_MAX > 0) {
            (handler.sync.progress.coerceIn(0, MaceratorSync.PROGRESS_MAX)
                .toFloat() / MaceratorSync.PROGRESS_MAX).coerceIn(0f, 1f)
        } else 0f
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val consumeRate = handler.sync.getSyncedConsumedAmount()

        // Energy gauge at (59, 37) — 14×14 bolt-style
        drawEnergyGauge(context, x + 59, y + 37, energyFrac)

        // Progress gauge at (75, 38) — 21×11 texture from scrapboxrecipes.png (177,1)-(197,11)
        drawProgressGauge(context, x + 75, y + 38, progressFrac)

        // Title text centered at y=6
        context.drawText(textRenderer, title, x + (176 - textRenderer.getWidth(title)) / 2, y + 6, 0x404040, false)

        // EU input/consume text on the left side
        val inputText = t("gui.ic2_120.input_eu", EnergyFormatUtils.formatEu(inputRate))
        val consumeText = t("gui.ic2_120.consume_eu", EnergyFormatUtils.formatEu(consumeRate))
        val sideTextWidth = maxOf(textRenderer.getWidth(inputText), textRenderer.getWidth(consumeText))
        val sideTextX = x - sideTextWidth - 4
        context.drawText(textRenderer, inputText, sideTextX, y + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, consumeText, sideTextX, y + 20, 0xAAAAAA, false)

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun drawEnergyGauge(context: DrawContext, gx: Int, gy: Int, fraction: Float) {
        val barW = 14
        val barH = 14
        val clamped = fraction.coerceIn(0f, 1f)
        context.fill(gx, gy, gx + barW, gy + barH, 0xFF333333.toInt())
        val fillH = (clamped * barH).toInt()
        if (fillH > 0) {
            val color = lerpColor(0xFFCC0000.toInt(), 0xFF00CC00.toInt(), clamped)
            context.fill(gx, gy + barH - fillH, gx + barW, gy + barH, color)
        }
        context.drawBorder(gx, gy, barW, barH, 0xFF888888.toInt())
    }

    private fun drawProgressGauge(context: DrawContext, gx: Int, gy: Int, fraction: Float) {
        val barW = 21
        val barH = 11
        val fillW = (fraction.coerceIn(0f, 1f) * barW).toInt()
        if (fillW <= 0) return
        context.enableScissor(gx, gy, gx + fillW, gy + barH)
        context.drawTexture(TEXTURE, gx, gy, 177f, 1f, barW, barH, 256, 256)
        context.disableScissor()
    }

    private fun lerpColor(a: Int, b: Int, t: Float): Int {
        val aa = (a shr 24) and 0xFF
        val ar = (a shr 16) and 0xFF
        val ag = (a shr 8) and 0xFF
        val ab = a and 0xFF
        val ba = (b shr 24) and 0xFF
        val br = (b shr 16) and 0xFF
        val bg = (b shr 8) and 0xFF
        val bb = b and 0xFF
        val u = t.coerceIn(0f, 1f)
        return ((aa + (ba - aa) * u).toInt() and 0xFF shl 24) or
            ((ar + (br - ar) * u).toInt() and 0xFF shl 16) or
            ((ag + (bg - ag) * u).toInt() and 0xFF shl 8) or
            ((ab + (bb - ab) * u).toInt() and 0xFF)
    }

    companion object {
        private val TEXTURE = Identifier("ic2", "textures/gui/scrapboxrecipes.png")
    }
}
