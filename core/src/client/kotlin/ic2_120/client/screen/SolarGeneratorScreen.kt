package ic2_120.client.screen

import ic2_120.content.block.SolarGeneratorBlock
import ic2_120.content.screen.SolarGeneratorScreenHandler
import ic2_120.registry.annotation.ModScreen
import ic2_120.client.compat.*
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Inventory
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

@ModScreen(block = SolarGeneratorBlock::class)
class SolarGeneratorScreen(
    val handler: SolarGeneratorScreenHandler,
    playerInventory: Inventory,
    title: Component
) : LegacyContainerScreen26<SolarGeneratorScreenHandler>(handler, playerInventory, title, 176, 166) {

    init {
    }

    override fun renderBg(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.blit(TEXTURE, leftPos, topPos, 0f, 0f, 176, 166, 256, 256)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)

        val isGenerating = menu.sync.isGenerating != 0

        // 标题居中于 y=6
        context.drawString(font, title, leftPos + (176 - font.width(title)) / 2, topPos + 6, 0x404040, false)

        // 太阳图标：纹理区域 (179,3)-(192,16) = 13×13，发电时渲染到 (81,44)
        val sunX = leftPos + 82
        val sunY = topPos + 44
        val sunSize = 13
        if (isGenerating) {
            context.blit(TEXTURE, sunX, sunY, 179f, 3f, sunSize, sunSize, 256, 256)
        }

        // 鼠标悬停太阳图标时显示发电量
        if (mouseX in sunX until (sunX + sunSize) && mouseY in sunY until (sunY + sunSize)) {
            val rate = menu.sync.getSyncedInsertedAmount()
            context.renderTooltip(font, Component.literal("发电：${rate} EU/t"), mouseX, mouseY)
        }

        renderTooltip(context, mouseX, mouseY)
    }

    companion object {
        private val TEXTURE = Identifier.fromNamespaceAndPath("ic2", "textures/gui/guisolargenerator.png")
    }
}
