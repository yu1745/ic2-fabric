package ic2_120.client.screen

import ic2_120.client.FluidUtils
import ic2_120.client.t
import ic2_120.content.screen.PumpAttachmentScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Inventory
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.client.renderer.Rect2i

@ModScreen(handlers = ["bronze_pump_attachment", "carbon_pump_attachment"])
class PumpAttachmentScreen(
    val handler: PumpAttachmentScreenHandler,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<PumpAttachmentScreenHandler>(handler, playerInventory, title) {

    init {
        imageWidth = 176
        imageHeight = 162
    }

    override fun renderBg(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        // 主 PNG 背景
        context.blit(TEXTURE, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, TEXTURE_SIZE, TEXTURE_SIZE)
        // 过滤器槽背景 (179,3)-(197,21) = 18×18，渲染至 (79,30)
        context.blit(
            TEXTURE, leftPos + SLOT_BG_X, topPos + SLOT_BG_Y,
            SLOT_BG_U.toFloat(), SLOT_BG_V.toFloat(),
            SLOT_BG_SIZE, SLOT_BG_SIZE,
            TEXTURE_SIZE, TEXTURE_SIZE
        )
        // 文本区域背景 (3,165)-(80,180) = 77×15，渲染至 (52,55)
        context.blit(
            TEXTURE, leftPos + TEXT_AREA_X, topPos + TEXT_AREA_Y,
            TEXT_AREA_U.toFloat(), TEXT_AREA_V.toFloat(),
            TEXT_AREA_W, TEXT_AREA_H,
            TEXTURE_SIZE, TEXTURE_SIZE
        )
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)

        // 幽灵过滤区始终为空，只绘制所选流体纹理。
        drawFilterFluid(context)

        drawJeiHint(context, mouseX, mouseY)

        // 标题居中
        context.drawString(font, title, leftPos + (imageWidth - font.width(title)) / 2, topPos + 6, 0x404040, false)

        // 过滤器状态文本，7px，居中于文本区域 (52,55)-(129,70)
        val fluid = menu.filterFluid()
        val line = when {
            fluid != null -> t("gui.ic2_120.pump_attachment.filter_current", fluid.defaultFluidState().createLegacyBlock().block.name.string)
            else -> t("gui.ic2_120.pump_attachment.filter_any")
        }
        val textScale = 7f / font.lineHeight
        val scaledW = (font.width(line) * textScale).toInt()
        val textX = leftPos + TEXT_AREA_X + (TEXT_AREA_W - scaledW) / 2
        val textCY = topPos + TEXT_AREA_Y + TEXT_AREA_H / 2
        context.pose().pushPose()
        context.pose().translate(textX.toDouble(), (textCY - 3.5), 0.0)
        context.pose().scale(textScale, textScale, 1f)
        context.drawString(font, line, 0, 0, 0xFF20EE7E.toInt(), false)
        context.pose().popPose()

        renderTooltip(context, mouseX, mouseY)
    }

    private fun drawFilterFluid(context: GuiGraphics) {
        val fluid = menu.filterFluid() ?: return
        context.blit(
            TEXTURE, leftPos + SLOT_BG_X, topPos + SLOT_BG_Y,
            SLOT_BG_U.toFloat(), SLOT_BG_V.toFloat(),
            SLOT_BG_SIZE, SLOT_BG_SIZE,
            TEXTURE_SIZE, TEXTURE_SIZE
        )
        val renderHandler = FluidRenderHandlerRegistry.INSTANCE.get(fluid)
        val sprite = renderHandler?.getFluidSprites(null, null, fluid.defaultFluidState())?.getOrNull(0) ?: return
        val color = FluidUtils.getFluidColor(fluid)
        val (r, g, b) = if (color != -1) {
            Triple(((color shr 16) and 0xFF) / 255f, ((color shr 8) and 0xFF) / 255f, (color and 0xFF) / 255f)
        } else {
            Triple(1f, 1f, 1f)
        }
        context.blit(leftPos + SLOT_BG_X + 1, topPos + SLOT_BG_Y + 1, 0, 16, 16, sprite, r, g, b, 1f)
    }

    private fun drawJeiHint(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (!FabricLoader.getInstance().isModLoaded("jei")) return
        context.blit(UPTIPS_TEXTURE, leftPos + HINT_X, topPos + HINT_Y, 0f, 0f, HINT_SIZE, HINT_SIZE, HINT_SIZE, HINT_SIZE)
        if (mouseX - leftPos in HINT_X until HINT_X + HINT_SIZE && mouseY - topPos in HINT_Y until HINT_Y + HINT_SIZE) {
            context.renderTooltip(font, Component.translatable("gui.ic2_120.jei.drag_filter_hint"), mouseX, mouseY)
        }
    }

    fun ghostFilterArea(): Rect2i = Rect2i(leftPos + 80, topPos + 31, 16, 16)

    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath("ic2", "textures/gui/guiother.png")
        private const val TEXTURE_SIZE = 256

        // 过滤器槽背景 (179,3)-(197,21) = 18×18
        private const val SLOT_BG_U = 179
        private const val SLOT_BG_V = 3
        private const val SLOT_BG_SIZE = 18
        private const val SLOT_BG_X = 79
        private const val SLOT_BG_Y = 30

        // 文本区域 (3,165)-(80,180) = 77×15
        private const val TEXT_AREA_U = 3
        private const val TEXT_AREA_V = 165
        private const val TEXT_AREA_W = 77
        private const val TEXT_AREA_H = 15
        private const val TEXT_AREA_X = 52
        private const val TEXT_AREA_Y = 55
        private val UPTIPS_TEXTURE = ResourceLocation.fromNamespaceAndPath("ic2", "textures/gui/uptips.png")
        private const val HINT_X = 4
        private const val HINT_Y = 4
        private const val HINT_SIZE = 16
    }
}
