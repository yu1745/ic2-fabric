package ic2_120.client.screen

import ic2_120.content.screen.StorageBoxScreenHandler
import ic2_120.registry.annotation.ModScreen
import ic2_120.client.compat.*
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Inventory
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

@ModScreen(handler = "storage_box")
class StorageBoxScreen(
    val handler: StorageBoxScreenHandler,
    playerInventory: Inventory,
    title: Component
) : LegacyContainerScreen26<StorageBoxScreenHandler>(handler, playerInventory, title, BoxType.of(handler.inventory.getContainerSize()).guiWidth, BoxType.of(handler.inventory.getContainerSize()).guiHeight) {

    private val boxType = BoxType.of(handler.inventory.getContainerSize())

    init {
    }

    override fun renderBg(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        val type = boxType
        if (type.texture == null) return

        context.blit(type.texture, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, type.textureSize, type.textureSize)

        for (row in 0 until type.rows) {
            for (col in 0 until type.columns) {
                context.blit(
                    type.texture,
                    leftPos + type.slotBgX + col * 18,
                    topPos + type.slotBgY + row * 18,
                    type.slotBgU.toFloat(), type.slotBgV.toFloat(),
                    SLOT_BG_SIZE, SLOT_BG_SIZE,
                    type.textureSize, type.textureSize
                )
            }
        }
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)

        if (boxType.texture != null) {
            context.drawString(font, title, leftPos + (imageWidth - font.width(title)) / 2, topPos + 6, 0x404040, false)
        }

        renderTooltip(context, mouseX, mouseY)
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
        WOODEN(Identifier.fromNamespaceAndPath("ic2", "textures/gui/guiwoodenbox.png"), 224, 162, 3, 9, 31, 18, 227, 3, 256),
        IRON(Identifier.fromNamespaceAndPath("ic2", "textures/gui/guiironbox.png"), 224, 202, 5, 9, 31, 17, 227, 3, 256),
        STEEL(Identifier.fromNamespaceAndPath("ic2", "textures/gui/guisteelbox.png"), 224, 236, 7, 9, 31, 17, 227, 3, 256),
        IRIDIUM(Identifier.fromNamespaceAndPath("ic2", "textures/gui/guiiridiumbox.png"), 262, 275, 9, 14, 5, 18, 265, 3, 300),
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
