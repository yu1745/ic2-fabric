package buildcraft_addon.client.screen

import buildcraft_addon.BuildCraftAddon
import buildcraft_addon.content.screen.RFEngineScreenHandler
import ic2_120.registry.annotation.ModScreen
import ic2_120.client.compat.*
import ic2_120.client.screen.LegacyContainerScreen26
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

@ModScreen(block = buildcraft_addon.content.block.RFEngineBlock::class)
class RFEngineScreen(
    handler: RFEngineScreenHandler,
    playerInventory: Inventory,
    title: Component
) : LegacyContainerScreen26<RFEngineScreenHandler>(handler, playerInventory, title, BG_WIDTH, BG_HEIGHT) {
    // yarn→mojmap 兼容别名
    private val x get() = leftPos
    private val y get() = topPos
    private val textRenderer get() = font

    companion object {
        val TEXTURE: Identifier = Identifier.fromNamespaceAndPath(BuildCraftAddon.MOD_ID, "textures/gui/rf_engine_gui.png")
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
    }

    override fun renderBg(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.blit(TEXTURE, leftPos, topPos, 0f, 0f, BG_WIDTH, BG_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE)

        // RF battery bar (bottom-up)
        val rfLevel = menu.getCurrentRF()
        val rfHeight = (rfLevel.toFloat() / MAX_RF * RF_BAR_H).toInt().coerceIn(0, RF_BAR_H)
        if (rfHeight > 0) {
            // Draw the RF bar from the texture, clipped
            context.blit(
                TEXTURE,
                x + RF_BAR_X, y + RF_BAR_Y + RF_BAR_H - rfHeight,
                RF_TEX_U.toFloat(), (RF_TEX_V + RF_BAR_H - rfHeight).toFloat(),
                RF_BAR_W, rfHeight,
                TEXTURE_SIZE, TEXTURE_SIZE
            )
        }

        // Semi-transparent overlay over gear area
        context.blit(
            TEXTURE,
            x + OVERLAY_TEX_X, y + OVERLAY_TEX_Y,
            OVERLAY_TEX_X.toFloat(), OVERLAY_TEX_Y.toFloat(),
            OVERLAY_W, OVERLAY_H,
            TEXTURE_SIZE, TEXTURE_SIZE
        )

        // Render gear icons as item stacks (matching BC8 behavior)
        context.renderItem(ItemStack(Items.IRON_INGOT), x + IRON_GEAR_X, y + IRON_GEAR_Y)
        context.renderItem(ItemStack(Items.GOLD_INGOT), x + GOLD_GEAR_X, y + GOLD_GEAR_Y)
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
