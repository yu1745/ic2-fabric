package ic2_120.client.screen

import ic2_120.content.block.WindKineticGeneratorBlock
import ic2_120.content.screen.WindKineticGeneratorScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Inventory
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

@ModScreen(block = WindKineticGeneratorBlock::class)
class WindKineticGeneratorScreen(
    val handler: WindKineticGeneratorScreenHandler,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<WindKineticGeneratorScreenHandler>(handler, playerInventory, title) {

    init {
        imageWidth = 175
        imageHeight = 166
    }

    override fun renderBg(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.blit(TEXTURE, leftPos, topPos, 0f, 0f, 175, 166, 256, 256)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)

        val outputKu = menu.sync.outputKu.coerceAtLeast(0)
        val generatedKu = menu.sync.generatedKu.coerceAtLeast(0)
        val blocked = menu.sync.isStuck != 0
        val windInsufficient = generatedKu == 0 && !blocked
        val hasRotor = menu.slots.isNotEmpty() && menu.slots[0] .hasItem()

        // 标题居中于 y=6
        context.drawString(font, title, leftPos + (175 - font.width(title)) / 2, topPos + 6, 0x404040, false)

        // 警告状态纹理：(180,4)-(209,28) = 29×24，阻挡或风力不足时绘制
        if (blocked || windInsufficient) {
            context.blit(TEXTURE, leftPos + 44, topPos + 18, 179f, 4f, 31, 24, 256, 256)
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

        renderTooltip(context, mouseX, mouseY)
    }

    private fun drawScaledText(context: GuiGraphics, text: String, rx: Int, ry: Int, rw: Int) {
        val scale = 7f / font.lineHeight
        val scaledWidth = (font.width(text) * scale).toInt()
        val scaledHeight = (font.lineHeight * scale).toInt()
        val textX = leftPos + rx + (rw - scaledWidth) / 2
        val textY = topPos + ry + (14 - scaledHeight) / 2
        context.pose().pushPose()
        context.pose().translate(textX.toDouble(), textY.toDouble(), 0.0)
        context.pose().scale(scale, scale, 1.0f)
        context.drawString(font, text, 0, 0, TEXT_COLOR, false)
        context.pose().popPose()
    }

    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath("ic2", "textures/gui/guiwindkineticgenerator.png")
        private val TEXT_COLOR = 0xFF90EE90.toInt()
    }
}
