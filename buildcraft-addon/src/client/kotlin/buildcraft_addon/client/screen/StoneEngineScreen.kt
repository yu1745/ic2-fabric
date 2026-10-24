package buildcraft_addon.client.screen

import buildcraft_addon.BuildCraftAddon
import buildcraft_addon.content.screen.StoneEngineScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Inventory
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

@ModScreen(block = buildcraft_addon.content.block.StoneEngineBlock::class)
class StoneEngineScreen(
    handler: StoneEngineScreenHandler,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<StoneEngineScreenHandler>(handler, playerInventory, title) {

    companion object {
        val TEXTURE: ResourceLocation = ResourceLocation(BuildCraftAddon.MOD_ID, "textures/gui/steam_engine_gui.png")
        const val TEXTURE_SIZE = 256
        const val BG_WIDTH = 176
        const val BG_HEIGHT = 166

        // Flame animation
        const val FLAME_X = 81
        const val FLAME_Y = 25
        const val FLAME_W = 14
        const val FLAME_H = 14
        const val FLAME_TEX_U = 176
        const val FLAME_TEX_V = 0
    }

    init {
        imageWidth = BG_WIDTH
        imageHeight = BG_HEIGHT
    }

    override fun drawBackground(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.blit(TEXTURE, x, y, 0f, 0f, BG_WIDTH, BG_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE)

        // Animated flame (bottom-up burn indicator)
        val burnProgress = handler.getBurnProgress()
        if (burnProgress > 0f) {
            val flameHeight = (burnProgress * FLAME_H).toInt().coerceIn(0, FLAME_H)
            context.blit(
                TEXTURE,
                x + FLAME_X, y + FLAME_Y + FLAME_H - flameHeight,
                FLAME_TEX_U.toFloat(), (FLAME_TEX_V + FLAME_H - flameHeight).toFloat(),
                FLAME_W, flameHeight + 2,
                TEXTURE_SIZE, TEXTURE_SIZE
            )
        }
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        renderTooltip(context, mouseX, mouseY)
    }

    override fun drawForeground(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        context.drawString(textRenderer, title, imageWidth / 2 - textRenderer.getWidth(title) / 2, 6, 0x404040, false)
        context.drawString(textRenderer, playerInventoryTitle, 8, BG_HEIGHT - 96, 0x404040, false)
    }
}
