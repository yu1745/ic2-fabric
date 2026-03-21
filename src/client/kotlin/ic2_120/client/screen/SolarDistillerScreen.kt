package ic2_120.client.screen

import ic2_120.client.compose.ComposeUI
import ic2_120.client.compose.*
import ic2_120.client.ui.FluidBar
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.SolarDistillerBlock
import ic2_120.content.screen.SolarDistillerScreenHandler
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.sync.SolarDistillerSync
import ic2_120.registry.annotation.ModScreen
import ic2_120.registry.type
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.slot.Slot
import net.minecraft.text.Text as McText

@ModScreen(block = SolarDistillerBlock::class)
class SolarDistillerScreen(
    handler: SolarDistillerScreenHandler,
    playerInventory: PlayerInventory,
    title: McText
) : HandledScreen<SolarDistillerScreenHandler>(handler, playerInventory, title) {

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
        titleY = 4
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context,
            x,
            y,
            SolarDistillerScreenHandler.PLAYER_INV_Y,
            SolarDistillerScreenHandler.HOTBAR_Y,
            SolarDistillerScreenHandler.SLOT_SIZE
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y

        val inputFraction = (handler.sync.waterInputMb.toFloat() / SolarDistillerSync.TANK_CAPACITY_MB).coerceIn(0f, 1f)
        val outputFraction = (handler.sync.distilledOutputMb.toFloat() / SolarDistillerSync.TANK_CAPACITY_MB).coerceIn(0f, 1f)
        val progressFraction = (handler.sync.progress.toFloat() / SolarDistillerSync.PRODUCE_INTERVAL_TICKS).coerceIn(0f, 1f)

        val content: UiScope.() -> Unit = {
            Column(
                x = left + 8,
                y = top + 8,
                spacing = 6,
                modifier = Modifier().width(backgroundWidth - 16).height(backgroundHeight - 16),
            ) {
                Text(title.string, color = 0xFFFFFF)

                // 三个流体槽 + 进度条
                Flex(
                    direction = FlexDirection.ROW,
                    justifyContent = JustifyContent.SPACE_BETWEEN,
                ) {
                    Column(spacing = 2) {
                        Text("输入水", color = 0xAAAAAA)
                        FluidBar(inputFraction, barWidth = 8, barHeight = 58, vertical = true, modifier = Modifier.EMPTY.width(8).height(58))
                        Text("${handler.sync.waterInputMb} mB", color = 0xFFFFFF, shadow = false)
                    }
                    Column(spacing = 2) {
                        Text("蒸馏进度", color = 0xAAAAAA)
                        FluidBar(progressFraction, barWidth = 8, barHeight = 58, vertical = true, modifier = Modifier.EMPTY.width(8).height(58))
                        Text("${handler.sync.progress}/${SolarDistillerSync.PRODUCE_INTERVAL_TICKS}", color = 0xFFFFFF, shadow = false)
                    }
                    Column(spacing = 2) {
                        Text("输出液", color = 0xAAAAAA)
                        FluidBar(outputFraction, barWidth = 8, barHeight = 58, vertical = true, modifier = Modifier.EMPTY.width(8).height(58))
                        Text("${handler.sync.distilledOutputMb} mB", color = 0xFFFFFF, shadow = false)
                    }
                }

                Text(
                    if (handler.sync.isWorking != 0) "状态: 工作中（80 tick -> 1 mB）" else "状态: 停止",
                    color = 0xAAAAAA,
                    shadow = false
                )

                // 8个槽位（2行4列）
                Flex(
                    direction = FlexDirection.ROW,
                    justifyContent = JustifyContent.SPACE_BETWEEN,
                ) {
                    Row(spacing = 4) {
                        repeat(4) { index ->
                            SlotAnchor(id = "slot.$index")
                        }
                    }
                }
                Flex(
                    direction = FlexDirection.ROW,
                    justifyContent = JustifyContent.SPACE_BETWEEN,
                ) {
                    Row(spacing = 4) {
                        repeat(4) { index ->
                            SlotAnchor(id = "slot.${index + 4}")
                        }
                    }
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

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    companion object {
        private val PANEL_WIDTH = GuiSize.STANDARD_UPGRADE.width
        private val PANEL_HEIGHT = GuiSize.STANDARD_UPGRADE.height
    }
}
