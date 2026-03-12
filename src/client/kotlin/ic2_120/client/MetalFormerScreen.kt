package ic2_120.client

import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.client.ui.ProgressBar
import ic2_120.content.sync.MetalFormerSync
import ic2_120.content.block.MetalFormerBlock
import ic2_120.content.screen.MetalFormerScreenHandler
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket
import net.minecraft.text.Text

@ModScreen(block = MetalFormerBlock::class)
class MetalFormerScreen(
    handler: MetalFormerScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<MetalFormerScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()
    private var blockPos: net.minecraft.util.math.BlockPos? = null

    init {
        backgroundWidth = PANEL_WIDTH
        backgroundHeight = PANEL_HEIGHT
        titleY = 4
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.draw(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y,
            MetalFormerScreenHandler.PLAYER_INV_Y,
            MetalFormerScreenHandler.HOTBAR_Y,
            MetalFormerScreenHandler.SLOT_SIZE
        )

        val borderColor = GuiBackground.BORDER_COLOR
        val slotSize = MetalFormerScreenHandler.SLOT_SIZE
        val borderOffset = 1

        // 绘制所有机器槽位的边框
        val inputSlot = handler.slots[MetalFormerScreenHandler.SLOT_INPUT_INDEX]
        val dischargingSlot = handler.slots[MetalFormerScreenHandler.SLOT_DISCHARGING_INDEX]
        val outputSlot = handler.slots[MetalFormerScreenHandler.SLOT_OUTPUT_INDEX]
        val secondaryInputSlot = handler.slots[MetalFormerScreenHandler.SLOT_SECONDARY_INPUT_INDEX]

        context.drawBorder(x + inputSlot.x - borderOffset, y + inputSlot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + dischargingSlot.x - borderOffset, y + dischargingSlot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + outputSlot.x - borderOffset, y + outputSlot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + secondaryInputSlot.x - borderOffset, y + secondaryInputSlot.y - borderOffset, slotSize, slotSize, borderColor)

        for (i in MetalFormerScreenHandler.SLOT_UPGRADE_INDEX_START..MetalFormerScreenHandler.SLOT_UPGRADE_INDEX_END) {
            val slot = handler.slots[i]
            context.drawBorder(x + slot.x - borderOffset, y + slot.y - borderOffset, slotSize, slotSize, borderColor)
        }

        // 绘制3个进度条（工具盒轮廓图案），分段显示总进度
        val progress = handler.sync.progress.coerceIn(0, MetalFormerSync.PROGRESS_MAX)
        val progressFrac = if (MetalFormerSync.PROGRESS_MAX > 0) (progress.toFloat() / MetalFormerSync.PROGRESS_MAX).coerceIn(0f, 1f) else 0f

        // 计算每段的进度（三个进度条分段显示）
        val segmentSize = 1f / 3f
        val bar1Frac = when {
            progressFrac <= segmentSize -> progressFrac / segmentSize
            else -> 1f
        }
        val bar2Frac = when {
            progressFrac <= segmentSize -> 0f
            progressFrac <= segmentSize * 2 -> (progressFrac - segmentSize) / segmentSize
            else -> 1f
        }
        val bar3Frac = when {
            progressFrac <= segmentSize * 2 -> 0f
            else -> (progressFrac - segmentSize * 2) / segmentSize
        }

        // 第一个进度条（输入槽右侧）
        val bar1X = x + inputSlot.x + slotSize + 2
        val bar1Y = y + inputSlot.y + 2
        val bar1W = 20
        val bar1H = 6
        ProgressBar.draw(context, bar1X, bar1Y, bar1W, bar1H, bar1Frac)

        // 第二个进度条（中间）
        val bar2X = bar1X + bar1W + 4
        val bar2Y = bar1Y
        val bar2W = 20
        val bar2H = 6
        ProgressBar.draw(context, bar2X, bar2Y, bar2W, bar2H, bar2Frac)

        // 第三个进度条（靠近输出槽，与前两个等长）
        val bar3X = bar2X + bar2W + 4
        val bar3Y = bar1Y
        val bar3W = 20
        val bar3H = 6
        ProgressBar.draw(context, bar3X, bar3Y, bar3W, bar3H, bar3Frac)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        val left = x
        val top = y
        val contentW = (backgroundWidth - 16).coerceAtLeast(0)
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1)
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val currentMode = handler.sync.getMode()
        val modeText = when (currentMode) {
            MetalFormerSync.Mode.ROLLING -> "辊压"
            MetalFormerSync.Mode.CUTTING -> "切割"
            MetalFormerSync.Mode.EXTRUDING -> "挤压"
        }
        // 直接使用后端滤波后的值
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val consumeRate = handler.sync.getSyncedConsumedAmount()

        ui.render(context, textRenderer, mouseX, mouseY) {
            Column(x = left + 8, y = top + 6, spacing = 2, modifier = Modifier.EMPTY.width(contentW).padding(0, 0, 8, 0)) {
                // 第一行：标题 + 小能量条 + 数值（右侧留边距防溢出）
                Flex(
                    direction = FlexDirection.ROW,
                    alignItems = AlignItems.CENTER,
                    gap = 4,
                    modifier = Modifier.EMPTY.width(contentW - 8)
                ) {
                    Text(title.string, color = 0xFFFFFF)
                    EnergyBar(
                        energyFraction,
                        barWidth = 40,
                        barHeight = 5,
                        modifier = Modifier.EMPTY.width(40)
                    )
                    Text("$energy/$cap EU", color = 0xCCCCCC, shadow = false)
                }
                // 第二行：当前模式 + 切换按钮
                Flex(
                    direction = FlexDirection.ROW,
                    alignItems = AlignItems.CENTER,
                    gap = 6,
                    modifier = Modifier.EMPTY.width(contentW - 8)
                ) {
                    Text("模式: $modeText", color = 0xAAAAFF, shadow = false)
                    Button("切换", onClick = {
                    client?.player?.networkHandler?.sendPacket(
                        ButtonClickC2SPacket(handler.syncId, MetalFormerScreenHandler.BUTTON_ID_MODE_CYCLE)
                    )
                })
                }
                // 第三行：输入/耗能速率
                Text(
                    "输入 ${formatEu(inputRate)} EU/t · 耗能 ${formatEu(consumeRate)} EU/t",
                    color = 0xAAAAAA,
                    shadow = false
                )
            }
        }
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun formatEu(value: Long): String {
        return when {
            value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
            value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
            else -> value.toString()
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        // 模式切换已由 Compose 的 Button 处理
        if (ui.mouseClicked(mouseX, mouseY, button)) return true
        return super.mouseClicked(mouseX, mouseY, button)
    }

    companion object {
        /** 原版 UI 宽度 + 升级槽列宽度 */
        private val PANEL_WIDTH = UpgradeSlotLayout.VANILLA_UI_WIDTH + UpgradeSlotLayout.SLOT_SPACING
        private const val PANEL_HEIGHT = 184
    }
}

