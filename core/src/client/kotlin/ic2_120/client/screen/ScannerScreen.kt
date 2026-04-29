package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.t
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.content.item.OdScannerItem
import ic2_120.content.network.OreScanEntry
import ic2_120.content.network.ScannerResultPacket
import ic2_120.content.screen.ScannerScreenHandler
import ic2_120.content.screen.GuiSize
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Identifier

/**
 * 扫描仪 GUI（客户端）。
 *
 * 上部：能量条 + 使用次数 + 扫描按钮
 * 下部：扫描结果列表（矿物名称 + 数量，可滚动）
 */
@ModScreen(handler = "scanner")
class ScannerScreen(
    handler: ScannerScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<ScannerScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()
    private val gui = GuiSize.LARGE

    init {
        backgroundWidth = gui.width
        backgroundHeight = gui.height
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.draw(context, x, y, gui.width, gui.height)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1)
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val type = OdScannerItem.getScannerType(handler.playerInventory.getStack(handler.playerInventory.selectedSlot))
        val usesRemaining = handler.sync.usesRemaining
        val maxUses = handler.sync.maxUses
        val scannerTitle = if (type.tier == 3) "OV 扫描仪" else "OD 扫描仪"
        val scanRangeText = "${type.scanRadius * 2 + 1} × ${type.scanRadius * 2 + 1}"
        val canScan = energy >= type.energyPerScan && usesRemaining > 0
        val results = lastResults

        super.render(context, mouseX, mouseY, delta)

        ui.render(context, textRenderer, mouseX, mouseY) {
            buildUi(
                x = x + 8,
                y = y + 6,
                energy = energy,
                cap = cap,
                energyFraction = energyFraction,
                scannerTitle = scannerTitle,
                scanRangeText = scanRangeText,
                canScan = canScan,
                usesRemaining = usesRemaining,
                maxUses = maxUses,
                results = results
            )
        }

        val tooltip = ui.getTooltipAt(mouseX, mouseY)
        if (tooltip != null) {
            context.drawTooltip(textRenderer, tooltip, mouseX, mouseY)
        } else {
            drawMouseoverTooltip(context, mouseX, mouseY)
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        if (ui.mouseClicked(mouseX, mouseY, button)) true
        else super.mouseClicked(mouseX, mouseY, button)

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, amount: Double): Boolean =
        ui.mouseScrolled(mouseX, mouseY, 0.0, amount)
                || super.mouseScrolled(mouseX, mouseY, horizontalAmount, amount)

    override fun mouseDragged(
        mouseX: Double, mouseY: Double, button: Int,
        deltaX: Double, deltaY: Double
    ): Boolean = ui.mouseDragged(mouseX, mouseY, button)
            || super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        ui.stopDrag()
        return super.mouseReleased(mouseX, mouseY, button)
    }

    // ─── 扫描结果（S2C 包更新）──────────────────────────────────────

    companion object {
        @JvmField
        var lastResults: List<OreScanEntry> = emptyList()

        fun receiveResults(packet: ScannerResultPacket) {
            lastResults = packet.results
        }

        private fun entryToItemStack(entry: OreScanEntry): ItemStack {
            val id = Identifier.tryParse(entry.blockId) ?: return ItemStack.EMPTY
            val block = Registries.BLOCK.getOrEmpty(id).orElse(null) ?: return ItemStack.EMPTY
            val item = block.asItem()
            if (item == net.minecraft.item.Items.AIR) return ItemStack.EMPTY
            return ItemStack(item, entry.count.coerceAtLeast(1))
        }
    }

    // ─── UI 构建 ───────────────────────────────────────────────────

    private fun UiScope.buildUi(
        x: Int,
        y: Int,
        energy: Long,
        cap: Long,
        energyFraction: Float,
        scannerTitle: String,
        scanRangeText: String,
        canScan: Boolean,
        usesRemaining: Int,
        maxUses: Int,
        results: List<OreScanEntry>
    ) {
        Flex(
            x = x,
            y = y,
            direction = FlexDirection.ROW,
            gap = 12,
            modifier = Modifier.EMPTY.width(gui.contentWidth).height(gui.height - 16)
        ) {
            // 左侧：信息面板
            Column(spacing = 4, modifier = Modifier().fractionWidth(0.5f)) {
                Text(scannerTitle, color = 0xFFFFFF)
                Text(
                    "${energy.toInt()} / $cap EU",
                    color = 0xCCCCCC,
                    shadow = false
                )
                EnergyBar(energyFraction)
                Text(
                    t("gui.ic2_120.scanner.remaining_uses", usesRemaining, maxUses),
                    color = if (usesRemaining > 0) 0xAAAAAA else 0xFF4A4A,
                    shadow = false
                )
                Text(
                    t("gui.ic2_120.scanner.scan_range", scanRangeText),
                    color = 0x888888,
                    shadow = false
                )
                Button(
                    text = if (canScan) t("gui.ic2_120.scan") else t("gui.ic2_120.status_no_energy"),
                    modifier = Modifier.EMPTY.width(100),
                    onClick = {
                        client?.player?.networkHandler?.sendPacket(
                            ButtonClickC2SPacket(handler.syncId, ScannerScreenHandler.BUTTON_ID_SCAN)
                        )
                    }
                )
            }

            // 右侧：扫描结果区
            Column(spacing = 4, modifier = Modifier().fractionWidth(0.5f)) {
                if (results.isNotEmpty()) {
                    ScrollView(
                        // width = gui.contentWidth - 120,
                        // height = gui.height - 16,
                        scrollbarWidth = 8
                    ) {
                        Column(spacing = 6) {
                            Text(t("gui.ic2_120.scanner.scan_results"), color = 0xFFFFFF)
                            for (entry in results) {
                                val oreStack = entryToItemStack(entry)
                                if (!oreStack.isEmpty) {
                                    Flex(justifyContent = JustifyContent.SPACE_BETWEEN) {
                                        ItemStack(oreStack, size = 16)
//                                        Text(
//                                            entry.blockId.substringAfterLast("/")
//                                                .replace("_", " ")
//                                                .replaceFirstChar { it.uppercase() },
//                                            color = 0xFFFFFF,
//                                            shadow = false
//                                        )
                                        Text(
                                            if (entry.count >= 1000) "${entry.count / 1000}k"
                                            else entry.count.toString(),
                                            color = 0xFFAA33,
                                            shadow = false,
                                            modifier = Modifier.EMPTY
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        t("gui.ic2_120.scanner.click_to_scan"),
                        color = 0x666666,
                        shadow = false
                    )
                }
            }
        }
    }
}
