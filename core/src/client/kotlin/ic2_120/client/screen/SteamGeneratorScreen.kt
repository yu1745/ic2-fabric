package ic2_120.client.screen

import ic2_120.client.t
import ic2_120.content.block.SteamGeneratorBlock
import ic2_120.content.screen.SteamGeneratorScreenHandler
import ic2_120.content.sync.SteamGeneratorSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket
import net.minecraft.text.Text as McText
import net.minecraft.util.Identifier

/**
 * 蒸汽发生器 GUI — 1:1 对齐 ic2_origin GuiSteamGenerator (176×220)。
 *
 * Ic2Gui(Ic2Gui, PlayerInventory, Text, 220) 调用:
 *   this(container, inv, title, 176, 220)
 *   → backgroundWidth=176, backgroundHeight=220
 *
 * 纹理: ic2:textures/gui/guisteamgenerator.png (256×256)
 * 仅绘制 (0,0)-(176,220) 区域。
 */
@ModScreen(block = SteamGeneratorBlock::class)
class SteamGeneratorScreen(
    handler: SteamGeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: McText
) : HandledScreen<SteamGeneratorScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = 176
        backgroundHeight = 220
    }

    // ==== 背景纹理 + 标题 — 对齐 ic2_origin drawBackgroundAndTitle ====

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, backgroundWidth, backgroundHeight, 256, 256)
        // 标题居中，颜色 0x404040 — 与 Ic2Gui.drawXCenteredString 一致
        context.drawText(
            textRenderer, title,
            x + (backgroundWidth - textRenderer.getWidth(title)) / 2,
            y + 6,
            0x404040, false
        )
    }

    // ==== 前景 — 动态填充 + 文字 (ic2_origin: TankGauge/LinkedGauge drawForeground) ====

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // super.render() → HandledScreen.render → drawBackground(背景纹理) + 槽位, 先画
        super.render(context, mouseX, mouseY, delta)

        val left = x; val top = y
        val sync = handler.sync

        val waterFrac = if (SteamGeneratorSync.WATER_TANK_CAPACITY > 0)
            sync.waterAmount.toFloat() / SteamGeneratorSync.WATER_TANK_CAPACITY else 0f
        val heatFrac = (sync.systemHeatMilli.toFloat() / SteamGeneratorSync.MAX_SYSTEM_HEAT_MILLI).coerceIn(0f, 1f)
        val calcFrac = if (SteamGeneratorSync.MAX_CALCIFICATION > 0)
            sync.calcification.toFloat() / SteamGeneratorSync.MAX_CALCIFICATION else 0f

        // ==== 填充（画在背景纹理之上） ====

        // 水罐填充 (TankGauge Plain at 10,155, 75×47)
        val tankX = left + 10; val tankY = top + 155; val tankW = 75; val tankH = 47
        val fh = (waterFrac.coerceIn(0f, 1f) * tankH).toInt()
        if (fh > 0) context.fill(tankX, tankY + tankH - fh, tankX + tankW, tankY + tankH, WATER_COLOR)

        // 热量条 (LinkedGauge HeatSteamGenerator at 13,70, 7×76, Up方向)
        val hFill = (heatFrac.coerceIn(0f, 1f) * 76).toInt()
        if (hFill > 0) context.fill(left + 13, top + 70 + 76 - hFill, left + 20, top + 70 + 76, HEAT_COLOR)

        // 水垢条 (LinkedGauge CalcificationSteamGenerator at 155,61, 7×58, Up方向)
        val cFill = (calcFrac.coerceIn(0f, 1f) * 58).toInt()
        if (cFill > 0) context.fill(left + 155, top + 61 + 58 - cFill, left + 162, top + 61 + 58, CALC_COLOR)

        // ==== 文本（画在填充之上） ====
        val textColor = 0xFF20EE7E.toInt()
        val pressure = sync.pressure.coerceIn(0, SteamGeneratorSync.MAX_PRESSURE)
        drawCentered(context, "${sync.inputMB.coerceIn(0, 1000)} mB/t", left + 91, top + 172, 59, 13, textColor)
        drawCentered(context, t("gui.ic2_120.heat_input_display2", sync.heatInput), left + 31, top + 133, 111, 13, textColor)
        drawCentered(context, "${pressure} bar", left + 22, top + 35, 42, 13, textColor)
        drawCentered(context, "${sync.outputMB} mB/t", left + 66, top + 25, 81, 13, textColor)
        val outName = when { sync.isSuperheated != 0 -> t("gui.ic2_120.output_superheated_steam"); sync.outputMB > 0 -> t("gui.ic2_120.output_steam"); else -> "" }
        drawCentered(context, outName, left + 66, top + 45, 100, 13, textColor)

        // ==== tooltips ====
        // 热量条 tooltip: 显示系统温度
        if (mouseX in (left + 13) until (left + 20) && mouseY in (top + 70) until (top + 70 + 76)) {
            context.drawTooltip(
                textRenderer,
                McText.literal("System Heat: ${"%.1f".format(sync.systemHeatMilli / 1000f)}°C / 500°C"),
                mouseX, mouseY
            )
        }

        // 水罐 tooltip: 显示液体类型和水量
        if (mouseX in tankX until (tankX + tankW) && mouseY in tankY until (tankY + tankH)) {
            val lines = mutableListOf<McText>()
            if (sync.waterAmount > 0) {
                val fluidName = if (sync.isWaterDistilled != 0)
                    t("gui.ic2_120.distilled_water") else t("gui.ic2_120.water")
                lines.add(McText.literal(fluidName))
            }
            lines.add(McText.literal("${sync.waterAmount.coerceAtLeast(0)} / ${SteamGeneratorSync.WATER_TANK_CAPACITY} mB"))
            context.drawTooltip(textRenderer, lines, mouseX, mouseY)
        }
    }

    // ==== 按钮 — 14 个 9×9, 位置与 ic2_origin SteamBoilerButton 完全一致 ====

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button)
        val l = x; val t = y
        val bw = 9; val bh = 9

        fun hit(bx: Int, by: Int): Boolean =
            mouseX.toInt() in (l + bx) until (l + bx + bw) && mouseY.toInt() in (t + by) until (t + by + bh)

        // 右列: inputMB (x=92+10*i, y=186 减 / y=162 增)
        for (i in 0..3) {
            val bx = 92 + i * 10
            if (hit(bx, 186)) { sendClick(i); return true }
            if (hit(bx, 162)) { sendClick(4 + (3 - i)); return true }
        }
        // 左列: pressure (x=23+10*i, y=49 减 / y=25 增), i≠3
        for (i in 0..2) {
            val bx = 23 + i * 10
            if (hit(bx, 49)) { sendClick(8 + (2 - i)); return true }
            if (hit(bx, 25)) { sendClick(11 + i); return true }
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    private fun sendClick(id: Int) {
        MinecraftClient.getInstance().player?.networkHandler?.sendPacket(ButtonClickC2SPacket(handler.syncId, id))
    }

    // ==== 绘制辅助 ====

    private fun drawCentered(ctx: DrawContext, text: String, ax: Int, ay: Int, aw: Int, ah: Int, color: Int) {
        val tw = textRenderer.getWidth(text)
        ctx.drawText(textRenderer, text, ax + (aw - tw) / 2, ay + (ah - 8) / 2, color, true)
    }

    companion object {
        private val TEXTURE = Identifier.of("ic2", "textures/gui/guisteamgenerator.png")
        private val WATER_COLOR = 0x993388FF.toInt()
        private val HEAT_COLOR = 0xFFFF4444.toInt()
        private val CALC_COLOR = 0xFF888888.toInt()
    }
}
