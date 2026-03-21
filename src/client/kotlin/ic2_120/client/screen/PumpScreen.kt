package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.PumpBlock
import ic2_120.content.screen.PumpScreenHandler
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.registry.annotation.ModScreen
import ic2_120.registry.type
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.slot.Slot
import net.minecraft.text.Text

@ModScreen(block = PumpBlock::class)
class PumpScreen(
    handler: PumpScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<PumpScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = PANEL_WIDTH
        backgroundHeight = PANEL_HEIGHT
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context,
            x,
            y,
            PumpScreenHandler.PLAYER_INV_Y,
            PumpScreenHandler.HOTBAR_Y,
            PumpScreenHandler.SLOT_SIZE
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1L)
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val contentW = (backgroundWidth - 16).coerceAtLeast(0)
        val barW = (contentW - 36).coerceAtLeast(0)
        val fluidAmount = handler.sync.fluidAmountMb.coerceAtLeast(0)

        val energyText = "$energy / $cap EU"
        val fluidText = "液体 $fluidAmount mB"
        val statusText = if (fluidAmount > 0) "状态: 已填充" else "状态: 空"

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

                // 机器槽位
                Flex(
                    direction = FlexDirection.ROW,
                    justifyContent = JustifyContent.SPACE_BETWEEN,
                ) {
                    SlotAnchor(id = "slot.${PumpScreenHandler.SLOT_INPUT_INDEX}")
                    SlotAnchor(id = "slot.${PumpScreenHandler.SLOT_OUTPUT_INDEX}")
                }

                // 放电槽
                SlotAnchor(id = "slot.${PumpScreenHandler.SLOT_DISCHARGING_INDEX}")

                Text(fluidText, color = 0xFFFFFF, shadow = false)
                Text(statusText, color = 0xAAAAAA, shadow = false)

                // 升级槽
                Flex(
                    direction = FlexDirection.ROW,
                    justifyContent = JustifyContent.SPACE_BETWEEN,
                ) {
                    repeat(4) { index ->
                        SlotAnchor(id = "slot.${PumpScreenHandler.SLOT_UPGRADE_INDEX_START + index}")
                    }
                }
            }
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
        private val PANEL_WIDTH = GuiSize.STANDARD_UPGRADE.width
        private val PANEL_HEIGHT = GuiSize.STANDARD_UPGRADE.height
    }
}
