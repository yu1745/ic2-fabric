package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.content.item.CropSeedData
import ic2_120.content.screen.CropnalyzerScreenHandler
import ic2_120.content.screen.GuiSize
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket
import net.minecraft.text.Text as McText

@ModScreen(handler = "cropnalyzer")
class CropnalyzerScreen(
    handler: CropnalyzerScreenHandler,
    playerInventory: PlayerInventory,
    title: McText
) : HandledScreen<CropnalyzerScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()
    private val gui = GuiSize.STANDARD

    init {
        backgroundWidth = gui.width
        backgroundHeight = gui.height
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
        val inset = GuiBackground.SLOT_ANCHOR_INSET
        GuiBackground.drawVanillaLikeSlot(
            context,
            x + handler.slots[CropnalyzerScreenHandler.SLOT_INDEX_SEED].x - inset,
            y + handler.slots[CropnalyzerScreenHandler.SLOT_INDEX_SEED].y - inset,
            GuiSize.SLOT_SIZE,
            GuiSize.SLOT_SIZE
        )
        GuiBackground.drawPlayerInventorySlotBorders(
            context,
            x,
            y,
            gui.playerInvY,
            gui.hotbarY,
            GuiSize.SLOT_SIZE
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y
        val energy = handler.energy.toLong().coerceAtLeast(0)
        val cap = handler.energyCapacity.toLong().coerceAtLeast(1)
        val fraction = (energy.toFloat() / cap.toFloat()).coerceIn(0f, 1f)
        val seed = handler.slots[CropnalyzerScreenHandler.SLOT_INDEX_SEED].stack
        val scanLevel = CropSeedData.readScanLevel(seed)
        val type = CropSeedData.readType(seed)?.let { CropSeedData.displayName(it).string } ?: "未知"
        val canScan = energy >= 50L && !seed.isEmpty

        val content: UiScope.() -> Unit = {
            val seedSlot = handler.slots[CropnalyzerScreenHandler.SLOT_INDEX_SEED]
            SlotAnchor(
                id = "slot.${CropnalyzerScreenHandler.SLOT_INDEX_SEED}",
                x = left + seedSlot.x,
                y = top + seedSlot.y,
                width = GuiSize.SLOT_SIZE,
                height = GuiSize.SLOT_SIZE,
                absolute = true
            )
            playerInventoryAndHotbarSlotAnchors(
                left = left,
                top = top,
                playerInvStart = CropnalyzerScreenHandler.PLAYER_INV_START,
                playerInvY = gui.playerInvY,
                hotbarY = gui.hotbarY
            )
            Column(
                x = left + 8,
                y = top + 6,
                spacing = 5,
                modifier = Modifier().width(gui.contentWidth)
            ) {
                Text("种子扫描仪", color = 0xFFFFFF)
                Flex(alignItems = AlignItems.CENTER, gap = 4) {
                    Text("$energy / $cap EU", color = 0xCCCCCC, shadow = false)
                    EnergyBar(fraction, barHeight = 8, modifier = Modifier().fractionWidth(1f))
                }
                Text("放入种子袋后点击扫描", color = 0xAAAAAA, shadow = false)
                Text("作物: $type", color = 0xFFFFFF, shadow = false)
                Text("扫描等级: $scanLevel/4", color = 0xFFFFFF, shadow = false)
                Button(
                    text = if (canScan) "扫描" else "无法扫描",
                    modifier = Modifier().width(90),
                    onClick = {
                        client?.player?.networkHandler?.sendPacket(
                            ButtonClickC2SPacket(handler.syncId, CropnalyzerScreenHandler.BUTTON_ID_SCAN)
                        )
                    }
                )
            }
        }

        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)
        handler.slots.forEachIndexed { index, slot ->
            val anchor = layout.anchors["slot.$index"] ?: return@forEachIndexed
            slot.x = anchor.x - left
            slot.y = anchor.y - top
        }

        super.render(context, mouseX, mouseY, delta)
        ui.render(context, textRenderer, mouseX, mouseY, content = content)

        val tooltip = ui.getTooltipAt(mouseX, mouseY)
        if (tooltip != null) {
            context.drawTooltip(textRenderer, tooltip, mouseX, mouseY)
        } else {
            drawMouseoverTooltip(context, mouseX, mouseY)
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)
}
