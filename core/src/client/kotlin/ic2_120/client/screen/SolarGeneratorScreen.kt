package ic2_120.client.screen

import ic2_120.content.block.SolarGeneratorBlock
import ic2_120.content.screen.SolarGeneratorScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Inventory
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

@ModScreen(block = SolarGeneratorBlock::class)
class SolarGeneratorScreen(
    handler: SolarGeneratorScreenHandler,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<SolarGeneratorScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = 176
        backgroundHeight = 166
    }

    override fun drawBackground(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, 176, 166, 256, 256)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        val isGenerating = handler.sync.isGenerating != 0

        // 标题居中于 y=6
        context.drawText(textRenderer, title, x + (176 - textRenderer.getWidth(title)) / 2, y + 6, 0x404040, false)

        // 太阳图标：纹理区域 (179,3)-(192,16) = 13×13，发电时渲染到 (81,44)
        val sunX = x + 82
        val sunY = y + 44
        val sunSize = 13
        if (isGenerating) {
            context.drawTexture(TEXTURE, sunX, sunY, 179f, 3f, sunSize, sunSize, 256, 256)
        }

        // 鼠标悬停太阳图标时显示发电量
        if (mouseX in sunX until (sunX + sunSize) && mouseY in sunY until (sunY + sunSize)) {
            val rate = handler.sync.getSyncedInsertedAmount()
            context.drawTooltip(textRenderer, Component.literal("发电：${rate} EU/t"), mouseX, mouseY)
        }

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath("ic2", "textures/gui/guisolargenerator.png")
    }
}
