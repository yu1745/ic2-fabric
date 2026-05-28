package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.FluidUtils
import ic2_120.client.t
import ic2_120.content.block.MatterGeneratorBlock
import ic2_120.content.fluid.ModFluids
import ic2_120.content.screen.MatterGeneratorScreenHandler
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import ic2_120.content.sync.MatterGeneratorSync
import ic2_120.registry.annotation.ModScreen
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = MatterGeneratorBlock::class)
class MatterGeneratorScreen(
    handler: MatterGeneratorScreenHandler, playerInventory: PlayerInventory, title: Text
) : HandledScreen<MatterGeneratorScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = 176
        backgroundHeight = 166
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, backgroundWidth, backgroundHeight, TEX_SIZE, TEX_SIZE)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        val left = x
        val top = y

        context.drawText(textRenderer, title, left + (backgroundWidth - textRenderer.getWidth(title)) / 2, top + 6, 0x404040, false)

        // 流体槽 (100,26)-(112,73) 12×47
        drawFluidTank(context, left, top)

        // 容量标示纹理 (181,6)-(192,52) = 11×46 渲染至 (101,27)
        if (handler.sync.fluidAmount > 0) {
            context.drawTexture(TEXTURE, left + TANK_OVERLAY_X, top + TANK_OVERLAY_Y,
                TANK_OVERLAY_U.toFloat(), TANK_OVERLAY_V.toFloat(), TANK_OVERLAY_W, TANK_OVERLAY_H, TEX_SIZE, TEX_SIZE)
        }

        // 进度文本 (12,43) — 百分比
        val pct = (handler.sync.progress.toFloat() / MatterGeneratorSync.PROGRESS_MAX * 100).toInt().coerceIn(0, 100)
        context.drawText(textRenderer, t("gui.ic2_120.matter_generator.progress_pct", pct), left + 12, top + 43, 0x000000, false)

        // uptips (4,4) 16×16
        context.drawTexture(UPTIPS_TEXTURE, left + 4, top + 4, 0f, 0f, 16, 16, 16, 16)

        // 悬停提示
        val relX = mouseX - left
        val relY = mouseY - top

        // 流体槽悬停
        if (relX in TANK_X until TANK_X + TANK_W && relY in TANK_Y until TANK_Y + TANK_H) {
            val amt = handler.sync.fluidAmount.coerceAtLeast(0)
            val cap = handler.sync.fluidCapacity.coerceAtLeast(1)
            val lines = if (amt > 0) listOf(Text.literal("UU物质"), Text.literal("${"%,d".format(amt / DROPLETS_PER_MB)} / ${"%,d".format(cap / DROPLETS_PER_MB)} mB"))
                        else listOf(Text.literal("空"))
            context.drawTooltip(textRenderer, lines, mouseX, mouseY)
        }

        // uptips悬停
        if (relX in 4 until 20 && relY in 4 until 20) {
            context.drawTooltip(textRenderer, listOf(
                Text.translatable("gui.ic2_120.matter_generator.uptips"),
                Text.literal("§7").append(Text.translatable("item.ic2_120.overclocker_upgrade")),
                Text.literal("§7").append(Text.translatable("item.ic2_120.transformer_upgrade")),
                Text.literal("§7").append(Text.translatable("item.ic2_120.energy_storage_upgrade")),
                Text.literal("§7").append(Text.translatable("item.ic2_120.ejector_upgrade")),
                Text.literal("§7").append(Text.translatable("item.ic2_120.fluid_pulling_upgrade"))
            ), mouseX, mouseY)
        }

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun drawFluidTank(context: DrawContext, left: Int, top: Int) {
        val amt = handler.sync.fluidAmount.coerceAtLeast(0)
        if (amt <= 0) return
        val cap = handler.sync.fluidCapacity.coerceAtLeast(1)
        val fraction = (amt.toFloat() / cap).coerceIn(0f, 1f)
        val fillH = (TANK_H * fraction).toInt().coerceAtLeast(1)
        val sx = left + TANK_X
        val sy = top + TANK_Y
        val sprite = uuMatterSprite ?: return
        val color = FluidUtils.getFluidColor(ModFluids.UU_MATTER_STILL)
        if (color == -1) return
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        val fillY = sy + TANK_H - fillH
        context.enableScissor(sx, fillY, sx + TANK_W, sy + TANK_H)
        for (cy in fillY until (sy + TANK_H) step 16) {
            val tileH = minOf(16, sy + TANK_H - cy)
            for (cx in sx until (sx + TANK_W) step 16) {
                val tileW = minOf(16, sx + TANK_W - cx)
                context.drawSprite(cx, cy, 0, tileW, tileH, sprite, r, g, b, 1f)
            }
        }
        context.disableScissor()
    }

    companion object {
        private val DROPLETS_PER_MB = (FluidConstants.BUCKET / 1000).toInt()
        private val TEXTURE = Identifier("ic2", "textures/gui/guimattergenerator.png")
        private val UPTIPS_TEXTURE = Identifier("ic2", "textures/gui/uptips.png")
        private const val TEX_SIZE = 256

        private const val TANK_X = 100
        private const val TANK_Y = 26
        private const val TANK_W = 12
        private const val TANK_H = 47

        private const val TANK_OVERLAY_U = 181
        private const val TANK_OVERLAY_V = 6
        private const val TANK_OVERLAY_W = 11
        private const val TANK_OVERLAY_H = 46
        private const val TANK_OVERLAY_X = 101
        private const val TANK_OVERLAY_Y = 27

        private val uuMatterSprite by lazy {
            FluidRenderHandlerRegistry.INSTANCE.get(ModFluids.UU_MATTER_STILL)
                ?.getFluidSprites(null, null, ModFluids.UU_MATTER_STILL.defaultState)?.getOrNull(0)
        }
    }
}
