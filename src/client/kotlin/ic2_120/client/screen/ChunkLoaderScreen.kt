package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.content.sync.ChunkLoaderSync
import ic2_120.content.block.ChunkLoaderBlock
import ic2_120.content.screen.ChunkLoaderScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.slot.Slot
import net.minecraft.text.Text

@ModScreen(block = ChunkLoaderBlock::class)
class ChunkLoaderScreen(
    handler: ChunkLoaderScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<ChunkLoaderScreenHandler>(handler, playerInventory, title) {

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
        val playerInvY = handler.slots[ChunkLoaderScreenHandler.PLAYER_INV_START].y
        GuiBackground.drawPlayerInventorySlotBorders(
            context,
            x,
            y,
            playerInvY,
            142,
            18
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = ChunkLoaderSync.ENERGY_CAPACITY
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val contentW = (backgroundWidth - 16).coerceAtLeast(0)
        val barW = (contentW - 36).coerceAtLeast(0)
        val chunkCount = ChunkLoaderSync.RADIUS_TO_CHUNK_COUNT.getOrElse(handler.sync.range.coerceIn(0, 2)) { 25 }
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val consumeRate = handler.sync.getSyncedConsumedAmount()

        val energyText = "$energy / $cap EU"
        val inputText = "输入 ${formatEu(inputRate)} EU/t"
        val consumeText = "耗能 ${formatEu(consumeRate)} EU/t ($chunkCount 区块)"
        val sideTextWidth = maxOf(
            textRenderer.getWidth(energyText),
            textRenderer.getWidth(inputText),
            textRenderer.getWidth(consumeText)
        )
        val sideTextX = left - sideTextWidth - 4

        val content: UiScope.() -> Unit = {
            Column(
                x = left + 8,
                y = top + 8,
                spacing = 6,
                modifier = Modifier().width(contentW).height(backgroundHeight - 16),
            ) {
                Row(spacing = 8) {
                    Text(title.string, color = 0xFFFFFF)
                    Text(energyText, color = 0xFFFFFF)
                }
                EnergyBar(energyFraction, modifier = Modifier().width(barW))

                // 放电槽
                SlotAnchor(id = "slot.${ChunkLoaderScreenHandler.SLOT_DISCHARGING_INDEX}")

                Text("加载范围: $chunkCount 区块", color = 0xFFFFFF, shadow = false)
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
        context.drawText(textRenderer, inputText, sideTextX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, consumeText, sideTextX, top + 20, 0xAAAAAA, false)

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    companion object {
        private const val PANEL_WIDTH = 176
        private const val PANEL_HEIGHT = 166
    }

    private fun formatEu(value: Long): String {
        return when {
            value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
            value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
            else -> value.toString()
        }
    }
}
