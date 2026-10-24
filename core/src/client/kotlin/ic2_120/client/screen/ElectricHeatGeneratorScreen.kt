package ic2_120.client.screen

import ic2_120.content.block.ElectricHeatGeneratorBlock
import ic2_120.content.screen.ElectricHeatGeneratorScreenHandler
import ic2_120.content.sync.ElectricHeatGeneratorSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Inventory
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

@ModScreen(block = ElectricHeatGeneratorBlock::class)
class ElectricHeatGeneratorScreen(
    val handler: ElectricHeatGeneratorScreenHandler,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<ElectricHeatGeneratorScreenHandler>(handler, playerInventory, title) {

    init {
        imageWidth = 176
        imageHeight = 166
    }

    override fun drawBackground(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.blit(TEXTURE, leftPos, topPos, 0f, 0f, 176, 166, 256, 256)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        val energy = menu.sync.energy.toLong().coerceAtLeast(0)
        val cap = ElectricHeatGeneratorSync.ENERGY_CAPACITY
        val energyFrac = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val outputRate = menu.sync.getSyncedOutputHeat().coerceAtLeast(0L)

        // 标题居中于 y=6
        context.drawString(font, title, leftPos + (176 - font.width(title)) / 2, topPos + 6, 0x404040, false)

        // 能量条：纹理区域 (179,3)-(192,16) = 13×13，渲染到 (10,45)，自底向上填充
        val gaugeX = leftPos + 10
        val gaugeY = topPos + 44
        val gaugeSize = 13
        drawEnergyGauge(context, gaugeX, gaugeY, energyFrac)

        // 鼠标悬停能量条时显示储能
        if (mouseX in gaugeX until (gaugeX + gaugeSize) && mouseY in gaugeY until (gaugeY + gaugeSize)) {
            context.renderComponentTooltip(font, Component.literal("储能：${"%,d".format(energy)} / ${"%,d".format(cap)} EU"), mouseX, mouseY)
        }

        // 输出文本：区域 (29,65)-(149,79)，居中常显，缩放至 7px
        val infoText = "输出 ${outputRate}/100 HU/t"
        val scale = 7f / font.lineHeight
        val scaledWidth = (font.width(infoText) * scale).toInt()
        val scaledHeight = (font.lineHeight * scale).toInt()
        val textX = leftPos + 29 + (120 - scaledWidth) / 2
        val textY = topPos + 65 + (14 - scaledHeight) / 2
        context.pose().pushPose()
        context.pose().translate(textX.toDouble(), textY.toDouble(), 0.0)
        context.pose().scale(scale, scale, 1.0f)
        context.drawString(font, infoText, 0, 0, 0xFFADD8E6.toInt(), false)
        context.pose().popPose()

        renderTooltip(context, mouseX, mouseY)
    }

    /**
     * 能量条自底向上填充，纹理区域 (179,3)-(192,16) = 13×13。
     */
    private fun drawEnergyGauge(context: GuiGraphics, gx: Int, gy: Int, fraction: Float) {
        val barW = 13
        val barH = 13
        val fillH = (fraction.coerceIn(0f, 1f) * barH).toInt()
        if (fillH <= 0) return
        context.enableScissor(gx, gy + barH - fillH, gx + barW, gy + barH)
        context.blit(TEXTURE, gx, gy, 179f, 3f, barW, barH, 256, 256)
        context.disableScissor()
    }

    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath("ic2", "textures/gui/guiheatingmachine.png")
    }
}
