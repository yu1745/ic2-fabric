package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.EnergyFormatUtils
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.FluidBar
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.FluidBottlerBlock
import ic2_120.content.screen.FluidBottlerScreenHandler
import ic2_120.content.screen.GuiSize
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.sync.FluidBottlerSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.slot.Slot
import net.minecraft.text.Text

@ModScreen(block = FluidBottlerBlock::class)
class FluidBottlerScreen(
    handler: FluidBottlerScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<FluidBottlerScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = PANEL_WIDTH
        backgroundHeight = PANEL_HEIGHT
        titleY = 4
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context,
            x,
            y,
            GUI_SIZE.playerInvY,
            GUI_SIZE.hotbarY,
            GuiSize.SLOT_SIZE
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1)
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val fluidAmount = handler.sync.fluidAmountMb.toLong()
        val fluidCapacity = handler.sync.fluidCapacityMb.toLong().coerceAtLeast(1)
        val fluidFraction = if (fluidCapacity > 0) (fluidAmount.toFloat() / fluidCapacity).coerceIn(0f, 1f) else 0f
        val progress = handler.sync.progress.coerceIn(0, FluidBottlerSync.PROGRESS_MAX)
        val progressFrac = (progress.toFloat() / FluidBottlerSync.PROGRESS_MAX).coerceIn(0f, 1f)
//        val contentW = (backgroundWidth - 16).coerceAtLeast(0)

        val energyText = "$energy / $cap EU"
        val fluidText = "$fluidAmount/$fluidCapacity mB"
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val consumeRate = handler.sync.getSyncedConsumedAmount()
        val inputText = "输入 ${EnergyFormatUtils.formatEu(inputRate)} EU/t"
        val consumeText = "耗能 ${EnergyFormatUtils.formatEu(consumeRate)} EU/t"
        val sideTextWidth = maxOf(
            textRenderer.getWidth(energyText),
            textRenderer.getWidth(fluidText),
            textRenderer.getWidth(inputText),
            textRenderer.getWidth(consumeText)
        )
        val sideTextX = left - sideTextWidth - 4

        val content: UiScope.() -> Unit = {
            Row(
                x = left + 8,
                y = top + 8,
                spacing = 8,
                modifier = Modifier.EMPTY.width(GUI_SIZE.contentWidth)
            ) {
                Column(
                    spacing = 6,
                    modifier = Modifier.EMPTY.width(GuiSize.STANDARD.contentWidth)
                ) {
                    Flex(direction = FlexDirection.ROW, alignItems = AlignItems.CENTER, gap = 8) {
                        Text(title.string, color = 0xFFFFFF)
                        Text(energyText, color = 0xFFFFFF, shadow = false)
                        Text(fluidText, color = 0xFFFFFF, shadow = false)
                    }
                    EnergyBar(energyFraction)

                    Flex(
                        direction = FlexDirection.ROW,
                        alignItems = AlignItems.CENTER,
                        gap = 4
                    ) {
                        SlotHost(FluidBottlerScreenHandler.SLOT_INPUT_FILLED_INDEX)
                        SlotHost(FluidBottlerScreenHandler.SLOT_INPUT_EMPTY_INDEX)
                        EnergyBar(progressFrac, modifier = Modifier.EMPTY.fractionWidth(1.0f))
                        SlotHost(FluidBottlerScreenHandler.SLOT_OUTPUT_INDEX)
                    }

                    SlotHost(FluidBottlerScreenHandler.SLOT_DISCHARGING_INDEX)
                }

                Column(
                    spacing = 4,
                    modifier = Modifier.EMPTY
                        .width(GuiSize.UPGRADE_COLUMN_WIDTH)
                        .padding(0, 8, 0, 0)
                ) {
                    for (slotIndex in FluidBottlerScreenHandler.SLOT_UPGRADE_INDEX_START..FluidBottlerScreenHandler.SLOT_UPGRADE_INDEX_END) {
                        SlotHost(slotIndex)
                    }
                }
            }

            playerInventoryAndHotbarSlotAnchors(
                left = left,
                top = top,
                playerInvStart = FluidBottlerScreenHandler.PLAYER_INV_START,
                playerInvY = GUI_SIZE.playerInvY,
                hotbarY = GUI_SIZE.hotbarY
            )
        }

        // 1) 预布局，不绘制
        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)

        // 2) 锚点写回 slot 相对坐标
        applyAnchoredSlots(layout, left, top)

        // 3) 原生 slot 渲染 + 交互
        super.render(context, mouseX, mouseY, delta)

        // 4) Compose overlay
        ui.render(context, textRenderer, mouseX, mouseY, content = content)
        context.drawText(textRenderer, inputText, sideTextX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, consumeText, sideTextX, top + 20, 0xAAAAAA, false)

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun UiScope.SlotHost(slotIndex: Int) {
        SlotAnchor(
            id = slotAnchorId(slotIndex),
            width = FluidBottlerScreenHandler.SLOT_SIZE,
            height = FluidBottlerScreenHandler.SLOT_SIZE
        )
    }

    private fun applyAnchoredSlots(layout: ComposeUI.LayoutSnapshot, left: Int, top: Int) {
        handler.slots.forEachIndexed { index, slot ->
            val anchor = layout.anchors[slotAnchorId(index)] ?: return@forEachIndexed
            slot.x = anchor.x - left
            slot.y = anchor.y - top
        }
    }

    private fun slotAnchorId(slotIndex: Int): String = "slot.$slotIndex"

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    companion object {
        private val GUI_SIZE = GuiSize.STANDARD_UPGRADE
        private val PANEL_WIDTH = GUI_SIZE.width
        private val PANEL_HEIGHT = GUI_SIZE.height
    }
}
