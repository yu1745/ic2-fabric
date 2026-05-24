package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.t
import ic2_120.content.block.UvLampBlock
import ic2_120.content.screen.UvLampScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = UvLampBlock::class)
class UvLampScreen(
    handler: UvLampScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<UvLampScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = 176
        backgroundHeight = 162
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, backgroundWidth, backgroundHeight, TEXTURE_SIZE, TEXTURE_SIZE)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)

        val left = x
        val top = y

        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val capacity = handler.sync.energyCapacity.toLong().coerceAtLeast(1L)
        val energyFraction = (energy.toFloat() / capacity).coerceIn(0f, 1f)
        val growthMultiplier = handler.sync.growthMultiplier

        val inputRateText = t("gui.ic2_120.input_eu", EnergyFormatUtils.formatEu(handler.sync.getSyncedInsertedAmount()))
        val consumeRateText = t("gui.ic2_120.consume_eu", EnergyFormatUtils.formatEu(handler.sync.getSyncedConsumedAmount()))

        // 标题居中
        context.drawText(textRenderer, title, left + (backgroundWidth - textRenderer.getWidth(title)) / 2, top + 6, 0x404040, false)

        // 电量条 (180,4)-(194,18) = 14×14，自下而上渲染至 (9,58)
        if (energyFraction > 0f) {
            val fillHeight = (ENERGY_BAR_H * energyFraction).toInt().coerceAtLeast(1)
            context.enableScissor(
                left + ENERGY_BAR_X,
                top + ENERGY_BAR_Y + ENERGY_BAR_H - fillHeight,
                left + ENERGY_BAR_X + ENERGY_BAR_W,
                top + ENERGY_BAR_Y + ENERGY_BAR_H
            )
            context.drawTexture(
                TEXTURE, left + ENERGY_BAR_X, top + ENERGY_BAR_Y,
                ENERGY_BAR_U.toFloat(), ENERGY_BAR_V.toFloat(),
                ENERGY_BAR_W, ENERGY_BAR_H,
                TEXTURE_SIZE, TEXTURE_SIZE
            )
            context.disableScissor()
        }

        // 文本区域 (49,59)-(127,71) 居中显示
        val boostColor = if (growthMultiplier > 0) 0xDDA0FF else 0xAAAAAA
        val growthText = t("gui.ic2_120.uv_lamp_growth", if (growthMultiplier > 0) "${growthMultiplier}x" else "-")
        val textWidth = textRenderer.getWidth(growthText)
        val textX = left + 49 + (127 - 49 - textWidth) / 2
        val textY = top + 59 + (71 - 59 - textRenderer.fontHeight) / 2
        context.drawText(textRenderer, growthText, textX, textY, boostColor, false)

        // 侧边文本
        val sideTextWidth = maxOf(textRenderer.getWidth(inputRateText), textRenderer.getWidth(consumeRateText))
        val sideX = left - sideTextWidth - 4
        context.drawText(textRenderer, inputRateText, sideX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, consumeRateText, sideX, top + 20, 0xAAAAAA, false)

        drawMouseoverTooltip(context, mouseX, mouseY)

        // 电量条悬停提示
        val relX = mouseX - left
        val relY = mouseY - top
        if (relX in ENERGY_BAR_X until ENERGY_BAR_X + ENERGY_BAR_W &&
            relY in ENERGY_BAR_Y until ENERGY_BAR_Y + ENERGY_BAR_H
        ) {
            context.drawTooltip(
                textRenderer,
                Text.literal("储能：${EnergyFormatUtils.formatRaw(energy)} / ${EnergyFormatUtils.formatRaw(capacity)} EU"),
                mouseX, mouseY
            )
        }
    }

    companion object {
        private val TEXTURE = Identifier.of("ic2", "textures/gui/guiuvlamp.png")
        private const val TEXTURE_SIZE = 256

        // 电量条纹理区域 (180,4)-(194,18) = 14×14
        private const val ENERGY_BAR_U = 180
        private const val ENERGY_BAR_V = 4
        private const val ENERGY_BAR_W = 14
        private const val ENERGY_BAR_H = 14
        // 电量条渲染位置
        private const val ENERGY_BAR_X = 9
        private const val ENERGY_BAR_Y = 57
    }
}
