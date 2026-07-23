package ic2_120.client.screen

import ic2_120.client.t
import ic2_120.content.screen.ItemUpgradeScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.util.math.Rect2i
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

    /** Slot index used by the AE2-style JEI fake-slot packet. */
    val ghostFilterSlotIndex: Int get() = handler.filterSlotIndex

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

        addDrawableChild(ButtonWidget.builder(Text.empty()) {
            client.networkHandler?.sendPacket(ButtonClickC2SPacket(handler.syncId, ItemUpgradeScreenHandler.BUTTON_CLEAR_FILTER))
        }.dimensions(x + 27, y + 37, 20, 14).build())

        // 六个方向分别独立开关，支持任意方向组合。
        for (dirIdx in Direction.entries.indices) {
            addDrawableChild(ButtonWidget.builder(Text.empty()) {
                client.networkHandler?.sendPacket(
                    ButtonClickC2SPacket(
                        handler.syncId,
                        ItemUpgradeScreenHandler.BUTTON_TOGGLE_DIR + dirIdx
                    )
                )
            }.dimensions(x + 9 + dirIdx * 16, y + 59, 16, 14).build())
        }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        drawFilterItem(context)
        drawJeiHint(context, mouseX, mouseY)

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

        drawDirectionButtons(context)

        // 7px 按钮文字覆盖
        draw7pxText(context, x + 27, y + 37, 20, 14, t("gui.ic2_120.item_upgrade.clear_filter"))

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun drawDirectionButtons(context: DrawContext) {
        Direction.entries.forEachIndexed { index, direction ->
            val glyph = when (direction) {
                Direction.DOWN -> "↓"
                Direction.UP -> "↑"
                Direction.NORTH -> "N"
                Direction.SOUTH -> "S"
                Direction.WEST -> "W"
                Direction.EAST -> "E"
            }
            val color = if (handler.isDirectionActive(index)) 0x55FF55 else 0xFFFFFF
            val glyphX = x + 17 + index * 16 - textRenderer.getWidth(glyph) / 2
            context.drawText(textRenderer, glyph, glyphX, y + 62, color, false)
        }
    }

    private fun drawFilterItem(context: DrawContext) {
        if (handler.itemRawId <= 0) return
        val item = Registries.ITEM.get(handler.itemRawId)
        if (item != net.minecraft.item.Items.AIR) context.drawItem(item.defaultStack, x + 8, y + 35)
    }

    private fun drawJeiHint(context: DrawContext, mouseX: Int, mouseY: Int) {
        if (!FabricLoader.getInstance().isModLoaded("jei")) return
        context.drawTexture(UPTIPS_TEXTURE, x + HINT_X, y + HINT_Y, 0f, 0f, HINT_SIZE, HINT_SIZE, HINT_SIZE, HINT_SIZE)
        if (mouseX - x in HINT_X until HINT_X + HINT_SIZE && mouseY - y in HINT_Y until HINT_Y + HINT_SIZE) {
            context.drawTooltip(textRenderer, Text.translatable("gui.ic2_120.jei.drag_filter_hint"), mouseX, mouseY)
        }
    }

    private fun draw7pxText(context: DrawContext, bx: Int, by: Int, bw: Int, bh: Int, text: String) {
        val scale = 7f / textRenderer.fontHeight
        val textW = textRenderer.getWidth(text)
        val scaledW = textW * scale
        val textX = bx + (bw - scaledW) / 2f
        val textY = by + (bh - 7f) / 2f
        context.matrices.push()
        context.matrices.translate(textX.toDouble(), textY.toDouble(), 0.0)
        context.matrices.scale(scale, scale, 1f)
        context.drawText(textRenderer, text, 0, 0, 0xFFFFFFFF.toInt(), false)
        context.matrices.pop()
    }

    companion object {
        private val TEXTURE = Identifier("ic2", "textures/gui/guiupgrade.png")
        private val UPTIPS_TEXTURE = Identifier("ic2", "textures/gui/uptips.png")
        private const val TEX_SIZE = 256
        private const val HINT_X = 4
        private const val HINT_Y = 4
        private const val HINT_SIZE = 16
    }

    /** JEI ghost ingredient drag target：容器槽位区域 (x+8, y+35, 16x16) */
    fun ghostFilterArea(): Rect2i = Rect2i(x + 8, y + 35, 16, 16)
}
