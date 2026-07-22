package ic2_120.client.screen

import ic2_120.client.t
import ic2_120.client.ui.GuiBackground
import ic2_120.content.crop.CropSystem
import ic2_120.content.item.CropSeedBagItem
import ic2_120.content.item.CropSeedData
import ic2_120.content.screen.CropnalyzerScreenHandler
import ic2_120.content.screen.GuiSize
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text as McText
import net.minecraft.util.Identifier

@ModScreen(handler = "cropnalyzer")
class CropnalyzerScreen(
    handler: CropnalyzerScreenHandler,
    playerInventory: PlayerInventory,
    title: McText
) : HandledScreen<CropnalyzerScreenHandler>(handler, playerInventory, title) {

    companion object {
        private val TEXTURE = Identifier("ic2", "textures/gui/guicropnalyzer.png")
        private const val TEXTURE_SIZE = 256
        private const val GUI_WIDTH = 176
        private const val GUI_HEIGHT = 223
        private const val PLAYER_INV_Y = 141
        private const val HOTBAR_Y = 199

        private const val BTN_X = 143
        private const val BTN_Y = 7
        private const val BTN_W = 27
        private const val BTN_H = 18

        private const val RESULT_X = 9
        private const val RESULT_Y = 38
        private const val RESULT_W = 159

        private val GREEN = 0x55FF55
        private val YELLOW = 0xFFFF55
        private val BLUE = 0x55AADD
        private val WHITE = 0xFFFFFF
    }

    init {
        backgroundWidth = GUI_WIDTH
        backgroundHeight = GUI_HEIGHT
        titleY = 6
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, backgroundWidth, backgroundHeight, TEXTURE_SIZE, TEXTURE_SIZE)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        val left = x
        val top = y

        setSlotPositions()

        val inset = GuiBackground.SLOT_ANCHOR_INSET
        GuiBackground.drawVanillaLikeSlot(
            context,
            left + handler.slots[CropnalyzerScreenHandler.SLOT_INDEX_INPUT].x - inset,
            top + handler.slots[CropnalyzerScreenHandler.SLOT_INDEX_INPUT].y - inset,
            GuiSize.SLOT_SIZE, GuiSize.SLOT_SIZE
        )
        GuiBackground.drawVanillaLikeSlot(
            context,
            left + handler.slots[CropnalyzerScreenHandler.SLOT_INDEX_OUTPUT].x - inset,
            top + handler.slots[CropnalyzerScreenHandler.SLOT_INDEX_OUTPUT].y - inset,
            GuiSize.SLOT_SIZE, GuiSize.SLOT_SIZE
        )
        GuiBackground.drawPlayerInventorySlotBorders(
            context, left, top, PLAYER_INV_Y, HOTBAR_Y, GuiSize.SLOT_SIZE
        )

        drawTitle(context, left, top)
        drawButton(context, left, top, mouseX, mouseY)
        drawResultText(context, left, top)

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun setSlotPositions() {
        handler.slots[CropnalyzerScreenHandler.SLOT_INDEX_INPUT].x = 8
        handler.slots[CropnalyzerScreenHandler.SLOT_INDEX_INPUT].y = 7
        handler.slots[CropnalyzerScreenHandler.SLOT_INDEX_OUTPUT].x = 41
        handler.slots[CropnalyzerScreenHandler.SLOT_INDEX_OUTPUT].y = 7

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                val idx = CropnalyzerScreenHandler.PLAYER_INV_START + col + row * 9
                handler.slots[idx].x = 8 + col * GuiSize.SLOT_SIZE
                handler.slots[idx].y = PLAYER_INV_Y + row * GuiSize.SLOT_SIZE
            }
        }
        for (col in 0 until 9) {
            val idx = CropnalyzerScreenHandler.PLAYER_INV_START + 27 + col
            handler.slots[idx].x = 8 + col * GuiSize.SLOT_SIZE
            handler.slots[idx].y = HOTBAR_Y
        }
    }

    private fun drawTitle(context: DrawContext, left: Int, top: Int) {
        val tx = left + (backgroundWidth - textRenderer.getWidth(title)) / 2
        context.drawText(textRenderer, title, tx, top + 6, 0x404040, false)
    }

    private fun drawButton(context: DrawContext, left: Int, top: Int, mouseX: Int, mouseY: Int) {
        val bx = left + BTN_X
        val by = top + BTN_Y
        val hovered = mouseX in bx until (bx + BTN_W) && mouseY in by until (by + BTN_H)
        val faceColor = if (hovered) 0xFF9D9D9D.toInt() else 0xFF8B8B8B.toInt()
        val dark = 0xFF373737.toInt()
        val light = 0xFFFFFFFF.toInt()
        val x1 = bx + BTN_W
        val y1 = by + BTN_H

        context.fill(bx, by, x1, y1, faceColor)
        context.fill(bx, by, x1, by + 1, light)
        context.fill(bx, by, bx + 1, y1, light)
        context.fill(bx, y1 - 1, x1, y1, dark)
        context.fill(x1 - 1, by, x1, y1, dark)
        context.fill(bx + 1, by + 1, x1 - 1, by + 2, 0xFFE6E6E6.toInt())
        context.fill(bx + 1, by + 1, bx + 2, y1 - 1, 0xFFE6E6E6.toInt())
        context.fill(bx + 1, y1 - 2, x1 - 1, y1 - 1, 0xFF555555.toInt())
        context.fill(x1 - 2, by + 1, x1 - 1, y1 - 1, 0xFF555555.toInt())

        val text = t("gui.ic2_120.cropnalyzer.scan")
        val tw = textRenderer.getWidth(text)
        val tx = bx + (BTN_W - tw) / 2
        val ty = by + (BTN_H - textRenderer.fontHeight) / 2
        context.drawTextWithShadow(textRenderer, text, tx, ty, 0xFFE0E0E0.toInt())
    }

    private fun drawResultText(context: DrawContext, left: Int, top: Int) {
        val outputSeed = handler.slots[CropnalyzerScreenHandler.SLOT_INDEX_OUTPUT].stack
        if (outputSeed.item !is CropSeedBagItem) return
        val type = CropSeedData.readType(outputSeed) ?: return
        val stats = CropSeedData.readStats(outputSeed)
        val def = CropSystem.definition(type)

        val harvestFirst = CropSystem.harvestItems(type).firstOrNull()
        val harvestName = harvestFirst?.let {
            McText.translatable(it.item.asItem().translationKey).string
        } ?: "-"

        val x0 = left + RESULT_X
        val x1 = left + RESULT_X + RESULT_W
        val lineH = textRenderer.fontHeight
        val gap = 5

        // Line 1: 作物名（左）| Tier: N（右）
        val name = CropSeedData.displayName(type).string
        drawSplitLine(context, name, t("gui.ic2_120.cropnalyzer.tier_label", def.tier), x0, x1, top + RESULT_Y, WHITE, WHITE)

        // Line 2: 成熟龄:（左）| N（右）
        val y2 = top + RESULT_Y + lineH + gap
        drawSplitLine(context, t("gui.ic2_120.cropnalyzer.mature_label"), def.maxVisualAge.toString(), x0, x1, y2, WHITE, WHITE)

        // Line 3: 属性:（左）| attr1/attr2/attr3（右）
        val y3 = y2 + lineH + gap
        drawSplitLine(context, t("gui.ic2_120.cropnalyzer.attr_label"), def.attributes.take(3).joinToString("/"), x0, x1, y3, WHITE, WHITE)

        // Line 4: 产物:（左）| 收获物名（右）
        val y4 = y3 + lineH + gap
        drawSplitLine(context, t("gui.ic2_120.cropnalyzer.product"), harvestName, x0, x1, y4, WHITE, WHITE)

        // 5px 间隔，然后 Growth/Gain/Resis.
        val y5 = y4 + lineH + gap + gap
        drawSplitLine(context, t("gui.ic2_120.cropnalyzer.growth"), stats.growth.toString(), x0, x1, y5, WHITE, GREEN)
        val y6 = y5 + lineH + gap
        drawSplitLine(context, t("gui.ic2_120.cropnalyzer.gain"), stats.gain.toString(), x0, x1, y6, WHITE, YELLOW)
        val y7 = y6 + lineH + gap
        drawSplitLine(context, t("gui.ic2_120.cropnalyzer.resistance"), stats.resistance.toString(), x0, x1, y7, WHITE, BLUE)
    }

    private fun drawSplitLine(context: DrawContext, leftText: String, rightText: String, x0: Int, x1: Int, y: Int, leftColor: Int, rightColor: Int) {
        context.drawText(textRenderer, leftText, x0, y, leftColor, false)
        val rw = textRenderer.getWidth(rightText)
        context.drawText(textRenderer, rightText, x1 - rw, y, rightColor, false)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) {
            val bx = x + BTN_X
            val by = y + BTN_Y
           if (mouseX in bx.toDouble()..(bx + BTN_W).toDouble() && mouseY in by.toDouble()..(by + BTN_H).toDouble()) {
                MinecraftClient.getInstance().soundManager.play(
                    PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f)
                )
                client?.player?.networkHandler?.sendPacket(
                    ButtonClickC2SPacket(handler.syncId, CropnalyzerScreenHandler.BUTTON_ID_SCAN)
                )
                return true
            }
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }
}
