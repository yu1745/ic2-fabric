package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.t
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.FluidHeatGeneratorBlock
import ic2_120.content.block.machines.FluidHeatGeneratorBlockEntity
import ic2_120.content.screen.FluidHeatGeneratorScreenHandler
import ic2_120.content.screen.GuiSize
import ic2_120.registry.annotation.ModScreen
import ic2_120.registry.type
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text as McText

@ModScreen(block = FluidHeatGeneratorBlock::class)
class FluidHeatGeneratorScreen(
    handler: FluidHeatGeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: McText
) : HandledScreen<FluidHeatGeneratorScreenHandler>(handler, playerInventory, title) {
    private val ui = ComposeUI()

    init {
        backgroundWidth = GUI_SIZE.width
        backgroundHeight = GUI_SIZE.height
        titleY = 4
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context = context,
            screenX = x,
            screenY = y,
            playerInvY = GUI_SIZE.playerInvY,
            hotbarY = GUI_SIZE.hotbarY,
            slotSize = GuiSize.SLOT_SIZE
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y

        val fuel = handler.sync.fuelAmountMb.coerceAtLeast(0)
        val generatedRate = handler.sync.getSyncedGeneratedHeat()
        val outputRate = handler.sync.getSyncedOutputHeat()

        val fuelText = t("gui.ic2_120.fluid_heat_generator.fuel", fuel)
        val generatedText = t("gui.ic2_120.generate_hu", generatedRate)
        val outputText = t("gui.ic2_120.output_hu", outputRate)
        val sideTextWidth = maxOf(
            textRenderer.getWidth(fuelText),
            textRenderer.getWidth(generatedText),
            textRenderer.getWidth(outputText)
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
                    Row(spacing = 8) {
                        Text(title.string, color = 0xFFFFFF)
                        Text(fuelText, color = 0xFFFFFF)
                    }

                    // 机器槽位（燃料容器和空容器）
                    Flex(
                        direction = FlexDirection.ROW,
                        justifyContent = JustifyContent.SPACE_BETWEEN,
                    ) {
                        SlotAnchor(id = "slot.${FluidHeatGeneratorBlockEntity.FUEL_SLOT}")
                        SlotAnchor(id = "slot.${FluidHeatGeneratorBlockEntity.EMPTY_CONTAINER_SLOT}")
                    }
                }

                Column(
                    spacing = 4,
                    modifier = Modifier.EMPTY
                        .width(GuiSize.UPGRADE_COLUMN_WIDTH)
                        .padding(0, 8, 0, 0)
                ) {
                    for (slotIndex in FluidHeatGeneratorScreenHandler.SLOT_UPGRADE_START..FluidHeatGeneratorScreenHandler.SLOT_UPGRADE_END) {
                        SlotAnchor(
                            id = "slot.$slotIndex",
                            width = FluidHeatGeneratorScreenHandler.SLOT_SIZE,
                            height = FluidHeatGeneratorScreenHandler.SLOT_SIZE
                        )
                    }
                }
            }

            playerInventoryAndHotbarSlotAnchors(
                left = left,
                top = top,
                playerInvStart = FluidHeatGeneratorScreenHandler.PLAYER_INV_START,
                playerInvY = GUI_SIZE.playerInvY,
                hotbarY = GUI_SIZE.hotbarY
            )
        }

        // 1) 预布局，不绘制
        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)

        // 2) 锚点写回 slot 相对坐标
        handler.slots.forEachIndexed { index, slot ->
            val anchor = layout.anchors["slot.$index"] ?: return@forEachIndexed
            slot.x = anchor.x - left
            slot.y = anchor.y - top
        }

        // 3) 原生 slot 渲染 + 交互
        super.render(context, mouseX, mouseY, delta)

        // 4) Compose overlay
        ui.render(context, textRenderer, mouseX, mouseY, content = content)
        context.drawText(textRenderer, generatedText, sideTextX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, outputText, sideTextX, top + 20, 0xAAAAAA, false)

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    companion object {
        private val GUI_SIZE = GuiSize.STANDARD_UPGRADE
    }
}

