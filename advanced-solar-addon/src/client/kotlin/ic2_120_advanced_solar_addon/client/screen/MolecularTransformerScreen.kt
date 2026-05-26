package ic2_120_advanced_solar_addon.client.screen

import ic2_120_advanced_solar_addon.content.block.MolecularTransformerBlock
import ic2_120_advanced_solar_addon.content.screen.MolecularTransformerScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = MolecularTransformerBlock::class)
class MolecularTransformerScreen(
    handler: MolecularTransformerScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<MolecularTransformerScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = GUI_WIDTH
        backgroundHeight = GUI_HEIGHT
        playerInventoryTitleY = -20000
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, GUI_WIDTH, GUI_HEIGHT, TEX_WIDTH, TEX_HEIGHT)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)

        val progress = handler.sync.progress.toLong().coerceAtLeast(0)
        val requiredEnergy = handler.sync.requiredEnergy.toLong().coerceAtLeast(0)
        val hasRecipe = requiredEnergy > 0
        val progressFraction = if (hasRecipe) (progress.toFloat() / requiredEnergy.toFloat()).coerceIn(0f, 1f) else 0f
        val inputEuPerTick = handler.sync.avgInserted.toLong().coerceAtLeast(0)

        // 工作进度条：纹理区域 (221,8)-(231,22) 11×15 → 渲染于 (23,48)
        val gaugeX = x + GAUGE_RENDER_X
        val gaugeY = y + GAUGE_RENDER_Y
        val fillH = (GAUGE_TEX_H * progressFraction).toInt().coerceAtLeast(0)
        if (fillH > 0) {
            // 从上到下填充
            context.enableScissor(gaugeX, gaugeY, gaugeX + GAUGE_TEX_W, gaugeY + fillH)
            context.drawTexture(TEXTURE, gaugeX, gaugeY, GAUGE_TEX_U.toFloat(), GAUGE_TEX_V.toFloat(), GAUGE_TEX_W, GAUGE_TEX_H, TEX_WIDTH, TEX_HEIGHT)
            context.disableScissor()
        }

        val tx = x + TEXT_X

        // 输入: itemName at (59, 25)
        val inputLabel = Text.translatable("gui.ic2_120_advanced_solar_addon.input").string
        context.drawText(textRenderer, inputLabel, tx, y + TEXT_Y_INPUT, 0xFFFFFF, false)
        if (hasRecipe) {
            val inputName = getItemName(handler.sync.inputItemId)
            context.drawText(textRenderer, inputName, tx + textRenderer.getWidth(inputLabel), y + TEXT_Y_INPUT, 0xFFFFFF, false)
        }

        // 输出: outputName at (59, 38)
        val outputLabel = Text.translatable("gui.ic2_120_advanced_solar_addon.output").string
        context.drawText(textRenderer, outputLabel, tx, y + TEXT_Y_OUTPUT, 0xFFFFFF, false)
        if (hasRecipe) {
            val outputName = getItemName(handler.sync.outputItemId)
            context.drawText(textRenderer, outputName, tx + textRenderer.getWidth(outputLabel), y + TEXT_Y_OUTPUT, 0xFFFFFF, false)
        }

        // 能耗: total required EU at (59, 51)
        val energyLabel = Text.translatable("gui.ic2_120_advanced_solar_addon.energy_per_operation").string
        context.drawText(textRenderer, energyLabel, tx, y + TEXT_Y_ENERGY, 0xFFFFFF, false)
        if (hasRecipe) {
            context.drawText(textRenderer, formatNum(requiredEnergy) + " EU", tx + textRenderer.getWidth(energyLabel), y + TEXT_Y_ENERGY, 0xFFFFFF, false)
        }

        // EU输入: avgInserted at (59, 64)
        val euInputLabel = Text.translatable("gui.ic2_120_advanced_solar_addon.energy_per_tick").string
        context.drawText(textRenderer, euInputLabel, tx, y + TEXT_Y_EU_INPUT, 0xFFFFFF, false)
        if (hasRecipe) {
            context.drawText(textRenderer, formatNum(inputEuPerTick) + " EU/t", tx + textRenderer.getWidth(euInputLabel), y + TEXT_Y_EU_INPUT, 0xFFFFFF, false)
        }

        // 进度: percentage% at (59, 77)
        val progressLabel = Text.translatable("gui.ic2_120_advanced_solar_addon.progress").string
        context.drawText(textRenderer, progressLabel, tx, y + TEXT_Y_PROGRESS, 0xFFFFFF, false)
        if (hasRecipe) {
            val progressPercent = String.format("%d%%", (progressFraction * 100f).toInt())
            context.drawText(textRenderer, progressPercent, tx + textRenderer.getWidth(progressLabel), y + TEXT_Y_PROGRESS, 0xFFFFFF, false)
        }

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun getItemName(rawId: Int): String {
        if (rawId == 0) return ""
        val item = Item.byRawId(rawId)
        return item.getName(ItemStack(item)).string
    }

    private fun formatNum(n: Long): String = String.format("%,d", n)

    companion object {
        private val TEXTURE = Identifier.of("ic2_120_advanced_solar_addon", "textures/gui/moleculartransformer.png")

        private const val TEX_WIDTH = 256
        private const val TEX_HEIGHT = 256
        private const val GUI_WIDTH = 220
        private const val GUI_HEIGHT = 193

        // 进度条
        private const val GAUGE_TEX_U = 221
        private const val GAUGE_TEX_V = 8
        private const val GAUGE_TEX_W = 11
        private const val GAUGE_TEX_H = 15
        private const val GAUGE_RENDER_X = 23
        private const val GAUGE_RENDER_Y = 48

        // 文本布局
        private const val TEXT_X = 59
        private const val TEXT_Y_INPUT = 25
        private const val TEXT_Y_OUTPUT = 38
        private const val TEXT_Y_ENERGY = 51
        private const val TEXT_Y_EU_INPUT = 64
        private const val TEXT_Y_PROGRESS = 77
    }
}
