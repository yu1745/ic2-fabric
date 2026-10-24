package ic2_120.client.screen

import ic2_120.content.block.WindKineticGeneratorBlock
import ic2_120.content.screen.WindKineticGeneratorScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = WindKineticGeneratorBlock::class)
class WindKineticGeneratorScreen(
    handler: WindKineticGeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<WindKineticGeneratorScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = 175
        backgroundHeight = 166
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, 175, 166, 256, 256)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        val outputKu = handler.sync.outputKu.coerceAtLeast(0)
        val generatedKu = handler.sync.generatedKu.coerceAtLeast(0)
        val blocked = handler.sync.isStuck != 0
        val windInsufficient = generatedKu == 0 && !blocked
        val hasRotor = handler.slots.isNotEmpty() && handler.slots[0].hasStack()

        // 标题居中于 y=6
        context.drawText(textRenderer, title, x + (175 - textRenderer.getWidth(title)) / 2, y + 6, 0x404040, false)

        // 警告状态纹理：(180,4)-(209,28) = 29×24，阻挡或风力不足时绘制
        if (blocked || windInsufficient) {
            context.drawTexture(TEXTURE, x + 44, y + 18, 179f, 4f, 31, 24, 256, 256)
        }

        // 文字1：区域 (16,47)-(160,61)，动能输出
        drawScaledText(context, "动能输出：${outputKu} KU/t", 16, 47, 144)

        // 文字2：区域 (16,65)-(160,79)，转子/状态
        val statusText2 = if (!hasRotor) {
            "需放入转子进行工作"
        } else {
            val state = when {
                blocked -> "阻挡"
                windInsufficient -> "风力不足"
                else -> "正常"
            }
            "工作状态：${state}"
        }
        drawScaledText(context, statusText2, 16, 65, 144)

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun drawScaledText(context: DrawContext, text: String, rx: Int, ry: Int, rw: Int) {
        val scale = 7f / textRenderer.fontHeight
        val scaledWidth = (textRenderer.getWidth(text) * scale).toInt()
        val scaledHeight = (textRenderer.fontHeight * scale).toInt()
        val textX = x + rx + (rw - scaledWidth) / 2
        val textY = y + ry + (14 - scaledHeight) / 2
        context.matrices.push()
        context.matrices.translate(textX.toDouble(), textY.toDouble(), 0.0)
        context.matrices.scale(scale, scale, 1.0f)
        context.drawText(textRenderer, text, 0, 0, TEXT_COLOR, false)
        context.matrices.pop()
    }

    companion object {
        private val TEXTURE = Identifier.of("ic2", "textures/gui/guiwindkineticgenerator.png")
        private val TEXT_COLOR = 0xFF90EE90.toInt()
    }
}
