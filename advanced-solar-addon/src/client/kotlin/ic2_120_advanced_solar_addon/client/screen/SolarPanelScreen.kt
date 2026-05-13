package ic2_120_advanced_solar_addon.client.screen

import ic2_120.client.compose.*
import ic2_120.client.EnergyFormatUtils
import ic2_120.client.t
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.content.screen.GuiSize
import ic2_120_advanced_solar_addon.content.screen.SolarPanelScreenHandler
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.slot.Slot
import net.minecraft.text.Text
import ic2_120.registry.annotation.ModScreen

@ModScreen(handlers = ["advanced_solar_panel", "hybrid_solar_panel", "ultimate_solar_panel", "quantum_solar_panel"])
class SolarPanelScreen(
    handler: SolarPanelScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<SolarPanelScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = GUI_SIZE.width
        backgroundHeight = GUI_SIZE.height
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        // 背景绘制已移至 render()，以控制 ui.render 在 super.render 之前执行
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val capacity = handler.sync.capacitySync.toLong().coerceAtLeast(1)
        val energyFraction = if (capacity > 0) (energy.toFloat() / capacity).coerceIn(0f, 1f) else 0f
        val state = handler.sync.generationState
        val dayPower = handler.sync.dayPower
        val nightPower = handler.sync.nightPower

        val stateText = when (state) {
            0 -> Text.translatable("gui.ic2_120_advanced_solar_addon.solar_state.none").string
            1 -> Text.translatable("gui.ic2_120_advanced_solar_addon.solar_state.night", EnergyFormatUtils.formatEu(nightPower.toLong())).string
            2 -> Text.translatable("gui.ic2_120_advanced_solar_addon.solar_state.day", EnergyFormatUtils.formatEu(dayPower.toLong())).string
            else -> Text.translatable("gui.ic2_120_advanced_solar_addon.solar_state.unknown").string
        }

        val genText = t("gui.ic2_120.generate_eu", EnergyFormatUtils.formatEu(handler.sync.avgInserted.toLong()))
        val outputText = t("gui.ic2_120.output_eu", EnergyFormatUtils.formatEu(handler.sync.avgExtracted.toLong()))
        val sideTextWidth = maxOf(textRenderer.getWidth(genText), textRenderer.getWidth(outputText))
        val sideTextX = left - sideTextWidth - 4

        val chargeLabel = Text.translatable("ic2_120.gui.charge_slots")

        val content: UiScope.() -> Unit = {
            Column(
                x = left + 8,
                y = top + 8,
                spacing = 6,
                modifier = Modifier.EMPTY.width(GUI_SIZE.contentWidth)
            ) {
                Flex(direction = FlexDirection.ROW, alignItems = AlignItems.CENTER, gap = 4) {
                    Text(title.string, color = 0xFFFFFF)
                    Text("${EnergyFormatUtils.formatEu(energy)} / ${EnergyFormatUtils.formatEu(capacity)} EU", color = 0xFFFFFF, shadow = false)
                }
                EnergyBar(energyFraction, barHeight = 12)

                Flex(direction = FlexDirection.ROW, alignItems = AlignItems.CENTER, gap = 4) {
                    Text(Text.translatable("gui.ic2_120_advanced_solar_addon.status").string + ": ", color = 0xAAAAAA)
                    Text(stateText, color = if (state == 2) 0xFFFF00 else 0xAAAAAA)
                }

                Flex(direction = FlexDirection.ROW, alignItems = AlignItems.CENTER, gap = 4) {
                    Text(Text.translatable("gui.ic2_120_advanced_solar_addon.day").string + ": ", color = 0xAAAAAA)
                    Text("${EnergyFormatUtils.formatEu(dayPower.toLong())} EU/t", color = 0xFFFFFF)
                    Text(Text.translatable("gui.ic2_120_advanced_solar_addon.night").string + ": ", color = 0xAAAAAA)
                    Text("${EnergyFormatUtils.formatEu(nightPower.toLong())} EU/t", color = 0xFFFFFF)
                }

                if (handler.machineSlotCount > 0) {
                    Flex(
                        direction = FlexDirection.ROW,
                        alignItems = AlignItems.CENTER,
                        gap = 4
                    ) {
                        Text(chargeLabel.string, color = 0xAAAAAA, shadow = false)
                        for (i in 0 until handler.machineSlotCount) {
                            SlotAnchor(
                                id = slotAnchorId(i),
                                width = 18,
                                height = 18
                            )
                        }
                    }
                }
            }

            playerInventoryAndHotbarSlotAnchors(
                left = left,
                top = top,
                playerInvStart = handler.machineSlotCount,
                playerInvY = GUI_SIZE.playerInvY,
                hotbarY = GUI_SIZE.hotbarY
            )
        }

        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)
        applyAnchoredSlots(layout, left, top)

        // 先绘制面板背景
        GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y,
            GUI_SIZE.playerInvY,
            GUI_SIZE.hotbarY,
            GuiSize.SLOT_SIZE
        )

        // 再绘制 UI（slot 背景、能量条等）
        ui.render(context, textRenderer, mouseX, mouseY, content = content)

        // 最后绘制物品（包括耐久条），确保物品在顶层
        super.render(context, mouseX, mouseY, delta)

        context.drawText(textRenderer, genText, sideTextX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, outputText, sideTextX, top + 20, 0xAAAAAA, false)
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun applyAnchoredSlots(layout: ComposeUI.LayoutSnapshot, left: Int, top: Int) {
        handler.slots.forEachIndexed { index, slot ->
            val anchor = layout.anchors[slotAnchorId(index)] ?: return@forEachIndexed
            slot.x = anchor.x - left
            slot.y = anchor.y - top
        }
    }

    private fun slotAnchorId(slotIndex: Int): String = "slot.$slotIndex"

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (ui.mouseClicked(mouseX, mouseY, button)) return true
        return super.mouseClicked(mouseX, mouseY, button)
    }

    companion object {
        private val GUI_SIZE = GuiSize.STANDARD
    }
}
