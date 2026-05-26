package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.FluidUtils
import ic2_120.content.block.CannerBlock
import ic2_120.content.screen.CannerScreenHandler
import ic2_120.content.sync.CannerSync
import ic2_120.registry.annotation.ModScreen
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.fluid.Fluid
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = CannerBlock::class)
class CannerScreen(
    handler: CannerScreenHandler, playerInventory: PlayerInventory, title: Text
) : HandledScreen<CannerScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = 176
        backgroundHeight = 184
        titleY = -1000
        playerInventoryTitleY = -1000
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, backgroundWidth, backgroundHeight, TEX_SIZE, TEX_SIZE)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        val left = x
        val top = y
        val mode = handler.sync.getMode()

        // 居中置顶标题
        val title = Text.translatable("block.ic2_120.canner")
        context.drawText(textRenderer, title, x + (backgroundWidth - textRenderer.getWidth(title)) / 2, y + 6, 0x404040, false)

        // 能量条
        drawEnergyBar(context, left, top)

        // 进度条
        drawProgressBar(context, left, top)

        // 左侧流体槽
        drawFluidTank(context, left = left, top = top, tankX = TANK_L_X, tankY = TANK_L_Y,
            tankW = TANK_W, tankH = TANK_H, fluidRawId = handler.sync.leftFluidRawId,
            amountMb = handler.sync.leftFluidAmountMb, capacityMb = handler.sync.leftFluidCapacityMb)

        // 右侧流体槽
        drawFluidTank(context, left = left, top = top, tankX = TANK_R_X, tankY = TANK_R_Y,
            tankW = TANK_W, tankH = TANK_H, fluidRawId = handler.sync.rightFluidRawId,
            amountMb = handler.sync.rightFluidAmountMb, capacityMb = handler.sync.rightFluidCapacityMb)

        // 左侧容量标示 (182,124)-(194,171) = 12×47 → (43,47)，有流体时渲染
        if (handler.sync.leftFluidAmountMb > 0) {
            context.drawTexture(TEXTURE, left + 43, top + 47, 182f, 124f, 12, 47, TEX_SIZE, TEX_SIZE)
        }
        // 右侧容量标示 (182,124)-(194,171) = 12×47 → (121,47)，有流体时渲染
        if (handler.sync.rightFluidAmountMb > 0) {
            context.drawTexture(TEXTURE, left + 121, top + 47, 182f, 124f, 12, 47, TEX_SIZE, TEX_SIZE)
        }

        // 模式标识纹理
        drawModeTexture(context, left, top, mode)

        // uptips
        context.drawTexture(UPTIPS_TEXTURE, left + UPTIPS_X, top + UPTIPS_Y, 0f, 0f, UPTIPS_SIZE, UPTIPS_SIZE, UPTIPS_SIZE, UPTIPS_SIZE)

        // 悬停高亮 & 提示
        drawHoverHighlights(context, mouseX, mouseY, left, top, mode)

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun drawEnergyBar(context: DrawContext, left: Int, top: Int) {
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1)
        val fraction = (energy.toFloat() / cap).coerceIn(0f, 1f)
        if (fraction <= 0f) return
        val fillH = (ENERGY_BAR_H * fraction).toInt().coerceAtLeast(1)
        context.enableScissor(
            left + ENERGY_BAR_X,
            top + ENERGY_BAR_Y + ENERGY_BAR_H - fillH,
            left + ENERGY_BAR_X + ENERGY_BAR_W,
            top + ENERGY_BAR_Y + ENERGY_BAR_H
        )
        context.drawTexture(TEXTURE, left + ENERGY_BAR_X, top + ENERGY_BAR_Y,
            ENERGY_BAR_U.toFloat(), ENERGY_BAR_V.toFloat(),
            ENERGY_BAR_W, ENERGY_BAR_H, TEX_SIZE, TEX_SIZE)
        context.disableScissor()
    }

    private fun drawProgressBar(context: DrawContext, left: Int, top: Int) {
        val progress = handler.sync.progress.coerceIn(0, CannerSync.PROGRESS_MAX)
        val fraction = progress.toFloat() / CannerSync.PROGRESS_MAX
        if (fraction <= 0f) return
        val fillW = (PROGRESS_W * fraction).toInt().coerceAtLeast(1)
        context.enableScissor(
            left + PROGRESS_X,
            top + PROGRESS_Y,
            left + PROGRESS_X + fillW,
            top + PROGRESS_Y + PROGRESS_H
        )
        context.drawTexture(TEXTURE, left + PROGRESS_X, top + PROGRESS_Y,
            PROGRESS_U.toFloat(), PROGRESS_V.toFloat(),
            PROGRESS_W, PROGRESS_H, TEX_SIZE, TEX_SIZE)
        context.disableScissor()
    }

    private fun drawFluidTank(
        context: DrawContext, left: Int, top: Int,
        tankX: Int, tankY: Int, tankW: Int, tankH: Int,
        fluidRawId: Int, amountMb: Int, capacityMb: Int
    ) {
        if (fluidRawId <= 0 || amountMb <= 0) return
        val fluid = Registries.FLUID.get(fluidRawId)
        val sprite = getFluidStillSprite(fluid) ?: return
        val color = FluidUtils.getFluidColor(fluid)
        if (color == -1) return
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        val fraction = (amountMb.toFloat() / capacityMb.coerceAtLeast(1)).coerceIn(0f, 1f)
        val fillH = (tankH * fraction).toInt().coerceAtLeast(1)
        val sx = left + tankX
        val sy = top + tankY
        val fillY = sy + tankH - fillH
        context.enableScissor(sx, fillY, sx + tankW, sy + tankH)
        for (cy in fillY until (sy + tankH) step 16) {
            val tileH = minOf(16, sy + tankH - cy)
            for (cx in sx until (sx + tankW) step 16) {
                val tileW = minOf(16, sx + tankW - cx)
                context.drawSprite(cx, cy, 0, tileW, tileH, sprite, r, g, b, 1f)
            }
        }
        context.disableScissor()
    }

    private fun drawModeTexture(context: DrawContext, left: Int, top: Int, mode: CannerSync.Mode) {
        val (u, v, w, h) = when (mode) {
            CannerSync.Mode.BOTTLE_SOLID -> MODE_SOLID
            CannerSync.Mode.EMPTY_LIQUID -> MODE_EMPTY
            CannerSync.Mode.BOTTLE_LIQUID -> MODE_BOTTLE
            CannerSync.Mode.ENRICH_LIQUID -> MODE_ENRICH
        }
        context.drawTexture(TEXTURE, left + MODE_TEX_X, top + MODE_TEX_Y, u.toFloat(), v.toFloat(), w, h, TEX_SIZE, TEX_SIZE)

        // 额外纹理
        when (mode) {
            CannerSync.Mode.EMPTY_LIQUID, CannerSync.Mode.BOTTLE_LIQUID -> {
                context.drawTexture(TEXTURE, left + EXTRA_EB_X, top + EXTRA_EB_Y,
                    EXTRA_EB_U.toFloat(), EXTRA_EB_V.toFloat(), EXTRA_EB_W, EXTRA_EB_H, TEX_SIZE, TEX_SIZE)
            }
            CannerSync.Mode.ENRICH_LIQUID -> {
                context.drawTexture(TEXTURE, left + EXTRA_ENRICH_X, top + EXTRA_ENRICH_Y,
                    EXTRA_ENRICH_U.toFloat(), EXTRA_ENRICH_V.toFloat(), EXTRA_ENRICH_W, EXTRA_ENRICH_H, TEX_SIZE, TEX_SIZE)
            }
            else -> {}
        }
    }

    private fun drawHoverHighlights(context: DrawContext, mouseX: Int, mouseY: Int, left: Int, top: Int, mode: CannerSync.Mode) {
        val relX = mouseX - left
        val relY = mouseY - top

        // 流体交换区域
        if (relX in SWAP_X1..SWAP_X2 && relY in SWAP_Y1..SWAP_Y2) {
            context.fill(left + SWAP_X1, top + SWAP_Y1, left + SWAP_X2 + 1, top + SWAP_Y2 + 1, 0x80FFFFFF.toInt())
            context.drawTooltip(textRenderer, listOf(Text.translatable("gui.ic2_120.canner.swap_tanks")), mouseX, mouseY)
        }

        // 模式切换区域
        if (relX in MODE_CLICK_X1..MODE_CLICK_X2 && relY in MODE_CLICK_Y1..MODE_CLICK_Y2) {
            context.fill(left + MODE_CLICK_X1, top + MODE_CLICK_Y1, left + MODE_CLICK_X2 + 1, top + MODE_CLICK_Y2 + 1, 0x80FFFFFF.toInt())
            val modeText = when (mode) {
                CannerSync.Mode.BOTTLE_SOLID -> Text.translatable("gui.ic2_120.canner.mode_bottle_solid")
                CannerSync.Mode.EMPTY_LIQUID -> Text.translatable("gui.ic2_120.canner.mode_empty_liquid")
                CannerSync.Mode.BOTTLE_LIQUID -> Text.translatable("gui.ic2_120.canner.mode_bottle_liquid")
                CannerSync.Mode.ENRICH_LIQUID -> Text.translatable("gui.ic2_120.canner.mode_enrich_liquid")
            }
            context.drawTooltip(textRenderer, listOf(modeText), mouseX, mouseY)
        }

        // 电量条悬停
        if (relX in ENERGY_BAR_X until ENERGY_BAR_X + ENERGY_BAR_W &&
            relY in ENERGY_BAR_Y until ENERGY_BAR_Y + ENERGY_BAR_H
        ) {
            val energy = handler.sync.energy.toLong().coerceAtLeast(0)
            val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1)
            context.drawTooltip(textRenderer,
                listOf(Text.literal("储能：${EnergyFormatUtils.formatEu(energy)} / ${EnergyFormatUtils.formatEu(cap)} EU")),
                mouseX, mouseY)
        }

        // 左侧流体槽悬停
        if (relX in TANK_L_X until TANK_L_X + TANK_W && relY in TANK_L_Y until TANK_L_Y + TANK_H) {
            val amt = handler.sync.leftFluidAmountMb.coerceAtLeast(0)
            val cap = handler.sync.leftFluidCapacityMb.coerceAtLeast(1)
            val lines = if (amt > 0) {
                val name = Registries.FLUID.get(handler.sync.leftFluidRawId).defaultState.blockState.block.name.string
                listOf(Text.literal(name), Text.literal("${"%,d".format(amt)} / ${"%,d".format(cap)} mB"))
            } else {
                listOf(Text.literal("空"))
            }
            context.drawTooltip(textRenderer, lines, mouseX, mouseY)
        }

        // 右侧流体槽悬停
        if (relX in TANK_R_X until TANK_R_X + TANK_W && relY in TANK_R_Y until TANK_R_Y + TANK_H) {
            val amt = handler.sync.rightFluidAmountMb.coerceAtLeast(0)
            val cap = handler.sync.rightFluidCapacityMb.coerceAtLeast(1)
            val lines = if (amt > 0) {
                val name = Registries.FLUID.get(handler.sync.rightFluidRawId).defaultState.blockState.block.name.string
                listOf(Text.literal(name), Text.literal("${"%,d".format(amt)} / ${"%,d".format(cap)} mB"))
            } else {
                listOf(Text.literal("空"))
            }
            context.drawTooltip(textRenderer, lines, mouseX, mouseY)
        }

        // uptips悬停
        if (relX in UPTIPS_X until UPTIPS_X + UPTIPS_SIZE && relY in UPTIPS_Y until UPTIPS_Y + UPTIPS_SIZE) {
            context.drawTooltip(textRenderer, listOf(
                Text.translatable("gui.ic2_120.canner.uptips"),
                Text.literal("§7").append(Text.translatable("item.ic2_120.overclocker_upgrade")),
                Text.literal("§7").append(Text.translatable("item.ic2_120.transformer_upgrade")),
                Text.literal("§7").append(Text.translatable("item.ic2_120.energy_storage_upgrade")),
                Text.literal("§7").append(Text.translatable("item.ic2_120.ejector_upgrade")),
                Text.literal("§7").append(Text.translatable("item.ic2_120.fluid_ejector_upgrade")),
                Text.literal("§7").append(Text.translatable("item.ic2_120.fluid_pulling_upgrade"))
            ), mouseX, mouseY)
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) {
            val relX = mouseX.toInt() - x
            val relY = mouseY.toInt() - y
            if (relX in SWAP_X1..SWAP_X2 && relY in SWAP_Y1..SWAP_Y2) {
                client?.player?.networkHandler?.sendPacket(
                    ButtonClickC2SPacket(handler.syncId, CannerScreenHandler.BUTTON_ID_SWAP_TANKS))
                return true
            }
            if (relX in MODE_CLICK_X1..MODE_CLICK_X2 && relY in MODE_CLICK_Y1..MODE_CLICK_Y2) {
                client?.player?.networkHandler?.sendPacket(
                    ButtonClickC2SPacket(handler.syncId, CannerScreenHandler.BUTTON_ID_MODE_CYCLE))
                return true
            }
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    companion object {
        private val TEXTURE = Identifier("ic2", "textures/gui/guicanner.png")
        private val UPTIPS_TEXTURE = Identifier("ic2", "textures/gui/uptips.png")
        private const val TEX_SIZE = 256

        // 能量条 (180,2)-(194,15) = 14×13, 渲染至 (9,61)
        private const val ENERGY_BAR_U = 180
        private const val ENERGY_BAR_V = 2
        private const val ENERGY_BAR_W = 14
        private const val ENERGY_BAR_H = 13
        private const val ENERGY_BAR_X = 9
        private const val ENERGY_BAR_Y = 61

        // 进度条 (180,81)-(203,95) = 23×14, 渲染至 (74,21)
        private const val PROGRESS_U = 180
        private const val PROGRESS_V = 80
        private const val PROGRESS_W = 23
        private const val PROGRESS_H = 15
        private const val PROGRESS_X = 74
        private const val PROGRESS_Y = 21

        // 模式纹理 渲染至 (63,80)
        private const val MODE_TEX_X = 63
        private const val MODE_TEX_Y = 80
        private val MODE_SOLID = Quad(180, 20, 50, 14)
        private val MODE_EMPTY = Quad(180, 34, 50, 14)
        private val MODE_BOTTLE = Quad(180, 48, 50, 14)
        private val MODE_ENRICH = Quad(180, 62, 50, 14)

        // 额外纹理：空/灌模式 (180,100)-(206,118) = 26×18, 渲染至 (71,42)
        private const val EXTRA_EB_U = 180
        private const val EXTRA_EB_V = 100
        private const val EXTRA_EB_W = 26
        private const val EXTRA_EB_H = 18
        private const val EXTRA_EB_X = 71
        private const val EXTRA_EB_Y = 42

        // 额外纹理：富集模式 (210,81)-(219,96) = 9×15, 渲染至 (109,62)
        private const val EXTRA_ENRICH_U = 210
        private const val EXTRA_ENRICH_V = 81
        private const val EXTRA_ENRICH_W = 9
        private const val EXTRA_ENRICH_H = 15
        private const val EXTRA_ENRICH_X = 109
        private const val EXTRA_ENRICH_Y = 62

        // 左侧流体槽 (43,46)-(55,93) = 12×47
        private const val TANK_L_X = 43
        private const val TANK_L_Y = 46
        private const val TANK_W = 12
        private const val TANK_H = 47

        // 右侧流体槽 (121,46)-(133,93) = 12×47
        private const val TANK_R_X = 121
        private const val TANK_R_Y = 46

        // 流体交换区域 (77,63)-(99,76)
        private const val SWAP_X1 = 77
        private const val SWAP_Y1 = 63
        private const val SWAP_X2 = 98
        private const val SWAP_Y2 = 75

        // 模式切换区域 (61,78)-(114,95)
        private const val MODE_CLICK_X1 = 61
        private const val MODE_CLICK_Y1 = 78
        private const val MODE_CLICK_X2 = 114
        private const val MODE_CLICK_Y2 = 95

        // uptips (4,4) 16×16
        private const val UPTIPS_X = 4
        private const val UPTIPS_Y = 4
        private const val UPTIPS_SIZE = 16
    }
}

private fun getFluidStillSprite(fluid: Fluid): net.minecraft.client.texture.Sprite? {
    val handler = FluidRenderHandlerRegistry.INSTANCE.get(fluid) ?: return null
    return handler.getFluidSprites(null, null, fluid.defaultState).getOrNull(0)
}

private data class Quad(val u: Int, val v: Int, val w: Int, val h: Int)
