package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.content.block.KineticGeneratorBlock
import ic2_120.content.screen.KineticGeneratorScreenHandler
import ic2_120.content.sync.KineticGeneratorSync
import ic2_120.registry.annotation.ModScreen
import ic2_120.client.compat.*
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Inventory
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

@ModScreen(block = KineticGeneratorBlock::class)
class KineticGeneratorScreen(
    val handler: KineticGeneratorScreenHandler,
    playerInventory: Inventory,
    title: Component
) : LegacyContainerScreen26<KineticGeneratorScreenHandler>(handler, playerInventory, title, 176, 166) {

    init {
    }

    override fun renderBg(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.blit(TEXTURE, leftPos, topPos, 0f, 0f, 176, 166, 256, 256)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)

        val energy = menu.sync.energy.toLong().coerceAtLeast(0)
        val cap = KineticGeneratorSync.ENERGY_CAPACITY
        val energyFrac = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val kuIn = menu.sync.currentKu.coerceAtLeast(0)
        val euOut = menu.sync.outputEu.coerceAtLeast(0)

        // 能量条：渲染区域 (60,23)-(117,37) = 57×14，纹理区域 (179,3)-(236,17) = 57×14，宽度一致
        drawEnergyGauge(context, leftPos + 60, topPos + 23, energyFrac)

        // 标题居中于 y=6
        context.drawString(font, title, leftPos + (176 - font.width(title)) / 2, topPos + 6, 0x404040, false)

        // 输入/输出文本：区域 (41,49)-(138,63)，居中常显，缩放至 7px
        val infoText = "输入：${kuIn} KU/t  输出：${EnergyFormatUtils.formatRaw(euOut.toLong())} EU/t"
        val scale = 6f / font.lineHeight
        val scaledWidth = (font.width(infoText) * scale).toInt()
        val scaledHeight = (font.lineHeight * scale).toInt()
        val textX = leftPos + 41 + (97 - scaledWidth) / 2
        val textY = topPos + 49 + (14 - scaledHeight) / 2
        context.pose().pushPose()
        context.pose().translate(textX.toDouble(), textY.toDouble(), 0.0)
        context.pose().scale(scale, scale, 1.0f)
        context.drawString(font, infoText, 0, 0, 0xFFADD8E6.toInt(), false)
        context.pose().popPose()

        // 能量条悬停 (60,23)-(117,37) = 57×14
        if (mouseX in leftPos + 60 until leftPos + 117 && mouseY in topPos + 23 until topPos + 37) {
            context.renderTooltip(
                font,
                Component.literal("储能：${EnergyFormatUtils.formatRaw(energy)} / ${EnergyFormatUtils.formatRaw(cap)} EU"),
                mouseX, mouseY
            )
        }

        renderTooltip(context, mouseX, mouseY)
    }

    /**
     * 平铺渲染能量条：纹理区域 (179,3)-(236,17) 从左侧根据 [fraction] 比例填充。
     */
    private fun drawEnergyGauge(context: GuiGraphics, gx: Int, gy: Int, fraction: Float) {
        val barW = 57
        val barH = 14
        val fillW = (fraction.coerceIn(0f, 1f) * barW).toInt()
        if (fillW <= 0) return
        context.enableScissor(gx, gy, gx + fillW, gy + barH)
        context.blit(TEXTURE, gx, gy, 179f, 3f, barW, barH, 256, 256)
        context.disableScissor()
    }

    companion object {
        private val TEXTURE = Identifier.fromNamespaceAndPath("ic2", "textures/gui/guielectrickineticgenerator.png")
    }
}
