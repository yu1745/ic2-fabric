package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.t
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.WaterKineticGeneratorBlock
import ic2_120.content.screen.WaterKineticGeneratorScreenHandler
import ic2_120.content.screen.GuiSize
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text as McText

@ModScreen(block = WaterKineticGeneratorBlock::class)
class WaterKineticGeneratorScreen(
    handler: WaterKineticGeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: McText
) : HandledScreen<WaterKineticGeneratorScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = GUI_SIZE.width
        backgroundHeight = GUI_SIZE.height
        titleY = -1000
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
        val generatedKu = handler.sync.generatedKu.coerceAtLeast(0)
        val outputKu = handler.sync.outputKu.coerceAtLeast(0)
        val blocked = handler.sync.isStuck != 0
        val submerged = handler.sync.isSubmerged != 0
        val flowBonus = handler.sync.waterFlowBonus != 0
        val rotorLifetimeTenthsHours = handler.sync.rotorLifetimeTenthsHours.coerceAtLeast(0)
        val generatedText = McText.translatable("ic2_120.jade.water_ku_generated", generatedKu).string
        val outputText = McText.translatable("ic2_120.jade.water_ku_output", outputKu).string
        val statusText = McText.translatable(
            when {
                blocked -> "gui.ic2_120.water_kinetic.blocked"
                !submerged -> "gui.ic2_120.water_kinetic.not_submerged"
                else -> "gui.ic2_120.water_kinetic.submerged"
            }
        ).string
        val flowText = if (flowBonus) McText.translatable("gui.ic2_120.water_kinetic.flow_bonus").string else ""
        val lifetimeText = McText.translatable(
            "gui.ic2_120.water_kinetic.lifetime",
            String.format("%.1f", rotorLifetimeTenthsHours / 10.0)
        ).string
        val sideTextWidth = maxOf(
            textRenderer.getWidth(generatedText),
            textRenderer.getWidth(outputText),
            textRenderer.getWidth(statusText),
            textRenderer.getWidth(lifetimeText),
            if (flowText.isNotEmpty()) textRenderer.getWidth(flowText) else 0
        )
        val sideTextX = left - sideTextWidth - 4

        val content: UiScope.() -> Unit = {
            Column(
                x = left + 8,
                y = top + 8,
                spacing = 6,
                modifier = Modifier.EMPTY.width(GUI_SIZE.contentWidth)
            ) {
                Text(title.string, color = 0xFFFFFF)
                Row(spacing = 8) {
                    SlotHost(0)
                    Text(t("gui.ic2_120.water_kinetic.rotor_slot"), color = 0xAAAAAA, shadow = false)
                }
                Text(t("gui.ic2_120.water_kinetic.rotor_hint"), color = 0xAAAAAA, shadow = false)
            }

            playerInventoryAndHotbarSlotAnchors(
                left = left,
                top = top,
                playerInvStart = WaterKineticGeneratorScreenHandler.PLAYER_INV_START,
                playerInvY = GUI_SIZE.playerInvY,
                hotbarY = GUI_SIZE.hotbarY
            )
        }
        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)
        applyAnchoredSlots(layout, left, top)

        super.render(context, mouseX, mouseY, delta)
        ui.render(context, textRenderer, mouseX, mouseY, content = content)
        context.drawText(textRenderer, generatedText, sideTextX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, outputText, sideTextX, top + 20, 0xAAAAAA, false)
        val statusColor = when {
            blocked -> 0xD65A5A
            !submerged -> 0xD6A052
            else -> 0x6FA85E
        }
        context.drawText(textRenderer, statusText, sideTextX, top + 32, statusColor, false)
        if (flowText.isNotEmpty()) {
            context.drawText(textRenderer, flowText, sideTextX, top + 44, 0x55AAFF, false)
        }
        context.drawText(textRenderer, lifetimeText, sideTextX, top + 56, 0xAAAAAA, false)
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun UiScope.SlotHost(slotIndex: Int) {
        SlotAnchor(
            id = slotAnchorId(slotIndex),
            width = WaterKineticGeneratorScreenHandler.SLOT_SIZE,
            height = WaterKineticGeneratorScreenHandler.SLOT_SIZE
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
        private val GUI_SIZE = GuiSize.STANDARD
    }
}