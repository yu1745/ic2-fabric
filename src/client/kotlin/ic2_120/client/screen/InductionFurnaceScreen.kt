package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.client.ui.ProgressBar
import ic2_120.content.sync.InductionFurnaceSync
import ic2_120.content.block.InductionFurnaceBlock
import ic2_120.content.screen.InductionFurnaceScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.slot.Slot
import net.minecraft.text.Text

/**
 * 感应炉 GUI（客户端）。
 * 显示：能量条、热量指示，双槽进度条。
 */
@ModScreen(block = InductionFurnaceBlock::class)
class InductionFurnaceScreen(
    handler: InductionFurnaceScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<InductionFurnaceScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()
    private val slotXField by lazy {
        Slot::class.java.getDeclaredField("x").apply { isAccessible = true }
    }
    private val slotYField by lazy {
        Slot::class.java.getDeclaredField("y").apply { isAccessible = true }
    }

    init {
        backgroundWidth = GUI_SIZE.width
        backgroundHeight = GUI_SIZE.height
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context,
            x,
            y,
            InductionFurnaceScreenHandler.PLAYER_INV_Y,
            InductionFurnaceScreenHandler.HOTBAR_Y,
            InductionFurnaceScreenHandler.SLOT_SIZE
        )

        // 双槽进度条（与 BlockEntity 一致：progressNeeded = baseTicks * HEAT_MAX / heat）
        val baseTicks = InductionFurnaceSync.BASE_TICKS_PER_OPERATION
        val heat = handler.sync.heat.coerceAtLeast(InductionFurnaceSync.MIN_HEAT_THRESHOLD)
        val progressNeeded = (baseTicks * InductionFurnaceSync.HEAT_MAX / heat).toInt().coerceAtLeast(baseTicks.toInt())

        val layout = ui.layout(context, textRenderer, mouseX, mouseY) {
            Column(
                x = x + 8,
                y = y + 8,
                spacing = 6,
                modifier = Modifier.EMPTY.width(GUI_SIZE.contentWidth)
            ) {
                Flex(
                    direction = FlexDirection.ROW,
                    justifyContent = JustifyContent.SPACE_BETWEEN,
                    alignItems = AlignItems.CENTER,
                    gap = 8,
                ) {
                    Text(title.string, color = 0xFFFFFF)
                }

                // 双槽位布局（仅用于定位）
                Flex(
                    direction = FlexDirection.ROW,
                    alignItems = AlignItems.CENTER,
                    gap = 4
                ) {
                    Column(spacing = 4) {
                        SlotAnchor(
                            id = "anchor.input0",
                            width = InductionFurnaceScreenHandler.SLOT_SIZE,
                            height = InductionFurnaceScreenHandler.SLOT_SIZE
                        )
                        SlotAnchor(
                            id = "anchor.input1",
                            width = InductionFurnaceScreenHandler.SLOT_SIZE,
                            height = InductionFurnaceScreenHandler.SLOT_SIZE
                        )
                    }
                    SlotAnchor(
                        id = "anchor.progressArea",
                        width = 40,
                        height = InductionFurnaceScreenHandler.SLOT_SIZE * 2 + 4
                    )
                    Column(spacing = 4) {
                        SlotAnchor(
                            id = "anchor.output0",
                            width = InductionFurnaceScreenHandler.SLOT_SIZE,
                            height = InductionFurnaceScreenHandler.SLOT_SIZE
                        )
                        SlotAnchor(
                            id = "anchor.output1",
                            width = InductionFurnaceScreenHandler.SLOT_SIZE,
                            height = InductionFurnaceScreenHandler.SLOT_SIZE
                        )
                    }
                    SlotAnchor(
                        id = "anchor.discharging",
                        width = InductionFurnaceScreenHandler.SLOT_SIZE,
                        height = InductionFurnaceScreenHandler.SLOT_SIZE
                    )
                }
            }
        }

        // 绘制进度条
        val input0Anchor = layout.anchors["anchor.input0"]
        val input1Anchor = layout.anchors["anchor.input1"]
        val progressAnchor = layout.anchors["anchor.progressArea"]

        if (input0Anchor != null && input1Anchor != null && progressAnchor != null) {
            val barH = 6
            val barW = progressAnchor.w

            // 槽 0 进度
            val progress0 = handler.sync.progressSlot0.coerceIn(0, progressNeeded)
            val progressFrac0 = if (progressNeeded > 0) progress0.toFloat() / progressNeeded else 0f
            val barY0 = input0Anchor.y + (InductionFurnaceScreenHandler.SLOT_SIZE - barH) / 2
            ProgressBar.draw(context, progressAnchor.x, barY0, barW, barH, progressFrac0)

            // 槽 1 进度
            val progress1 = handler.sync.progressSlot1.coerceIn(0, progressNeeded)
            val progressFrac1 = if (progressNeeded > 0) progress1.toFloat() / progressNeeded else 0f
            val barY1 = input1Anchor.y + (InductionFurnaceScreenHandler.SLOT_SIZE - barH) / 2
            ProgressBar.draw(context, progressAnchor.x, barY1, barW, barH, progressFrac1)
        }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y

        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1)
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f

        val inputRate = handler.sync.getSyncedInsertedAmount()
        val consumeRate = handler.sync.getSyncedConsumedAmount()

        val inputText = "输入 ${formatEu(inputRate)} EU/t"
        val consumeText = "耗能 ${formatEu(consumeRate)} EU/t"
        val sideTextWidth = maxOf(textRenderer.getWidth(inputText), textRenderer.getWidth(consumeText))
        val sideTextX = left - sideTextWidth - 4

        val heatFactor = handler.sync.heat / InductionFurnaceSync.HEAT_MAX.toFloat()
        val heatPercent = handler.sync.heat / 100

        val content: UiScope.() -> Unit = {
            Column(
                x = left + 8,
                y = top + 8,
                spacing = 6,
                modifier = Modifier.EMPTY.width(GUI_SIZE.contentWidth)
            ) {
                Flex(direction = FlexDirection.ROW, alignItems = AlignItems.CENTER, gap = 8) {
                    Text(title.string, color = 0xFFFFFF)
                    Text("$energy / $cap EU", color = 0xFFFFFF, shadow = false)
                }
                EnergyBar(
                    energyFraction,
                    barHeight = 12,
                )

                // 双槽位布局（用于实际槽位定位）
                Flex(
                    direction = FlexDirection.ROW,
                    alignItems = AlignItems.CENTER,
                    gap = 4
                ) {
                    Column(spacing = 4) {
                        SlotHost(InductionFurnaceScreenHandler.SLOT_INPUT_0_INDEX)
                        SlotHost(InductionFurnaceScreenHandler.SLOT_INPUT_1_INDEX)
                    }
                    Column(spacing = 4) {
                        // 占位，保持布局一致（进度条在 drawBackground 中绘制）
                        SlotAnchor(
                            id = "placeholder.progress0",
                            width = 40,
                            height = InductionFurnaceScreenHandler.SLOT_SIZE,
                            showBorder = false
                        )
                        SlotAnchor(
                            id = "placeholder.progress1",
                            width = 40,
                            height = InductionFurnaceScreenHandler.SLOT_SIZE,
                            showBorder = false
                        )
                    }
                    Column(spacing = 4) {
                        SlotHost(InductionFurnaceScreenHandler.SLOT_OUTPUT_0_INDEX)
                        SlotHost(InductionFurnaceScreenHandler.SLOT_OUTPUT_1_INDEX)
                    }
                    SlotHost(InductionFurnaceScreenHandler.SLOT_DISCHARGING_INDEX)
                }

                // 热量条
                Flex(
                    direction = FlexDirection.ROW,
                    alignItems = AlignItems.CENTER,
                    gap = 8
                ) {
                    Text("热量 $heatPercent%", color = 0xFFFFFF)
                    EnergyBar(
                        heatFactor,
                        barHeight = 8,
                        modifier = Modifier.EMPTY.fractionWidth(1.0f)
                    )
                }
            }
        }

        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)
        applyAnchoredSlots(layout, left, top)

        super.render(context, mouseX, mouseY, delta)
        ui.render(context, textRenderer, mouseX, mouseY, content = content)
        context.drawText(textRenderer, inputText, sideTextX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, consumeText, sideTextX, top + 20, 0xAAAAAA, false)
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun UiScope.SlotHost(slotIndex: Int) {
        SlotAnchor(
            id = slotAnchorId(slotIndex),
            width = InductionFurnaceScreenHandler.SLOT_SIZE,
            height = InductionFurnaceScreenHandler.SLOT_SIZE
        )
    }

    private fun applyAnchoredSlots(layout: ComposeUI.LayoutSnapshot, left: Int, top: Int) {
        handler.slots.forEachIndexed { index, slot ->
            val anchor = layout.anchors[slotAnchorId(index)] ?: return@forEachIndexed
            slotXField.setInt(slot, anchor.x - left)
            slotYField.setInt(slot, anchor.y - top)
        }
    }

    private fun slotAnchorId(slotIndex: Int): String = "slot.$slotIndex"

    private fun formatEu(value: Long): String {
        return when {
            value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
            value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
            else -> value.toString()
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (ui.mouseClicked(mouseX, mouseY, button)) return true
        return super.mouseClicked(mouseX, mouseY, button)
    }

    companion object {
        private val GUI_SIZE = GuiSize.STANDARD
    }
}
