package ic2_120.client.screen

import ic2_120.content.block.CondenserBlock
import ic2_120.content.fluid.ModFluids
import ic2_120.content.screen.CondenserScreenHandler
import ic2_120.content.sync.CondenserSync
import ic2_120.registry.annotation.ModScreen
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.fluid.Fluid
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
        renderBackground(context)
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

        // ==== 电量条 (178,2)-(192,15) = 14×13，自下而上渲染至 (9,25) ====
        val energyFrac = if (CondenserSync.ENERGY_CAPACITY > 0) energy.toFloat() / CondenserSync.ENERGY_CAPACITY else 0f
        if (energyFrac > 0f) {
            val fillH = (ENERGY_BAR_H * energyFrac).toInt().coerceAtLeast(1)
            context.enableScissor(
                left + ENERGY_BAR_X,
                top + ENERGY_BAR_Y + ENERGY_BAR_H - fillH,
                left + ENERGY_BAR_X + ENERGY_BAR_W,
                top + ENERGY_BAR_Y + ENERGY_BAR_H
            )
            context.drawTexture(
                TEXTURE, left + ENERGY_BAR_X, top + ENERGY_BAR_Y,
                ENERGY_BAR_U.toFloat(), ENERGY_BAR_V.toFloat(),
                ENERGY_BAR_W, ENERGY_BAR_H,
                256, 256
            )
            context.disableScissor()
        }

        // ==== 蒸汽槽 (46,26)-(130,59) 84×33 ====
        val steamFrac = if (CondenserSync.STEAM_TANK_CAPACITY > 0) steamAmount.toFloat() / CondenserSync.STEAM_TANK_CAPACITY else 0f
        drawFluidTank(context, left + 46, top + 26, 84, 33, steamFrac, ModFluids.STEAM_STILL)

        // ==== 蒸馏水槽 (46,73)-(130,88) 84×15 ====
        val waterFrac = if (CondenserSync.WATER_TANK_CAPACITY > 0) waterAmount.toFloat() / CondenserSync.WATER_TANK_CAPACITY else 0f
        drawFluidTank(context, left + 46, top + 73, 84, 15, waterFrac, ModFluids.DISTILLED_WATER_STILL)

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

        // 电量条悬停
        if (mx in ENERGY_BAR_X until ENERGY_BAR_X + ENERGY_BAR_W &&
            my in ENERGY_BAR_Y until ENERGY_BAR_Y + ENERGY_BAR_H
        ) {
            context.drawTooltip(
                textRenderer,
                McText.literal("储能：$energy / ${CondenserSync.ENERGY_CAPACITY} EU"),
                mouseX, mouseY
            )
        }

        // 蒸汽槽悬停 (46,26)-(130,59)
        if (mx in 46 until 130 && my in 26 until 59) {
            val name = ModFluids.STEAM_STILL.defaultState.blockState.block.name
            val lines = if (steamAmount > 0) listOf(name, McText.literal("${"%,d".format(steamAmount)} / ${"%,d".format(CondenserSync.STEAM_TANK_CAPACITY)} mB"))
                        else listOf(McText.literal("空"))
            context.drawTooltip(textRenderer, lines, mouseX, mouseY)
        }

        // 蒸馏水槽悬停 (46,73)-(130,88)
        if (mx in 46 until 130 && my in 73 until 88) {
            val name = ModFluids.DISTILLED_WATER_STILL.defaultState.blockState.block.name
            val lines = if (waterAmount > 0) listOf(name, McText.literal("${"%,d".format(waterAmount)} / ${"%,d".format(CondenserSync.WATER_TANK_CAPACITY)} mB"))
                        else listOf(McText.literal("空"))
            context.drawTooltip(textRenderer, lines, mouseX, mouseY)
        }
    }

    /** 自下而上渲染流体纹理填充 */
    private fun drawFluidTank(context: DrawContext, gx: Int, gy: Int, w: Int, h: Int, fraction: Float, fluid: Fluid) {
        val fillH = (fraction.coerceIn(0f, 1f) * h).toInt()
        if (fillH <= 0) return

        val handler = FluidRenderHandlerRegistry.INSTANCE.get(fluid) ?: return
        val sprites = handler.getFluidSprites(null, null, fluid.defaultState) ?: return
        val sprite = sprites[0]

        val fillY = gy + h - fillH
        context.enableScissor(gx, fillY, gx + w, gy + h)

        for (sy in fillY until (gy + h) step 16) {
            val tileH = minOf(16, gy + h - sy)
            for (sx in gx until (gx + w) step 16) {
                val tileW = minOf(16, gx + w - sx)
                context.drawSprite(sx, sy, 0, tileW, tileH, sprite)
            }
        }

        context.disableScissor()
    }

    companion object {
        private val TEXTURE = Identifier("ic2", "textures/gui/guicondenser.png")
        private const val PROGRESS_WIDTH = 82

        // 电量条 (178,2)-(192,15) = 14×13，渲染至 (9,25)
        private const val ENERGY_BAR_U = 178
        private const val ENERGY_BAR_V = 2
        private const val ENERGY_BAR_W = 14
        private const val ENERGY_BAR_H = 13
        private const val ENERGY_BAR_X = 9
        private const val ENERGY_BAR_Y = 25

        // 槽位 hover tooltip (slotX, slotY, langKey) — 对齐实际槽位坐标
        private val SLOT_TOOLTIPS = listOf(
            Triple(26, 25, "gui.ic2_120.slot.vent"),
            Triple(134, 25, "gui.ic2_120.slot.vent"),
            Triple(26, 43, "gui.ic2_120.slot.vent"),
            Triple(134, 43, "gui.ic2_120.slot.vent"),
            Triple(152, 72, "gui.ic2_120.slot.upgrade"),
            Triple(8, 43, "gui.ic2_120.slot.discharge"),
            Triple(26, 72, "gui.ic2_120.slot.water_input"),
            Triple(134, 72, "gui.ic2_120.slot.water_output")
        )
    }
}
