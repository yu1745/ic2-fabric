package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.ui.GuiBackground
import ic2_120.client.t
import ic2_120.content.screen.FluidUpgradeScreenHandler
import ic2_120.content.screen.GuiSize
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.math.Direction

@ModScreen(handler = "fluid_upgrade")
class FluidUpgradeScreen(
    handler: FluidUpgradeScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<FluidUpgradeScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()
    private val gui = GuiSize.STANDARD

    init {
        backgroundWidth = gui.width
        backgroundHeight = gui.height
        titleY = 6
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y
        val client = client!!

        // 当前状态
        val dirOrdinal = handler.directionOrdinal
        val dirName = if (dirOrdinal in 0..5) {
            val dir = Direction.entries[dirOrdinal]
            t("gui.ic2_120.direction.${dir.name.lowercase()}")
        } else {
            t("gui.ic2_120.fluid_upgrade.any_direction")
        }
        val filterName = if (handler.fluidRawId > 0) {
            val fluid = Registries.FLUID.get(handler.fluidRawId)
            val block = fluid.defaultState.blockState.block
            if (block != null && block.name.string.isNotBlank()) {
                block.name.string
            } else {
                Registries.FLUID.getId(fluid).path
            }
        } else {
            t("gui.ic2_120.fluid_upgrade.no_filter")
        }

        val content: UiScope.() -> Unit = {
            // 第1行：方向显示 + 切换按钮
            Flex(
                x = left + 8,
                y = top + 18,
                alignItems = AlignItems.CENTER,
                gap = 8
            ) {
                Text(
                    t("gui.ic2_120.fluid_upgrade.direction", dirName),
                    color = 0xFFFFFF
                )
                Button(
                    text = t("gui.ic2_120.fluid_upgrade.cycle_direction_short"),
                    modifier = Modifier().width(50),
                    onClick = {
                        client.networkHandler?.sendPacket(
                            ButtonClickC2SPacket(handler.syncId, FluidUpgradeScreenHandler.BUTTON_CYCLE_DIRECTION)
                        )
                    }
                )
            }

            // 第2行：槽位 + 两个过滤按钮
            Flex(
                x = left + 8,
                y = top + 38,
                alignItems = AlignItems.CENTER,
                gap = 4
            ) {
                SlotAnchor(
                    id = slotAnchorId(FluidUpgradeScreenHandler.SLOT_CONTAINER),
                    width = GuiSize.SLOT_SIZE,
                    height = GuiSize.SLOT_SIZE
                )
                Button(
                    text = t("gui.ic2_120.fluid_upgrade.set_filter"),
                    modifier = Modifier().width(56),
                    onClick = {
                        client.networkHandler?.sendPacket(
                            ButtonClickC2SPacket(handler.syncId, FluidUpgradeScreenHandler.BUTTON_SET_FILTER)
                        )
                    }
                )
                Button(
                    text = t("gui.ic2_120.fluid_upgrade.clear_filter"),
                    modifier = Modifier().width(56),
                    onClick = {
                        client.networkHandler?.sendPacket(
                            ButtonClickC2SPacket(handler.syncId, FluidUpgradeScreenHandler.BUTTON_CLEAR_FILTER)
                        )
                    }
                )
            }

            // 第3行：当前过滤显示
            Text(
                x = left + 8,
                y = top + 62,
                text = t("gui.ic2_120.fluid_upgrade.filter_display", filterName),
                color = if (handler.fluidRawId > 0) 0x55FF55 else 0xAAAAAA,
                shadow = false
            )

            // 玩家物品栏
            playerInventoryAndHotbarSlotAnchors(
                left = left,
                top = top,
                playerInvStart = FluidUpgradeScreenHandler.PLAYER_INV_START,
                playerInvY = gui.playerInvY,
                hotbarY = gui.hotbarY
            )
        }

        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)
        applyAnchoredSlots(layout, left, top)

        GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
        val inset = GuiBackground.SLOT_ANCHOR_INSET
        val slotSize = GuiSize.SLOT_SIZE
        // 绘制容器槽位背景
        val slot = handler.slots[FluidUpgradeScreenHandler.SLOT_CONTAINER]
        GuiBackground.drawVanillaLikeSlot(
            context,
            x + slot.x - inset,
            y + slot.y - inset,
            slotSize,
            slotSize
        )
        // 绘制玩家物品栏边框
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y, gui.playerInvY, gui.hotbarY, slotSize
        )

        ui.render(context, textRenderer, mouseX, mouseY, content = content)
        super.render(context, mouseX, mouseY, delta)
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
