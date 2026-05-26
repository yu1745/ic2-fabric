package ic2_120_advanced_solar_addon.client.screen

import ic2_120_advanced_solar_addon.content.block.QuantumGeneratorBlock
import ic2_120_advanced_solar_addon.content.screen.QuantumGeneratorScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = QuantumGeneratorBlock::class)
class QuantumGeneratorScreen(
    handler: QuantumGeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<QuantumGeneratorScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = GUI_WIDTH
        backgroundHeight = GUI_HEIGHT
        playerInventoryTitleY = -20000
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, GUI_WIDTH, GUI_HEIGHT, TEX_WIDTH, TEX_HEIGHT)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)

        val energyMac = handler.sync.energyMac
        val variable = handler.sync.variable
        val isActive = handler.sync.isActive != 0

        // 标题居中
        val titleWidth = textRenderer.getWidth(title)
        context.drawText(textRenderer, title, x + (GUI_WIDTH - titleWidth) / 2, y + 6, 0xFFFFFF, false)

        // 输出: energyMac at (70,24) / (92,24)
        val outputLabel = Text.translatable("gui.ic2_120_advanced_solar_addon.output").string
        context.drawText(textRenderer, outputLabel, x + 68, y + 24, 0xFFFFFF, false)
        context.drawText(textRenderer, "$energyMac EU/t", x + 94, y + 24, 0xFFFFFF, false)

        // 输入: variable at (70,68) / (92,68)
        val inputLabel = Text.translatable("gui.ic2_120_advanced_solar_addon.input").string
        context.drawText(textRenderer, inputLabel, x + 68, y + 68, 0xFFFFFF, false)
        context.drawText(textRenderer, "$variable", x + 94, y + 68, 0xFFFFFF, false)

        // 工作指示纹理: 运行时渲染 (176,3) 14x14 @ (146,22)
        if (isActive) {
            context.drawTexture(TEXTURE, x + 146, y + 22, 176f, 3f, 14, 14, TEX_WIDTH, TEX_HEIGHT)
        }

        // Draw all buttons
        for (btn in ALL_BUTTONS) {
            drawButton(context, btn, mouseX, mouseY)
        }

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun drawButton(context: DrawContext, btn: ButtonDef, mouseX: Int, mouseY: Int) {
        val bx = x + btn.x
        val by = y + btn.y
        val hovered = mouseX in bx until (bx + btn.w) && mouseY in by until (by + btn.h)

        val bgColor = if (hovered) 0xFF888888.toInt() else 0xFF555555.toInt()
        context.fill(bx, by, bx + btn.w, by + btn.h, bgColor)
        // 3D 边框
        context.fill(bx, by, bx + btn.w, by + 1, 0xFFAAAAAA.toInt())
        context.fill(bx, by, bx + 1, by + btn.h, 0xFFAAAAAA.toInt())
        context.fill(bx, by + btn.h - 1, bx + btn.w, by + btn.h, 0xFF333333.toInt())
        context.fill(bx + btn.w - 1, by, bx + btn.w, by + btn.h, 0xFF333333.toInt())

        val textW = textRenderer.getWidth(btn.text)
        context.drawText(textRenderer, btn.text, bx + (btn.w - textW) / 2, by + (btn.h - 8) / 2, 0xFFFFFF, false)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) {
            for (btn in ALL_BUTTONS) {
                val bx = x + btn.x
                val by = y + btn.y
                if (mouseX.toInt() in bx until (bx + btn.w) && mouseY.toInt() in by until (by + btn.h)) {
                    client?.networkHandler?.sendPacket(ButtonClickC2SPacket(handler.syncId, btn.id))
                    return true
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    companion object {
        private val TEXTURE = Identifier.of("ic2_120_advanced_solar_addon", "textures/gui/quantumgenerator.png")

        private const val TEX_WIDTH = 256
        private const val TEX_HEIGHT = 256
        private const val GUI_WIDTH = 175
        private const val GUI_HEIGHT = 191

        private data class ButtonDef(val id: Int, val x: Int, val y: Int, val w: Int, val h: Int, val text: String)

        private val ALL_BUTTONS = listOf(
            // EnergyMac 行 (y=40)
            ButtonDef(QuantumGeneratorScreenHandler.BTN_EM_MINUS_100, 7, 40, 26, 16, "-100"),
            ButtonDef(QuantumGeneratorScreenHandler.BTN_EM_MINUS_10, 38, 40, 23, 16, "-10"),
            ButtonDef(QuantumGeneratorScreenHandler.BTN_EM_MINUS_1, 66, 40, 16, 16, "-1"),
            ButtonDef(QuantumGeneratorScreenHandler.BTN_EM_PLUS_1, 92, 40, 16, 16, "+1"),
            ButtonDef(QuantumGeneratorScreenHandler.BTN_EM_PLUS_10, 114, 40, 23, 16, "+10"),
            ButtonDef(QuantumGeneratorScreenHandler.BTN_EM_PLUS_100, 144, 40, 26, 16, "+100"),

            // Variable 行 (y=84)
            ButtonDef(QuantumGeneratorScreenHandler.BTN_VAR_1, 7, 84, 16, 16, "1"),
            ButtonDef(QuantumGeneratorScreenHandler.BTN_VAR_2, 27, 84, 16, 16, "2"),
            ButtonDef(QuantumGeneratorScreenHandler.BTN_VAR_3, 47, 84, 16, 16, "3"),
            ButtonDef(QuantumGeneratorScreenHandler.BTN_VAR_4, 67, 84, 16, 16, "4"),
            ButtonDef(QuantumGeneratorScreenHandler.BTN_VAR_5, 87, 84, 16, 16, "5"),
            ButtonDef(QuantumGeneratorScreenHandler.BTN_VAR_6, 107, 84, 16, 16, "6"),
            ButtonDef(QuantumGeneratorScreenHandler.BTN_VAR_MAX, 144, 84, 26, 16, "MAX")
        )
    }
}
