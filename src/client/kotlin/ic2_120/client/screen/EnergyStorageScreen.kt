package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.storage.EnergyStorageConfig
import ic2_120.content.screen.EnergyStorageScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.registry.Registries
import net.minecraft.screen.slot.Slot
import net.minecraft.text.Text

/**
 * 储电盒 GUI。四个等级（BatBox/CESU/MFE/MFSU）共用。
 */
@ModScreen(handlers = ["batbox", "cesu", "mfe", "mfsu"])
class EnergyStorageScreen(
    handler: EnergyStorageScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<EnergyStorageScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()
    private val capacity: Long = resolveCapacity()
    private val useEquipmentSlots: Boolean = resolveUseEquipmentSlots()

    private fun resolveCapacity(): Long {
        return handler.context.get({ world, pos ->
            val block = world.getBlockState(pos).block
            val id = Registries.BLOCK.getId(block)
            EnergyStorageConfig.fromBlockPath(id.path)?.capacity ?: EnergyStorageConfig.BATBOX.capacity
        }, EnergyStorageConfig.BATBOX.capacity)
    }

    private fun resolveUseEquipmentSlots(): Boolean {
        return handler.context.get({ world, pos ->
            val block = world.getBlockState(pos).block
            val id = Registries.BLOCK.getId(block)
            EnergyStorageConfig.fromBlockPath(id.path)?.useEquipmentSlots ?: false
        }, false)
    }

    init {
        backgroundWidth = GUI_SIZE.width
        backgroundHeight = GUI_SIZE.height
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y,
            EnergyStorageScreenHandler.PLAYER_INV_Y,
            EnergyStorageScreenHandler.HOTBAR_Y,
            EnergyStorageScreenHandler.SLOT_SIZE
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val outputRate = handler.sync.getSyncedExtractedAmount()
        val cap = capacity
        val fraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f

        val inputText = "输入 ${formatEu(inputRate)} EU/t"
        val outputText = "输出 ${formatEu(outputRate)} EU/t"
        val sideTextWidth = maxOf(textRenderer.getWidth(inputText), textRenderer.getWidth(outputText))
        val sideTextX = left - sideTextWidth - 4

        val equipLabel = Text.translatable("ic2_120.gui.equipment_slots")
        val chargeLabel = Text.translatable("ic2_120.gui.charge_slots")

        val content: UiScope.() -> Unit = {
            Column(
                x = left + 8,
                y = top + 8,
                spacing = 6,
                modifier = Modifier.EMPTY.width(GUI_SIZE.contentWidth)
            ) {
                Flex(direction = FlexDirection.ROW, alignItems = AlignItems.CENTER, gap = 8) {
                    Text(title.string, color = 0xFFFFFF)
                    Text(
                        "${formatEu(energy)} / ${formatEu(cap)} EU",
                        color = 0xFFFFFF,
                        shadow = false
                    )
                }
                EnergyBar(fraction, barHeight = 12)

                if (useEquipmentSlots) {
                    Column(spacing = 4) {
                        Flex(
                            direction = FlexDirection.ROW,
                            alignItems = AlignItems.CENTER,
                            gap = 4
                        ) {
                            Text(equipLabel.string, color = 0xAAAAAA, shadow = false)
                            // 装备槽（左侧 4 格）
                            for (i in 0 until 4) {
                                SlotAnchor(
                                    id = slotAnchorId(i),
                                    width = 18,
                                    height = 18
                                )
                            }
                        }
                        Flex(
                            direction = FlexDirection.ROW,
                            alignItems = AlignItems.CENTER,
                            gap = 4
                        ) {
                            Text(chargeLabel.string, color = 0xAAAAAA, shadow = false)
                            // 充电槽（右侧 1 格）
                            SlotAnchor(
                                id = slotAnchorId(4),
                                width = 18,
                                height = 18
                            )
                        }
                    }
                }
            }
        }

        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)
        applyAnchoredSlots(layout, left, top)

        super.render(context, mouseX, mouseY, delta)
        ui.render(context, textRenderer, mouseX, mouseY, content = content)
        context.drawText(textRenderer, inputText, sideTextX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, outputText, sideTextX, top + 20, 0xAAAAAA, false)
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

    private fun formatEu(value: Long): String {
        return when {
            value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
            value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
            else -> value.toString()
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    companion object {
        private val GUI_SIZE = GuiSize.STANDARD
    }
}
