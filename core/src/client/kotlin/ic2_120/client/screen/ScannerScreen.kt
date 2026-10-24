package ic2_120.client.screen

import ic2_120.content.item.OdScannerItem
import ic2_120.content.item.ScannerType
import ic2_120.content.network.OreScanEntry
import ic2_120.content.network.ScannerResultPacket
import ic2_120.content.screen.ScannerScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.components.Button
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

@ModScreen(handler = "scanner")
class ScannerScreen(
    val handler: ScannerScreenHandler,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<ScannerScreenHandler>(handler, playerInventory, title) {

    init {
        imageWidth = 176
        imageHeight = 231
        titleLabelY = 6
    }

    private lateinit var scanBtn: Button
    private var scrollOffset = 0.0

    override fun init() {
        super.init()

        // 背包槽位
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                val idx = ScannerScreenHandler.PLAYER_INV_START + row * 9 + col
                menu.slots[idx].x = INV_X + col * 18
                menu.slots[idx].y = INV_Y + row * 18
            }
        }
        // 快捷栏槽位
        for (col in 0 until 9) {
            val idx = ScannerScreenHandler.PLAYER_INV_START + 27 + col
            menu.slots[idx].x = INV_X + col * 18
            menu.slots[idx].y = HOTBAR_Y
        }

        scanBtn = addRenderableWidget(Button.builder(
            Component.literal("")
        ) {
            minecraft.connection?.send(
                ServerboundContainerButtonClickPacket(menu.containerId, ScannerScreenHandler.BUTTON_ID_SCAN)
            )
        }.bounds(leftPos + SCAN_BTN_X, topPos + SCAN_BTN_Y, SCAN_BTN_W, SCAN_BTN_H).build())
    }

    override fun drawBackground(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.blit(TEXTURE, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, TEXTURE_SIZE, TEXTURE_SIZE)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        val stack = menu.playerInventory.getItem(menu.playerInventory.selectedSlot)
        val type = OdScannerItem.getScannerType(stack)
        val results = lastResults

        // 标题
        val scannerTitle = if (type.tier == 3) "OV 扫描仪" else "OD 扫描仪"
        context.drawString(font, Component.literal(scannerTitle), leftPos + 8, topPos + 6, 0x404040, false)

        // 扫描按钮文字（7px）
        drawScanButtonText(context, leftPos + SCAN_BTN_X, topPos + SCAN_BTN_Y)

        val scissorLeft = leftPos + RESULTS_X
        val scissorTop = topPos + RESULTS_Y
        val scrollAreaTop = scissorTop + 22
        val scrollAreaH = RESULTS_H - 22

        if (results.isEmpty()) {
            // 空状态提示
            context.drawString(
                font, Component.literal("点击扫描按钮开始扫描"), scissorLeft, scissorTop + 4, 0x666666, false
            )
        } else {
            // 扫描结果： 固定标题，不滚动
            context.drawString(
                font, Component.literal("扫描结果："), scissorLeft, scissorTop, RESULTS_TITLE_COLOR, false
            )

            // 列头：名称（左） 数量（中），7px 亮绿色，不滚动
            drawSmallText(context, "名称", scissorLeft, scissorTop + 12, RESULTS_TITLE_COLOR)
            val qtyText = "数量"
            val qtyX = scissorLeft + (RESULTS_W - (font.width(qtyText) * 7f / font.lineHeight).toInt()) / 2
            drawSmallText(context, qtyText, qtyX, scissorTop + 12, RESULTS_TITLE_COLOR)

            // 结果行滚动区域
            context.enableScissor(scissorLeft, scrollAreaTop, scissorLeft + RESULTS_W, scrollAreaTop + scrollAreaH)

            val startY = scrollAreaTop - scrollOffset.toInt()
            for ((index, entry) in results.withIndex()) {
                val rowY = startY + index * 18
                if (rowY + 16 < scrollAreaTop) continue
                if (rowY > scrollAreaTop + scrollAreaH) break
                val oreStack = entryToItemStack(entry)
                val label = "${oreStack.name.string}"

                context.drawString(font, Component.literal(label), scissorLeft, rowY + 4, 0xFFFFFF, false)
                context.drawString(font, Component.literal("${entry.count}"), scissorLeft + RESULTS_W / 2, rowY + 4, 0xFFFFFF, false)
                if (!oreStack.isEmpty) {
                    context.renderItem(oreStack, scissorLeft + RESULTS_W - 18, rowY)
                }
            }

            context.disableScissor()
        }

        renderTooltip(context, mouseX, mouseY)
    }

    override fun removed() {
        super.removed()
        lastResults = emptyList()
        scrollOffset = 0.0
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if (lastResults.isEmpty()) return false

        val relX = mouseX - leftPos
        val relY = mouseY - topPos
        if (relX < RESULTS_X || relX > RESULTS_X + RESULTS_W ||
            relY < RESULTS_Y || relY > RESULTS_Y + RESULTS_H
        ) return false

        val contentH = lastResults.size * 18
        val scrollAreaH = RESULTS_H - 22
        val maxScroll = (contentH - scrollAreaH).coerceAtLeast(0).toDouble()
        scrollOffset = (scrollOffset - amount * 12.0).coerceIn(0.0, maxScroll)
        return true
    }

    private fun drawSmallText(context: GuiGraphics, text: String, tx: Int, ty: Int, color: Int) {
        val scale = 7f / font.lineHeight
        context.pose().pushPose()
        context.pose().translate(tx.toDouble(), ty.toDouble(), 0.0)
        context.pose().scale(scale, scale, 1f)
        context.drawString(font, text, 0, 0, color, false)
        context.pose().popPose()
    }

    private fun drawScanButtonText(context: GuiGraphics, bx: Int, by: Int) {
        val text = "扫描"
        val scale = 8f / font.lineHeight
        val tw = font.width(text)

        context.pose().pushPose()
        context.pose().translate((bx + SCAN_BTN_W / 2).toDouble(), (by + SCAN_BTN_H / 2).toDouble(), 0.0)
        context.pose().scale(scale, scale, 1f)
        context.drawString(font, text, -tw / 2, -font.lineHeight / 2, 0xFFFFFF, false)
        context.pose().popPose()
    }

    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath("ic2", "textures/gui/guitoolscanner.png")
        private const val TEXTURE_SIZE = 256

        private const val SCAN_BTN_X = 148
        private const val SCAN_BTN_Y = 5
        private const val SCAN_BTN_W = 20
        private const val SCAN_BTN_H = 10

        private const val RESULTS_X = 9
        private const val RESULTS_Y = 18
        private const val RESULTS_W = 159
        private const val RESULTS_H = 127
        private const val RESULTS_TITLE_COLOR = 0x55FF55

        private const val INV_X = 8
        private const val INV_Y = 149
        private const val HOTBAR_Y = 207

        @JvmField
        var lastResults: List<OreScanEntry> = emptyList()

        fun receiveResults(packet: ScannerResultPacket) {
            lastResults = packet.results
        }

        private fun entryToItemStack(entry: OreScanEntry): ItemStack {
            val id = ResourceLocation.tryParse(entry.blockId) ?: return ItemStack.EMPTY
            val block = BuiltInRegistries.BLOCK.getOptional(id).orElse(null) ?: return ItemStack.EMPTY
            val item = block.asItem()
            if (item == net.minecraft.world.item.Items.AIR) return ItemStack.EMPTY
            return ItemStack(item, entry.count.coerceAtLeast(1))
        }
    }
}
