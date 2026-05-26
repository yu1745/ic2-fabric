package ic2_120.client.screen

import ic2_120.client.t
import ic2_120.content.block.UuScannerBlock
import ic2_120.content.screen.UuScannerScreenHandler
import ic2_120.content.sync.UuScannerSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = UuScannerBlock::class)
class UuScannerScreen(
    handler: UuScannerScreenHandler, playerInventory: PlayerInventory, title: Text
) : HandledScreen<UuScannerScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = 175
        backgroundHeight = 165
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, backgroundWidth, backgroundHeight, TEX_SIZE, TEX_SIZE)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)

        val left = x
        val top = y

        context.drawText(textRenderer, title, left + (backgroundWidth - textRenderer.getWidth(title)) / 2, top + 6, 0x404040, false)

        val status = handler.sync.status
        val isComplete = status == UuScannerSync.STATUS_COMPLETE
        val isScanning = status == UuScannerSync.STATUS_SCANNING
        // 电量条 (179,3)-(193,16) = 14×13 渲染至 (8,24)
        drawEnergyBar(context, left, top)

        // 工作进度条 (30,20) — 扫描时自左向右重复动画
        drawProgressBar(context, left, top, isScanning)

        // 扫描进度/状态文本 (10,67)
        // 无物品 → 待机中，工作中 → 工作中，有物品但不能工作 → 显示原因（亮绿色）
        val (statusLine, statusColor) = when (status) {
            UuScannerSync.STATUS_SCANNING -> {
                val pct = (handler.sync.progress.toFloat() / UuScannerSync.PROGRESS_MAX * 100).toInt().coerceIn(0, 100)
                "扫描中($pct%)" to 0x55FF55
            }
            UuScannerSync.STATUS_NO_STORAGE -> "需要邻接模式存储机/放入存储水晶" to 0x55FF55
            UuScannerSync.STATUS_NOT_WHITELISTED -> "物品不在复制白名单中" to 0x55FF55
            UuScannerSync.STATUS_NO_ENERGY -> "能量不足" to 0x55FF55
            else -> t("gui.ic2_120.uu_scanner.status_idle") to 0x55FF55
        }
        context.drawText(textRenderer, Text.literal(statusLine), left + 10, top + 68, statusColor, false)

        // 扫描完成后：模板信息 + 删除/存入按钮
        if (isComplete) {
            val cost = handler.sync.currentCostUb
            val rawId = handler.sync.cachedItemRawId
            if (rawId > 0 && cost > 0) {
                val item = Registries.ITEM.get(rawId)
                val name = item.name.string
                val scale = 7f / textRenderer.fontHeight
                val infoX = left + 102
                // 物品名称
                drawScaledText(context, name, infoX, top + 22, scale, 0xFFAA33)
                // UU消耗标签
                val labelText = "UU消耗："
                drawScaledText(context, labelText, infoX, top + 30, scale, 0xFFAA33)
                // 消耗值 — 居中于标签下方
                val valueText = "%,d uB".format(cost)
                val labelW = (textRenderer.getWidth(labelText) * scale).toInt()
                val valueW = (textRenderer.getWidth(valueText) * scale).toInt()
                val valueX = infoX + (labelW - valueW) / 2 + 5
                drawScaledText(context, valueText, valueX, top + 38, scale, 0xFFAA33)
            }

            // 存入水晶 (213,4)-(237,16) = 24×12 渲染至 (142,48)
            context.drawTexture(TEXTURE, left + SAVE_X, top + SAVE_Y,
                SAVE_U.toFloat(), SAVE_V.toFloat(), 24, 12, TEX_SIZE, TEX_SIZE)
            // 删除模板 (197,4)-(209,16) = 12×12 渲染至 (129,48)
            context.drawTexture(TEXTURE, left + DEL_X, top + DEL_Y,
                DEL_U.toFloat(), DEL_V.toFloat(), 12, 12, TEX_SIZE, TEX_SIZE)
        }

        // 悬停高亮
        val relX = mouseX - left
        val relY = mouseY - top

        // 电量条悬停
        if (relX in ENERGY_X until ENERGY_X + ENERGY_W && relY in ENERGY_Y until ENERGY_Y + ENERGY_H) {
            val energy = handler.sync.energy.toLong().coerceAtLeast(0)
            val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1)
            context.drawTooltip(textRenderer,
                listOf(Text.literal("储能：${"%,d".format(energy)} / ${"%,d".format(cap)} EU")),
                mouseX, mouseY)
        }

        if (isComplete) {
            if (relX in DEL_X until DEL_X + 12 && relY in DEL_Y until DEL_Y + 12) {
                context.fill(left + DEL_X, top + DEL_Y, left + DEL_X + 12, top + DEL_Y + 12, 0x80FFFFFF.toInt())
                context.drawTooltip(textRenderer, listOf(Text.translatable("gui.ic2_120.uu_scanner.delete_template")), mouseX, mouseY)
            }
            if (relX in SAVE_X until SAVE_X + 24 && relY in SAVE_Y until SAVE_Y + 12) {
                context.fill(left + SAVE_X, top + SAVE_Y, left + SAVE_X + 24, top + SAVE_Y + 12, 0x80FFFFFF.toInt())
                context.drawTooltip(textRenderer, listOf(Text.translatable("gui.ic2_120.uu_scanner.save_to_crystal")), mouseX, mouseY)
            }
        }

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun drawEnergyBar(context: DrawContext, left: Int, top: Int) {
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1)
        val fraction = (energy.toFloat() / cap).coerceIn(0f, 1f)
        if (fraction <= 0f) return
        val fillH = (ENERGY_H * fraction).toInt().coerceAtLeast(1)
        context.enableScissor(
            left + ENERGY_X, top + ENERGY_Y + ENERGY_H - fillH,
            left + ENERGY_X + ENERGY_W, top + ENERGY_Y + ENERGY_H
        )
        context.drawTexture(TEXTURE, left + ENERGY_X, top + ENERGY_Y,
            ENERGY_U.toFloat(), ENERGY_V.toFloat(), ENERGY_W, ENERGY_H, TEX_SIZE, TEX_SIZE)
        context.disableScissor()
    }

    private fun drawProgressBar(context: DrawContext, left: Int, top: Int, isScanning: Boolean) {
        if (!isScanning) return
        val cycleMs = 1000L
        val fraction = ((System.currentTimeMillis() % cycleMs).toFloat() / cycleMs).coerceIn(0f, 1f)
        val fillW = (PROGRESS_W * fraction).toInt().coerceAtLeast(1)
        context.enableScissor(
            left + PROGRESS_X, top + PROGRESS_Y,
            left + PROGRESS_X + fillW, top + PROGRESS_Y + PROGRESS_H
        )
        context.drawTexture(TEXTURE, left + PROGRESS_X, top + PROGRESS_Y,
            PROGRESS_U.toFloat(), PROGRESS_V.toFloat(), PROGRESS_W, PROGRESS_H, TEX_SIZE, TEX_SIZE)
        context.disableScissor()
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0 && handler.sync.status == UuScannerSync.STATUS_COMPLETE) {
            val relX = mouseX.toInt() - x
            val relY = mouseY.toInt() - y
            if (relX in DEL_X until DEL_X + 12 && relY in DEL_Y until DEL_Y + 12) {
                client?.player?.networkHandler?.sendPacket(
                    ButtonClickC2SPacket(handler.syncId, UuScannerScreenHandler.BUTTON_DELETE_TEMPLATE))
                return true
            }
            if (relX in SAVE_X until SAVE_X + 24 && relY in SAVE_Y until SAVE_Y + 12) {
                client?.player?.networkHandler?.sendPacket(
                    ButtonClickC2SPacket(handler.syncId, UuScannerScreenHandler.BUTTON_SAVE_TO_CRYSTAL))
                return true
            }
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    private fun drawScaledText(context: DrawContext, text: String, tx: Int, ty: Int, scale: Float, color: Int) {
        context.matrices.push()
        context.matrices.translate(tx.toDouble(), ty.toDouble(), 0.0)
        context.matrices.scale(scale, scale, 1f)
        context.drawText(textRenderer, Text.literal(text), 0, 0, color, false)
        context.matrices.pop()
    }

    companion object {
        private val TEXTURE = Identifier.of("ic2", "textures/gui/guiuuscanner.png")
        private const val TEX_SIZE = 256

        private const val ENERGY_U = 179
        private const val ENERGY_V = 3
        private const val ENERGY_W = 14
        private const val ENERGY_H = 13
        private const val ENERGY_X = 8
        private const val ENERGY_Y = 24

        private const val PROGRESS_U = 179
        private const val PROGRESS_V = 20
        private const val PROGRESS_X = 29
        private const val PROGRESS_Y = 19
        private const val PROGRESS_W = 67
        private const val PROGRESS_H = 43

        private const val DEL_U = 197
        private const val DEL_V = 4
        private const val DEL_X = 129
        private const val DEL_Y = 48

        private const val SAVE_U = 213
        private const val SAVE_V = 4
        private const val SAVE_X = 142
        private const val SAVE_Y = 48
    }
}
