package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.t
import ic2_120.content.block.MagnetizerBlock
import ic2_120.content.screen.GuiSize
import ic2_120.content.screen.MagnetizerScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = MagnetizerBlock::class)
class MagnetizerScreen(
    handler: MagnetizerScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<MagnetizerScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = GUI_SIZE.width
        backgroundHeight = GUI_SIZE.height
    }

    override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // no-op: panel drawn in render() directly, prevents dark overlay on top of GUI
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, BG_W, BG_H, TEX_SIZE, TEX_SIZE)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)

        val left = x
        val top = y

        context.drawText(textRenderer, title, left + (backgroundWidth - textRenderer.getWidth(title)) / 2, top + 6, 0x404040, false)

        // 能量条 (180,5)-(194,18) = 14×13 → (10,30)，自下而上
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1)
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        if (energyFraction > 0f) {
            val fillH = (ENERGY_BAR_H * energyFraction).toInt().coerceAtLeast(1)
            context.enableScissor(
                left + ENERGY_BAR_X,
                top + ENERGY_BAR_Y + ENERGY_BAR_H - fillH,
                left + ENERGY_BAR_X + ENERGY_BAR_W,
                top + ENERGY_BAR_Y + ENERGY_BAR_H
            )
            context.drawTexture(TEXTURE, left + ENERGY_BAR_X, top + ENERGY_BAR_Y,
                ENERGY_BAR_U.toFloat(), ENERGY_BAR_V.toFloat(),
                ENERGY_BAR_W, ENERGY_BAR_H, TEX_SIZE, TEX_SIZE)
            context.disableScissor()
        }

        // 状态文本
        val hasBoots = handler.sync.isWearingMetalBoots != 0
        val bootsText = if (hasBoots)
            Text.translatable("gui.ic2_120.magnetizer.metal_boots_detected")
        else
            Text.translatable("gui.ic2_120.magnetizer.metal_boots_not_detected")
        val bootsColor = if (hasBoots) 0x55FF55 else 0xFF5555

        context.drawText(textRenderer, bootsText, left + 31, top + 62, bootsColor, false)

        // uptips
        context.drawTexture(UPTIPS_TEXTURE, left + UPTIPS_X, top + UPTIPS_Y, 0f, 0f, UPTIPS_SIZE, UPTIPS_SIZE, UPTIPS_SIZE, UPTIPS_SIZE)

        // 侧边文字
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val consumeRate = handler.sync.getSyncedConsumedAmount()
        val inputText = t("gui.ic2_120.input_eu", EnergyFormatUtils.formatEu(inputRate))
        val consumeText = t("gui.ic2_120.consume_eu", EnergyFormatUtils.formatEu(consumeRate))
        val sideTextWidth = maxOf(textRenderer.getWidth(inputText), textRenderer.getWidth(consumeText))
        val sideTextX = left - sideTextWidth - 4
        context.drawText(textRenderer, inputText, sideTextX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, consumeText, sideTextX, top + 20, 0xAAAAAA, false)

        drawMouseoverTooltip(context, mouseX, mouseY)

        val relX = mouseX - left
        val relY = mouseY - top

        // 能量条悬停
        if (relX in ENERGY_BAR_X until ENERGY_BAR_X + ENERGY_BAR_W &&
            relY in ENERGY_BAR_Y until ENERGY_BAR_Y + ENERGY_BAR_H
        ) {
            context.drawTooltip(textRenderer,
                Text.literal("储能：${EnergyFormatUtils.formatRaw(energy)} / ${EnergyFormatUtils.formatRaw(cap)} EU"),
                mouseX, mouseY)
        }

        // uptips 悬停
        if (relX in UPTIPS_X until UPTIPS_X + UPTIPS_SIZE && relY in UPTIPS_Y until UPTIPS_Y + UPTIPS_SIZE) {
            context.drawTooltip(textRenderer, listOf(
                Text.translatable("gui.ic2_120.magnetizer.uptips"),
                Text.literal("§7").append(Text.translatable("item.ic2_120.overclocker_upgrade")),
                Text.literal("§7").append(Text.translatable("item.ic2_120.transformer_upgrade")),
                Text.literal("§7").append(Text.translatable("item.ic2_120.energy_storage_upgrade")),
                Text.literal("§7").append(Text.translatable("item.ic2_120.redstone_inverter_upgrade"))
            ), mouseX, mouseY)
        }
    }

    companion object {
        private val TEXTURE = Identifier.of("ic2", "textures/gui/guimagnetizer.png")
        private const val TEX_SIZE = 256
        private val GUI_SIZE = GuiSize.STANDARD_UPGRADE

        private const val BG_W = 176
        private const val BG_H = 161
        private const val SLOT_S = 18

        // 能量条 (180,5)-(194,18) = 14×13 → (9,29)
        private const val ENERGY_BAR_U = 180
        private const val ENERGY_BAR_V = 5
        private const val ENERGY_BAR_W = 14
        private const val ENERGY_BAR_H = 13
        private const val ENERGY_BAR_X = 9
        private const val ENERGY_BAR_Y = 29

        // 鞋子槽 (80,32)
        private const val BOOTS_X = 80
        private const val BOOTS_Y = 32

        // uptips (4,4)
        private val UPTIPS_TEXTURE = Identifier.of("ic2", "textures/gui/uptips.png")
        private const val UPTIPS_X = 4
        private const val UPTIPS_Y = 4
        private const val UPTIPS_SIZE = 16
    }
}
