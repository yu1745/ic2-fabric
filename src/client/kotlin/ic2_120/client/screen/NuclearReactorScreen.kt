package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.nuclear.NuclearReactorBlock
import ic2_120.content.network.SlotHeatEnergyInfo
import ic2_120.content.screen.NuclearReactorScreenHandler
import ic2_120.content.sync.NuclearReactorSync
import ic2_120.registry.annotation.ModScreen
import ic2_120.registry.type
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

@ModScreen(block = NuclearReactorBlock::class)
class NuclearReactorScreen(
    handler: NuclearReactorScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<NuclearReactorScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    /** 整体 UI 下移偏移，避免顶部文字溢出 */
    private val guiOffsetY = 8

    /** 能量条/温度条宽度 */
    private val barWidth = 14

    /** 左侧文本边距 */
    private val leftTextMargin = 4

    /** 热模式时左右各多出的流体条宽度（barWidth + 间距） */
    private val thermalExtraWidth = barWidth + 4

    /** 流体条高度：能量条高度 - 2个slot高度，使 流体条+两槽 总高等于能量条 */
    private val fluidBarHeight =
        9 * NuclearReactorScreenHandler.SLOT_SIZE - 2 * NuclearReactorScreenHandler.SLOT_SIZE  // 126 = 7*18

    init {
        backgroundWidth = NuclearReactorScreenHandler.FRAME_WIDTH
        backgroundHeight = handler.hotbarY + 18 + 8
        titleY = -1000
        playerInventoryTitleY = -1000  // 隐藏 "Inv" 文本
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        val isThermal = isThermalLayout()
        val effectiveWidth =
            if (isThermal) NuclearReactorScreenHandler.FRAME_WIDTH + thermalExtraWidth * 2 else NuclearReactorScreenHandler.FRAME_WIDTH
        val bgX = if (isThermal) x - thermalExtraWidth else x
        GuiBackground.draw(context, bgX, y, effectiveWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y,
            handler.playerInvY - 4,
            handler.hotbarY - 4,
            NuclearReactorScreenHandler.SLOT_SIZE,
            playerInvX = NuclearReactorScreenHandler.PLAYER_INV_X
        )
        val borderColor = GuiBackground.BORDER_COLOR
        val slotSize = NuclearReactorScreenHandler.SLOT_SIZE
        val borderOffset = 1

        // 反应堆槽位整体 9x9 区域外边框
        val gridX = x + NuclearReactorScreenHandler.SLOT_GRID_X - borderOffset
        val gridY = y + NuclearReactorScreenHandler.SLOT_GRID_Y - borderOffset
        val gridW = 9 * slotSize + borderOffset * 2
        val gridH = 9 * slotSize + borderOffset * 2
        // context.drawBorder(gridX, gridY, gridW, gridH, borderColor)

        // 为每个反应堆槽位绘制外边框（参考 GeneratorScreen），让用户清楚实际有多少 slot
        for (i in 0 until handler.reactorSlotCount) {
            val slot = handler.slots[i]
            context.drawBorder(x + slot.x - borderOffset, y + slot.y - borderOffset, slotSize, slotSize, borderColor)
        }

        // 热模式：为 4 个流体槽绘制边框
        if (isThermal) {
            for (i in handler.reactorSlotCount until handler.reactorSlotCount + 4) {
                val slot = handler.slots[i]
                context.drawBorder(
                    x + slot.x - borderOffset,
                    y + slot.y - borderOffset,
                    slotSize,
                    slotSize,
                    borderColor
                )
            }
        }

        // 能量条和温度条位置（固定）
        val energyBarX = x + 9
        val tempBarX = x + NuclearReactorScreenHandler.SLOT_GRID_X + 9 * slotSize + 4

        if (isThermal) {
            // 热模式：流体条+两槽 与能量条垂直对齐（能量条 y=18~180）
            val fluidBarY = NuclearReactorScreenHandler.SLOT_GRID_Y + slotSize  // 上槽(18-36) + 条(36-162) + 下槽(162-180)

            // 冷却液条（左侧）
            val inputFraction = handler.sync.inputCoolantMb.toFloat() / NuclearReactorSync.COOLANT_TANK_CAPACITY_MB
            val inputX = energyBarX - barWidth - 4
            drawVerticalFluidBar(
                context, inputX, y + fluidBarY,
                barWidth, fluidBarHeight, 0xFF00CCFF.toInt(), inputFraction
            )

            // 热冷却液条（右侧）
            val outputFraction = handler.sync.outputHotCoolantMb.toFloat() / NuclearReactorSync.COOLANT_TANK_CAPACITY_MB
            val outputX = tempBarX + barWidth + 4
            drawVerticalFluidBar(
                context, outputX, y + fluidBarY,
                barWidth, fluidBarHeight, 0xFFFF6600.toInt(), outputFraction
            )
        }

        // 绘制能量和温度条（位置不变）
        drawVerticalEnergyBar(context, energyBarX, y + NuclearReactorScreenHandler.SLOT_GRID_Y, barWidth, 9 * slotSize)
        drawVerticalTemperatureBar(
            context,
            tempBarX,
            y + NuclearReactorScreenHandler.SLOT_GRID_Y,
            barWidth,
            9 * slotSize
        )
    }

    private fun drawVerticalEnergyBar(context: DrawContext, barX: Int, barY: Int, w: Int, h: Int) {
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = NuclearReactorSync.ENERGY_CAPACITY
        val fraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f

        context.fill(barX, barY, barX + w, barY + h, 0xFF333333.toInt())
        val filledH = (fraction * h).toInt()
        if (filledH > 0) {
            val fillY = barY + h - filledH
            context.enableScissor(barX, fillY, barX + w, barY + h)
            val strips = maxOf(2, h)
            for (i in 0 until strips) {
                val t = i.toFloat() / (strips - 1).coerceAtLeast(1)
                val color = lerpArgb(0xFFCC0000.toInt(), 0xFF00CC00.toInt(), t)
                val y1 = fillY + (i * h / strips)
                val y2 = fillY + ((i + 1) * h / strips).coerceAtMost(fillY + h)
                context.fill(barX, y1, barX + w, y2, color)
            }
            context.disableScissor()
        }
        context.drawBorder(barX, barY, w, h, 0xFF888888.toInt())
    }

    private fun drawVerticalTemperatureBar(context: DrawContext, barX: Int, barY: Int, w: Int, h: Int) {
        val temp = handler.sync.temperature.coerceIn(0, NuclearReactorSync.HEAT_CAPACITY)
        val fraction = temp.toFloat() / NuclearReactorSync.HEAT_CAPACITY

        context.fill(barX, barY, barX + w, barY + h, 0xFF333333.toInt())
        val filledH = (fraction * h).toInt()
        if (filledH > 0) {
            val fillY = barY + h - filledH
            context.enableScissor(barX, fillY, barX + w, barY + h)
            val strips = maxOf(2, h)
            for (i in 0 until strips) {
                val t = i.toFloat() / (strips - 1).coerceAtLeast(1)
                val color = lerpArgb(0xFF0066CC.toInt(), 0xFFCC0000.toInt(), t)
                val y1 = fillY + (i * h / strips)
                val y2 = fillY + ((i + 1) * h / strips).coerceAtMost(fillY + h)
                context.fill(barX, y1, barX + w, y2, color)
            }
            context.disableScissor()
        }
        context.drawBorder(barX, barY, w, h, 0xFF888888.toInt())
    }

    private fun drawVerticalFluidBar(
        context: DrawContext,
        barX: Int,
        barY: Int,
        w: Int,
        h: Int,
        color: Int,
        fraction: Float
    ) {
        context.fill(barX, barY, barX + w, barY + h, 0xFF333333.toInt())
        val filledH = (fraction.coerceIn(0f, 1f) * h).toInt()
        if (filledH > 0) {
            val fillY = barY + h - filledH
            context.enableScissor(barX, fillY, barX + w, barY + h)
            context.fill(barX, fillY, barX + w, barY + h, color)
            context.disableScissor()
        }
        context.drawBorder(barX, barY, w, h, 0xFF888888.toInt())
    }

    private fun lerpArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val aa = (a shr 24) and 0xFF
        val ar = (a shr 16) and 0xFF
        val ag = (a shr 8) and 0xFF
        val ab = a and 0xFF
        val ba = (b shr 24) and 0xFF
        val br = (b shr 16) and 0xFF
        val bg = (b shr 8) and 0xFF
        val bb = b and 0xFF
        return ((aa + (ba - aa) * u).toInt() and 0xFF shl 24) or
                ((ar + (br - ar) * u).toInt() and 0xFF shl 16) or
                ((ag + (bg - ag) * u).toInt() and 0xFF shl 8) or
                ((ab + (bb - ab) * u).toInt() and 0xFF)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        val left = x
        val top = y + guiOffsetY  // 内容下移避免顶部溢出
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = NuclearReactorSync.ENERGY_CAPACITY
        val temp = handler.sync.temperature.coerceIn(0, NuclearReactorSync.HEAT_CAPACITY)
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val outputRate = handler.sync.getSyncedExtractedAmount()


        val heatProduced = handler.sync.totalHeatProduced
        val heatDissipated = handler.sync.totalHeatDissipated
        val actualHeatDissipated = handler.sync.actualHeatDissipated
        val thermalHeatOutput = handler.sync.thermalHeatOutput
        // 在 GUI 左侧外部绘制状态信息（缩短文本避免溢出）
        val lines = mutableListOf<String>()
        if (!isThermalLayout()) {
            lines.add("能量 ${formatEu(energy)}/${formatEu(cap)}")
            lines.add("发电 ${formatEu(inputRate)} 输出 ${formatEu(outputRate)} EU/t")
        }
        lines.add("堆温 $temp")
        if (isThermalLayout()) {
            lines.add("(流体堆发热翻倍)")
            lines.add("热输出 ${thermalHeatOutput/20} HU/t")
            lines.add("产热 ${heatProduced} 散热能力 ${heatDissipated}")
            lines.add("实际散热 ${actualHeatDissipated/20} HU/t")
        } else {
            lines.add("产热 ${heatProduced} 散热能力 ${heatDissipated}")
            lines.add("HU/s")
        }


        // 热模式：冷却液数据
        if (isThermalLayout()) {
            val inputMb = handler.sync.inputCoolantMb.coerceAtLeast(0)
            val outputMb = handler.sync.outputHotCoolantMb.coerceAtLeast(0)
            val capMb = NuclearReactorSync.COOLANT_TANK_CAPACITY_MB
            lines.add("冷液 $inputMb/$capMb")
            lines.add("${"%.1f".format(inputMb.toFloat() / capMb.toFloat() * 100)}%")
            lines.add("热液 $outputMb/$capMb")
            lines.add("${"%.1f".format(outputMb.toFloat() / capMb.toFloat() * 100)}%")
        }

        // 槽位产热/散热/发电：只在鼠标悬停时显示
        val hoveredSlotIndex = findHoveredReactorSlot(mouseX, mouseY)
        if (hoveredSlotIndex != null) {
            val reactor = handler.reactor
            reactor?.let {
                val heatInfo = it.slotHeatInfo[hoveredSlotIndex]
                heatInfo?.let { info ->
                    lines.add("§e产热 ${info.heatProduced} 散热 ${info.heatDissipated}")
                    if (info.energyOutput > 0 && !isThermalLayout()) {
                        lines.add("§a发电 ${if (isThermalLayout()) info.energyOutput / 5 else info.energyOutput}")
                    }
                }
            }
        }

        // 绘制所有文本在 GUI 左侧外部；热模式时多两个流体条，需额外左移避免重叠
        var textY = y + 42
        val maxWidth = lines.maxOf { textRenderer.getWidth(it) }
        val thermalTextOffset = if (isThermalLayout()) thermalExtraWidth else 0
        val textX = left - maxWidth - leftTextMargin - thermalTextOffset

        for (line in lines) {
            val color = when {
                line.startsWith("§e") -> 0xFFFFAA
                line.startsWith("§a") -> 0xAAFFAA
                line.isBlank() -> 0xFFFFFF
                else -> 0xFFFFFF
            }
            val displayLine = line.replace("§e", "").replace("§a", "")
            context.drawText(textRenderer, displayLine, textX, textY, color, false)
            textY += textRenderer.fontHeight + 2
        }

        // 手动调用 tooltip 绘制，确保物品 tooltip 显示在最上层
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun drawMouseoverTooltip(context: DrawContext, mouseX: Int, mouseY: Int) {
        // 绘制默认的 tooltip（物品信息）
        super.drawMouseoverTooltip(context, mouseX, mouseY)
    }

    /** 查找鼠标悬停的反应堆槽位索引，如果没有悬停则返回 null */
    private fun findHoveredReactorSlot(mouseX: Int, mouseY: Int): Int? {
        for (i in 0 until handler.reactorSlotCount) {
            val slot = handler.slots[i]
            if (isPointOverSlot(slot, mouseX, mouseY)) {
                return i
            }
        }
        return null
    }

    private fun isPointOverSlot(slot: net.minecraft.screen.slot.Slot, mouseX: Int, mouseY: Int): Boolean {
        val slotX = x + slot.x
        val slotY = y + slot.y
        return mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18
    }

    private fun formatEu(value: Long): String = when {
        value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
        value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
        else -> value.toString()
    }

    private fun formatMb(value: Int): String = when {
        value >= 1_000_000 -> String.format("%.3fM", value / 1_000_000.0)
        value >= 1_000 -> String.format("%.3fk", value / 1_000.0)
        else -> value.toString()
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    override fun isClickOutsideBounds(mouseX: Double, mouseY: Double, left: Int, top: Int, button: Int): Boolean {
        if (!isThermalLayout()) {
            return super.isClickOutsideBounds(mouseX, mouseY, left, top, button)
        }

        // 热模式下两侧有额外的流体槽和流体条，需要扩展“界面内点击区域”。
        val extendedLeft = left - thermalExtraWidth
        val extendedWidth = backgroundWidth + thermalExtraWidth * 2
        val withinX = mouseX >= extendedLeft && mouseX < (extendedLeft + extendedWidth)
        val withinY = mouseY >= top && mouseY < (top + backgroundHeight)
        return !(withinX && withinY)
    }

    private fun isThermalLayout(): Boolean = handler.isThermalMode || handler.sync.isThermalMode == 1
}

