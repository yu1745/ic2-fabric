package ic2_120.client.screen

import ic2_120.client.FluidUtils
import ic2_120.client.t
import ic2_120.content.screen.FluidUpgradeScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.Direction
import net.minecraft.client.util.math.Rect2i

@ModScreen(handler = "fluid_upgrade")
class FluidUpgradeScreen(
    handler: FluidUpgradeScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<FluidUpgradeScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = 176
        backgroundHeight = 166
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, backgroundWidth, backgroundHeight, TEX_SIZE, TEX_SIZE)
    }

    override fun init() {
        super.init()
        val client = client ?: return

        addDrawableChild(ButtonWidget.builder(Text.empty()) {
            client.networkHandler?.sendPacket(ButtonClickC2SPacket(handler.syncId, FluidUpgradeScreenHandler.BUTTON_CLEAR_FILTER))
        }.dimensions(x + 27, y + 37, 20, 14).build())

        // 六个方向分别独立开关，避免用一个循环按钮无法表达任意组合。
        for (dirIdx in Direction.entries.indices) {
            addDrawableChild(ButtonWidget.builder(Text.empty()) {
                client.networkHandler?.sendPacket(
                    ButtonClickC2SPacket(
                        handler.syncId,
                        FluidUpgradeScreenHandler.BUTTON_TOGGLE_DIR + dirIdx
                    )
                )
            }.dimensions(x + 9 + dirIdx * 16, y + 59, 16, 14).build())
        }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        // 幽灵过滤区始终为空，只绘制所选流体纹理。
        drawFilterFluid(context)

        drawJeiHint(context, mouseX, mouseY)

        context.drawText(textRenderer, title, x + (backgroundWidth - textRenderer.getWidth(title)) / 2, y + 6, 0x404040, false)

        // 流体限制文本 (9,18)-(104,32)
        val filterName = if (handler.fluidRawId > 0) {
            val fluid = Registries.FLUID.get(handler.fluidRawId)
            fluid.defaultState.blockState.block.name.string
        } else {
            t("gui.ic2_120.fluid_upgrade.no_filter")
        }
        val filterText = t("gui.ic2_120.fluid_upgrade.filter_display", filterName)
        context.drawText(textRenderer, filterText, x + 9, y + 18 + (14 - textRenderer.fontHeight) / 2, 0x55FF55, false)

        drawDirectionButtons(context)

        // 7px 按钮文字覆盖
        draw7pxText(context, x + 27, y + 37, 20, 14, t("gui.ic2_120.fluid_upgrade.clear_filter"))

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun drawFilterFluid(context: DrawContext) {
        if (handler.fluidRawId <= 0) return
        val fluid = Registries.FLUID.get(handler.fluidRawId)
        context.drawTexture(TEXTURE, x + 7, y + 34, 179f, 3f, 18, 18, TEX_SIZE, TEX_SIZE)
        val renderHandler = FluidRenderHandlerRegistry.INSTANCE.get(fluid)
        val sprite = renderHandler?.getFluidSprites(null, null, fluid.defaultState)?.getOrNull(0) ?: return
        val color = FluidUtils.getFluidColor(fluid)
        val (r, g, b) = if (color != -1) {
            Triple(((color shr 16) and 0xFF) / 255f, ((color shr 8) and 0xFF) / 255f, (color and 0xFF) / 255f)
        } else {
            Triple(1f, 1f, 1f)
        }
        context.drawSprite(x + 8, y + 35, 0, 16, 16, sprite, r, g, b, 1f)
    }

    private fun drawJeiHint(context: DrawContext, mouseX: Int, mouseY: Int) {
        if (!FabricLoader.getInstance().isModLoaded("jei")) return
        context.drawTexture(UPTIPS_TEXTURE, x + HINT_X, y + HINT_Y, 0f, 0f, HINT_SIZE, HINT_SIZE, HINT_SIZE, HINT_SIZE)
        if (mouseX - x in HINT_X until HINT_X + HINT_SIZE && mouseY - y in HINT_Y until HINT_Y + HINT_SIZE) {
            context.drawTooltip(textRenderer, Text.translatable("gui.ic2_120.jei.drag_filter_hint"), mouseX, mouseY)
        }
    }

    private fun drawDirectionButtons(context: DrawContext) {
        Direction.entries.forEachIndexed { index, direction ->
            val glyph = when (direction) {
                Direction.DOWN -> "↓"
                Direction.UP -> "↑"
                Direction.NORTH -> "N"
                Direction.SOUTH -> "S"
                Direction.WEST -> "W"
                Direction.EAST -> "E"
            }
            val color = if (handler.isDirectionActive(index)) 0x55FF55 else 0xFFFFFF
            val glyphX = x + 17 + index * 16 - textRenderer.getWidth(glyph) / 2
            context.drawText(textRenderer, glyph, glyphX, y + 62, color, false)
        }
    }

    fun ghostFilterArea(): Rect2i = Rect2i(x + 8, y + 35, 16, 16)

    private fun draw7pxText(context: DrawContext, bx: Int, by: Int, bw: Int, bh: Int, text: String) {
        val scale = 7f / textRenderer.fontHeight
        val textW = textRenderer.getWidth(text)
        val scaledW = textW * scale
        val textX = bx + (bw - scaledW) / 2f
        val textY = by + (bh - 7f) / 2f
        context.matrices.push()
        context.matrices.translate(textX.toDouble(), textY.toDouble(), 0.0)
        context.matrices.scale(scale, scale, 1f)
        context.drawText(textRenderer, text, 0, 0, 0xFFFFFFFF.toInt(), false)
        context.matrices.pop()
    }

    companion object {
        private val TEXTURE = Identifier("ic2", "textures/gui/guiupgrade.png")
        private val UPTIPS_TEXTURE = Identifier("ic2", "textures/gui/uptips.png")
        private const val TEX_SIZE = 256
        private const val HINT_X = 4
        private const val HINT_Y = 4
        private const val HINT_SIZE = 16
    }
}
