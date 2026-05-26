package ic2_120_advanced_solar_addon.client.screen

import ic2_120_advanced_solar_addon.content.screen.SolarPanelScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(handlers = ["advanced_solar_panel", "hybrid_solar_panel", "ultimate_solar_panel", "quantum_solar_panel"])
class SolarPanelScreen(
    handler: SolarPanelScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<SolarPanelScreenHandler>(handler, playerInventory, title) {

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

        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val capacity = handler.sync.capacitySync.toLong().coerceAtLeast(1)
        val energyFraction = if (capacity > 0) (energy.toFloat() / capacity).coerceIn(0f, 1f) else 0f
        val state = handler.sync.generationState
        val dayPower = handler.sync.dayPower
        val nightPower = handler.sync.nightPower
        val maxOutput = handler.sync.maxOutput

        // 能量条：纹理 (195,1) 25×14 → 渲染于 (19,23)，按存储比例填充
        val barX = x + BAR_RENDER_X
        val barY = y + BAR_RENDER_Y
        val fillW = (BAR_TEX_W * energyFraction).toInt().coerceAtLeast(0)
        if (fillW > 0) {
            context.enableScissor(barX, barY, barX + fillW, barY + BAR_TEX_H)
            context.drawTexture(TEXTURE, barX, barY, BAR_TEX_U.toFloat(), BAR_TEX_V.toFloat(), BAR_TEX_W, BAR_TEX_H, TEX_WIDTH, TEX_HEIGHT)
            context.disableScissor()
        }

        // 太阳 / 月亮 图标
        val iconX = x + ICON_RENDER_X
        val iconY = y + ICON_RENDER_Y
        when (state) {
            GENERATION_STATE_DAY -> {
                context.drawTexture(TEXTURE, iconX, iconY, SUN_TEX_U.toFloat(), SUN_TEX_V.toFloat(), ICON_TEX_W, ICON_TEX_H, TEX_WIDTH, TEX_HEIGHT)
            }
            GENERATION_STATE_NIGHT -> {
                context.drawTexture(TEXTURE, iconX, iconY, MOON_TEX_U.toFloat(), MOON_TEX_V.toFloat(), ICON_TEX_W, ICON_TEX_H, TEX_WIDTH, TEX_HEIGHT)
            }
        }

        // 标题居中
        val titleWidth = textRenderer.getWidth(title)
        context.drawText(textRenderer, title, x + (GUI_WIDTH - titleWidth) / 2, y + 6, 0xFFFFFF, false)

        // 存储: a/b  (52, 23)
        val storageLabel = Text.translatable("gui.ic2_120_advanced_solar_addon.storage").string + ": "
        val storageValue = "$energy/$capacity"
        context.drawText(textRenderer, storageLabel + storageValue, x + TEXT_X, y + TEXT_Y_STORAGE, 0xFFFFFF, false)

        // 最大输出: b EU/t  (52, 33)
        val maxOutputLabel = Text.translatable("gui.ic2_120_advanced_solar_addon.max_output").string
        val maxOutputValue = maxOutput.toString() + " EU/t"
        context.drawText(textRenderer, maxOutputLabel + maxOutputValue, x + TEXT_X, y + TEXT_Y_MAX_OUTPUT, 0xFFFFFF, false)

        // 产生: c EU/t  (52, 43)
        val genLabel = Text.translatable("gui.ic2_120_advanced_solar_addon.generating").string
        val currentGen = when (state) {
            GENERATION_STATE_DAY -> dayPower
            GENERATION_STATE_NIGHT -> nightPower
            else -> 0
        }
        val genValue = currentGen.toString() + " EU/t"
        context.drawText(textRenderer, genLabel + genValue, x + TEXT_X, y + TEXT_Y_GEN, 0xFFFFFF, false)

        // 太阳/月亮图标悬停提示
        if (mouseX in iconX until (iconX + ICON_TEX_W) && mouseY in iconY until (iconY + ICON_TEX_H)) {
            if (currentGen > 0) {
                context.drawTooltip(textRenderer,
                    Text.literal(Text.translatable("gui.ic2_120_advanced_solar_addon.generate").string + ": " + currentGen + " EU/t"),
                    mouseX, mouseY)
            }
        }

        // 能量条悬停提示
        if (mouseX in barX until (barX + BAR_TEX_W) && mouseY in barY until (barY + BAR_TEX_H)) {
            context.drawTooltip(textRenderer,
                Text.literal(energy.toString() + " / " + capacity.toString() + " EU"),
                mouseX, mouseY)
        }

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    companion object {
        private val TEXTURE = Identifier.of("ic2_120_advanced_solar_addon", "textures/gui/advancedsolarpanel.png")

        private const val TEX_WIDTH = 256
        private const val TEX_HEIGHT = 256
        private const val GUI_WIDTH = 194
        private const val GUI_HEIGHT = 168

        // 能量条
        private const val BAR_TEX_U = 195
        private const val BAR_TEX_V = 1
        private const val BAR_TEX_W = 25
        private const val BAR_TEX_H = 14
        private const val BAR_RENDER_X = 19
        private const val BAR_RENDER_Y = 25

        // 太阳图标
        private const val SUN_TEX_U = 195
        private const val SUN_TEX_V = 16

        // 月亮图标
        private const val MOON_TEX_U = 210
        private const val MOON_TEX_V = 16

        private const val ICON_TEX_W = 15
        private const val ICON_TEX_H = 14
        private const val ICON_RENDER_X = 24
        private const val ICON_RENDER_Y = 42

        // 文本布局
        private const val TEXT_X = 52
        private const val TEXT_Y_STORAGE = 23
        private const val TEXT_Y_MAX_OUTPUT = 33
        private const val TEXT_Y_GEN = 43

        // 发电状态常量（与 GenerationState 枚举序数一致）
        private const val GENERATION_STATE_NONE = 0
        private const val GENERATION_STATE_NIGHT = 1
        private const val GENERATION_STATE_DAY = 2
    }
}
