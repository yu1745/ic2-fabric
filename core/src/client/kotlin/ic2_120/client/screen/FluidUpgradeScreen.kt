package ic2_120.client.screen

import ic2_120.client.t
import ic2_120.content.screen.FluidUpgradeScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.Direction

@ModScreen(handler = "fluid_upgrade")
class FluidUpgradeScreen(
    handler: FluidUpgradeScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<FluidUpgradeScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = 176
        backgroundHeight = 166
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, backgroundWidth, backgroundHeight, TEX_SIZE, TEX_SIZE)
    }

    override fun init() {
        super.init()
        val client = client ?: return

        addDrawableChild(ButtonWidget.builder(Text.empty()) {
            client.networkHandler?.sendPacket(ButtonClickC2SPacket(handler.syncId, FluidUpgradeScreenHandler.BUTTON_SET_FILTER))
        }.dimensions(x + 27, y + 37, 20, 14).build())

        addDrawableChild(ButtonWidget.builder(Text.empty()) {
            client.networkHandler?.sendPacket(ButtonClickC2SPacket(handler.syncId, FluidUpgradeScreenHandler.BUTTON_CYCLE_DIRECTION))
        }.dimensions(x + 108, y + 60, 20, 12).build())
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        context.drawText(textRenderer, title, x + (backgroundWidth - textRenderer.getWidth(title)) / 2, y + 6, 0x404040, false)

        // 流体限制文本 (9,18)-(104,32)
        val filterName = if (handler.fluidRawId > 0) {
            val fluid = Registries.FLUID.get(handler.fluidRawId)
            fluid.defaultState.blockState.block.name.string
        } else {
            t("gui.ic2_120.fluid_upgrade.no_filter")
        }
        val filterText = t("gui.ic2_120.fluid_upgrade.filter_display", filterName)
        context.drawText(textRenderer, filterText, x + 9, y + 18 + (14 - textRenderer.fontHeight) / 2, 0x55FF55, false)

        // 方向文本 (9,59)-(104,73)
        val dirText = buildActiveDirectionsText()
        context.drawText(textRenderer, dirText, x + 9, y + 59 + (14 - textRenderer.fontHeight) / 2, 0x55FF55, false)

        // 7px 按钮文字覆盖
        draw7pxText(context, x + 27, y + 37, 20, 14, t("gui.ic2_120.fluid_upgrade.set_filter"))
        draw7pxText(context, x + 108, y + 60, 20, 12, t("gui.ic2_120.fluid_upgrade.cycle_direction_short"))

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun buildActiveDirectionsText(): String {
        val activeDirs = Direction.entries.filter { handler.isDirectionActive(it.ordinal) }
        val dirLabel = if (activeDirs.isEmpty()) {
            t("gui.ic2_120.fluid_upgrade.any_direction")
        } else {
            activeDirs.joinToString(", ") { t("gui.ic2_120.direction.${it.name.lowercase()}") }
        }
        return t("gui.ic2_120.fluid_upgrade.direction", dirLabel)
    }

    private fun draw7pxText(context: DrawContext, bx: Int, by: Int, bw: Int, bh: Int, text: String) {
        val scale = 7f / textRenderer.fontHeight
        val textW = textRenderer.getWidth(text)
        val scaledW = textW * scale
        val textX = bx + (bw - scaledW) / 2f
        val textY = by + (bh - 7f) / 2f
        context.matrices.push()
        context.matrices.translate(textX.toDouble(), textY.toDouble(), 0.0)
        context.matrices.scale(scale, scale, 1f)
        context.drawText(textRenderer, text, 0, 0, 0xFFFFFFFF.toInt(), false)
        context.matrices.pop()
    }

    companion object {
        private val TEXTURE = Identifier("ic2", "textures/gui/guiupgrade.png")
        private const val TEX_SIZE = 256
    }
}
