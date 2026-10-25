package ic2_120_advanced_solar_addon.client.screen

import ic2_120_advanced_solar_addon.content.block.MolecularTransformerBlock
import ic2_120_advanced_solar_addon.content.screen.MolecularTransformerScreenHandler
import ic2_120.registry.annotation.ModScreen
import ic2_120.client.compat.*
import ic2_120.client.screen.LegacyContainerScreen26
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

@ModScreen(block = MolecularTransformerBlock::class)
class MolecularTransformerScreen(
    handler: MolecularTransformerScreenHandler,
    playerInventory: Inventory,
    title: Component
) : LegacyContainerScreen26<MolecularTransformerScreenHandler>(handler, playerInventory, title, GUI_WIDTH, GUI_HEIGHT) {
    // yarn→mojmap 兼容别名
    private val x get() = leftPos
    private val y get() = topPos
    private val textRenderer get() = font

    init {
        inventoryLabelY = -20000
    }

    override fun renderBg(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.blit(TEXTURE, leftPos, topPos, 0f, 0f, GUI_WIDTH, GUI_HEIGHT, TEX_WIDTH, TEX_HEIGHT)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)

        val progress = menu.sync.progress.toLong().coerceAtLeast(0)
        val requiredEnergy = menu.sync.requiredEnergy.toLong().coerceAtLeast(0)
        val hasRecipe = requiredEnergy > 0
        val progressFraction = if (hasRecipe) (progress.toFloat() / requiredEnergy.toFloat()).coerceIn(0f, 1f) else 0f
        val inputEuPerTick = menu.sync.avgInserted.toLong().coerceAtLeast(0)

        // 工作进度条：纹理区域 (221,8)-(231,22) 11×15 → 渲染于 (23,48)
        val gaugeX = leftPos + GAUGE_RENDER_X
        val gaugeY = topPos + GAUGE_RENDER_Y
        val fillH = (GAUGE_TEX_H * progressFraction).toInt().coerceAtLeast(0)
        if (fillH > 0) {
            // 从上到下填充
            context.enableScissor(gaugeX, gaugeY, gaugeX + GAUGE_TEX_W, gaugeY + fillH)
            context.blit(TEXTURE, gaugeX, gaugeY, GAUGE_TEX_U.toFloat(), GAUGE_TEX_V.toFloat(), GAUGE_TEX_W, GAUGE_TEX_H, TEX_WIDTH, TEX_HEIGHT)
            context.disableScissor()
        }

        val tx = leftPos + TEXT_X

        // 输入: itemName at (59, 25)
        val inputLabel = Component.translatable("gui.ic2_120_advanced_solar_addon.input").string
        context.drawString(font, inputLabel, tx, y + TEXT_Y_INPUT, 0xFFFFFF, false)
        if (hasRecipe) {
            val inputName = getItemName(menu.sync.inputItemId)
            context.drawString(font, inputName, tx + font.width(inputLabel), y + TEXT_Y_INPUT, 0xFFFFFF, false)
        }

        // 输出: outputName at (59, 38)
        val outputLabel = Component.translatable("gui.ic2_120_advanced_solar_addon.output").string
        context.drawString(font, outputLabel, tx, y + TEXT_Y_OUTPUT, 0xFFFFFF, false)
        if (hasRecipe) {
            val outputName = getItemName(menu.sync.outputItemId)
            context.drawString(font, outputName, tx + font.width(outputLabel), y + TEXT_Y_OUTPUT, 0xFFFFFF, false)
        }

        // 能耗: total required EU at (59, 51)
        val energyLabel = Component.translatable("gui.ic2_120_advanced_solar_addon.energy_per_operation").string
        context.drawString(font, energyLabel, tx, y + TEXT_Y_ENERGY, 0xFFFFFF, false)
        if (hasRecipe) {
            context.drawString(font, formatNum(requiredEnergy) + " EU", tx + font.width(energyLabel), y + TEXT_Y_ENERGY, 0xFFFFFF, false)
        }

        // EU输入: avgInserted at (59, 64)
        val euInputLabel = Component.translatable("gui.ic2_120_advanced_solar_addon.energy_per_tick").string
        context.drawString(font, euInputLabel, tx, y + TEXT_Y_EU_INPUT, 0xFFFFFF, false)
        if (hasRecipe) {
            context.drawString(font, formatNum(inputEuPerTick) + " EU/t", tx + font.width(euInputLabel), y + TEXT_Y_EU_INPUT, 0xFFFFFF, false)
        }

        // 进度: percentage% at (59, 77)
        val progressLabel = Component.translatable("gui.ic2_120_advanced_solar_addon.progress").string
        context.drawString(font, progressLabel, tx, y + TEXT_Y_PROGRESS, 0xFFFFFF, false)
        if (hasRecipe) {
            val progressPercent = String.format("%d%%", (progressFraction * 100f).toInt())
            context.drawString(font, progressPercent, tx + font.width(progressLabel), y + TEXT_Y_PROGRESS, 0xFFFFFF, false)
        }

        renderTooltip(context, mouseX, mouseY)
    }

    private fun getItemName(rawId: Int): String {
        if (rawId == 0) return ""
        val item = Item.byId(rawId)
        return item.getName(ItemStack(item)).string
    }

    private fun formatNum(n: Long): String = String.format("%,d", n)

    companion object {
        private val TEXTURE = Identifier.fromNamespaceAndPath("ic2_120_advanced_solar_addon", "textures/gui/moleculartransformer.png")

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
