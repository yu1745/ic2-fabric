package buildcraft_addon.client.screen

import buildcraft_addon.BuildCraftAddon
import buildcraft_addon.content.screen.IronEngineScreenHandler
import ic2_120.registry.annotation.ModScreen
import ic2_120.client.compat.*
import ic2_120.client.screen.LegacyContainerScreen26
import net.minecraft.world.entity.player.Inventory
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

@ModScreen(block = buildcraft_addon.content.block.IronEngineBlock::class)
class IronEngineScreen(
    handler: IronEngineScreenHandler,
    playerInventory: Inventory,
    title: Component
) : LegacyContainerScreen26<IronEngineScreenHandler>(handler, playerInventory, title, BG_WIDTH, BG_HEIGHT) {
    // yarn→mojmap 兼容别名
    private val x get() = leftPos
    private val y get() = topPos
    private val textRenderer get() = font

    companion object {
        val TEXTURE: Identifier = Identifier.fromNamespaceAndPath(BuildCraftAddon.MOD_ID, "textures/gui/combustion_engine_gui.png")
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
    }

    override fun renderBg(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.blit(TEXTURE, leftPos, topPos, 0f, 0f, BG_WIDTH, BG_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE)

        // Draw tank overlays + fluid levels for 3 tanks
        drawTank(context, TANK_X, menu.getFuelAmount(), 0xFFFF4444.toInt())    // Red-ish for fuel
        drawTank(context, TANK_X + TANK_SPACING, menu.getCoolantAmount(), 0xFF4488FF.toInt())  // Blue for coolant
        drawTank(context, TANK_X + TANK_SPACING * 2, menu.getResidueAmount(), 0xFF885522.toInt())  // Brown for residue

        // Draw tank backgrounds (overlay texture)
        for (i in 0..2) {
            val tx = leftPos + TANK_X + i * TANK_SPACING
            context.blit(TEXTURE, tx, y + TANK_Y, TANK_TEX_U.toFloat(), TANK_TEX_V.toFloat(), TANK_W, TANK_H, TEXTURE_SIZE, TEXTURE_SIZE)
        }
    }

    private fun drawTank(context: GuiGraphics, tankX: Int, amount: Int, color: Int) {
        val filled = (amount.toFloat() / MAX_FLUID * TANK_H).toInt().coerceIn(0, TANK_H)
        if (filled > 0) {
            context.fill(x + tankX + 1, y + TANK_Y + TANK_H - filled, x + tankX + TANK_W - 1, y + TANK_Y + TANK_H, color)
        }
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        renderTooltip(context, mouseX, mouseY)
    }

    override fun extractLabels(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        context.drawString(font, title, imageWidth / 2 - font.width(title) / 2, 6, 0x404040, false)
        context.drawString(font, playerInventoryTitle, 8, BG_HEIGHT - 96, 0x404040, false)
    }
}
