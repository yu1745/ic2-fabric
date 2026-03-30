package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.PatternStorageBlock
import ic2_120.content.block.machines.PatternStorageBlockEntity
import ic2_120.content.screen.PatternStorageScreenHandler
import ic2_120.content.screen.GuiSize
import ic2_120.content.uu.UuTemplateEntry
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = PatternStorageBlock::class)
class PatternStorageScreen(
    handler: PatternStorageScreenHandler, playerInventory: net.minecraft.entity.player.PlayerInventory, title: Text
) : HandledScreen<PatternStorageScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    private val guiSize = GuiSize.STANDARD_TALL

    init {
        backgroundWidth = guiSize.width
        backgroundHeight = guiSize.height
        titleY = 4
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y, guiSize.playerInvY, guiSize.hotbarY, GuiSize.SLOT_SIZE
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y
        val world = client?.world ?: client?.player?.world
        val storage = world?.let(handler::getPatternStorage)
        val templates = storage?.getTemplatesSnapshot().orEmpty()
        val selectedIndex = storage?.selectedTemplateIndex ?: -1

        val content: UiScope.() -> Unit = {
            Flex(
                x = left + 8,
                y = top + 8,
                direction = FlexDirection.ROW,
                gap = 8,
                modifier = Modifier.EMPTY.width(guiSize.contentWidth).height(guiSize.contentHeight)
            ) {
                // 左侧：操作区
                Column(
                    spacing = 4, modifier = Modifier.EMPTY.width(LEFT_WIDTH).fractionWidth(1.0f)
                ) {
                    Text(title.string, color = 0xFFFFFF)
                    // 水晶槽
                    SlotAnchor(
                        id = slotAnchorId(PatternStorageScreenHandler.SLOT_CRYSTAL_INDEX), width = 18, height = 18
                    )
                    // 按钮
                    Button(
                        text = "写入水晶", modifier = Modifier.EMPTY.width(70)
                    ) {
                        client?.player?.networkHandler?.sendPacket(
                            ButtonClickC2SPacket(handler.syncId, PatternStorageScreenHandler.BUTTON_EXPORT_TO_CRYSTAL)
                        )
                    }
                    Button(
                        text = "导入水晶", modifier = Modifier.EMPTY.width(70)
                    ) {
                        client?.player?.networkHandler?.sendPacket(
                            ButtonClickC2SPacket(handler.syncId, PatternStorageScreenHandler.BUTTON_IMPORT_FROM_CRYSTAL)
                        )
                    }

                    Flex(
                        alignItems = AlignItems.CENTER,
                    ) {
                        // 当前选中模板
                        val selected = templates.getOrNull(selectedIndex)
                        if (selected != null) {
                            val stack = templateToStack(selected)
                            if (!stack.isEmpty) {
                                ItemStack(stack, size = 18)
                            }
                            Text(selected.displayName().string, color = 0xFFFFFF, shadow = false, center = true)
                            Text("${selected.uuCostUb} uB", color = 0xFFAA33, shadow = false, center = true)
                        } else {
                            Text("<空>", color = 0x666666, shadow = false, center = true)
                        }
                    }
                }

                // 右侧：模板列表
                Flex(
                    direction = FlexDirection.COLUMN,
                    modifier = Modifier.EMPTY.fractionWidth(1.0f).height(guiSize.contentHeight),
                    gap = 2,
                ) {
                    Text("共 ${templates.size} 个", color = 0x666666, shadow = false)

                    ScrollView(
                        scrollbarWidth = 8, modifier = Modifier.EMPTY.fractionHeight(1.0f)
                    ) {
                        Column(spacing = 2) {
                            if (templates.isEmpty()) {
                                Text("暂无模板", color = 0x666666, shadow = false)
                            } else {
                                templates.forEachIndexed { index, template ->
                                    Row(spacing = 0, modifier = Modifier.EMPTY.fractionWidth(1.0f)) {
                                        val stack = templateToStack(template)
                                        if (!stack.isEmpty) {
                                            ItemStack(stack, size = 18)
                                        }
                                        Button(
                                            text = templateLine(index, selectedIndex, template),
                                            modifier = Modifier.EMPTY.fractionWidth(1.0f),
                                            tooltip = listOf(
                                                template.displayName().copy(), Text.literal("${template.uuCostUb} uB")
                                            ),
                                            onClick = {
                                                client?.player?.networkHandler?.sendPacket(
                                                    ButtonClickC2SPacket(
                                                        handler.syncId,
                                                        PatternStorageScreenHandler.BUTTON_SELECT_BASE + index
                                                    )
                                                )
                                            })
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
                playerInvStart = PatternStorageScreenHandler.PLAYER_INV_START,
                playerInvY = guiSize.playerInvY,
                hotbarY = guiSize.hotbarY
            )
        }

        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)
        applyAnchoredSlots(layout, left, top)

        super.render(context, mouseX, mouseY, delta)
        ui.render(context, textRenderer, mouseX, mouseY, content = content)
        val tooltip = ui.getTooltipAt(mouseX, mouseY)
        if (!tooltip.isNullOrEmpty()) {
            context.drawTooltip(textRenderer, tooltip, mouseX, mouseY)
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean =
        ui.mouseScrolled(mouseX, mouseY, 0.0, amount) || super.mouseScrolled(mouseX, mouseY, amount)

    override fun mouseDragged(
        mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double
    ): Boolean = ui.mouseDragged(mouseX, mouseY, button) || super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        ui.stopDrag()
        return super.mouseReleased(mouseX, mouseY, button)
    }

    private fun slotAnchorId(slotIndex: Int): String = "slot.$slotIndex"

    private fun applyAnchoredSlots(layout: ComposeUI.LayoutSnapshot, left: Int, top: Int) {
        handler.slots.forEachIndexed { index, slot ->
            val anchor = layout.anchors[slotAnchorId(index)] ?: return@forEachIndexed
            slot.x = anchor.x - left
            slot.y = anchor.y - top
        }
    }

    private fun templateLine(index: Int, selectedIndex: Int, template: UuTemplateEntry): String {
        return "${template.displayName().string} (${template.uuCostUb} uB)"
    }

    private fun templateToStack(template: UuTemplateEntry): ItemStack {
        val id = Identifier.tryParse(template.itemId) ?: return ItemStack.EMPTY
        val item = Registries.ITEM.getOrEmpty(id).orElse(null) ?: return ItemStack.EMPTY
        return if (item == net.minecraft.item.Items.AIR) ItemStack.EMPTY else ItemStack(item)
    }

    companion object {
        private const val LEFT_WIDTH = 76
    }
}
