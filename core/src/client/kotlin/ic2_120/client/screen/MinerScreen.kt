package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.t
import ic2_120.content.screen.MinerScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(handlers = ["miner", "advanced_miner"])
class MinerScreen(
    handler: MinerScreenHandler, playerInventory: PlayerInventory, title: Text
) : HandledScreen<MinerScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = 176
        backgroundHeight = if (handler.isAdvanced) 203 else 165
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(
            if (handler.isAdvanced) TEX_ADVANCED else TEX_ORDINARY,
            x, y, 0f, 0f, backgroundWidth, backgroundHeight, TEXTURE_SIZE, TEXTURE_SIZE
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        val left = x
        val top = y

        context.drawText(textRenderer, title, left + (backgroundWidth - textRenderer.getWidth(title)) / 2, top + 6, 0x404040, false)

        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1)
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val consumeRate = handler.sync.getSyncedConsumedAmount()

        if (handler.isAdvanced) {
            // 电量条 (179,3)-(193,18) = 14x15，自下而上，渲染至 10,64
            if (energyFraction > 0f) {
                val fillH = (ADV_EB_H * energyFraction).toInt().coerceAtLeast(1)
                context.enableScissor(left + ADV_EB_X, top + ADV_EB_Y + ADV_EB_H - fillH,
                    left + ADV_EB_X + ADV_EB_W, top + ADV_EB_Y + ADV_EB_H)
                context.drawTexture(TEX_ADVANCED, left + ADV_EB_X, top + ADV_EB_Y,
                    ADV_EB_U.toFloat(), ADV_EB_V.toFloat(), ADV_EB_W, ADV_EB_H, TEXTURE_SIZE, TEXTURE_SIZE)
                context.disableScissor()
            }

            // 模式文本 (37,28)-(121,40)
            val modeValue = if (handler.sync.mode == 0) t("gui.ic2_120.miner.mode_whitelist")
            else t("gui.ic2_120.miner.mode_blacklist")
            val modeText = t("gui.ic2_120.miner.current_mode", modeValue)
            context.drawText(textRenderer, modeText, left + 37, top + 31, 0x66FF00, false)

            // 精准模式文本 (129,45)-(147,60)
            val silkText = if (handler.sync.silkTouch == 0) t("gui.ic2_120.miner.off") else t("gui.ic2_120.miner.on")
            context.drawText(textRenderer, silkText, left + 129, top + 49, 0xAAAAAA, false)

            // 光标文本 (9,103)-(132,115)
            val cursorText = t("gui.ic2_120.miner.scan_cursor", handler.sync.cursorX, handler.sync.cursorY, handler.sync.cursorZ)
            context.drawText(textRenderer, cursorText, left + 9, top + 105, 0x55FF55, false)

            // uptips
            context.drawTexture(UPTIPS_TEX, left + UPTIPS_X, top + UPTIPS_Y, 0f, 0f, UPTIPS_SIZE, UPTIPS_SIZE, UPTIPS_SIZE, UPTIPS_SIZE)
        } else {
            // 电量条 (179,2)-(193,17) = 14x15，自下而上，渲染至 153,40
            if (energyFraction > 0f) {
                val fillH = (ORD_EB_H * energyFraction).toInt().coerceAtLeast(1)
                context.enableScissor(left + ORD_EB_X, top + ORD_EB_Y + ORD_EB_H - fillH,
                    left + ORD_EB_X + ORD_EB_W, top + ORD_EB_Y + ORD_EB_H)
                context.drawTexture(TEX_ORDINARY, left + ORD_EB_X, top + ORD_EB_Y,
                    ORD_EB_U.toFloat(), ORD_EB_V.toFloat(), ORD_EB_W, ORD_EB_H, TEXTURE_SIZE, TEXTURE_SIZE)
                context.disableScissor()
            }

            // uptips
            context.drawTexture(UPTIPS_TEX, left + UPTIPS_X, top + UPTIPS_Y, 0f, 0f, UPTIPS_SIZE, UPTIPS_SIZE, UPTIPS_SIZE, UPTIPS_SIZE)
        }

        // 侧边文本
        val inputText = t("gui.ic2_120.input_eu", EnergyFormatUtils.formatRaw(inputRate))
        val consumeText = t("gui.ic2_120.consume_eu", EnergyFormatUtils.formatRaw(consumeRate))
        val sideTextWidth = maxOf(textRenderer.getWidth(inputText), textRenderer.getWidth(consumeText))
        val sideTextX = left - sideTextWidth - 4
        context.drawText(textRenderer, inputText, sideTextX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, consumeText, sideTextX, top + 20, 0xAAAAAA, false)

        drawMouseoverTooltip(context, mouseX, mouseY)

        val relX = mouseX - left
        val relY = mouseY - top

        if (handler.isAdvanced) {
            // 电量条悬停
            if (relX in ADV_EB_X until ADV_EB_X + ADV_EB_W && relY in ADV_EB_Y until ADV_EB_Y + ADV_EB_H) {
                context.drawTooltip(textRenderer, Text.literal("${EnergyFormatUtils.formatRaw(energy)} / ${EnergyFormatUtils.formatRaw(cap)} EU"), mouseX, mouseY)
            }
            // 模式切换区域高亮 (123,27)-(141,42)
            if (relX in 123 until 142 && relY in 27 until 43) {
                context.drawBorder(left + 123, top + 27, 19, 16, 0xFFFFFFFF.toInt())
                context.drawTooltip(textRenderer, Text.translatable("gui.ic2_120.miner.mode_switch"), mouseX, mouseY)
            }
            // 精准模式区域高亮 (129,45)-(147,60)
            if (relX in 129 until 148 && relY in 45 until 61) {
                context.drawBorder(left + 129, top + 45, 19, 16, 0xFFFFFFFF.toInt())
                val silkHoverText = if (handler.sync.silkTouch == 0)
                    t("gui.ic2_120.miner.silk_off") else t("gui.ic2_120.miner.silk_on")
                context.drawTooltip(textRenderer, Text.literal(silkHoverText), mouseX, mouseY)
            }
            // 重置区域高亮 (133,101)-(169,116)
            if (relX in 133 until 170 && relY in 101 until 117) {
                context.drawBorder(left + 133, top + 101, 37, 16, 0xFFFFFFFF.toInt())
                context.drawTooltip(textRenderer, Text.translatable("gui.ic2_120.miner.reset_tooltip"), mouseX, mouseY)
            }
            // uptips 悬停
            if (relX in UPTIPS_X until UPTIPS_X + UPTIPS_SIZE && relY in UPTIPS_Y until UPTIPS_Y + UPTIPS_SIZE) {
                context.drawTooltip(textRenderer, listOf(
                    Text.translatable("gui.ic2_120.miner.uptips_advanced"),
                    Text.literal("§7").append(Text.translatable("item.ic2_120.overclocker_upgrade")),
                    Text.literal("§7").append(Text.translatable("item.ic2_120.transformer_upgrade")),
                    Text.literal("§7").append(Text.translatable("item.ic2_120.redstone_inverter_upgrade"))
                ), mouseX, mouseY)
            }
        } else {
            // 电量条悬停
            if (relX in ORD_EB_X until ORD_EB_X + ORD_EB_W && relY in ORD_EB_Y until ORD_EB_Y + ORD_EB_H) {
                context.drawTooltip(textRenderer, Text.literal("${EnergyFormatUtils.formatRaw(energy)} / ${EnergyFormatUtils.formatRaw(cap)} EU"), mouseX, mouseY)
            }
            // uptips 悬停
            if (relX in UPTIPS_X until UPTIPS_X + UPTIPS_SIZE && relY in UPTIPS_Y until UPTIPS_Y + UPTIPS_SIZE) {
                context.drawTooltip(textRenderer, listOf(
                    Text.translatable("gui.ic2_120.miner.uptips"),
                    Text.literal("§7").append(Text.translatable("item.ic2_120.ejector_upgrade")),
                    Text.literal("§7").append(Text.translatable("item.ic2_120.pulling_upgrade"))
                ), mouseX, mouseY)
            }
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (handler.isAdvanced && button == 0) {
            val relX = (mouseX - x).toInt()
            val relY = (mouseY - y).toInt()
            if (relX in 123 until 142 && relY in 27 until 43) {
                client?.player?.networkHandler?.sendPacket(ButtonClickC2SPacket(handler.syncId, MinerScreenHandler.BUTTON_TOGGLE_MODE))
                return true
            }
            if (relX in 129 until 148 && relY in 45 until 61) {
                client?.player?.networkHandler?.sendPacket(ButtonClickC2SPacket(handler.syncId, MinerScreenHandler.BUTTON_TOGGLE_SILK))
                return true
            }
            if (relX in 133 until 170 && relY in 101 until 117) {
                client?.player?.networkHandler?.sendPacket(ButtonClickC2SPacket(handler.syncId, MinerScreenHandler.BUTTON_RECOVER_PIPES))
                return true
            }
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    companion object {
        private val TEX_ORDINARY = Identifier("ic2", "textures/gui/guiminer.png")
        private val TEX_ADVANCED = Identifier("ic2", "textures/gui/guiadvminer.png")
        private val UPTIPS_TEX = Identifier("ic2", "textures/gui/uptips.png")
        private const val TEXTURE_SIZE = 256
        private const val UPTIPS_X = 4
        private const val UPTIPS_Y = 4
        private const val UPTIPS_SIZE = 16

        // 普通: 电量条 (179,2)-(193,17) = 14x15, 渲染至 153,40
        private const val ORD_EB_U = 179; private const val ORD_EB_V = 2
        private const val ORD_EB_W = 14; private const val ORD_EB_H = 15
        private const val ORD_EB_X = 152; private const val ORD_EB_Y = 39

        // 高级: 电量条 (179,3)-(193,18) = 14x15, 渲染至 10,64
        private const val ADV_EB_U = 179; private const val ADV_EB_V = 3
        private const val ADV_EB_W = 14; private const val ADV_EB_H = 15
        private const val ADV_EB_X = 9; private const val ADV_EB_Y = 62
    }
}
