package ic2_120.client.screen

import ic2_120.client.FluidUtils
import ic2_120.client.t
import ic2_120.content.screen.FluidUpgradeScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.components.Button
import net.minecraft.world.entity.player.Inventory
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.Direction
import net.minecraft.client.renderer.Rect2i

@ModScreen(handler = "fluid_upgrade")
class FluidUpgradeScreen(
    val handler: FluidUpgradeScreenHandler,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<FluidUpgradeScreenHandler>(handler, playerInventory, title) {

    init {
        imageWidth = 176
        imageHeight = 166
    }

    override fun drawBackground(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.blit(TEXTURE, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, TEX_SIZE, TEX_SIZE)
    }

    override fun init() {
        super.init()
        val client = client ?: return

        addRenderableWidget(Button.builder(Component.empty()) {
            minecraft.getConnection()?.send(ServerboundContainerButtonClickPacket(menu.containerId, FluidUpgradeScreenHandler.BUTTON_CLEAR_FILTER))
        }.bounds(leftPos + 27, topPos + 37, 20, 14).build())

        // 六个方向分别独立开关，避免用一个循环按钮无法表达任意组合。
        for (dirIdx in Direction.entries.indices) {
            addRenderableWidget(Button.builder(Component.empty()) {
                minecraft.getConnection()?.send(
                    ServerboundContainerButtonClickPacket(
                        menu.containerId,
                        FluidUpgradeScreenHandler.BUTTON_TOGGLE_DIR + dirIdx
                    )
                )
            }.bounds(leftPos + 9 + dirIdx * 16, topPos + 59, 16, 14).build())
        }
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        // 幽灵过滤区始终为空，只绘制所选流体纹理。
        drawFilterFluid(context)

        drawJeiHint(context, mouseX, mouseY)

        context.drawString(font, title, leftPos + (imageWidth - font.width(title)) / 2, topPos + 6, 0x404040, false)

        // 流体限制文本 (9,18)-(104,32)
        val filterName = if (menu.fluidRawId > 0) {
            val fluid = BuiltInRegistries.FLUID.get(handler.fluidRawId)
            fluid.defaultFluidState().createLegacyBlock().blockState.block.name.string
        } else {
            t("gui.ic2_120.fluid_upgrade.no_filter")
        }
        val filterText = t("gui.ic2_120.fluid_upgrade.filter_display", filterName)
        context.drawString(font, filterText, leftPos + 9, topPos + 18 + (14 - font.lineHeight) / 2, 0x55FF55, false)

        drawDirectionButtons(context)

        // 7px 按钮文字覆盖
        draw7pxText(context, leftPos + 27, topPos + 37, 20, 14, t("gui.ic2_120.fluid_upgrade.clear_filter"))

        renderTooltip(context, mouseX, mouseY)
    }

    private fun drawFilterFluid(context: GuiGraphics) {
        if (menu.fluidRawId <= 0) return
        val fluid = BuiltInRegistries.FLUID.get(handler.fluidRawId)
        context.blit(TEXTURE, leftPos + 7, topPos + 34, 179f, 3f, 18, 18, TEX_SIZE, TEX_SIZE)
        val renderHandler = FluidRenderHandlerRegistry.INSTANCE.get(fluid)
        val sprite = renderHandler?.getFluidSprites(null, null, fluid.defaultFluidState().createLegacyBlock())?.getOrNull(0) ?: return
        val color = FluidUtils.getFluidColor(fluid)
        val (r, g, b) = if (color != -1) {
            Triple(((color shr 16) and 0xFF) / 255f, ((color shr 8) and 0xFF) / 255f, (color and 0xFF) / 255f)
        } else {
            Triple(1f, 1f, 1f)
        }
        context.blit(leftPos + 8, topPos + 35, 0, 16, 16, sprite, r, g, b, 1f)
    }

    private fun drawJeiHint(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (!FabricLoader.getInstance().isModLoaded("jei")) return
        context.blit(UPTIPS_TEXTURE, leftPos + HINT_X, topPos + HINT_Y, 0f, 0f, HINT_SIZE, HINT_SIZE, HINT_SIZE, HINT_SIZE)
        if (mouseX - leftPos in HINT_X until HINT_X + HINT_SIZE && mouseY - topPos in HINT_Y until HINT_Y + HINT_SIZE) {
            context.renderComponentTooltip(font, Component.translatable("gui.ic2_120.jei.drag_filter_hint"), mouseX, mouseY)
        }
    }

    private fun drawDirectionButtons(context: GuiGraphics) {
        Direction.entries.forEachIndexed { index, direction ->
            val glyph = when (direction) {
                Direction.DOWN -> "↓"
                Direction.UP -> "↑"
                Direction.NORTH -> "N"
                Direction.SOUTH -> "S"
                Direction.WEST -> "W"
                Direction.EAST -> "E"
            }
            val color = if (menu.isDirectionActive(index)) 0x55FF55 else 0xFFFFFF
            val glyphX = leftPos + 17 + index * 16 - font.width(glyph) / 2
            context.drawString(font, glyph, glyphX, topPos + 62, color, false)
        }
    }

    fun ghostFilterArea(): Rect2i = Rect2i(leftPos + 8, topPos + 35, 16, 16)

    private fun draw7pxText(context: GuiGraphics, bx: Int, by: Int, bw: Int, bh: Int, text: String) {
        val scale = 7f / font.lineHeight
        val textW = font.width(text)
        val scaledW = textW * scale
        val textX = bx + (bw - scaledW) / 2f
        val textY = by + (bh - 7f) / 2f
        context.pose().pushPose()
        context.pose().translate(textX.toDouble(), textY.toDouble(), 0.0)
        context.pose().scale(scale, scale, 1f)
        context.drawString(font, text, 0, 0, 0xFFFFFFFF.toInt(), false)
        context.pose().popPose()
    }

    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath("ic2", "textures/gui/guiupgrade.png")
        private val UPTIPS_TEXTURE = ResourceLocation.fromNamespaceAndPath("ic2", "textures/gui/uptips.png")
        private const val TEX_SIZE = 256
        private const val HINT_X = 4
        private const val HINT_Y = 4
        private const val HINT_SIZE = 16
    }
}
