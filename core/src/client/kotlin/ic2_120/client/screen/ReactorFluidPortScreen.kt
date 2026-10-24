package ic2_120.client.screen

import ic2_120.content.block.nuclear.ReactorFluidPortBlock
import ic2_120.content.screen.ReactorFluidPortScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Inventory
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

@ModScreen(block = ReactorFluidPortBlock::class)
class ReactorFluidPortScreen(
    val handler: ReactorFluidPortScreenHandler,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<ReactorFluidPortScreenHandler>(handler, playerInventory, title) {

    init {
        imageWidth = 176
        imageHeight = 162
    }

    override fun drawBackground(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        // 主 PNG 背景
        context.blit(TEXTURE, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, TEXTURE_SIZE, TEXTURE_SIZE)
        // 升级槽背景 (179,3)-(197,21) = 18×18，渲染至 (79,30) — 必须在 drawBackground 内，早于 super.render 的物品绘制
        context.blit(
            TEXTURE, leftPos + SLOT_BG_X, topPos + SLOT_BG_Y,
            SLOT_BG_U.toFloat(), SLOT_BG_V.toFloat(),
            SLOT_BG_SIZE, SLOT_BG_SIZE,
            TEXTURE_SIZE, TEXTURE_SIZE
        )
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        // 标题居中
        context.drawString(font, title, leftPos + (imageWidth - font.width(title)) / 2, topPos + 6, 0x404040, false)

        renderTooltip(context, mouseX, mouseY)
    }

    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath("ic2", "textures/gui/guiother.png")
        private const val TEXTURE_SIZE = 256

        private const val SLOT_BG_U = 179
        private const val SLOT_BG_V = 3
        private const val SLOT_BG_SIZE = 18
        private const val SLOT_BG_X = 79
        private const val SLOT_BG_Y = 30
    }
}
