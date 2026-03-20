package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.EnergyBarOrientation
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.GeneratorBlock
import ic2_120.content.block.machines.MachineBlockEntity
import ic2_120.content.screen.GeneratorScreenHandler
import ic2_120.content.sync.GeneratorSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.screen.slot.Slot

@ModScreen(block = GeneratorBlock::class)
class GeneratorScreen(
    handler: GeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<GeneratorScreenHandler>(handler, playerInventory, title) {

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
            playerInvY = GeneratorScreenHandler.PLAYER_INV_Y,
            hotbarY = GeneratorScreenHandler.HOTBAR_Y,
            slotSize = GeneratorScreenHandler.SLOT_SIZE
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y

        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val outputRate = handler.sync.getSyncedExtractedAmount()
        val cap = GeneratorSync.ENERGY_CAPACITY
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f

        val totalBurn = handler.sync.totalBurnTime.coerceAtLeast(1)
        val burnTime = handler.sync.burnTime.coerceIn(0, totalBurn)
        val burnFrac = (burnTime.toFloat() / totalBurn).coerceIn(0f, 1f)
        val energyText = "${formatEu(energy)} / ${formatEu(cap)} EU"
        val statusText1 = "发电 ${formatEu(inputRate)} EU/t"
        val statusText2 = "输出 ${formatEu(outputRate)} EU/t"
        val sideTextWidth = maxOf(
            textRenderer.getWidth(energyText),
            textRenderer.getWidth(statusText1),
            textRenderer.getWidth(statusText2)
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
                    Text(energyText, color = 0xFFFFFF)
                }
                EnergyBar(energyFraction)

                // 机器槽（横向排列，SlotAnchor 导出锚点给原生 slot 渲染）
                Flex(
                    direction = FlexDirection.ROW,
                    justifyContent = JustifyContent.SPACE_BETWEEN,
                ) {
                    Row(spacing = 8) {
                        SlotAnchor(id = "slot.${MachineBlockEntity.FUEL_SLOT}")
                        // 燃烧进度（竖向，红→蓝 表示燃料逐渐用尽）
                        EnergyBar(
                            burnFrac,
                            orientation = EnergyBarOrientation.VERTICAL,
                            shortEdge = 8,
                            barHeight = 18,
                            emptyColor = 0xFF0033CC.toInt(),
                            fullColor = 0xFFCC0000.toInt(),
                        )
                    }
                    SlotAnchor(id = "slot.${MachineBlockEntity.BATTERY_SLOT}")
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
        context.drawText(textRenderer, statusText1, sideTextX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, statusText2, sideTextX, top + 20, 0xAAAAAA, false)

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun formatEu(value: Long): String {
        return when {
            value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
            value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
            else -> value.toString()
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    companion object {
        private val PANEL_WIDTH = GuiSize.STANDARD.width
        private val PANEL_HEIGHT = GuiSize.STANDARD.height
    }
}
