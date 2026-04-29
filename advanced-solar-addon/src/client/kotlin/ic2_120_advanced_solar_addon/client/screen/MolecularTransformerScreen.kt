package ic2_120_advanced_solar_addon.client.screen

import ic2_120.client.compose.*
import ic2_120.client.EnergyFormatUtils
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.content.screen.GuiSize
import ic2_120_advanced_solar_addon.content.block.MolecularTransformerBlock
import ic2_120_advanced_solar_addon.content.screen.MolecularTransformerScreenHandler
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.slot.Slot
import net.minecraft.text.Text
import ic2_120.registry.annotation.ModScreen
import kotlin.math.ceil

@ModScreen(block = MolecularTransformerBlock::class)
class MolecularTransformerScreen(
    handler: MolecularTransformerScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<MolecularTransformerScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = GUI_SIZE.width
        backgroundHeight = GUI_SIZE.height
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y,
            GUI_SIZE.playerInvY,
            GUI_SIZE.hotbarY,
            GuiSize.SLOT_SIZE
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val progress = handler.sync.progress.toLong().coerceAtLeast(0)
        val requiredEnergy = handler.sync.requiredEnergy.toLong().coerceAtLeast(1)
        val progressFraction = (progress.toFloat() / requiredEnergy.toFloat()).coerceIn(0f, 1f)
        val progressPercent = progressFraction * 100f
        val consumePerTick = handler.sync.avgConsumed.toLong().coerceAtLeast(0)
        val remainingEnergy = (requiredEnergy - progress).coerceAtLeast(0)
        val remainingTicks = if (consumePerTick > 0 && remainingEnergy > 0) {
            ceil(remainingEnergy.toDouble() / consumePerTick.toDouble()).toLong()
        } else 0L

        val content: UiScope.() -> Unit = {
            Column(
                x = left + 8,
                y = top + 8,
                spacing = 6,
                modifier = Modifier.EMPTY.width(GUI_SIZE.contentWidth)
            ) {
                Flex(direction = FlexDirection.ROW, alignItems = AlignItems.CENTER, gap = 8) {
                    Text(title.string, color = 0xFFFFFF)
                    Text("${EnergyFormatUtils.formatEu(energy)} EU", color = 0xFFFFFF, shadow = false)
                }

                Flex(direction = FlexDirection.ROW, alignItems = AlignItems.CENTER, gap = 4) {
                    SlotHost(MolecularTransformerBlock.INPUT_SLOT)
                    EnergyBar(progressFraction, modifier = Modifier.EMPTY.fractionWidth(1.0f))
                    SlotHost(MolecularTransformerBlock.OUTPUT_SLOT)
                }

                Flex(direction = FlexDirection.ROW, alignItems = AlignItems.CENTER, gap = 8) {
                    Text(Text.translatable("gui.ic2_120_advanced_solar_addon.progress").string + " ", color = 0xAAAAAA)
                    Text(
                        "${EnergyFormatUtils.formatEu(progress)} / ${EnergyFormatUtils.formatEu(requiredEnergy)} (${String.format("%.2f", progressPercent)}%)",
                        color = 0xFFFFFF,
                        shadow = false
                    )
                }

                Flex(direction = FlexDirection.ROW, alignItems = AlignItems.CENTER, gap = 8) {
                    Text("Time ", color = 0xAAAAAA)
                    Text(
                        if (consumePerTick > 0) {
                            "${formatTicksAsTime(remainingTicks)} @ ${EnergyFormatUtils.formatEu(consumePerTick)} EU/t"
                        } else {
                            "--:-- @ 0 EU/t"
                        },
                        color = 0xFFFFFF,
                        shadow = false
                    )
                }
            }

            playerInventoryAndHotbarSlotAnchors(
                left = left,
                top = top,
                playerInvStart = MolecularTransformerScreenHandler.PLAYER_INV_START,
                playerInvY = GUI_SIZE.playerInvY,
                hotbarY = GUI_SIZE.hotbarY
            )
        }

        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)
        applyAnchoredSlots(layout, left, top)

        super.render(context, mouseX, mouseY, delta)
        ui.render(context, textRenderer, mouseX, mouseY, content = content)
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun UiScope.SlotHost(slotIndex: Int) {
        SlotAnchor(
            id = slotAnchorId(slotIndex),
            width = MolecularTransformerScreenHandler.SLOT_SIZE,
            height = MolecularTransformerScreenHandler.SLOT_SIZE
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

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (ui.mouseClicked(mouseX, mouseY, button)) return true
        return super.mouseClicked(mouseX, mouseY, button)
    }

    private fun formatTicksAsTime(ticks: Long): String {
        val totalSeconds = ticks / 20L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
        else String.format("%02d:%02d", minutes, seconds)
    }

    companion object {
        private val GUI_SIZE = GuiSize.STANDARD
    }
}
