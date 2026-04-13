package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.FluidBar
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.AnimalmatronBlock
import ic2_120.content.screen.AnimalmatronScreenHandler
import ic2_120.content.screen.GuiSize
import ic2_120.content.sync.AnimalmatronSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

@ModScreen(block = AnimalmatronBlock::class)
class AnimalmatronScreen(
    handler: AnimalmatronScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<AnimalmatronScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = GuiSize.STANDARD_UPGRADE.width
        backgroundHeight = GuiSize.STANDARD_UPGRADE.height
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context,
            x,
            y,
            GuiSize.STANDARD_UPGRADE.playerInvY,
            GuiSize.STANDARD_UPGRADE.hotbarY,
            GuiSize.SLOT_SIZE
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y

        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val capacity = handler.sync.energyCapacity.toLong().coerceAtLeast(1L)
        val energyFraction = (energy.toFloat() / capacity).coerceIn(0f, 1f)

        val waterFraction = (handler.sync.waterAmountMb.toFloat() / AnimalmatronSync.WATER_TANK_CAPACITY_MB).coerceIn(0f, 1f)
        val weedExFraction = (handler.sync.weedExAmountMb.toFloat() / AnimalmatronSync.WEED_EX_TANK_CAPACITY_MB).coerceIn(0f, 1f)

        val inputRateText = "输入 ${EnergyFormatUtils.formatEu(handler.sync.getSyncedInsertedAmount())} EU/t"
        val consumeRateText = "耗能 ${EnergyFormatUtils.formatEu(handler.sync.getSyncedConsumedAmount())} EU/t"

        val content: UiScope.() -> Unit = {
            Row(
                x = left + 8,
                y = top + 4,
                spacing = 8,
                modifier = Modifier.EMPTY.width(GuiSize.STANDARD_UPGRADE.contentWidth)
            ) {
                Column(spacing = 4, modifier = Modifier.EMPTY.width(GuiSize.STANDARD.contentWidth)) {
                    Flex(direction = FlexDirection.ROW, alignItems = AlignItems.CENTER, gap = 4) {
                        Text(title.string, color = 0xFFFFFF)
                        EnergyBar(energyFraction, barHeight = 8, modifier = Modifier.EMPTY.fractionWidth(1f))
                        Text("$energy / $capacity EU", color = 0xFFFFFF, shadow = false)
                    }

                    Flex(direction = FlexDirection.ROW, justifyContent = JustifyContent.SPACE_BETWEEN, alignItems = AlignItems.END) {
                        Column(spacing = 2) {
                            Text("水", color = 0xAAAAAA, shadow = false)
                            SlotAnchor(id = slotAnchorId(AnimalmatronScreenHandler.SLOT_WATER_INPUT_INDEX))
                            SlotAnchor(id = slotAnchorId(AnimalmatronScreenHandler.SLOT_WATER_OUTPUT_INDEX))
                        }
                        Column(spacing = 2) {
                            FluidBar(
                                waterFraction,
                                barWidth = 10,
                                barHeight = 40,
                                vertical = true,
                                modifier = Modifier.EMPTY.width(10).height(40)
                            )
                            Text("${handler.sync.waterAmountMb} mB", color = 0xFFFFFF, shadow = false)
                        }
                        Column(spacing = 2) {
                            FluidBar(
                                weedExFraction,
                                barWidth = 10,
                                barHeight = 40,
                                vertical = true,
                                modifier = Modifier.EMPTY.width(10).height(40)
                            )
                            Text("${handler.sync.weedExAmountMb} mB", color = 0xFFFFFF, shadow = false)
                        }
                        Column(spacing = 2) {
                            Text("Weed-EX", color = 0xAAAAAA, shadow = false)
                            SlotAnchor(id = slotAnchorId(AnimalmatronScreenHandler.SLOT_WEED_EX_INPUT_INDEX))
                            SlotAnchor(id = slotAnchorId(AnimalmatronScreenHandler.SLOT_WEED_EX_OUTPUT_INDEX))
                        }
                    }

                    Row(spacing = 2) {
                        SlotAnchor(id = slotAnchorId(AnimalmatronScreenHandler.SLOT_DISCHARGING_INDEX))
                        for (i in AnimalmatronScreenHandler.SLOT_FEED_INDEX_START..AnimalmatronScreenHandler.SLOT_FEED_INDEX_END) {
                            SlotAnchor(id = slotAnchorId(i))
                        }
                    }
                }

                Column(
                    spacing = 4,
                    modifier = Modifier.EMPTY.width(GuiSize.UPGRADE_COLUMN_WIDTH).padding(0, 8, 0, 0)
                ) {
                    for (slotIndex in AnimalmatronScreenHandler.SLOT_UPGRADE_INDEX_START..AnimalmatronScreenHandler.SLOT_UPGRADE_INDEX_END) {
                        SlotAnchor(id = slotAnchorId(slotIndex), width = AnimalmatronScreenHandler.SLOT_SIZE, height = AnimalmatronScreenHandler.SLOT_SIZE)
                    }
                }
            }

            playerInventoryAndHotbarSlotAnchors(
                left = left,
                top = top,
                playerInvStart = AnimalmatronScreenHandler.PLAYER_INV_START,
                playerInvY = GuiSize.STANDARD_UPGRADE.playerInvY,
                hotbarY = GuiSize.STANDARD_UPGRADE.hotbarY
            )
        }

        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)
        handler.slots.forEachIndexed { index, slot ->
            val anchor = layout.anchors[slotAnchorId(index)] ?: return@forEachIndexed
            slot.x = anchor.x - left
            slot.y = anchor.y - top
        }

        super.render(context, mouseX, mouseY, delta)
        ui.render(context, textRenderer, mouseX, mouseY, content = content)

        val animalCountText = "监管动物 ${handler.sync.animalCount} 只"
        val sideTextWidth = maxOf(textRenderer.getWidth(inputRateText), textRenderer.getWidth(consumeRateText), textRenderer.getWidth(animalCountText))
        val sideX = left - sideTextWidth - 4
        context.drawText(textRenderer, animalCountText, sideX, top + 8, 0x55FF55, false)
        context.drawText(textRenderer, inputRateText, sideX, top + 20, 0xAAAAAA, false)
        context.drawText(textRenderer, consumeRateText, sideX, top + 32, 0xAAAAAA, false)

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    private fun slotAnchorId(slotIndex: Int): String = "slot.$slotIndex"
}
