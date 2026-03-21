package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.storage.StorageBoxBlockEntity
import ic2_120.content.screen.StorageBoxScreenHandler
import ic2_120.registry.annotation.ModScreen
import ic2_120.registry.type
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.slot.Slot
import net.minecraft.text.Text

/**
 * 储物箱 GUI
 *
 * 动态背景高度，根据储物箱容量调整行数。
 */
@ModScreen(handler = "storage_box")
class StorageBoxScreen(
    handler: StorageBoxScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<StorageBoxScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    /** 根据储物箱容量计算 GUI 尺寸 */
    private val guiSize: GuiSize = calculateGuiSize()

    init {
        backgroundWidth = guiSize.width
        backgroundHeight = guiSize.height
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        // 绘制背景
        if (guiSize.isDoubleColumn) {
            // 双列布局：分别绘制左右列背景
            // 左列背景
            GuiBackground.drawVanillaLikePanel(context, x + guiSize.leftColumnX, y, 162, guiSize.leftColumnHeight)
            // 右列背景（主背景）
            GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
        } else {
            // 单列背景
            GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
        }

        // 绘制玩家背包槽位边框（使用 ScreenHandler 计算的位置）
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y,
            handler.playerInventoryY,
            handler.hotbarY,
            SLOT_SIZE
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y

        val inventorySize = handler.inventory.size()
        val useDoubleColumn = inventorySize > 45

        val content: UiScope.() -> Unit = {
            Column(
                x = left + 8,
                y = top + 6,
                spacing = 4
            ) {
                Text(
                    title.string,
                    color = 0xFFFFFF
                )
                Text(
                    "容量: ${handler.inventory.size()} 格",
                    color = 0xAAAAAA,
                    shadow = false
                )

                // 储物箱槽位布局
                if (useDoubleColumn) {
                    // 双列布局
                    val rightColumnSlots = inventorySize.coerceAtMost(81)
                    val rightColumnRows = (rightColumnSlots + 8) / 9
                    val leftColumnSlots = inventorySize - rightColumnSlots
                    val leftColumnRows = (leftColumnSlots + 8) / 9

                    Flex(direction = FlexDirection.ROW) {
                        // 右列（主列）
                        Column(
                            modifier = Modifier.EMPTY.width(162)  // 9 * 18
                        ) {
                            for (row in 0 until rightColumnRows) {
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

                        // 左列（向左扩展）
                        if (leftColumnSlots > 0) {
                            Column(
                                modifier = Modifier.EMPTY.width(162)  // 9 * 18
                            ) {
                                for (row in 0 until leftColumnRows) {
                                    Flex(direction = FlexDirection.ROW) {
                                        for (col in 0 until 9) {
                                            val slotIndex = rightColumnSlots + row * 9 + col
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
                } else {
                    // 单列布局
                    val rows = (inventorySize + 8) / 9
                    Column(
                        modifier = Modifier.EMPTY.width(162)  // 9 * 18
                    ) {
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

    /**
     * 根据储物箱容量计算 GUI 尺寸
     */
    private fun calculateGuiSize(): GuiSize {
        val inventorySize = handler.inventory.size()
        val useDoubleColumn = inventorySize > 45  // 钢制(63)和铱(126)使用双列

        if (useDoubleColumn) {
            // 双列布局：分别计算左右列
            val rightColumnSlots = inventorySize.coerceAtMost(81)  // 右列最多81格
            val rightColumnRows = (rightColumnSlots + 8) / 9
            val rightColumnHeight = rightColumnRows * 18 + 18

            val leftColumnSlots = inventorySize - rightColumnSlots
            val leftColumnRows = (leftColumnSlots + 8) / 9
            val leftColumnHeight = leftColumnRows * 18 + 18

            // 使用较大的高度作为总高度
            val maxColumnHeight = maxOf(rightColumnHeight, leftColumnHeight)
            val playerInventoryY = maxColumnHeight + 14
            val hotbarY = playerInventoryY + 58
            val height = hotbarY + 18 + 8

            return GuiSize(
                width = 338,  // 176 + 162
                height = height,
                playerInventoryY = playerInventoryY,
                hotbarY = hotbarY,
                storageHeight = rightColumnHeight,
                leftColumnHeight = leftColumnHeight,
                isDoubleColumn = true,
                leftColumnX = -162
            )
        } else {
            // 单列布局
            val rows = (inventorySize + 8) / 9
            val boxSlotsHeight = rows * 18 + 18
            val playerInventoryY = boxSlotsHeight + 14
            val hotbarY = playerInventoryY + 58
            val height = hotbarY + 18 + 8

            return GuiSize(
                width = 176,
                height = height,
                playerInventoryY = playerInventoryY,
                hotbarY = hotbarY,
                storageHeight = boxSlotsHeight,
                leftColumnHeight = 0,
                isDoubleColumn = false,
                leftColumnX = 0
            )
        }
    }

    /**
     * GUI 尺寸数据
     */
    private data class GuiSize(
        val width: Int,
        val height: Int,
        val playerInventoryY: Int,
        val hotbarY: Int,
        val storageHeight: Int,      // 右列（主列）高度
        val leftColumnHeight: Int,   // 左列高度
        val isDoubleColumn: Boolean,
        val leftColumnX: Int
    )

    companion object {
        private const val SLOT_SIZE = 18
    }
}
