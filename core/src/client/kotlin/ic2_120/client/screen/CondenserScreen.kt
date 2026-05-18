package ic2_120.client.screen

import ic2_120.content.block.CondenserBlock
import ic2_120.content.screen.CondenserScreenHandler
import ic2_120.content.sync.CondenserSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text as McText
import net.minecraft.util.Identifier

/**
 * 冷凝器 GUI — 对齐 ic2_origin GuiCondenser (176×184)。
 *
 * 纹理: ic2:textures/gui/guicondenser.png (256×256)
 *
 * ic2_origin 元素位置:
 *   - 蒸汽罐: TankGauge Plain at (46, 27, 84×33)
 *   - 蒸馏水罐: TankGauge Plain at (46, 74, 84×15)
 *   - 散热口槽: SlotGrid 1×2 at (25, 25) + (133, 25)
 *   - 能量条: EnergyGauge asBolt at (12, 26)
 *   - 进度条: LinkedGauge ProgressCondenser at (47, 63)
 */
@ModScreen(block = CondenserBlock::class)
class CondenserScreen(
    handler: CondenserScreenHandler,
    playerInventory: PlayerInventory,
    title: McText
) : HandledScreen<CondenserScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = 176
        backgroundHeight = 184
    }

    // ==== 背景纹理 + 标题 — 对齐 ic2_origin drawBackgroundAndTitle ====

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        // 背景: guicondenser.png (0,0) 到 (176,184)
        context.drawTexture(TEXTURE, x, y, 0f, 0f, backgroundWidth, backgroundHeight, 256, 256)
        // 标题: 居中, 颜色 0x404040 (4210752) — 与 Ic2Gui.drawXCenteredString 一致
        context.drawText(
            textRenderer, title,
            x + (backgroundWidth - textRenderer.getWidth(title)) / 2,
            y + 6,
            0x404040, false
        )
    }

    // ==== 前景 — 能量条(纹理) + 液位(色块) + 进度条(纹理) + 槽位 tooltip ====

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)

        val left = x
        val top = y

        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val steamAmount = handler.sync.steamAmount.coerceAtLeast(0)
        val waterAmount = handler.sync.waterAmount.coerceAtLeast(0)

        // ==== 能量条 ====
        context.drawTexture(COMMON, left + 8, top + 25, 96f, 64f, 16, 16, 256, 256)
        val energyFrac = if (CondenserSync.ENERGY_CAPACITY > 0) energy.toFloat() / CondenserSync.ENERGY_CAPACITY else 0f
        val boltHeight = (energyFrac.coerceIn(0f, 1f) * BOLT_HEIGHT).toInt()
        if (boltHeight > 0) {
            context.drawTexture(COMMON,
                left + 12, top + 26 + BOLT_HEIGHT - boltHeight,
                116f, 65f + BOLT_HEIGHT - boltHeight,
                7, boltHeight, 256, 256
            )
        }

        // ==== 液位罐填充 ====
        val steamFrac = if (CondenserSync.STEAM_TANK_CAPACITY > 0) steamAmount.toFloat() / CondenserSync.STEAM_TANK_CAPACITY else 0f
        drawTankFill(context, left + 46, top + 27, 84, 33, steamFrac)
        val waterFrac = if (CondenserSync.WATER_TANK_CAPACITY > 0) waterAmount.toFloat() / CondenserSync.WATER_TANK_CAPACITY else 0f
        drawTankFill(context, left + 46, top + 74, 84, 15, waterFrac)

        // ==== 进度条 ====
        val progFrac = if (CondenserSync.PROGRESS_MAX > 0) handler.sync.progress.toFloat() / CondenserSync.PROGRESS_MAX else 0f
        val progW = (progFrac.coerceIn(0f, 1f) * PROGRESS_WIDTH).toInt()
        if (progW > 0) {
            context.drawTexture(TEXTURE, left + 47, top + 63, 1f, 185f, progW, 7, 256, 256)
        }

        // ==== 左侧提示：散热口耗电但 EU 不足 ====
        if (handler.sync.ventCount > 0 && handler.sync.coolingRate <= 0) {
            val warning = McText.translatable("gui.ic2_120.condenser.no_power_warning")
            context.drawText(textRenderer, warning,
                left - textRenderer.getWidth(warning) - 4,
                top + 28, 0xFF5555, false)
        }

        // ==== 槽位 tooltip：空槽显示槽位名称，有物品时显示物品 tooltip ====
        val mx = mouseX - left
        val my = mouseY - top
        var handled = false
        val hoveredSlot = handler.slots.firstOrNull { s ->
            mx >= s.x - 1 && mx < s.x + 17 && my >= s.y - 1 && my < s.y + 17
        }
        if (hoveredSlot != null && !hoveredSlot.hasStack()) {
            for ((sx, sy, key) in SLOT_TOOLTIPS) {
                if (hoveredSlot.x == sx && hoveredSlot.y == sy) {
                    context.drawTooltip(textRenderer, McText.translatable(key), mouseX, mouseY)
                    handled = true
                    break
                }
            }
        }
        if (!handled) drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun drawTankFill(context: DrawContext, x: Int, y: Int, width: Int, height: Int, fraction: Float) {
        val f = fraction.coerceIn(0f, 1f)
        val fillH = (f * height).toInt()
        if (fillH > 0) {
            context.fill(x, y + height - fillH, x + width, y + height, TANK_FILL_COLOR)
        }
    }

    companion object {
        private val TEXTURE = Identifier.of("ic2", "textures/gui/guicondenser.png")
        private val COMMON = Identifier.of("ic2", "textures/gui/common.png")
        private const val PROGRESS_WIDTH = 82
        private const val BOLT_HEIGHT = 13
        private const val TANK_FILL_COLOR = 0x993388FF.toInt()

        // 槽位 hover tooltip (slotX, slotY, langKey) — 对齐实际槽位坐标
        private val SLOT_TOOLTIPS = listOf(
            Triple(26, 26, "gui.ic2_120.slot.vent"),
            Triple(134, 26, "gui.ic2_120.slot.vent"),
            Triple(26, 44, "gui.ic2_120.slot.vent"),
            Triple(134, 44, "gui.ic2_120.slot.vent"),
            Triple(152, 73, "gui.ic2_120.slot.upgrade"),
            Triple(8, 44, "gui.ic2_120.slot.discharge"),
            Triple(26, 73, "gui.ic2_120.slot.water_input"),
            Triple(134, 73, "gui.ic2_120.slot.water_output")
        )
    }
}
