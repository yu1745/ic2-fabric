package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.FluidHeatGeneratorBlock
import ic2_120.content.block.machines.FluidHeatGeneratorBlockEntity
import ic2_120.content.screen.FluidHeatGeneratorScreenHandler
import ic2_120.registry.annotation.ModScreen
import ic2_120.registry.type
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.slot.Slot
import net.minecraft.text.Text as McText

@ModScreen(block = FluidHeatGeneratorBlock::class)
class FluidHeatGeneratorScreen(
    handler: FluidHeatGeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: McText
) : HandledScreen<FluidHeatGeneratorScreenHandler>(handler, playerInventory, title) {
    private val ui = ComposeUI()

    private val slotXField by lazy {
        Slot::class.java.getDeclaredField("x").apply { isAccessible = true }
    }
    private val slotYField by lazy {
        Slot::class.java.getDeclaredField("y").apply { isAccessible = true }
    }

    init {
        backgroundWidth = PANEL_WIDTH
        backgroundHeight = PANEL_HEIGHT
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context = context,
            screenX = x,
            screenY = y,
            playerInvY = FluidHeatGeneratorScreenHandler.PLAYER_INV_Y,
            hotbarY = FluidHeatGeneratorScreenHandler.HOTBAR_Y,
            slotSize = FluidHeatGeneratorScreenHandler.SLOT_SIZE
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y

        val fuel = handler.sync.fuelAmountMb.coerceAtLeast(0)
        val generatedRate = handler.sync.getSyncedGeneratedHeat()
        val outputRate = handler.sync.getSyncedOutputHeat()

        val fuelText = "燃料: $fuel mB"
        val generatedText = "产热 $generatedRate HU/t"
        val outputText = "输出 $outputRate HU/t"
        val sideTextWidth = maxOf(
            textRenderer.getWidth(fuelText),
            textRenderer.getWidth(generatedText),
            textRenderer.getWidth(outputText)
        )
        val sideTextX = left - sideTextWidth - 4

        val content: UiScope.() -> Unit = {
            Column(
                x = left + 8,
                y = top + 8,
                spacing = 6,
                modifier = Modifier().width(backgroundWidth - 16).height(backgroundHeight - 16),
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
        }

        // 1) 预布局，不绘制
        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)

        // 2) 锚点写回 slot 相对坐标
        handler.slots.forEachIndexed { index, slot ->
            val anchor = layout.anchors["slot.$index"] ?: return@forEachIndexed
            slotXField.setInt(slot, anchor.x - left)
            slotYField.setInt(slot, anchor.y - top)
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
        private val PANEL_WIDTH = GuiSize.STANDARD.width
        private val PANEL_HEIGHT = GuiSize.STANDARD.height
    }
}

