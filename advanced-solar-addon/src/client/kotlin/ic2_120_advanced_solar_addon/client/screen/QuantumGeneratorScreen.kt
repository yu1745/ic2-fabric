package ic2_120_advanced_solar_addon.client.screen

import ic2_120_advanced_solar_addon.content.block.QuantumGeneratorBlock
import ic2_120_advanced_solar_addon.content.screen.QuantumGeneratorScreenHandler
import ic2_120.registry.annotation.ModScreen
import ic2_120.client.compat.*
import ic2_120.client.screen.LegacyContainerScreen26
import net.minecraft.world.entity.player.Inventory
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.client.Minecraft

@ModScreen(block = QuantumGeneratorBlock::class)
class QuantumGeneratorScreen(
    handler: QuantumGeneratorScreenHandler,
    playerInventory: Inventory,
    title: Component
) : LegacyContainerScreen26<QuantumGeneratorScreenHandler>(handler, playerInventory, title, GUI_WIDTH, GUI_HEIGHT) {
    // yarn→mojmap 兼容别名
    private val x get() = leftPos
    private val y get() = topPos
    private val textRenderer get() = font

    init {
        inventoryLabelY = -20000
    }

    override fun renderBg(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.blit(TEXTURE, leftPos, topPos, 0f, 0f, GUI_WIDTH, GUI_HEIGHT, TEX_WIDTH, TEX_HEIGHT)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)

        val energyMac = menu.sync.energyMac
        val variable = menu.sync.variable
        val isActive = menu.sync.isActive != 0

        // 标题居中
        val titleWidth = font.width(title)
        context.drawString(font, title, x + (GUI_WIDTH - titleWidth) / 2, y + 6, 0xFFFFFF, false)

        // 输出: energyMac at (70,24) / (92,24)
        val outputLabel = Component.translatable("gui.ic2_120_advanced_solar_addon.output").string
        context.drawString(font, outputLabel, x + 68, y + 24, 0xFFFFFF, false)
        context.drawString(font, "$energyMac EU/t", x + 94, y + 24, 0xFFFFFF, false)

        // 输入: variable at (70,68) / (92,68)
        val inputLabel = Component.translatable("gui.ic2_120_advanced_solar_addon.input").string
        context.drawString(font, inputLabel, x + 68, y + 68, 0xFFFFFF, false)
        context.drawString(font, "$variable", x + 94, y + 68, 0xFFFFFF, false)

        // 工作指示纹理: 运行时渲染 (176,3) 14x14 @ (146,22)
        if (isActive) {
            context.blit(TEXTURE, x + 146, y + 22, 176f, 3f, 14, 14, TEX_WIDTH, TEX_HEIGHT)
        }

        // Draw all buttons
        for (btn in ALL_BUTTONS) {
            drawButton(context, btn, mouseX, mouseY)
        }

        renderTooltip(context, mouseX, mouseY)
    }

    private fun drawButton(context: GuiGraphics, btn: ButtonDef, mouseX: Int, mouseY: Int) {
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

        val textW = font.width(btn.text)
        context.drawString(font, btn.text, bx + (btn.w - textW) / 2, by + (btn.h - 8) / 2, 0xFFFFFF, false)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) {
            for (btn in ALL_BUTTONS) {
                val bx = x + btn.x
                val by = y + btn.y
                if (mouseX.toInt() in bx until (bx + btn.w) && mouseY.toInt() in by until (by + btn.h)) {
                    Minecraft.getInstance().connection?.send(ServerboundContainerButtonClickPacket(menu.containerId, btn.id))
                    return true
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    companion object {
        private val TEXTURE = Identifier.fromNamespaceAndPath("ic2_120_advanced_solar_addon", "textures/gui/quantumgenerator.png")

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
