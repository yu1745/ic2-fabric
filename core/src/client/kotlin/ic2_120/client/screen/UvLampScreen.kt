package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.compose.*
import ic2_120.client.t
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.UvLampBlock
import ic2_120.content.block.machines.UvLampBlockEntity
import ic2_120.content.energy.EnergyTier
import ic2_120.content.screen.GuiSize
import ic2_120.content.screen.UvLampScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

@ModScreen(block = UvLampBlock::class)
class UvLampScreen(
    handler: UvLampScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<UvLampScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = GuiSize.STANDARD.width
        backgroundHeight = GuiSize.STANDARD.height
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context,
            x,
            y,
            GuiSize.STANDARD.playerInvY,
            GuiSize.STANDARD.hotbarY,
            GuiSize.SLOT_SIZE
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y

        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val capacity = handler.sync.energyCapacity.toLong().coerceAtLeast(1L)
        val energyFraction = (energy.toFloat() / capacity).coerceIn(0f, 1f)
        val growthMultiplier = handler.sync.growthMultiplier

        val ocCount = handler.sync.growthMultiplier.let { if (it > 0) it - 1 else 0 }
        val effectiveTier = ocCount + 1
        val euPerTick = EnergyTier.euPerTickFromTier(effectiveTier)

        val inputRateText = t("gui.ic2_120.input_eu", EnergyFormatUtils.formatEu(handler.sync.getSyncedInsertedAmount()))
        val consumeRateText = t("gui.ic2_120.consume_eu", EnergyFormatUtils.formatEu(handler.sync.getSyncedConsumedAmount()))

        val content: UiScope.() -> Unit = {
            Column(
                x = left + 8,
                y = top + 4,
                spacing = 4,
                modifier = Modifier.EMPTY.width(GuiSize.STANDARD.contentWidth)
            ) {
                Flex(direction = FlexDirection.ROW, alignItems = AlignItems.CENTER, gap = 4) {
                    Text(title.string, color = 0xFFFFFF)
                    EnergyBar(energyFraction, barHeight = 8, modifier = Modifier.EMPTY.fractionWidth(1f))
                    Text("${EnergyFormatUtils.formatEu(energy)} / ${EnergyFormatUtils.formatEu(capacity)} EU", color = 0xFFFFFF, shadow = false)
                }

                Flex(direction = FlexDirection.ROW, justifyContent = JustifyContent.SPACE_BETWEEN, alignItems = AlignItems.CENTER) {
                    Column(spacing = 2) {
                        Text(t("gui.ic2_120.uv_lamp_upgrade"), color = 0xAAAAAA, shadow = false)
                        SlotAnchor(id = slotAnchorId(UvLampScreenHandler.SLOT_UPGRADE_INDEX))
                    }
                    Column(spacing = 2, modifier = Modifier.EMPTY.padding(8, 0, 0, 0)) {
                        val boostColor = if (growthMultiplier > 0) 0xDDA0FF else 0xAAAAAA
                        Text(
                            t("gui.ic2_120.uv_lamp_growth", if (growthMultiplier > 0) "${growthMultiplier}x" else "-"),
                            color = boostColor,
                            shadow = false
                        )
                        Text(
                            t("gui.ic2_120.uv_lamp_eu_per_tick", EnergyFormatUtils.formatEu(euPerTick)),
                            color = 0xAAAAAA,
                            shadow = false
                        )
                    }
                }
            }

            playerInventoryAndHotbarSlotAnchors(
                left = left,
                top = top,
                playerInvStart = UvLampScreenHandler.PLAYER_INV_START,
                playerInvY = GuiSize.STANDARD.playerInvY,
                hotbarY = GuiSize.STANDARD.hotbarY
            )
        }

        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)
        handler.slots.forEachIndexed { index, slot ->
            val anchor = layout.anchors[slotAnchorId(index)] ?: return@forEachIndexed
            slot.x = anchor.x - left
            slot.y = anchor.y - top
        }

        super.render(context, mouseX, mouseY, delta)
        ui.render(context, textRenderer, mouseX, mouseY, content = content)

        val sideTextWidth = maxOf(textRenderer.getWidth(inputRateText), textRenderer.getWidth(consumeRateText))
        val sideX = left - sideTextWidth - 4
        context.drawText(textRenderer, inputRateText, sideX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, consumeRateText, sideX, top + 20, 0xAAAAAA, false)

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    private fun slotAnchorId(slotIndex: Int): String = "slot.$slotIndex"
}
