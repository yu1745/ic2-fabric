package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.nuclear.ReactorFluidPortBlock
import ic2_120.content.screen.ReactorFluidPortScreenHandler
import ic2_120.content.screen.GuiSize
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text as McText

/**
 * 反应堆流体接口：升级槽 + 玩家栏，布局与标准 [GuiSize.STANDARD] 机器一致。
 */
@ModScreen(block = ReactorFluidPortBlock::class)
class ReactorFluidPortScreen(
    handler: ReactorFluidPortScreenHandler,
    playerInventory: PlayerInventory,
    title: McText
) : HandledScreen<ReactorFluidPortScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()
    private val gui = GuiSize.STANDARD

    init {
        backgroundWidth = gui.width
        backgroundHeight = gui.height
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context,
            x,
            y,
            gui.playerInvY,
            gui.hotbarY,
            GuiSize.SLOT_SIZE
        )
        val inset = GuiBackground.SLOT_ANCHOR_INSET
        val slot = handler.slots[ReactorFluidPortScreenHandler.UPGRADE_SLOT_INDEX]
        GuiBackground.drawVanillaLikeSlot(
            context,
            x + slot.x - inset,
            y + slot.y - inset,
            GuiSize.SLOT_SIZE,
            GuiSize.SLOT_SIZE
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y
        val content: UiScope.() -> Unit = {
            Column(
                x = left + 8,
                y = top + 8,
                spacing = 6,
                modifier = Modifier().width(gui.contentWidth)
            ) {
                Text(title.string, color = 0xFFFFFF)
                Text("流体接口", color = 0xAAAAAA, shadow = false)
                Flex(justifyContent = JustifyContent.CENTER, alignItems = AlignItems.CENTER) {
                    SlotAnchor(
                        id = slotAnchorId(ReactorFluidPortScreenHandler.UPGRADE_SLOT_INDEX),
                        width = GuiSize.SLOT_SIZE,
                        height = GuiSize.SLOT_SIZE
                    )
                }
            }
            playerInventoryAndHotbarSlotAnchors(
                left = left,
                top = top,
                playerInvStart = ReactorFluidPortScreenHandler.PLAYER_INV_START,
                playerInvY = gui.playerInvY,
                hotbarY = gui.hotbarY
            )
        }

        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)
        applyAnchoredSlots(layout, left, top)

        super.render(context, mouseX, mouseY, delta)
        ui.render(context, textRenderer, mouseX, mouseY, content = content)
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    private fun applyAnchoredSlots(layout: ComposeUI.LayoutSnapshot, left: Int, top: Int) {
        handler.slots.forEachIndexed { index, slot ->
            val anchor = layout.anchors[slotAnchorId(index)] ?: return@forEachIndexed
            slot.x = anchor.x - left
            slot.y = anchor.y - top
        }
    }

    private fun slotAnchorId(slotIndex: Int): String = "slot.$slotIndex"
}
