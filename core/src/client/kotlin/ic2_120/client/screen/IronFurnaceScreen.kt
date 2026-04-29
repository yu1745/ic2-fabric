package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.t
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.IronFurnaceBlock
import ic2_120.content.block.machines.IronFurnaceBlockEntity
import ic2_120.content.screen.IronFurnaceScreenHandler
import ic2_120.content.screen.GuiSize
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
        backgroundWidth = GUI_SIZE.width
        backgroundHeight = GUI_SIZE.height
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y,
            GUI_SIZE.playerInvY,
            GUI_SIZE.hotbarY,
            GuiSize.SLOT_SIZE
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y
        val cookFrac = if (IronFurnaceSync.COOK_TIME_MAX > 0) {
            (handler.sync.cookTime.coerceIn(0, IronFurnaceSync.COOK_TIME_MAX).toFloat() / IronFurnaceSync.COOK_TIME_MAX)
                .coerceIn(0f, 1f)
        } else 0f
        val totalBurn = handler.sync.totalBurnTime.coerceAtLeast(1)
        val burnFrac = (handler.sync.burnTime.coerceIn(0, totalBurn).toFloat() / totalBurn).coerceIn(0f, 1f)
        val burnPercent = (burnFrac * 100f).toInt()

        val content: UiScope.() -> Unit = {
            Column(
                x = left + 8,
                y = top + 8,
                spacing = 6,
                modifier = Modifier.EMPTY.width(GUI_SIZE.contentWidth)
            ) {
                Flex(direction = FlexDirection.ROW, alignItems = AlignItems.CENTER, gap = 8) {
                    Text(title.string, color = 0xFFFFFF)
                    Text(t("gui.ic2_120.iron_furnace.burning", burnPercent), color = 0xFFFFFF, shadow = false)
                }
                EnergyBar(burnFrac, barHeight = 12)

                Flex(
                    direction = FlexDirection.ROW,
                    alignItems = AlignItems.CENTER,
                    gap = 4
                ) {
                    SlotHost(IronFurnaceBlockEntity.SLOT_INPUT)
                    EnergyBar(cookFrac, modifier = Modifier.EMPTY.fractionWidth(1.0f))
                    SlotHost(IronFurnaceBlockEntity.SLOT_OUTPUT)
                }
                Flex(direction = FlexDirection.ROW, alignItems = AlignItems.CENTER, gap = 4) {
                    SlotHost(IronFurnaceBlockEntity.SLOT_FUEL)
                    Text(t("gui.ic2_120.iron_furnace.fuel_slot"), color = 0xAAAAAA, shadow = false)
                }
            }

            playerInventoryAndHotbarSlotAnchors(
                left = left,
                top = top,
                playerInvStart = IronFurnaceScreenHandler.PLAYER_INV_START,
                playerInvY = GUI_SIZE.playerInvY,
                hotbarY = GUI_SIZE.hotbarY
            )
        }

        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)
        applyAnchoredSlots(layout, left, top)

        super.render(context, mouseX, mouseY, delta)
        ui.render(context, textRenderer, mouseX, mouseY, content = content)

        // 左侧显示累积经验值
        val xpRaw = handler.sync.experienceDisplay
        val xpWhole = xpRaw / 10
        val xpFrac = xpRaw % 10
        val xpText = if (xpWhole > 0) "XP $xpWhole.$xpFrac" else "XP 0.0"
        val xpX = left - textRenderer.getWidth(xpText) - 4
        context.drawText(textRenderer, xpText, xpX, top + 8, 0xA5FE74, false)

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun UiScope.SlotHost(slotIndex: Int) {
        SlotAnchor(
            id = slotAnchorId(slotIndex),
            width = IronFurnaceScreenHandler.SLOT_SIZE,
            height = IronFurnaceScreenHandler.SLOT_SIZE
        )
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

    companion object {
        private val GUI_SIZE = GuiSize.STANDARD
    }
}
