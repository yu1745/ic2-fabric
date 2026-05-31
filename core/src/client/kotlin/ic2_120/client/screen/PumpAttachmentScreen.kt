package ic2_120.client.screen

import ic2_120.client.t
import ic2_120.content.screen.PumpAttachmentScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(handlers = ["bronze_pump_attachment", "carbon_pump_attachment"])
class PumpAttachmentScreen(
    handler: PumpAttachmentScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<PumpAttachmentScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = 176
        backgroundHeight = 162
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        // 主 PNG 背景
        context.drawTexture(TEXTURE, x, y, 0f, 0f, backgroundWidth, backgroundHeight, TEXTURE_SIZE, TEXTURE_SIZE)
        // 过滤器槽背景 (179,3)-(197,21) = 18×18，渲染至 (79,30)
        context.drawTexture(
            TEXTURE, x + SLOT_BG_X, y + SLOT_BG_Y,
            SLOT_BG_U.toFloat(), SLOT_BG_V.toFloat(),
            SLOT_BG_SIZE, SLOT_BG_SIZE,
            TEXTURE_SIZE, TEXTURE_SIZE
        )
        // 文本区域背景 (3,165)-(80,180) = 77×15，渲染至 (52,55)
        context.drawTexture(
            TEXTURE, x + TEXT_AREA_X, y + TEXT_AREA_Y,
            TEXT_AREA_U.toFloat(), TEXT_AREA_V.toFloat(),
            TEXT_AREA_W, TEXT_AREA_H,
            TEXTURE_SIZE, TEXTURE_SIZE
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)

        // 标题居中
        context.drawText(textRenderer, title, x + (backgroundWidth - textRenderer.getWidth(title)) / 2, y + 6, 0x404040, false)

        // 过滤器状态文本，7px，居中于文本区域 (52,55)-(129,70)
        val stack = handler.slots[0].stack
        val line = if (stack.isEmpty) t("gui.ic2_120.pump_attachment.filter_any") else t("gui.ic2_120.pump_attachment.filter_current", stack.name.string)
        val textScale = 7f / textRenderer.fontHeight
        val scaledW = (textRenderer.getWidth(line) * textScale).toInt()
        val textX = x + TEXT_AREA_X + (TEXT_AREA_W - scaledW) / 2
        val textCY = y + TEXT_AREA_Y + TEXT_AREA_H / 2
        context.matrices.push()
        context.matrices.translate(textX.toDouble(), (textCY - 3.5), 0.0)
        context.matrices.scale(textScale, textScale, 1f)
        context.drawText(textRenderer, line, 0, 0, 0xFF20EE7E.toInt(), false)
        context.matrices.pop()

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    companion object {
        private val TEXTURE = Identifier.of("ic2", "textures/gui/guiother.png")
        private const val TEXTURE_SIZE = 256

        // 过滤器槽背景 (179,3)-(197,21) = 18×18
        private const val SLOT_BG_U = 179
        private const val SLOT_BG_V = 3
        private const val SLOT_BG_SIZE = 18
        private const val SLOT_BG_X = 79
        private const val SLOT_BG_Y = 30

        // 文本区域 (3,165)-(80,180) = 77×15
        private const val TEXT_AREA_U = 3
        private const val TEXT_AREA_V = 165
        private const val TEXT_AREA_W = 77
        private const val TEXT_AREA_H = 15
        private const val TEXT_AREA_X = 52
        private const val TEXT_AREA_Y = 55
    }
}
