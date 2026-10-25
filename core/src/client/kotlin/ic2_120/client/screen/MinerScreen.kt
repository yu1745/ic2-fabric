package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.t
import ic2_120.content.screen.MinerScreenHandler
import ic2_120.registry.annotation.ModScreen
import ic2_120.client.compat.*
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Inventory
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.client.Minecraft

@ModScreen(handlers = ["miner", "advanced_miner"])
class MinerScreen(
    val handler: MinerScreenHandler, playerInventory: Inventory, title: Component
) : LegacyContainerScreen26<MinerScreenHandler>(handler, playerInventory, title, 176, if (handler.isAdvanced) 203 else 165) {

    init {
    }

    override fun renderBg(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.blit(
            if (menu.isAdvanced) TEX_ADVANCED else TEX_ORDINARY,
            leftPos, topPos, 0f, 0f, imageWidth, imageHeight, TEXTURE_SIZE, TEXTURE_SIZE
        )
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)

        val left = leftPos
        val top = topPos

        context.drawString(font, title, left + (imageWidth - font.width(title)) / 2, top + 6, 0x404040, false)

        val energy = menu.sync.energy.toLong().coerceAtLeast(0)
        val cap = menu.sync.energyCapacity.toLong().coerceAtLeast(1)
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val inputRate = menu.sync.getSyncedInsertedAmount()
        val consumeRate = menu.sync.getSyncedConsumedAmount()

        if (menu.isAdvanced) {
            // 电量条 (179,3)-(193,18) = 14x15，自下而上，渲染至 10,64
            if (energyFraction > 0f) {
                val fillH = (ADV_EB_H * energyFraction).toInt().coerceAtLeast(1)
                context.enableScissor(left + ADV_EB_X, top + ADV_EB_Y + ADV_EB_H - fillH,
                    left + ADV_EB_X + ADV_EB_W, top + ADV_EB_Y + ADV_EB_H)
                context.blit(TEX_ADVANCED, left + ADV_EB_X, top + ADV_EB_Y,
                    ADV_EB_U.toFloat(), ADV_EB_V.toFloat(), ADV_EB_W, ADV_EB_H, TEXTURE_SIZE, TEXTURE_SIZE)
                context.disableScissor()
            }

            // 模式文本 (37,28)-(121,40)
            val modeValue = if (menu.sync.mode == 0) t("gui.ic2_120.miner.mode_whitelist")
            else t("gui.ic2_120.miner.mode_blacklist")
            val modeText = t("gui.ic2_120.miner.current_mode", modeValue)
            context.drawString(font, modeText, left + 37, top + 31, 0x66FF00, false)

            // 精准模式文本 (129,45)-(147,60)
            val silkText = if (menu.sync.silkTouch == 0) t("gui.ic2_120.miner.off") else t("gui.ic2_120.miner.on")
            context.drawString(font, silkText, left + 129, top + 49, 0xAAAAAA, false)

            // 光标文本 (9,103)-(132,115)
            val cursorText = t("gui.ic2_120.miner.scan_cursor", menu.sync.cursorX, menu.sync.cursorY, menu.sync.cursorZ)
            context.drawString(font, cursorText, left + 9, top + 105, 0x55FF55, false)

            // uptips
            context.blit(UPTIPS_TEX, left + UPTIPS_X, top + UPTIPS_Y, 0f, 0f, UPTIPS_SIZE, UPTIPS_SIZE, UPTIPS_SIZE, UPTIPS_SIZE)
        } else {
            // 电量条 (179,2)-(193,17) = 14x15，自下而上，渲染至 153,40
            if (energyFraction > 0f) {
                val fillH = (ORD_EB_H * energyFraction).toInt().coerceAtLeast(1)
                context.enableScissor(left + ORD_EB_X, top + ORD_EB_Y + ORD_EB_H - fillH,
                    left + ORD_EB_X + ORD_EB_W, top + ORD_EB_Y + ORD_EB_H)
                context.blit(TEX_ORDINARY, left + ORD_EB_X, top + ORD_EB_Y,
                    ORD_EB_U.toFloat(), ORD_EB_V.toFloat(), ORD_EB_W, ORD_EB_H, TEXTURE_SIZE, TEXTURE_SIZE)
                context.disableScissor()
            }

            // uptips
            context.blit(UPTIPS_TEX, left + UPTIPS_X, top + UPTIPS_Y, 0f, 0f, UPTIPS_SIZE, UPTIPS_SIZE, UPTIPS_SIZE, UPTIPS_SIZE)
        }

        // 侧边文本
        val inputText = t("gui.ic2_120.input_eu", EnergyFormatUtils.formatRaw(inputRate))
        val consumeText = t("gui.ic2_120.consume_eu", EnergyFormatUtils.formatRaw(consumeRate))
        val sideTextWidth = maxOf(font.width(inputText), font.width(consumeText))
        val sideTextX = left - sideTextWidth - 4
        context.drawString(font, inputText, sideTextX, top + 8, 0xAAAAAA, false)
        context.drawString(font, consumeText, sideTextX, top + 20, 0xAAAAAA, false)
        val cursorText = t("gui.ic2_120.miner.scan_cursor", menu.sync.cursorX, menu.sync.cursorY, menu.sync.cursorZ)
        val cursorTextWidth = font.width(cursorText)
        context.drawString(font, cursorText, left - cursorTextWidth - 4, top + 32, 0x55FF55, false)
        if (menu.sync.fluidBlocked != 0) {
            val fluidText = t("gui.ic2_120.miner.fluid_blocked")
            val fluidTextWidth = font.width(fluidText)
            context.drawString(font, fluidText, left - fluidTextWidth - 4, top + 44, 0xFF5555, false)
        }
        renderTooltip(context, mouseX, mouseY)

        val relX = mouseX - left
        val relY = mouseY - top

        if (menu.isAdvanced) {
            // 电量条悬停
            if (relX in ADV_EB_X until ADV_EB_X + ADV_EB_W && relY in ADV_EB_Y until ADV_EB_Y + ADV_EB_H) {
                context.renderTooltip(font, Component.literal("${EnergyFormatUtils.formatRaw(energy)} / ${EnergyFormatUtils.formatRaw(cap)} EU"), mouseX, mouseY)
            }
            // 模式切换区域高亮 (123,27)-(141,42)
            if (relX in 123 until 142 && relY in 27 until 43) {
                context.renderOutline(left + 123, top + 27, 19, 16, 0xFFFFFFFF.toInt())
                context.renderTooltip(font, Component.translatable("gui.ic2_120.miner.mode_switch"), mouseX, mouseY)
            }
            // 精准模式区域高亮 (129,45)-(147,60)
            if (relX in 129 until 148 && relY in 45 until 61) {
                context.renderOutline(left + 129, top + 45, 19, 16, 0xFFFFFFFF.toInt())
                val silkHoverText = if (menu.sync.silkTouch == 0)
                    t("gui.ic2_120.miner.silk_off") else t("gui.ic2_120.miner.silk_on")
                context.renderTooltip(font, Component.literal(silkHoverText), mouseX, mouseY)
            }
            // 重置区域高亮 (133,101)-(169,116)
            if (relX in 133 until 170 && relY in 101 until 117) {
                context.renderOutline(left + 133, top + 101, 37, 16, 0xFFFFFFFF.toInt())
                context.renderTooltip(font, Component.translatable("gui.ic2_120.miner.reset_tooltip"), mouseX, mouseY)
            }
            // uptips 悬停
            if (relX in UPTIPS_X until UPTIPS_X + UPTIPS_SIZE && relY in UPTIPS_Y until UPTIPS_Y + UPTIPS_SIZE) {
                context.renderComponentTooltip(font, listOf(
                    Component.translatable("gui.ic2_120.miner.uptips_advanced"),
                    Component.literal("§7").append(Component.translatable("item.ic2_120.overclocker_upgrade")),
                    Component.literal("§7").append(Component.translatable("item.ic2_120.transformer_upgrade")),
                    Component.literal("§7").append(Component.translatable("item.ic2_120.redstone_inverter_upgrade")),
                    Component.literal("§7").append(Component.translatable("item.ic2_120.ejector_upgrade")),
                    Component.literal("§7").append(Component.translatable("item.ic2_120.fluid_ejector_upgrade"))
                ), mouseX, mouseY)
            }
        } else {
            // 电量条悬停
            if (relX in ORD_EB_X until ORD_EB_X + ORD_EB_W && relY in ORD_EB_Y until ORD_EB_Y + ORD_EB_H) {
                context.renderTooltip(font, Component.literal("${EnergyFormatUtils.formatRaw(energy)} / ${EnergyFormatUtils.formatRaw(cap)} EU"), mouseX, mouseY)
            }
            // uptips 悬停
            if (relX in UPTIPS_X until UPTIPS_X + UPTIPS_SIZE && relY in UPTIPS_Y until UPTIPS_Y + UPTIPS_SIZE) {
                context.renderComponentTooltip(font, listOf(
                    Component.translatable("gui.ic2_120.miner.uptips"),
                    Component.literal("§7${EnergyFormatUtils.formatRaw(energy)} / ${EnergyFormatUtils.formatRaw(cap)} EU"),
                    Component.literal("§7").append(Component.translatable("item.ic2_120.energy_storage_upgrade")),
                    Component.literal("§7").append(Component.translatable("item.ic2_120.transformer_upgrade")),
                    Component.literal("§7").append(Component.translatable("item.ic2_120.ejector_upgrade")),
                    Component.literal("§7").append(Component.translatable("item.ic2_120.pulling_upgrade")),
                    Component.literal("§7").append(Component.translatable("item.ic2_120.fluid_ejector_upgrade")),
                    Component.literal("§7").append(Component.translatable("item.ic2_120.fluid_pulling_upgrade"))
                ), mouseX, mouseY)
            }
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (menu.isAdvanced && button == 0) {
            val relX = (mouseX - leftPos).toInt()
            val relY = (mouseY - topPos).toInt()
            if (relX in 123 until 142 && relY in 27 until 43) {
                Minecraft.getInstance().connection?.send(ServerboundContainerButtonClickPacket(menu.containerId, MinerScreenHandler.BUTTON_TOGGLE_MODE))
                return true
            }
            if (relX in 129 until 148 && relY in 45 until 61) {
                Minecraft.getInstance().connection?.send(ServerboundContainerButtonClickPacket(menu.containerId, MinerScreenHandler.BUTTON_TOGGLE_SILK))
                return true
            }
            if (relX in 133 until 170 && relY in 101 until 117) {
                Minecraft.getInstance().connection?.send(ServerboundContainerButtonClickPacket(menu.containerId, MinerScreenHandler.BUTTON_RESTART))
                return true
            }
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    companion object {
        private val TEX_ORDINARY = Identifier.fromNamespaceAndPath("ic2", "textures/gui/guiminer.png")
        private val TEX_ADVANCED = Identifier.fromNamespaceAndPath("ic2", "textures/gui/guiadvminer.png")
        private val UPTIPS_TEX = Identifier.fromNamespaceAndPath("ic2", "textures/gui/uptips.png")
        private const val TEXTURE_SIZE = 256
        private const val UPTIPS_X = 4
        private const val UPTIPS_Y = 4
        private const val UPTIPS_SIZE = 16

        // 普通: 电量条 (179,2)-(193,17) = 14x15, 渲染至 130,42
        private const val ORD_EB_U = 179; private const val ORD_EB_V = 2
        private const val ORD_EB_W = 14; private const val ORD_EB_H = 15
        private const val ORD_EB_X = 129; private const val ORD_EB_Y = 40

        // 高级：闪电能量槽 (12,55)
        private const val ADV_EB_U = 179; private const val ADV_EB_V = 3
        private const val ADV_EB_W = 14; private const val ADV_EB_H = 15
        private const val ADV_EB_X = 12; private const val ADV_EB_Y = 55
    }
}
