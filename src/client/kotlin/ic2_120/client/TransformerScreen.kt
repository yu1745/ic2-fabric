package ic2_120.client

import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.content.block.misc.FilteredValue
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.TransformerBlock
import ic2_120.content.sync.TransformerSync
import ic2_120.content.screen.TransformerScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket
import net.minecraft.text.Text
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong

/**
 * 变压器的 GUI 界面。
 * 显示当前模式（升压/降压）和能量条，提供切换按钮。
 *
 * 所有电压等级的变压器（LV、MV、HV、EV）共用此 UI。
 */
@ModScreen(handlers = ["lv_transformer", "mv_transformer", "hv_transformer", "ev_transformer"])
class TransformerScreen(
    handler: TransformerScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<TransformerScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()
    private var filteredInputRate by FilteredValue()
    private var filteredOutputRate by FilteredValue()

    // 用于调试：跟踪上次同步的值，只在变化时记录日志
    private val lastLoggedInput = AtomicLong(-1)
    private val lastLoggedOutput = AtomicLong(-1)

    init {
        backgroundWidth = PANEL_WIDTH
        backgroundHeight = PANEL_HEIGHT
        titleY = 4
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.draw(context, x, y, backgroundWidth, backgroundHeight)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        val left = x
        val top = y
        val contentW = (backgroundWidth - 16).coerceAtLeast(0)
        val energy = handler.sync.amount.toLong().coerceAtLeast(0)
        val cap = handler.sync.capacity.toLong().coerceAtLeast(1)
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val currentMode = handler.sync.getMode()

        // 更新滤波值：使用后端同步的真实输入/输出电流
        val rawInput = handler.sync.getSyncedInsertedAmount()
        val rawOutput = handler.sync.getSyncedExtractedAmount()
        filteredInputRate = rawInput
        filteredOutputRate = rawOutput

        // 调试日志：当输入/输出值变化时记录
        val lastIn = lastLoggedInput.get()
        val lastOut = lastLoggedOutput.get()
        if (rawInput != lastIn || rawOutput != lastOut) {
            if (lastLoggedInput.compareAndSet(lastIn, rawInput) ||
                lastLoggedOutput.compareAndSet(lastOut, rawOutput)) {
                val modeText = when (currentMode) {
                    TransformerSync.Mode.STEP_UP -> "升压"
                    TransformerSync.Mode.STEP_DOWN -> "降压"
                }
//                LOGGER.info(
//                    "Transformer GUI - Mode: {}, Energy: {} EU, Input: {} EU/t, Output: {} EU/t",
//                    modeText,
//                    formatEu(energy),
//                    formatEu(rawInput),
//                    formatEu(rawOutput)
//                )
            }
        }

        val modeText = when (currentMode) {
            TransformerSync.Mode.STEP_UP -> "升压 (低→高)"
            TransformerSync.Mode.STEP_DOWN -> "降压 (高→低)"
        }
        val modeColor = when (currentMode) {
            TransformerSync.Mode.STEP_UP -> 0xAAFFAA  // 绿色表示升压
            TransformerSync.Mode.STEP_DOWN -> 0xFFAAAA  // 红色表示降压
        }

        // 在UI左侧绘制速度文本
        val inputText = "输入 ${formatEu(filteredInputRate)} EU/t"
        val outputText = "输出 ${formatEu(filteredOutputRate)} EU/t"
        val inputTextWidth = inputText.length * 6
        val outputTextWidth = outputText.length * 6
        val textX = left - maxOf(inputTextWidth, outputTextWidth) - 4  // 留4像素边距
        context.drawText(textRenderer, inputText, textX, top + 6, 0xFFFFAA, false)
        context.drawText(textRenderer, outputText, textX, top + 18, 0xFFFFAA, false)

        ui.render(context, textRenderer, mouseX, mouseY) {
            Column(x = left + 8, y = top + 6, spacing = 4, modifier = Modifier.EMPTY.width(contentW).padding(0, 0, 8, 0)) {
                // 标题行
                Text(title.string, color = 0xFFFFFF)

                // 能量条 + 数值
                Flex(
                    direction = FlexDirection.ROW,
                    alignItems = AlignItems.CENTER,
                    gap = 6,
                    modifier = Modifier.EMPTY.width(contentW - 8)
                ) {
                    Text("能量:", color = 0xCCCCCC, shadow = false)
                    EnergyBar(
                        energyFraction,
                        barWidth = 100,
                        barHeight = 6
                    )
                    Text("$energy/$cap EU", color = 0xCCCCCC, shadow = false)
                }

                // 模式显示 + 切换按钮
                Flex(
                    direction = FlexDirection.ROW,
                    alignItems = AlignItems.CENTER,
                    gap = 8,
                    modifier = Modifier.EMPTY.width(contentW - 8)
                ) {
                    Text("模式:", color = 0xCCCCCC, shadow = false)
                    Text(modeText, color = modeColor, shadow = false)
                    Button("切换", onClick = {
                        client?.player?.networkHandler?.sendPacket(
                            ButtonClickC2SPacket(handler.syncId, TransformerScreenHandler.BUTTON_ID_TOGGLE_MODE)
                        )
                    })
                }

                // 说明文本
                val description = when (currentMode) {
                    TransformerSync.Mode.STEP_UP -> {
                        val inputTier = handler.sync.lowTier
                        val outputTier = handler.sync.highTier
                        val inputEu = handler.sync.getLowEuPerTick()
                        val outputEu = handler.sync.getHighEuPerTick()
                        "正面接收 $inputEu EU/t (级${inputTier})，其他面输出 $outputEu EU/t (级${outputTier})"
                    }
                    TransformerSync.Mode.STEP_DOWN -> {
                        val inputTier = handler.sync.highTier
                        val outputTier = handler.sync.lowTier
                        val inputEu = handler.sync.getHighEuPerTick()
                        val outputEu = handler.sync.getLowEuPerTick()
                        "其他面接收 $inputEu EU/t (级${inputTier})，正面输出 $outputEu EU/t (级${outputTier})"
                    }
                }
                Text(description, color = 0x999999, shadow = false)
            }
        }
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (ui.mouseClicked(mouseX, mouseY, button)) return true
        return super.mouseClicked(mouseX, mouseY, button)
    }

    private fun formatEu(value: Long): String {
        return when {
            value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
            value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
            else -> value.toString()
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TransformerScreen::class.java)
        private const val PANEL_WIDTH = 176
        private const val PANEL_HEIGHT = 120
    }
}
