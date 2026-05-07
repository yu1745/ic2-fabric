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
 * 上方：信息 + 范围控制（X/Y/Z 步进按钮）+ 扫描按钮
 * 下方：扫描结果 4 列网格（可滚动）
 *
 * Y 轴范围按钮仅作显示，扫描不受限（从世界底部扫到顶部）。
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
        GuiBackground.drawVanillaLikePanel(context, x, y, gui.width, gui.height)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1)
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val type = OdScannerItem.getScannerType(handler.playerInventory.getStack(handler.playerInventory.selectedSlot))
        val usesRemaining = handler.sync.usesRemaining
        val maxUses = handler.sync.maxUses
        val scannerTitle = if (type.tier == 3) "OV 扫描仪" else "OD 扫描仪"
        val rangeX = handler.sync.rangeX
        val rangeY = handler.sync.rangeY
        val rangeZ = handler.sync.rangeZ
        val energyCost = ScannerScreenHandler.computeEnergyCost(type, rangeX, rangeY, rangeZ)
        val canScan = energy >= energyCost && usesRemaining > 0
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
                canScan = canScan,
                usesRemaining = usesRemaining,
                maxUses = maxUses,
                results = results,
                energyCost = energyCost,
                rangeX = rangeX,
                rangeY = rangeY,
                rangeZ = rangeZ
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

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean =
        ui.mouseScrolled(mouseX, mouseY, 0.0, amount)
                || super.mouseScrolled(mouseX, mouseY, amount)

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
        canScan: Boolean,
        usesRemaining: Int,
        maxUses: Int,
        results: List<OreScanEntry>,
        rangeX: Int,
        rangeY: Int,
        rangeZ: Int,
        energyCost: Int
    ) {
        Flex(
            x = x,
            y = y,
            direction = FlexDirection.COLUMN,
            gap = 6,
            modifier = Modifier.EMPTY.width(gui.contentWidth).height(gui.height - 16)
        ) {
            // 上方：信息 + 范围控制 + 扫描按钮
            Flex(direction = FlexDirection.ROW, gap = 8) {
                // 左侧：基础信息
                Column(spacing = 4, modifier = Modifier().fractionWidth(0.4f)) {
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
                        "本次消耗: ${energyCost}EU",
                        color = if (canScan) 0x88FF88 else 0xFF4A4A,
                        shadow = false
                    )
                }

                // 右侧：范围控制
                Column(spacing = 4, modifier = Modifier().fractionWidth(0.6f)) {
                    Text("范围控制", color = 0x888888, shadow = false)

                    // X
                    Flex(gap = 2, alignItems = AlignItems.CENTER) {
                        Text("X", color = 0xAAAAAA)
                        Button("-", modifier = Modifier().width(18), onClick = {
                            client?.player?.networkHandler?.sendPacket(
                                ButtonClickC2SPacket(handler.syncId, ScannerScreenHandler.BUTTON_RANGE_X_DEC)
                            )
                        })
                        Text("$rangeX", color = 0xFFFFFF, modifier = Modifier().width(18), center = true)
                        Button("+", modifier = Modifier().width(18), onClick = {
                            client?.player?.networkHandler?.sendPacket(
                                ButtonClickC2SPacket(handler.syncId, ScannerScreenHandler.BUTTON_RANGE_X_INC)
                            )
                        })
                        Text("  Z", color = 0xAAAAAA)
                        Button("-", modifier = Modifier().width(18), onClick = {
                            client?.player?.networkHandler?.sendPacket(
                                ButtonClickC2SPacket(handler.syncId, ScannerScreenHandler.BUTTON_RANGE_Z_DEC)
                            )
                        })
                        Text("$rangeZ", color = 0xFFFFFF, modifier = Modifier().width(18), center = true)
                        Button("+", modifier = Modifier().width(18), onClick = {
                            client?.player?.networkHandler?.sendPacket(
                                ButtonClickC2SPacket(handler.syncId, ScannerScreenHandler.BUTTON_RANGE_Z_INC)
                            )
                        })
                    }

                    // Y（带 10 步进）
                    Flex(gap = 2, alignItems = AlignItems.CENTER) {
                        Text("Y", color = 0xAAAAAA)
                        Button("-", modifier = Modifier().width(18), onClick = {
                            client?.player?.networkHandler?.sendPacket(
                                ButtonClickC2SPacket(handler.syncId, ScannerScreenHandler.BUTTON_RANGE_Y_DEC)
                            )
                        })
                        Button("-10", modifier = Modifier().width(28), onClick = {
                            client?.player?.networkHandler?.sendPacket(
                                ButtonClickC2SPacket(handler.syncId, ScannerScreenHandler.BUTTON_RANGE_Y_DEC_10)
                            )
                        })
                        Text("$rangeY", color = 0xFFFFFF, modifier = Modifier().width(18), center = true)
                        Button("+", modifier = Modifier().width(18), onClick = {
                            client?.player?.networkHandler?.sendPacket(
                                ButtonClickC2SPacket(handler.syncId, ScannerScreenHandler.BUTTON_RANGE_Y_INC)
                            )
                        })
                        Button("+10", modifier = Modifier().width(28), onClick = {
                            client?.player?.networkHandler?.sendPacket(
                                ButtonClickC2SPacket(handler.syncId, ScannerScreenHandler.BUTTON_RANGE_Y_INC_10)
                            )
                        })
                    }
                }
            }

            // 扫描按钮
            Button(
                text = if (canScan) t("gui.ic2_120.scan") else t("gui.ic2_120.status_no_energy"),
                modifier = Modifier.EMPTY.width(100),
                onClick = {
                    client?.player?.networkHandler?.sendPacket(
                        ButtonClickC2SPacket(handler.syncId, ScannerScreenHandler.BUTTON_ID_SCAN)
                    )
                }
            )

            // 下方：扫描结果区（4 列网格，占满剩余高度）
            if (results.isNotEmpty()) {
                ScrollView(modifier = Modifier().fractionHeight(1.0f), scrollbarWidth = 8) {
                    Column(spacing = 4) {
                        Text(t("gui.ic2_120.scanner.scan_results"), color = 0xFFFFFF)
                        results.chunked(4).forEach { row ->
                            Flex(gap = 4) {
                                for (entry in row) {
                                    val oreStack = entryToItemStack(entry)
                                    if (!oreStack.isEmpty) {
                                        Column(modifier = Modifier().fractionWidth(0.25f), spacing = 2) {
                                            ItemStack(oreStack, size = 16)
                                            Text(
                                                if (entry.count >= 1000) "${entry.count / 1000}k"
                                                else entry.count.toString(),
                                                color = 0xFFAA33,
                                                shadow = false
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    t("gui.ic2_120.scanner.click_to_scan"),
                    color = 0x666666,
                    shadow = false,
                    modifier = Modifier().fractionHeight(1.0f)
                )
            }
        }
    }
}
