package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.t
import ic2_120.content.block.WindGeneratorBlock
import ic2_120.content.block.machines.WindGeneratorBlockEntity
import ic2_120.content.screen.GuiSize
import ic2_120.content.screen.WindGeneratorScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Container
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

@ModScreen(block = WindGeneratorBlock::class)
class WindGeneratorScreen(
    handler: WindGeneratorScreenHandler,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<WindGeneratorScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = GUI_SIZE.width
        backgroundHeight = GUI_SIZE.height
    }

    override fun drawBackground(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, BG_W, BG_H, TEX_SIZE, TEX_SIZE)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        val left = x
        val top = y

        context.drawText(textRenderer, title, left + (backgroundWidth - textRenderer.getWidth(title)) / 2, top + 6, 0x404040, false)

        // 侧边文字
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val outputRate = handler.sync.getSyncedExtractedAmount()
        val inputText = t("gui.ic2_120.generate_eu", EnergyFormatUtils.formatEu(inputRate))
        val outputText = t("gui.ic2_120.output_eu", EnergyFormatUtils.formatEu(outputRate))
        val statusKey = if (handler.sync.status == WindGeneratorBlockEntity.STATUS_TOO_LOW) {
            "gui.ic2_120.wind_generator.too_low"
        } else {
            "gui.ic2_120.wind_generator.generating"
        }
        val statusText = t(statusKey)
        val sideTextWidth = maxOf(textRenderer.getWidth(inputText), textRenderer.getWidth(outputText), textRenderer.getWidth(statusText))
        val sideTextX = left - sideTextWidth - 4
        context.drawText(textRenderer, inputText, sideTextX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, outputText, sideTextX, top + 20, 0xAAAAAA, false)
        context.drawText(
            textRenderer,
            statusText,
            sideTextX,
            top + 32,
            if (handler.sync.status == WindGeneratorBlockEntity.STATUS_TOO_LOW) 0xFF5555 else 0x55FF55,
            false
        )

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath("ic2", "textures/gui/guiwindgenerator.png")
        private const val TEX_SIZE = 256
        private val GUI_SIZE = GuiSize.STANDARD

        private const val BG_W = 176
        private const val BG_H = 161
    }
}
