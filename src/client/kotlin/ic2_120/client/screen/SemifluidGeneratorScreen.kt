package ic2_120.client.screen

import ic2_120.client.FluidUtils
import ic2_120.client.compose.*
import ic2_120.client.EnergyFormatUtils
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.EnergyBarOrientation
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.SemifluidGeneratorBlock
import ic2_120.content.block.machines.SemifluidGeneratorBlockEntity
import ic2_120.content.screen.SemifluidGeneratorScreenHandler
import ic2_120.content.screen.GuiSize
import ic2_120.content.sync.SemifluidGeneratorSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.registry.Registries
import net.minecraft.screen.slot.Slot
import net.minecraft.text.Text as McText

@ModScreen(block = SemifluidGeneratorBlock::class)
class SemifluidGeneratorScreen(
    handler: SemifluidGeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: McText
) : HandledScreen<SemifluidGeneratorScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = GUI_SIZE.width
        backgroundHeight = GUI_SIZE.height
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y,
            GUI_SIZE.playerInvY,
            GUI_SIZE.hotbarY,
            GuiSize.SLOT_SIZE
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val outputRate = handler.sync.getSyncedExtractedAmount()
        val cap = SemifluidGeneratorSync.ENERGY_CAPACITY
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f

        // 燃料储量
        val fuelMb = handler.sync.fuelAmountMb.coerceAtLeast(0)
        val fuelCapMb = 8 * 1000
        val fuelFrac = if (fuelCapMb > 0) (fuelMb.toFloat() / fuelCapMb).coerceIn(0f, 1f) else 0f

        // 燃料颜色
        val fluidRawId = handler.sync.fuelFluidRawId
        val sampledColor = if (fluidRawId >= 0) {
            val fluid = Registries.FLUID.get(fluidRawId)
            FluidUtils.getFluidColor(fluid)
        } else -1
        val fuelColor = if (sampledColor != -1) bgrToArgb(sampledColor) else handler.sync.fuelColorArgb
        handler.sync.fuelColorArgb = fuelColor

        val inputText = "发电 ${EnergyFormatUtils.formatEu(inputRate)} EU/t"
        val outputText = "输出 ${EnergyFormatUtils.formatEu(outputRate)} EU/t"
        val sideTextWidth = maxOf(textRenderer.getWidth(inputText), textRenderer.getWidth(outputText))
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
                        Text("$energy / $cap EU", color = 0xFFFFFF, shadow = false)
                    }
                    EnergyBar(
                        energyFraction,
                        barHeight = 12,
                    )

                    Flex(
                        direction = FlexDirection.ROW,
                        justifyContent = JustifyContent.SPACE_AROUND,
                        alignItems = AlignItems.CENTER,
                        gap = 4,
                        modifier = Modifier().height(60)
                    ) {
                        Flex(
                            direction = FlexDirection.COLUMN,
                            justifyContent = JustifyContent.SPACE_BETWEEN,
                            alignItems = AlignItems.CENTER,
                        ) {
                            SlotAnchor(
                                id = slotAnchorId(SemifluidGeneratorBlockEntity.FUEL_SLOT),
                                width = SemifluidGeneratorScreenHandler.SLOT_SIZE,
                                height = SemifluidGeneratorScreenHandler.SLOT_SIZE
                            )
                            SlotAnchor(
                                id = slotAnchorId(SemifluidGeneratorBlockEntity.EMPTY_CONTAINER_SLOT),
                                width = SemifluidGeneratorScreenHandler.SLOT_SIZE,
                                height = SemifluidGeneratorScreenHandler.SLOT_SIZE
                            )
                        }
                        // 燃料储量竖向条
                        EnergyBar(
                            fuelFrac,
                            orientation = EnergyBarOrientation.VERTICAL,
                            emptyColor = 0xFF333333.toInt(),
                            fullColor = fuelColor,
                            modifier = Modifier().fractionHeight(1f)
                        )
                        SlotAnchor(
                            id = slotAnchorId(SemifluidGeneratorBlockEntity.BATTERY_SLOT),
                            width = SemifluidGeneratorScreenHandler.SLOT_SIZE,
                            height = SemifluidGeneratorScreenHandler.SLOT_SIZE
                        )
                    }
                }

                Column(
                    spacing = 4,
                    modifier = Modifier.EMPTY
                        .width(GuiSize.UPGRADE_COLUMN_WIDTH)
                        .padding(0, 8, 0, 0)
                ) {
                    for (slotIndex in SemifluidGeneratorScreenHandler.SLOT_UPGRADE_INDEX_START..SemifluidGeneratorScreenHandler.SLOT_UPGRADE_INDEX_END) {
                        SlotAnchor(
                            id = slotAnchorId(slotIndex),
                            width = SemifluidGeneratorScreenHandler.SLOT_SIZE,
                            height = SemifluidGeneratorScreenHandler.SLOT_SIZE
                        )
                    }
                }
            }

            playerInventoryAndHotbarSlotAnchors(
                left = left,
                top = top,
                playerInvStart = SemifluidGeneratorScreenHandler.PLAYER_INV_START,
                playerInvY = GUI_SIZE.playerInvY,
                hotbarY = GUI_SIZE.hotbarY
            )
        }

        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)
        applyAnchoredSlots(layout, left, top)

        super.render(context, mouseX, mouseY, delta)
        ui.render(context, textRenderer, mouseX, mouseY, content = content)
        context.drawText(textRenderer, inputText, sideTextX, top + 8, 0xAAAAAA, false)
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

    private fun bgrToArgb(color: Int): Int {
        val a = (color ushr 24) and 0xFF
        val b = (color ushr 16) and 0xFF
        val g = (color ushr 8) and 0xFF
        val r = color and 0xFF
        val alpha = if (a == 0) 0xFF else a
        return (alpha shl 24) or (r shl 16) or (g shl 8) or b
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    companion object {
        private val GUI_SIZE = GuiSize.STANDARD_UPGRADE
    }
}
