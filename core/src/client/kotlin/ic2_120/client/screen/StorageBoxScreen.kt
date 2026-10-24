package ic2_120.client.screen

import ic2_120.content.screen.StorageBoxScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(handler = "storage_box")
class StorageBoxScreen(
    handler: StorageBoxScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<StorageBoxScreenHandler>(handler, playerInventory, title) {

    private val boxType = BoxType.of(handler.inventory.size())

    init {
        backgroundWidth = boxType.guiWidth
        backgroundHeight = boxType.guiHeight
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        val type = boxType
        if (type.texture == null) return

        context.drawTexture(type.texture, x, y, 0f, 0f, backgroundWidth, backgroundHeight, type.textureSize, type.textureSize)

        for (row in 0 until type.rows) {
            for (col in 0 until type.columns) {
                context.drawTexture(
                    type.texture,
                    x + type.slotBgX + col * 18,
                    y + type.slotBgY + row * 18,
                    type.slotBgU.toFloat(), type.slotBgV.toFloat(),
                    SLOT_BG_SIZE, SLOT_BG_SIZE,
                    type.textureSize, type.textureSize
                )
            }
        }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        if (boxType.texture != null) {
            context.drawText(textRenderer, title, x + (backgroundWidth - textRenderer.getWidth(title)) / 2, y + 6, 0x404040, false)
        }

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    companion object {
        private const val SLOT_BG_SIZE = 18
    }

    private enum class BoxType(
        val texture: Identifier?,
        val guiWidth: Int,
        val guiHeight: Int,
        val rows: Int,
        val columns: Int,
        val slotBgX: Int,
        val slotBgY: Int,
        val slotBgU: Int,
        val slotBgV: Int,
        val textureSize: Int
    ) {
        WOODEN(Identifier("ic2", "textures/gui/guiwoodenbox.png"), 224, 162, 3, 9, 31, 18, 227, 3, 256),
        IRON(Identifier("ic2", "textures/gui/guiironbox.png"), 224, 202, 5, 9, 31, 17, 227, 3, 256),
        STEEL(Identifier("ic2", "textures/gui/guisteelbox.png"), 224, 236, 7, 9, 31, 17, 227, 3, 256),
        IRIDIUM(Identifier("ic2", "textures/gui/guiiridiumbox.png"), 262, 275, 9, 14, 5, 18, 265, 3, 300),
        OTHER(null, 176, 166, 0, 0, 0, 0, 0, 0, 256);

        companion object {
            fun of(size: Int): BoxType = when (size) {
                27 -> WOODEN
                45 -> IRON
                63 -> STEEL
                126 -> IRIDIUM
                else -> OTHER
            }
        }
    }
}
