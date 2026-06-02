package buildcraft_addon.client.screen

import buildcraft_addon.BuildCraftAddon
import buildcraft_addon.content.screen.IronEngineScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = buildcraft_addon.content.block.IronEngineBlock::class)
class IronEngineScreen(
    handler: IronEngineScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<IronEngineScreenHandler>(handler, playerInventory, title) {

    companion object {
        val TEXTURE: Identifier = Identifier.of(BuildCraftAddon.MOD_ID, "textures/gui/combustion_engine_gui.png")
        const val TEXTURE_SIZE = 256
        const val BG_WIDTH = 176
        const val BG_HEIGHT = 177

        // Tank positions (BC8)
        const val TANK_X = 26
        const val TANK_Y = 18
        const val TANK_W = 16
        const val TANK_H = 60
        const val TANK_SPACING = 54  // 80 = 26+54, 134 = 80+54

        // Tank overlay in texture
        const val TANK_TEX_U = 176
        const val TANK_TEX_V = 0

        const val MAX_FLUID = 10000
    }

    init {
        backgroundWidth = BG_WIDTH
        backgroundHeight = BG_HEIGHT
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, BG_WIDTH, BG_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE)

        // Draw tank overlays + fluid levels for 3 tanks
        drawTank(context, TANK_X, handler.getFuelAmount(), 0xFFFF4444.toInt())    // Red-ish for fuel
        drawTank(context, TANK_X + TANK_SPACING, handler.getCoolantAmount(), 0xFF4488FF.toInt())  // Blue for coolant
        drawTank(context, TANK_X + TANK_SPACING * 2, handler.getResidueAmount(), 0xFF885522.toInt())  // Brown for residue

        // Draw tank backgrounds (overlay texture)
        for (i in 0..2) {
            val tx = x + TANK_X + i * TANK_SPACING
            context.drawTexture(TEXTURE, tx, y + TANK_Y, TANK_TEX_U.toFloat(), TANK_TEX_V.toFloat(), TANK_W, TANK_H, TEXTURE_SIZE, TEXTURE_SIZE)
        }
    }

    private fun drawTank(context: DrawContext, tankX: Int, amount: Int, color: Int) {
        val filled = (amount.toFloat() / MAX_FLUID * TANK_H).toInt().coerceIn(0, TANK_H)
        if (filled > 0) {
            context.fill(x + tankX + 1, y + TANK_Y + TANK_H - filled, x + tankX + TANK_W - 1, y + TANK_Y + TANK_H, color)
        }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun drawForeground(context: DrawContext, mouseX: Int, mouseY: Int) {
        context.drawText(textRenderer, title, backgroundWidth / 2 - textRenderer.getWidth(title) / 2, 6, 0x404040, false)
        context.drawText(textRenderer, playerInventoryTitle, 8, BG_HEIGHT - 96, 0x404040, false)
    }
}
