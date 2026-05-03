package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.t
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.LeashKineticGeneratorBlock
import ic2_120.content.screen.GuiSize
import ic2_120.content.screen.LeashKineticGeneratorScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text as McText

@ModScreen(block = LeashKineticGeneratorBlock::class)
class LeashKineticGeneratorScreen(
    handler: LeashKineticGeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: McText
) : HandledScreen<LeashKineticGeneratorScreenHandler>(handler, playerInventory, title) {

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
        val hasAnimal = handler.sync.hasAnimal
        val leashLengthCm = handler.sync.leashLengthCm.coerceAtLeast(0)
        val angularVelocityDegPerSec = handler.sync.angularVelocityDegPerSec.coerceAtLeast(0)

        val genText = t("gui.ic2_120.leash_kinetic.generated", generatedKu)
        val outText = t("gui.ic2_120.leash_kinetic.output", outputKu)
        val animalText = t("gui.ic2_120.leash_kinetic.animal_status",
            if (hasAnimal) "gui.ic2_120.leash_kinetic.attached" else "gui.ic2_120.leash_kinetic.no_animal"
        )
        val lengthText = t("gui.ic2_120.leash_kinetic.leash_length", leashLengthCm)
        val velocityText = t("gui.ic2_120.leash_kinetic.angular_velocity", angularVelocityDegPerSec)

        val sideTexts = listOf(genText, outText, animalText, lengthText, velocityText)
        val sideTextWidth = sideTexts.maxOf { textRenderer.getWidth(it) }
        val sideTextX = left - sideTextWidth - 4

        val content: UiScope.() -> Unit = {
            Column(
                x = left + 8,
                y = top + 8,
                spacing = 6,
                modifier = Modifier.EMPTY.width(GUI_SIZE.contentWidth)
            ) {
                Text(title.string, color = 0xFFFFFF)
                Text(t("gui.ic2_120.leash_kinetic.status",
                    if (hasAnimal) "gui.ic2_120.leash_kinetic.running" else "gui.ic2_120.leash_kinetic.idle"
                ), color = if (hasAnimal) 0x55FF55 else 0xAAAAAA, shadow = false)
            }

            playerInventoryAndHotbarSlotAnchors(
                left = left,
                top = top,
                playerInvStart = LeashKineticGeneratorScreenHandler.PLAYER_INV_START,
                playerInvY = GUI_SIZE.playerInvY,
                hotbarY = GUI_SIZE.hotbarY
            )
        }
        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)
        applyAnchoredSlots(layout, left, top)

        super.render(context, mouseX, mouseY, delta)
        ui.render(context, textRenderer, mouseX, mouseY, content = content)
        var textY = top + 8
        for (text in sideTexts) {
            context.drawText(textRenderer, text, sideTextX, textY, 0xAAAAAA, false)
            textY += 12
        }
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun applyAnchoredSlots(layout: ComposeUI.LayoutSnapshot, left: Int, top: Int) {
        handler.slots.forEachIndexed { index, slot ->
            val anchor = layout.anchors[slotAnchorId(index)] ?: return@forEachIndexed
            slot.x = anchor.x - left
            slot.y = anchor.y - top
        }
    }

    private fun slotAnchorId(slotIndex: Int): String = "slot.$slotIndex"

    companion object {
        private val GUI_SIZE = GuiSize.STANDARD
    }
}
