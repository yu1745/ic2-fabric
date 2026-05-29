package ic2_120.client.screen

import ic2_120.client.FluidUtils
import ic2_120.content.block.CokeKilnBlock
import ic2_120.content.screen.CokeKilnScreenHandler
import ic2_120.content.screen.GuiSize
import ic2_120.content.sync.CokeKilnSync
import ic2_120.registry.annotation.ModScreen
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = CokeKilnBlock::class)
class CokeKilnScreen(
    handler: CokeKilnScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<CokeKilnScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = GUI_SIZE.width
        backgroundHeight = GUI_SIZE.height
    }

    override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // no-op: panel drawn in render() directly, prevents dark overlay on top of GUI
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, BG_W, BG_H, TEX_SIZE, TEX_SIZE)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)

        val left = x
        val top = y

        context.drawText(textRenderer, title, left + (backgroundWidth - textRenderer.getWidth(title)) / 2, top + 6, 0x404040, false)

        // 工作进度 (181,8)-(195,21) = 14×13 → (81,27)，满纹理从上至下减少
        val progressFrac = if (CokeKilnSync.PROGRESS_MAX > 0) {
            (handler.sync.progress.coerceIn(0, CokeKilnSync.PROGRESS_MAX).toFloat() / CokeKilnSync.PROGRESS_MAX).coerceIn(0f, 1f)
        } else 0f
        if (handler.sync.progress > 0) {
            val drainedH = (PROGRESS_H * progressFrac).toInt()
            context.enableScissor(
                left + PROGRESS_X,
                top + PROGRESS_Y + drainedH,
                left + PROGRESS_X + PROGRESS_W,
                top + PROGRESS_Y + PROGRESS_H
            )
            context.drawTexture(TEXTURE, left + PROGRESS_X, top + PROGRESS_Y,
                PROGRESS_U.toFloat(), PROGRESS_V.toFloat(),
                PROGRESS_W, PROGRESS_H, TEX_SIZE, TEX_SIZE)
            context.disableScissor()
        }

        // 流体槽 (115,41) 16×16，有流体时满平铺
        val fluidAmount = handler.sync.fluidAmount.coerceAtLeast(0)
        if (fluidAmount > 0) run {
            val fluid = Registries.FLUID.get(handler.sync.fluidRawId)
            val fluidHandler = FluidRenderHandlerRegistry.INSTANCE.get(fluid) ?: return@run
            val sprite = fluidHandler.getFluidSprites(null, null, fluid.defaultState).getOrNull(0) ?: return@run
            val color = FluidUtils.getFluidColor(fluid)
            if (color == -1) return@run
            val r = ((color shr 16) and 0xFF) / 255f
            val g = ((color shr 8) and 0xFF) / 255f
            val b = (color and 0xFF) / 255f
            val sx = left + TANK_X + (SLOT_S - TANK_S) / 2
            val sy = top + TANK_Y + (SLOT_S - TANK_S) / 2
            // 居中的 16×16 tile（居中在槽内）
            for (cy in sy until (sy + TANK_S) step 16) {
                val tileH = minOf(16, sy + TANK_S - cy)
                for (cx in sx until (sx + TANK_S) step 16) {
                    val tileW = minOf(16, sx + TANK_S - cx)
                    context.drawSprite(cx, cy, 0, tileW, tileH, sprite, r, g, b, 1f)
                }
            }
        }

        drawMouseoverTooltip(context, mouseX, mouseY)

        // 流体槽悬停
        val relX = mouseX - left
        val relY = mouseY - top
        if (relX in TANK_X until TANK_X + SLOT_S && relY in TANK_Y until TANK_Y + SLOT_S) {
            val lines = if (fluidAmount > 0) {
                val fluid = Registries.FLUID.get(handler.sync.fluidRawId)
                val mb = fluidAmount / DROPLETS_PER_MB
                val capMb = (8 * FluidConstants.BUCKET / DROPLETS_PER_MB).toLong()
                listOf(Text.literal(fluid.defaultState.blockState.block.name.string),
                    Text.literal("${"%,d".format(mb)} / ${"%,d".format(capMb)} mB"))
            } else {
                listOf(Text.literal("空"))
            }
            context.drawTooltip(textRenderer, lines, mouseX, mouseY)
        }
    }

    companion object {
        private val TEXTURE = Identifier.of("ic2", "textures/gui/guicokeoven.png")
        private const val TEX_SIZE = 256
        private val GUI_SIZE = GuiSize.STANDARD
        private val DROPLETS_PER_MB = (FluidConstants.BUCKET / 1000).toInt()

        private const val BG_W = 176
        private const val BG_H = 161
        private const val SLOT_S = 18
        private const val TANK_S = 16

        // 工作进度 (181,8)-(195,21) = 14×13 → (81,27)
        private const val PROGRESS_U = 181
        private const val PROGRESS_V = 8
        private const val PROGRESS_W = 14
        private const val PROGRESS_H = 13
        private const val PROGRESS_X = 81
        private const val PROGRESS_Y = 27

        // 流体槽 (115,41) 16×16
        private const val TANK_X = 115
        private const val TANK_Y = 41
    }
}
