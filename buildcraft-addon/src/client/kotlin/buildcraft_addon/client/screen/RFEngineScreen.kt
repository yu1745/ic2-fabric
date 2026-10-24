package buildcraft_addon.client.screen

import buildcraft_addon.BuildCraftAddon
import buildcraft_addon.content.screen.RFEngineScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = buildcraft_addon.content.block.RFEngineBlock::class)
class RFEngineScreen(
    handler: RFEngineScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<RFEngineScreenHandler>(handler, playerInventory, title) {

    companion object {
        val TEXTURE: Identifier = Identifier(BuildCraftAddon.MOD_ID, "textures/gui/rf_engine_gui.png")
        const val TEXTURE_SIZE = 256
        const val BG_WIDTH = 176
        const val BG_HEIGHT = 177

        // RF battery bar
        const val RF_BAR_X = 31
        const val RF_BAR_Y = 18
        const val RF_BAR_W = 6
        const val RF_BAR_H = 60

        // RF overlay in texture
        const val RF_TEX_U = 176
        const val RF_TEX_V = 0

        // Semi-transparent overlay in texture
        const val OVERLAY_TEX_X = 57
        const val OVERLAY_TEX_Y = 18
        const val OVERLAY_W = 80
        const val OVERLAY_H = 23

        // Gear renders
        const val IRON_GEAR_X = 78
        const val IRON_GEAR_Y = 22
        const val GOLD_GEAR_X = 101
        const val GOLD_GEAR_Y = 22

        const val MAX_RF = 10000
    }

    init {
        backgroundWidth = BG_WIDTH
        backgroundHeight = BG_HEIGHT
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, BG_WIDTH, BG_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE)

        // RF battery bar (bottom-up)
        val rfLevel = handler.getCurrentRF()
        val rfHeight = (rfLevel.toFloat() / MAX_RF * RF_BAR_H).toInt().coerceIn(0, RF_BAR_H)
        if (rfHeight > 0) {
            // Draw the RF bar from the texture, clipped
            context.drawTexture(
                TEXTURE,
                x + RF_BAR_X, y + RF_BAR_Y + RF_BAR_H - rfHeight,
                RF_TEX_U.toFloat(), (RF_TEX_V + RF_BAR_H - rfHeight).toFloat(),
                RF_BAR_W, rfHeight,
                TEXTURE_SIZE, TEXTURE_SIZE
            )
        }

        // Semi-transparent overlay over gear area
        context.drawTexture(
            TEXTURE,
            x + OVERLAY_TEX_X, y + OVERLAY_TEX_Y,
            OVERLAY_TEX_X.toFloat(), OVERLAY_TEX_Y.toFloat(),
            OVERLAY_W, OVERLAY_H,
            TEXTURE_SIZE, TEXTURE_SIZE
        )

        // Render gear icons as item stacks (matching BC8 behavior)
        context.drawItem(ItemStack(Items.IRON_INGOT), x + IRON_GEAR_X, y + IRON_GEAR_Y)
        context.drawItem(ItemStack(Items.GOLD_INGOT), x + GOLD_GEAR_X, y + GOLD_GEAR_Y)
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
