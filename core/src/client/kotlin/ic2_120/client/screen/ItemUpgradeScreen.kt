package ic2_120.client.screen

import ic2_120.client.t
import ic2_120.content.screen.ItemUpgradeScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.Direction

@ModScreen(handler = "item_upgrade")
class ItemUpgradeScreen(
    handler: ItemUpgradeScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<ItemUpgradeScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = 176
        backgroundHeight = 166
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, backgroundWidth, backgroundHeight, TEX_SIZE, TEX_SIZE)
    }

    override fun init() {
        super.init()
        val client = client ?: return

        addDrawableChild(ButtonWidget.builder(Text.literal(t("gui.ic2_120.item_upgrade.set_filter"))) {
            client.networkHandler?.sendPacket(ButtonClickC2SPacket(handler.syncId, ItemUpgradeScreenHandler.BUTTON_SET_FILTER))
        }.dimensions(x + 27, y + 37, 20, 14).build())

        addDrawableChild(ButtonWidget.builder(Text.literal(t("gui.ic2_120.item_upgrade.cycle_direction_short"))) {
            client.networkHandler?.sendPacket(ButtonClickC2SPacket(handler.syncId, ItemUpgradeScreenHandler.BUTTON_CYCLE_DIRECTION))
        }.dimensions(x + 108, y + 60, 20, 12).build())
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)

        context.drawText(textRenderer, title, x + (backgroundWidth - textRenderer.getWidth(title)) / 2, y + 6, 0x404040, false)

        // 过滤文本 (9,18)-(104,32)
        val filterName = if (handler.itemRawId > 0) {
            Registries.ITEM.get(handler.itemRawId).name.string
        } else {
            t("gui.ic2_120.item_upgrade.no_filter")
        }
        val filterText = t("gui.ic2_120.item_upgrade.filter_display", filterName)
        val filterTextX = x + 9
        val filterTextY = y + 18 + (14 - textRenderer.fontHeight) / 2
        context.drawText(textRenderer, filterText, filterTextX, filterTextY, 0x55FF55, false)

        // 方向文本 (9,59)-(104,73)
        val dirOrdinal = handler.directionOrdinal
        val dirName = if (dirOrdinal in 0..5) {
            t("gui.ic2_120.direction.${Direction.entries[dirOrdinal].name.lowercase()}")
        } else {
            t("gui.ic2_120.fluid_upgrade.any_direction")
        }
        val dirText = t("gui.ic2_120.fluid_upgrade.direction", dirName)
        context.drawText(textRenderer, dirText, x + 9, y + 59 + (14 - textRenderer.fontHeight) / 2, 0x55FF55, false)

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    companion object {
        private val TEXTURE = Identifier.of("ic2", "textures/gui/guiupgrade.png")
        private const val TEX_SIZE = 256
    }
}
