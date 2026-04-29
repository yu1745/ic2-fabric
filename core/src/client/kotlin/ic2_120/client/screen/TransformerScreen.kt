package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.EnergyFormatUtils
import ic2_120.client.t
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.TransformerBlock
import ic2_120.client.ui.FilteredValue
import ic2_120.content.sync.TransformerSync
import ic2_120.content.screen.TransformerScreenHandler
import ic2_120.content.screen.GuiSize
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
    private val lastLoggedInput = AtomicLong(-1)
    private val lastLoggedOutput = AtomicLong(-1)

    init {
        backgroundWidth = GUI_SIZE.width
        backgroundHeight = GUI_SIZE.height
        titleY = 4
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y
        val energy = handler.sync.amount.toLong().coerceAtLeast(0)
        val cap = handler.sync.capacity.toLong().coerceAtLeast(1)
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val currentMode = handler.sync.getMode()

        val rawInput = handler.sync.getSyncedInsertedAmount()
        val rawOutput = handler.sync.getSyncedExtractedAmount()
        filteredInputRate = rawInput
        filteredOutputRate = rawOutput

        val lastIn = lastLoggedInput.get()
        val lastOut = lastLoggedOutput.get()
        if (rawInput != lastIn || rawOutput != lastOut) {
            if (lastLoggedInput.compareAndSet(lastIn, rawInput) ||
                lastLoggedOutput.compareAndSet(lastOut, rawOutput)) {
                val modeText = when (currentMode) {
                    TransformerSync.Mode.STEP_UP -> t("gui.ic2_120.transformer.step_up")
                    TransformerSync.Mode.STEP_DOWN -> t("gui.ic2_120.transformer.step_down")
                }
            }
        }

        val modeText = when (currentMode) {
            TransformerSync.Mode.STEP_UP -> t("gui.ic2_120.transformer.step_up_full")
            TransformerSync.Mode.STEP_DOWN -> t("gui.ic2_120.transformer.step_down_full")
        }
        val modeColor = when (currentMode) {
            TransformerSync.Mode.STEP_UP -> 0xAAFFAA
            TransformerSync.Mode.STEP_DOWN -> 0xFFAAAA
        }

        val inputText = t("gui.ic2_120.input_eu", EnergyFormatUtils.formatEu(filteredInputRate))
        val outputText = t("gui.ic2_120.output_eu", EnergyFormatUtils.formatEu(filteredOutputRate))
        val sideTextWidth = maxOf(textRenderer.getWidth(inputText), textRenderer.getWidth(outputText))
        val sideTextX = left - sideTextWidth - 4

        val content: UiScope.() -> Unit = {
            Column(
                x = left + 8,
                y = top + 6,
                spacing = 4,
                modifier = Modifier.EMPTY.width(GUI_SIZE.contentWidth).padding(0, 0, 8, 0)
            ) {
                Text(title.string, color = 0xFFFFFF)

                Flex(
                    direction = FlexDirection.ROW,
                    alignItems = AlignItems.CENTER,
                    gap = 6,
                    modifier = Modifier.EMPTY.width(GUI_SIZE.contentWidth - 8)
                ) {
                    Text(t("gui.ic2_120.transformer.energy_label"), color = 0xFFFFFF, shadow = false)
                    EnergyBar(
                        energyFraction,
                        barWidth = 100,
                        barHeight = 6
                    )
                    Text("$energy/$cap EU", color = 0xFFFFFF, shadow = false)
                }

                Flex(
                    direction = FlexDirection.ROW,
                    alignItems = AlignItems.CENTER,
                    gap = 8,
                    modifier = Modifier.EMPTY.width(GUI_SIZE.contentWidth - 8)
                ) {
                    Text(t("gui.ic2_120.transformer.mode_label"), color = 0xFFFFFF, shadow = false)
                    Text(modeText, color = modeColor, shadow = false)
                    Button(t("gui.ic2_120.transformer.switch_button"), onClick = {
                        client?.player?.networkHandler?.sendPacket(
                            ButtonClickC2SPacket(handler.syncId, TransformerScreenHandler.BUTTON_ID_TOGGLE_MODE)
                        )
                    })
                }

                val (inputDesc, outputDesc) = when (currentMode) {
                    TransformerSync.Mode.STEP_UP -> {
                        val inputTier = handler.sync.lowTier
                        val outputTier = handler.sync.highTier
                        val inputEu = handler.sync.getLowEuPerTick()
                        val outputEu = handler.sync.getHighEuPerTick()
                        t("gui.ic2_120.transformer.face_receive", "$inputEu EU/t", inputTier) to t("gui.ic2_120.transformer.other_output", "$outputEu EU/t", outputTier)
                    }
                    TransformerSync.Mode.STEP_DOWN -> {
                        val inputTier = handler.sync.highTier
                        val outputTier = handler.sync.lowTier
                        val inputEu = handler.sync.getHighEuPerTick()
                        val outputEu = handler.sync.getLowEuPerTick()
                        t("gui.ic2_120.transformer.other_receive", "$inputEu EU/t", inputTier) to t("gui.ic2_120.transformer.face_output", "$outputEu EU/t", outputTier)
                    }
                }
                Text(inputDesc, color = 0x999999, shadow = false)
                Text(outputDesc, color = 0x999999, shadow = false)
            }
        }

        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)

        super.render(context, mouseX, mouseY, delta)
        ui.render(context, textRenderer, mouseX, mouseY, content = content)
        context.drawText(textRenderer, inputText, sideTextX, top + 6, 0xFFFFAA, false)
        context.drawText(textRenderer, outputText, sideTextX, top + 18, 0xFFFFAA, false)
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (ui.mouseClicked(mouseX, mouseY, button)) return true
        return super.mouseClicked(mouseX, mouseY, button)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TransformerScreen::class.java)
        private val GUI_SIZE = GuiSize.STANDARD
    }
}
