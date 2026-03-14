package ic2_120.client

import ic2_120.client.compose.*
import ic2_120.client.ui.GuiBackground
import ic2_120.client.ui.ProgressBar
import ic2_120.content.block.IronFurnaceBlock
import ic2_120.content.block.machines.IronFurnaceBlockEntity
import ic2_120.content.screen.IronFurnaceScreenHandler
import ic2_120.content.sync.IronFurnaceSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

@ModScreen(block = IronFurnaceBlock::class)
class IronFurnaceScreen(
    handler: IronFurnaceScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<IronFurnaceScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = PANEL_WIDTH
        backgroundHeight = PANEL_HEIGHT
        titleY = -1000
        playerInventoryTitleY = -1000
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.draw(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y,
            IronFurnaceScreenHandler.PLAYER_INV_Y,
            IronFurnaceScreenHandler.HOTBAR_Y,
            IronFurnaceScreenHandler.SLOT_SIZE
        )
        val borderColor = GuiBackground.BORDER_COLOR
        val slotSize = IronFurnaceScreenHandler.SLOT_SIZE
        val borderOffset = 1

        // 输入槽边框
        val inputSlot = handler.slots[IronFurnaceBlockEntity.SLOT_INPUT]
        context.drawBorder(x + inputSlot.x - borderOffset, y + inputSlot.y - borderOffset, slotSize, slotSize, borderColor)

        // 燃料槽边框
        val fuelSlot = handler.slots[IronFurnaceBlockEntity.SLOT_FUEL]
        context.drawBorder(x + fuelSlot.x - borderOffset, y + fuelSlot.y - borderOffset, slotSize, slotSize, borderColor)

        // 输出槽边框
        val outputSlot = handler.slots[IronFurnaceBlockEntity.SLOT_OUTPUT]
        context.drawBorder(x + outputSlot.x - borderOffset, y + outputSlot.y - borderOffset, slotSize, slotSize, borderColor)

        // 燃烧进度条：竖向渐变（红→蓝），表示燃料逐渐用尽
        val totalBurn = handler.sync.totalBurnTime.coerceAtLeast(1)
        val burnTime = handler.sync.burnTime.coerceIn(0, totalBurn)
        val burnFrac = (burnTime.toFloat() / totalBurn).coerceIn(0f, 1f)
        val fuelBarX = x + fuelSlot.x - 6
        val fuelBarW = 4  // 竖向条宽度
        val fuelBarH = slotSize  // 高度与槽位相同
        val fuelBarY = y + fuelSlot.y
        ProgressBar.drawVerticalFuelBar(context, fuelBarX, fuelBarY, fuelBarW, fuelBarH, burnFrac)

        // 烧制进度条：横向进度条（与电炉类似）
        val cookTime = handler.sync.cookTime.coerceIn(0, IronFurnaceSync.COOK_TIME_MAX)
        val cookFrac = (cookTime.toFloat() / IronFurnaceSync.COOK_TIME_MAX).coerceIn(0f, 1f)
        val cookBarX = x + inputSlot.x + slotSize + 10
        val cookBarW = (outputSlot.x - (inputSlot.x + slotSize) - 12).coerceAtLeast(8)
        val cookBarH = 8
        val cookBarY = y + outputSlot.y + (slotSize - cookBarH) / 2
        ProgressBar.draw(context, cookBarX, cookBarY, cookBarW, cookBarH, cookFrac)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        val left = x
        val top = y
        val contentW = (backgroundWidth - 16).coerceAtLeast(0)
        ui.render(context, textRenderer, mouseX, mouseY) {
            Column(x = left + 8, y = top + 8, spacing = 6, absolute = true) {
                Flex(
                    direction = FlexDirection.ROW,
                    alignItems = AlignItems.CENTER,
                    modifier = Modifier.EMPTY.width(contentW)
                ) {
                    Text(title.string, color = 0xFFFFFF, shadow = false)
                }
                Flex(
                    direction = FlexDirection.ROW,
                    alignItems = AlignItems.CENTER,
                    modifier = Modifier.EMPTY.width(contentW)
                ) {
                    Text("铁炉", color = 0xAAAAAA, shadow = false)
                }
            }
        }
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    companion object {
        private const val PANEL_WIDTH = 176
        private const val PANEL_HEIGHT = 166
    }
}
