package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.t
import ic2_120.content.block.WindGeneratorBlock
import ic2_120.content.block.machines.WindGeneratorBlockEntity
import ic2_120.content.screen.GuiSize
import ic2_120.content.screen.WindGeneratorScreenHandler
import ic2_120.registry.annotation.ModScreen
import ic2_120.client.compat.*
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Inventory
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

@ModScreen(block = WindGeneratorBlock::class)
class WindGeneratorScreen(
    val handler: WindGeneratorScreenHandler,
    playerInventory: Inventory,
    title: Component
) : LegacyContainerScreen26<WindGeneratorScreenHandler>(handler, playerInventory, title, GUI_SIZE.width, GUI_SIZE.height) {

    init {
    }

    override fun renderBg(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.blit(TEXTURE, leftPos, topPos, 0f, 0f, BG_W, BG_H, TEX_SIZE, TEX_SIZE)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)

        val left = leftPos
        val top = topPos

        context.drawString(font, title, left + (imageWidth - font.width(title)) / 2, top + 6, 0x404040, false)

        // 侧边文字
        val inputRate = menu.sync.getSyncedInsertedAmount()
        val outputRate = menu.sync.getSyncedExtractedAmount()
        val inputText = t("gui.ic2_120.generate_eu", EnergyFormatUtils.formatEu(inputRate))
        val outputText = t("gui.ic2_120.output_eu", EnergyFormatUtils.formatEu(outputRate))
        val statusKey = if (menu.sync.status == WindGeneratorBlockEntity.STATUS_TOO_LOW) {
            "gui.ic2_120.wind_generator.too_low"
        } else {
            "gui.ic2_120.wind_generator.generating"
        }
        val statusText = t(statusKey)
        val sideTextWidth = maxOf(font.width(inputText), font.width(outputText), font.width(statusText))
        val sideTextX = left - sideTextWidth - 4
        context.drawString(font, inputText, sideTextX, top + 8, 0xAAAAAA, false)
        context.drawString(font, outputText, sideTextX, top + 20, 0xAAAAAA, false)
        context.drawString(
            font,
            statusText,
            sideTextX,
            top + 32,
            if (menu.sync.status == WindGeneratorBlockEntity.STATUS_TOO_LOW) 0xFF5555 else 0x55FF55,
            false
        )

        renderTooltip(context, mouseX, mouseY)
    }

    companion object {
        private val TEXTURE = Identifier.fromNamespaceAndPath("ic2", "textures/gui/guiwindgenerator.png")
        private const val TEX_SIZE = 256
        private val GUI_SIZE = GuiSize.STANDARD

        private const val BG_W = 176
        private const val BG_H = 161
    }
}
