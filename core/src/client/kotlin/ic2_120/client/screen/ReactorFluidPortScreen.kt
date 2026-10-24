package ic2_120.client.screen

import ic2_120.content.block.nuclear.ReactorFluidPortBlock
import ic2_120.content.screen.ReactorFluidPortScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = ReactorFluidPortBlock::class)
class ReactorFluidPortScreen(
    handler: ReactorFluidPortScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<ReactorFluidPortScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = 176
        backgroundHeight = 162
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        // 主 PNG 背景
        context.drawTexture(TEXTURE, x, y, 0f, 0f, backgroundWidth, backgroundHeight, TEXTURE_SIZE, TEXTURE_SIZE)
        // 升级槽背景 (179,3)-(197,21) = 18×18，渲染至 (79,30) — 必须在 drawBackground 内，早于 super.render 的物品绘制
        context.drawTexture(
            TEXTURE, x + SLOT_BG_X, y + SLOT_BG_Y,
            SLOT_BG_U.toFloat(), SLOT_BG_V.toFloat(),
            SLOT_BG_SIZE, SLOT_BG_SIZE,
            TEXTURE_SIZE, TEXTURE_SIZE
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        // 标题居中
        context.drawText(textRenderer, title, x + (backgroundWidth - textRenderer.getWidth(title)) / 2, y + 6, 0x404040, false)

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    companion object {
        private val TEXTURE = Identifier("ic2", "textures/gui/guiother.png")
        private const val TEXTURE_SIZE = 256

        private const val SLOT_BG_U = 179
        private const val SLOT_BG_V = 3
        private const val SLOT_BG_SIZE = 18
        private const val SLOT_BG_X = 79
        private const val SLOT_BG_Y = 30
    }
}
