package ic2_120.client.screen

import ic2_120.client.compose.ComposeUI
import ic2_120.client.compose.*
import ic2_120.client.t
import ic2_120.client.ui.FluidBar
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.FluidHeatExchangerBlock
import ic2_120.content.block.machines.FluidHeatExchangerBlockEntity
import ic2_120.content.screen.FluidHeatExchangerScreenHandler
import ic2_120.content.screen.GuiSize
import ic2_120.content.sync.FluidHeatExchangerSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.slot.Slot
import net.minecraft.text.Text as McText

@ModScreen(block = FluidHeatExchangerBlock::class)
class FluidHeatExchangerScreen(
    handler: FluidHeatExchangerScreenHandler,
    playerInventory: PlayerInventory,
    title: McText
) : HandledScreen<FluidHeatExchangerScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = GUI_SIZE.width
        backgroundHeight = GUI_SIZE.height
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
        val centerX = left + backgroundWidth / 2
        val inputFraction = (handler.sync.inputFluidMb.toFloat() / FluidHeatExchangerSync.TANK_CAPACITY_MB).coerceIn(0f, 1f)
        val outputFraction = (handler.sync.outputFluidMb.toFloat() / FluidHeatExchangerSync.TANK_CAPACITY_MB).coerceIn(0f, 1f)
        val exchangerCount = FluidHeatExchangerBlockEntity.SLOT_EXCHANGER_INDICES.count { handler.slots[it].hasStack() }
        val generatedRate = handler.sync.getSyncedGeneratedHeat()
        val outputRate = handler.sync.getSyncedOutputHeat()

        val content: UiScope.() -> Unit = {
            Column(
                x = left + 8,
                y = top + 8,
                spacing = 6,
                modifier = Modifier().width(backgroundWidth - 16).height(backgroundHeight - 16),
            ) {
                // 标题居中
                Flex(
                    direction = FlexDirection.ROW,
                    justifyContent = JustifyContent.CENTER,
                ) {
                    Text(title.string, color = 0xFFFFFF)
                }
                Flex(
                    direction = FlexDirection.ROW,
                    justifyContent = JustifyContent.CENTER,
                ) {
                    Text(t("gui.ic2_120.fluid_heat_exchanger.exchanger_count", exchangerCount), color = 0xAAAAAA)
                }

                // 两个流体槽
                Flex(
                    direction = FlexDirection.ROW,
                    justifyContent = JustifyContent.SPACE_BETWEEN,
                ) {
                    Column(spacing = 2) {
                        Text(t("gui.ic2_120.fluid_heat_exchanger.input_fluid"), color = 0xAAAAAA)
                        FluidBar(inputFraction, barWidth = 8, barHeight = 58, vertical = true, modifier = Modifier.EMPTY.width(8).height(58))
                        Text("${handler.sync.inputFluidMb} mB", color = 0xFFFFFF, shadow = false)
                    }
                    Column(spacing = 2) {
                        Text(t("gui.ic2_120.fluid_heat_exchanger.output_fluid"), color = 0xAAAAAA)
                        FluidBar(outputFraction, barWidth = 8, barHeight = 58, vertical = true, modifier = Modifier.EMPTY.width(8).height(58))
                        Text("${handler.sync.outputFluidMb} mB", color = 0xFFFFFF, shadow = false)
                    }
                }

                // 状态信息
                Flex(
                    direction = FlexDirection.ROW,
                    justifyContent = JustifyContent.CENTER,
                ) {
                    Text(
                        if (handler.sync.isWorking != 0) t("gui.ic2_120.status_working") else t("gui.ic2_120.status_stopped"),
                        color = 0xAAAAAA,
                        shadow = false
                    )
                }
                Flex(
                    direction = FlexDirection.ROW,
                    justifyContent = JustifyContent.CENTER,
                ) {
                    Text(t("gui.ic2_120.fluid_heat_exchanger.heat_line", generatedRate, outputRate), color = 0xAAAAAA)
                }

                // 机器槽位（8个槽位）
                Flex(
                    direction = FlexDirection.ROW,
                    justifyContent = JustifyContent.SPACE_BETWEEN,
                ) {
                    Row(spacing = 4) {
                        repeat(4) { index ->
                            val slotIndex = FluidHeatExchangerScreenHandler.EXCHANGER_SLOT_INDEX_START + index
                            SlotAnchor(id = "slot.$slotIndex")
                        }
                    }
                }
                Flex(
                    direction = FlexDirection.ROW,
                    justifyContent = JustifyContent.SPACE_BETWEEN,
                ) {
                    Row(spacing = 4) {
                        repeat(4) { index ->
                            val slotIndex = FluidHeatExchangerScreenHandler.EXCHANGER_SLOT_INDEX_START + index + 4
                            SlotAnchor(id = "slot.$slotIndex")
                        }
                    }
                }
            }

            playerInventoryAndHotbarSlotAnchors(
                left = left,
                top = top,
                playerInvStart = FluidHeatExchangerScreenHandler.PLAYER_INV_START,
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

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    companion object {
        private val GUI_SIZE = GuiSize.STANDARD_TALL
    }
}
