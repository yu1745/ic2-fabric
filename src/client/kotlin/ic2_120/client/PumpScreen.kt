package ic2_120.client

import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.PumpBlock
import ic2_120.content.screen.PumpScreenHandler
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

@ModScreen(block = PumpBlock::class)
class PumpScreen(
    handler: PumpScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<PumpScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = UpgradeSlotLayout.VANILLA_UI_WIDTH + UpgradeSlotLayout.SLOT_SPACING
        backgroundHeight = 166
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.draw(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y,
            PumpScreenHandler.PLAYER_INV_Y,
            PumpScreenHandler.HOTBAR_Y,
            PumpScreenHandler.SLOT_SIZE
        )

        val borderColor = GuiBackground.BORDER_COLOR
        val slotSize = PumpScreenHandler.SLOT_SIZE
        val borderOffset = 1
        val inputSlot = handler.slots[PumpScreenHandler.SLOT_INPUT_INDEX]
        val outputSlot = handler.slots[PumpScreenHandler.SLOT_OUTPUT_INDEX]
        val dischargeSlot = handler.slots[PumpScreenHandler.SLOT_DISCHARGING_INDEX]
        context.drawBorder(x + inputSlot.x - borderOffset, y + inputSlot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + outputSlot.x - borderOffset, y + outputSlot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + dischargeSlot.x - borderOffset, y + dischargeSlot.y - borderOffset, slotSize, slotSize, borderColor)
        for (i in PumpScreenHandler.SLOT_UPGRADE_INDEX_START..PumpScreenHandler.SLOT_UPGRADE_INDEX_END) {
            val slot = handler.slots[i]
            context.drawBorder(x + slot.x - borderOffset, y + slot.y - borderOffset, slotSize, slotSize, borderColor)
        }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        val left = x
        val top = y
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1L)
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val contentW = (backgroundWidth - 16).coerceAtLeast(0)
        val barW = (contentW - 36).coerceAtLeast(0)
        val fluidAmount = handler.sync.fluidAmountMb.coerceAtLeast(0)

        ui.render(context, textRenderer, mouseX, mouseY) {
            Column(x = left + 8, y = top + 8, spacing = 6) {
                Text(title.string, color = 0xFFFFFF)
                Flex(direction = FlexDirection.ROW, alignItems = AlignItems.CENTER, gap = 8, modifier = Modifier.EMPTY.width(contentW)) {
                    Text("能量", color = 0xAAAAAA)
                    EnergyBar(energyFraction, barWidth = 0, barHeight = 9, modifier = Modifier.EMPTY.width(barW))
                }
                Text("$energy / $cap EU", color = 0xCCCCCC, shadow = false)
                Text("液体 $fluidAmount mB", color = 0xAAAAAA, shadow = false)
                Text(if (fluidAmount > 0) "状态: 已填充" else "状态: 空", color = 0xAAAAAA, shadow = false)
            }
        }
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)
}
