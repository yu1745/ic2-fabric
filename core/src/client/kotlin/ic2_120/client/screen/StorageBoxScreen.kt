package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.t
import ic2_120.client.ui.GuiBackground
import ic2_120.content.screen.StorageBoxScreenHandler
import ic2_120.content.screen.GuiSize
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

/**
 * 储物箱 GUI
 *
 * 使用标准 176px 宽度，所有容量通过 ScrollView 滚动显示。
 */
@ModScreen(handler = "storage_box")
class StorageBoxScreen(
    handler: StorageBoxScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<StorageBoxScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = GuiSize.STANDARD.width
        backgroundHeight = GUI_HEIGHT
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y,
            PLAYER_INV_Y,
            HOTBAR_Y,
            SLOT_SIZE
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y
        val inventorySize = handler.inventory.size()
        val rows = (inventorySize + 8) / 9

        val content: UiScope.() -> Unit = {
            Column(
                x = left + 8,
                y = top + 6,
                spacing = 4
            ) {
                // 标题区
                Text(title.string, color = 0xFFFFFF)
                Text(
                    t("gui.ic2_120.storage_box.capacity", inventorySize),
                    color = 0xAAAAAA,
                    shadow = false
                )

                // 滚动槽位区（宽度 162 = 9×18，与玩家背包对齐）
                ScrollView(
                    width = CONTENT_WIDTH,
                    height = SCROLL_HEIGHT,
                    scrollbarWidth = 8
                ) {
                    Column(spacing = 0) {
                        for (row in 0 until rows) {
                            Flex(direction = FlexDirection.ROW) {
                                for (col in 0 until 9) {
                                    val slotIndex = row * 9 + col
                                    if (slotIndex < inventorySize) {
                                        SlotAnchor(
                                            id = slotAnchorId(slotIndex),
                                            width = SLOT_SIZE,
                                            height = SLOT_SIZE
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            playerInventoryAndHotbarSlotAnchors(
                left = left,
                top = top,
                playerInvStart = handler.playerInventorySlotStart,
                playerInvY = PLAYER_INV_Y,
                hotbarY = HOTBAR_Y
            )
        }

        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)
        applyAnchoredSlots(layout, left, top)

        super.render(context, mouseX, mouseY, delta)
        ui.render(context, textRenderer, mouseX, mouseY, content = content)
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun applyAnchoredSlots(layout: ComposeUI.LayoutSnapshot, left: Int, top: Int) {
        handler.slots.forEachIndexed { index, slot ->
            val anchor = layout.anchors[slotAnchorId(index)] ?: return@forEachIndexed
            slot.x = anchor.x - left
            slot.y = anchor.y - top
        }
    }

    private fun slotAnchorId(slotIndex: Int): String = "slot.$slotIndex"

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean =
        ui.mouseScrolled(mouseX, mouseY, 0.0, amount)
                || super.mouseScrolled(mouseX, mouseY, amount)

    override fun mouseDragged(
        mouseX: Double, mouseY: Double, button: Int,
        deltaX: Double, deltaY: Double
    ): Boolean = ui.mouseDragged(mouseX, mouseY, button)
            || super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        ui.stopDrag()
        return super.mouseReleased(mouseX, mouseY, button)
    }

    private companion object {
        private const val SLOT_SIZE = 18
        // 储物槽区域滚动高度（每行 18px + gap 4px）
        const val SCROLL_HEIGHT = 120
        /**
         * GUI 总高度；与 [GuiSize.computeHeight] 一致（主内容区高度 + 8 顶边距 + 玩家栏块 76）。
         * [PLAYER_INV_Y] / [HOTBAR_Y] 与滚动区高度绑定，非固定枚举档位。
         */
        val GUI_HEIGHT = GuiSize.computeHeight(6 + 34 + 4 + SCROLL_HEIGHT)
        // 9列 × 18px - 2px inset = 160
        const val CONTENT_WIDTH = 160
        // 玩家背包 Y 起始
        const val PLAYER_INV_Y = 6 + 34 + 4 + SCROLL_HEIGHT + 8
        const val HOTBAR_Y = PLAYER_INV_Y + 58
    }
}
