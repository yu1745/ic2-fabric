package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.t
import ic2_120.content.block.GeneratorBlock
import ic2_120.content.screen.GeneratorScreenHandler
import ic2_120.content.sync.GeneratorSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Inventory
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

@ModScreen(block = GeneratorBlock::class)
class GeneratorScreen(
    val handler: GeneratorScreenHandler,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<GeneratorScreenHandler>(handler, playerInventory, title) {

    init {
        imageWidth = 176
        imageHeight = 161
    }

    override fun drawBackground(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.blit(TEXTURE, leftPos, topPos, 0f, 0f, 176, 161, 256, 256)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        val energy = menu.sync.energy.toLong().coerceAtLeast(0)
        val energyFrac = if (GeneratorSync.ENERGY_CAPACITY > 0) {
            (energy.toFloat() / GeneratorSync.ENERGY_CAPACITY).coerceIn(0f, 1f)
        } else 0f
        val totalBurn = menu.sync.totalBurnTime.coerceAtLeast(1)
        val burnTime = menu.sync.burnTime.coerceIn(0, totalBurn)
        val burnFrac = (burnTime.toFloat() / totalBurn).coerceIn(0f, 1f)
        val inputRate = menu.sync.getSyncedInsertedAmount()
        val outputRate = menu.sync.getSyncedExtractedAmount()

        // 能量条位于 (79, 35) — 24×16 水平纹理来自 guipowergenerator.png (178,3)-(202,19)
        drawEnergyGauge(context, leftPos + 79, topPos + 35, energyFrac)

        // 燃料条位于 (58, 38) — 13×13 垂直纹理来自 guipowergenerator.png (179,22)-(191,34)
        drawFuelGauge(context, leftPos + 57, topPos + 38, burnFrac)

        // 标题文字居中于 y=6
        context.drawString(font, title, leftPos + (176 - font.width(title)) / 2, topPos + 6, 0x404040, false)

        // EU发电/输出文字显示在左侧
        val generateText = t("gui.ic2_120.generate_eu", EnergyFormatUtils.formatRaw(inputRate))
        val outputText = t("gui.ic2_120.output_eu", EnergyFormatUtils.formatRaw(outputRate))
        val sideTextWidth = maxOf(font.width(generateText), font.width(outputText))
        val sideTextX = leftPos - sideTextWidth - 4
        context.drawString(font, generateText, sideTextX, topPos + 8, 0xAAAAAA, false)
        context.drawString(font, outputText, sideTextX, topPos + 20, 0xAAAAAA, false)

        // 能量条悬停 (79,35)-(103,51) = 24×16
        if (mouseX in leftPos + 79 until leftPos + 103 && mouseY in topPos + 35 until topPos + 51) {
            context.renderComponentTooltip(
                font,
                Component.literal("储能：${EnergyFormatUtils.formatRaw(energy)} / ${EnergyFormatUtils.formatRaw(GeneratorSync.ENERGY_CAPACITY)} EU"),
                mouseX, mouseY
            )
        }

        renderTooltip(context, mouseX, mouseY)
    }

    private fun drawEnergyGauge(context: GuiGraphics, gx: Int, gy: Int, fraction: Float) {
        val barW = 24
        val barH = 16
        val fillW = (fraction.coerceIn(0f, 1f) * barW).toInt()
        if (fillW <= 0) return
        context.enableScissor(gx, gy, gx + fillW, gy + barH)
        context.blit(TEXTURE, gx, gy, 178f, 3f, barW, barH, 256, 256)
        context.disableScissor()
    }

    private fun drawFuelGauge(context: GuiGraphics, gx: Int, gy: Int, fraction: Float) {
        val barW = 13
        val barH = 13
        val fillH = (fraction.coerceIn(0f, 1f) * barH).toInt()
        if (fillH <= 0) return
        context.enableScissor(gx, gy + barH - fillH, gx + barW, gy + barH)
        context.blit(TEXTURE, gx, gy, 178f, 22f, barW, barH, 256, 256)
        context.disableScissor()
    }

    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath("ic2", "textures/gui/guipowergenerator.png")
    }
}
