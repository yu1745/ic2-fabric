package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.t
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.LeashKineticGeneratorBlock
import ic2_120.content.screen.GuiSize
import ic2_120.content.screen.LeashKineticGeneratorScreenHandler
import ic2_120.registry.annotation.ModScreen
import ic2_120.client.compat.*
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Inventory
import net.minecraft.network.chat.Component as McText

@ModScreen(block = LeashKineticGeneratorBlock::class)
class LeashKineticGeneratorScreen(
    val handler: LeashKineticGeneratorScreenHandler,
    playerInventory: Inventory,
    title: McText
) : LegacyContainerScreen26<LeashKineticGeneratorScreenHandler>(handler, playerInventory, title, GUI_SIZE.width, GUI_SIZE.height) {

    private val ui = ComposeUI()

    init {
        titleLabelY = -1000
    }

    override fun renderBg(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        // 背景绘制已移至 render()，以控制 ui.render 在 super.render 之前执行
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        val left = leftPos
        val top = topPos
        val generatedKu = menu.sync.generatedKu.coerceAtLeast(0)
        val outputKu = menu.sync.outputKu.coerceAtLeast(0)
        val hasAnimal = menu.sync.hasAnimal
        val leashLengthCm = menu.sync.leashLengthCm.coerceAtLeast(0)
        val angularVelocityDegPerSec = menu.sync.angularVelocityDegPerSec.coerceAtLeast(0)

        val genText = t("gui.ic2_120.leash_kinetic.generated", generatedKu)
        val outText = t("gui.ic2_120.leash_kinetic.output", outputKu)
        val animalText = t("gui.ic2_120.leash_kinetic.animal_status",
            if (hasAnimal) "gui.ic2_120.leash_kinetic.attached" else "gui.ic2_120.leash_kinetic.no_animal"
        )
        val lengthText = t("gui.ic2_120.leash_kinetic.leash_length", leashLengthCm)
        val velocityText = t("gui.ic2_120.leash_kinetic.angular_velocity", angularVelocityDegPerSec)

        val sideTexts = listOf(genText, outText, animalText, lengthText, velocityText)
        val sideTextWidth = sideTexts.maxOf { font.width(it) }
        val sideTextX = left - sideTextWidth - 4

        val content: UiScope.() -> Unit = {
            Column(
                x = left + 8,
                y = top + 8,
                spacing = 6,
                modifier = Modifier.EMPTY.width(GUI_SIZE.contentWidth)
            ) {
                Component(title.string, color = 0xFFFFFF)
                Component(t("gui.ic2_120.leash_kinetic.status",
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
        val layout = ui.layout(context, font, mouseX, mouseY, content = content)
        applyAnchoredSlots(layout, left, top)

        // 先绘制面板背景
        GuiBackground.drawVanillaLikePanel(context, leftPos, topPos, imageWidth, imageHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, leftPos, topPos,
            GUI_SIZE.playerInvY,
            GUI_SIZE.hotbarY,
            GuiSize.SLOT_SIZE
        )

        // 再绘制 UI（slot 背景等），确保它们在物品下方
        ui.render(context, font, mouseX, mouseY, content = content)

        // 最后绘制物品（包括耐久条），确保物品在顶层
        super.render(context, mouseX, mouseY, delta)
        var textY = top + 8
        for (text in sideTexts) {
            context.drawString(font, text, sideTextX, textY, 0xAAAAAA, false)
            textY += 12
        }
        renderTooltip(context, mouseX, mouseY)
    }

    private fun applyAnchoredSlots(layout: ComposeUI.LayoutSnapshot, left: Int, top: Int) {
        menu.slots.forEachIndexed { index, slot ->
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
