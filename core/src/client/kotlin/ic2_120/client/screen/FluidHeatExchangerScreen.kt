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
import net.minecraft.text.Text as McText

@ModScreen(block = FluidHeatExchangerBlock::class)
class FluidHeatExchangerScreen(
    handler: FluidHeatExchangerScreenHandler, playerInventory: PlayerInventory, title: McText
) : HandledScreen<FluidHeatExchangerScreenHandler>(handler, playerInventory, title) {

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
        val inputFraction =
            (handler.sync.inputFluidMb.toFloat() / FluidHeatExchangerSync.TANK_CAPACITY_MB).coerceIn(0f, 1f)
        val outputFraction =
            (handler.sync.outputFluidMb.toFloat() / FluidHeatExchangerSync.TANK_CAPACITY_MB).coerceIn(0f, 1f)
        val exchangerCount = FluidHeatExchangerBlockEntity.SLOT_EXCHANGER_INDICES.count { handler.slots[it].hasStack() }
        val generatedRate = handler.sync.getSyncedGeneratedHeat()
        val outputRate = handler.sync.getSyncedOutputHeat()

        val content: UiScope.() -> Unit = {
            Row(
                x = left + 8, y = top + 8, spacing = 8, modifier = Modifier.EMPTY.width(GUI_SIZE.contentWidth)
            ) {
                // 三列：输入流体柱 | 交换器+信息 | 输出流体柱
                Flex(
                    direction = FlexDirection.ROW,
                    justifyContent = JustifyContent.SPACE_BETWEEN,
                    alignItems = AlignItems.CENTER,
                    modifier = Modifier().width(GUI_SIZE.contentWidth - 24)/*.height(GUI_SIZE.contentHeight)*/
                ) {
                    // 输入流体柱：满容器 → 液位条 → 空容器
                    Flex(direction = FlexDirection.COLUMN, alignItems = AlignItems.CENTER, gap = 2) {
                        Text(t("gui.ic2_120.fluid_heat_exchanger.input_fluid"), color = 0xAAAAAA)
                        SlotHost(FluidHeatExchangerScreenHandler.SLOT_INPUT_FILLED_CONTAINER_INDEX)
                        FluidBar(
                            inputFraction,
                            barWidth = 8,
                            barHeight = 58,
                            fullColor = HOT_COOLANT_COLOR,
                            gradient = false,
                            vertical = true,
                            modifier = Modifier.EMPTY.width(8).height(58)
                        )
                        SlotHost(FluidHeatExchangerScreenHandler.SLOT_INPUT_EMPTY_CONTAINER_INDEX)
                        Text("${handler.sync.inputFluidMb} mB", color = 0xFFFFFF, shadow = false)
                    }

                    // 中间列：标题 + 状态 + 交换器
                    Flex(
                        gap = 4,
                        direction = FlexDirection.COLUMN,
                        justifyContent = JustifyContent.START,
                        alignItems = AlignItems.CENTER,
                        modifier = Modifier().fractionWidth(1f)
                    ) {
                        Text(title.string, color = 0xFFFFFF)
                        Text(
                            t("gui.ic2_120.fluid_heat_exchanger.exchanger_count", exchangerCount), color = 0xAAAAAA
                        )
                        Text(
                            if (handler.sync.isWorking != 0) t("gui.ic2_120.status_working") else t("gui.ic2_120.status_stopped"),
                            color = 0xAAAAAA,
                            shadow = false
                        )
                        Text(
                            t("gui.ic2_120.fluid_heat_exchanger.heat_line", generatedRate, outputRate), color = 0xAAAAAA
                        )
                        Row(spacing = 4) {
                            repeat(5) { index ->
                                SlotHost(FluidHeatExchangerScreenHandler.EXCHANGER_SLOT_INDEX_START + index)
                            }
                        }
                        Row(spacing = 4) {
                            repeat(5) { index ->
                                SlotHost(FluidHeatExchangerScreenHandler.EXCHANGER_SLOT_INDEX_START + index + 5)
                            }
                        }
                    }

                    // 输出流体柱：空容器 → 液位条 → 满容器
                    Flex(direction = FlexDirection.COLUMN, alignItems = AlignItems.CENTER, gap = 2) {
                        Text(t("gui.ic2_120.fluid_heat_exchanger.output_fluid"), color = 0xAAAAAA)
                        SlotHost(FluidHeatExchangerScreenHandler.SLOT_OUTPUT_EMPTY_CONTAINER_INDEX)
                        FluidBar(
                            outputFraction,
                            barWidth = 8,
                            barHeight = 58,
                            fullColor = COOLANT_COLOR,
                            gradient = false,
                            vertical = true,
                            modifier = Modifier.EMPTY.width(8).height(58)
                        )
                        SlotHost(FluidHeatExchangerScreenHandler.SLOT_OUTPUT_FILLED_CONTAINER_INDEX)
                        Text("${handler.sync.outputFluidMb} mB", color = 0xFFFFFF, shadow = false)
                    }
                }

                // 升级槽列
                Column(
                    spacing = 4, modifier = Modifier.EMPTY.width(GuiSize.UPGRADE_COLUMN_WIDTH).padding(0, 8, 0, 0)
                ) {
                    for (slotIndex in FluidHeatExchangerScreenHandler.SLOT_UPGRADE_START..FluidHeatExchangerScreenHandler.SLOT_UPGRADE_END) {
                        SlotHost(slotIndex)
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
        applyAnchoredSlots(layout, left, top)

        // 3) 先绘制面板背景
        GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y, GUI_SIZE.playerInvY, GUI_SIZE.hotbarY, GuiSize.SLOT_SIZE
        )

        // 4) 再绘制 UI（slot 背景），确保它们在物品下方
        ui.render(context, textRenderer, mouseX, mouseY, content = content)

        // 5) 最后绘制物品（包括耐久条），确保物品在顶层
        super.render(context, mouseX, mouseY, delta)

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun UiScope.SlotHost(slotIndex: Int) {
        SlotAnchor(
            id = slotAnchorId(slotIndex),
            width = FluidHeatExchangerScreenHandler.SLOT_SIZE,
            height = FluidHeatExchangerScreenHandler.SLOT_SIZE
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
        private val GUI_SIZE = GuiSize.UPGRADE_TALL

        /** 热冷却液颜色（输入侧） */
        private const val HOT_COOLANT_COLOR = 0xFFDD5500.toInt()

        /** 冷却液颜色（输出侧） */
        private const val COOLANT_COLOR = 0xFF22AADD.toInt()
    }
}
