package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.t
import ic2_120.content.block.InductionFurnaceBlock
import ic2_120.content.screen.InductionFurnaceScreenHandler
import ic2_120.content.sync.InductionFurnaceSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = InductionFurnaceBlock::class)
class InductionFurnaceScreen(
    handler: InductionFurnaceScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<InductionFurnaceScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = 176
        backgroundHeight = 166
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, backgroundWidth, backgroundHeight, TEXTURE_SIZE, TEXTURE_SIZE)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)

        val left = x
        val top = y

        context.drawText(textRenderer, title, left + (backgroundWidth - textRenderer.getWidth(title)) / 2, top + 6, 0x404040, false)

        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1)
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val consumeRate = handler.sync.getSyncedConsumedAmount()
        val heatPercent = handler.sync.heat / 100

        // 电量条 (181,4)-(195,18) = 14×14，自下而上
        if (energyFraction > 0f) {
            val fillHeight = (ENERGY_BAR_H * energyFraction).toInt().coerceAtLeast(1)
            context.enableScissor(
                left + ENERGY_BAR_X,
                top + ENERGY_BAR_Y + ENERGY_BAR_H - fillHeight,
                left + ENERGY_BAR_X + ENERGY_BAR_W,
                top + ENERGY_BAR_Y + ENERGY_BAR_H
            )
            context.drawTexture(
                TEXTURE, left + ENERGY_BAR_X, top + ENERGY_BAR_Y,
                ENERGY_BAR_U.toFloat(), ENERGY_BAR_V.toFloat(),
                ENERGY_BAR_W, ENERGY_BAR_H,
                TEXTURE_SIZE, TEXTURE_SIZE
            )
            context.disableScissor()
        }

        // 工作进度纹理 (181,23)-(203,38) = 22×15，自左向右（仅处理物品时渲染）
        if (handler.sync.progressSlot0 > 0) {
            val baseTicks = InductionFurnaceSync.BASE_TICKS_PER_OPERATION
            val heat = handler.sync.heat.coerceAtLeast(InductionFurnaceSync.MIN_HEAT_THRESHOLD)
            val progressNeeded = (baseTicks * InductionFurnaceSync.HEAT_MAX / heat).toInt().coerceAtLeast(baseTicks.toInt())
            val progressFrac = (handler.sync.progressSlot0.coerceIn(0, progressNeeded).toFloat() / progressNeeded).coerceIn(0f, 1f)
            val arrowWidth = (PROGRESS_W * progressFrac).toInt().coerceAtLeast(1)
            context.enableScissor(
                left + PROGRESS_X,
                top + PROGRESS_Y,
                left + PROGRESS_X + arrowWidth,
                top + PROGRESS_Y + PROGRESS_H
            )
            context.drawTexture(
                TEXTURE, left + PROGRESS_X, top + PROGRESS_Y,
                PROGRESS_U.toFloat(), PROGRESS_V.toFloat(),
                PROGRESS_W, PROGRESS_H,
                TEXTURE_SIZE, TEXTURE_SIZE
            )
            context.disableScissor()
        }

        // uptips 纹理
        context.drawTexture(
            UPTIPS_TEXTURE, left + UPTIPS_X, top + UPTIPS_Y,
            0f, 0f, UPTIPS_SIZE, UPTIPS_SIZE,
            UPTIPS_SIZE, UPTIPS_SIZE
        )

        // 热量文本
        context.drawText(
            textRenderer,
            Text.literal("热量：${heatPercent}%"),
            left + 64, top + 64, 0xFFFFFF, false
        )

        // 侧边文本
        val inputText = t("gui.ic2_120.input_eu", EnergyFormatUtils.formatRaw(inputRate))
        val consumeText = t("gui.ic2_120.consume_eu", EnergyFormatUtils.formatRaw(consumeRate))
        val sideTextWidth = maxOf(textRenderer.getWidth(inputText), textRenderer.getWidth(consumeText))
        val sideTextX = left - sideTextWidth - 4
        context.drawText(textRenderer, inputText, sideTextX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, consumeText, sideTextX, top + 20, 0xAAAAAA, false)

        drawMouseoverTooltip(context, mouseX, mouseY)

        // 悬停提示
        val relX = mouseX - left
        val relY = mouseY - top

        // uptips 悬停
        if (relX in UPTIPS_X until UPTIPS_X + UPTIPS_SIZE &&
            relY in UPTIPS_Y until UPTIPS_Y + UPTIPS_SIZE
        ) {
            context.drawTooltip(
                textRenderer,
                listOf(
                    Text.translatable("gui.ic2_120.induction_furnace.uptips"),
                    Text.literal(" §7").append(Text.translatable("item.ic2_120.ejector_upgrade")),
                    Text.literal(" §7").append(Text.translatable("item.ic2_120.pulling_upgrade"))
                ),
                mouseX, mouseY
            )
        }

        // 电量条悬停
        if (relX in ENERGY_BAR_X until ENERGY_BAR_X + ENERGY_BAR_W &&
            relY in ENERGY_BAR_Y until ENERGY_BAR_Y + ENERGY_BAR_H
        ) {
            context.drawTooltip(
                textRenderer,
                Text.literal("储能：${EnergyFormatUtils.formatRaw(energy)} / ${EnergyFormatUtils.formatRaw(cap)} EU"),
                mouseX, mouseY
            )
        }
    }

    companion object {
        private val TEXTURE = Identifier.of("ic2", "textures/gui/guiinductionfurnace.png")
        private val UPTIPS_TEXTURE = Identifier.of("ic2", "textures/gui/uptips.png")
        private const val TEXTURE_SIZE = 256

        // uptips
        private const val UPTIPS_X = 4
        private const val UPTIPS_Y = 4
        private const val UPTIPS_SIZE = 16

        // 电量条 (181,4)-(195,18) = 14×14
        private const val ENERGY_BAR_U = 181
        private const val ENERGY_BAR_V = 4
        private const val ENERGY_BAR_W = 14
        private const val ENERGY_BAR_H = 14
        private const val ENERGY_BAR_X = 37
        private const val ENERGY_BAR_Y = 35

        // 工作进度 (181,23)-(203,38) = 22×15
        private const val PROGRESS_U = 181
        private const val PROGRESS_V = 23
        private const val PROGRESS_W = 22
        private const val PROGRESS_H = 15
        private const val PROGRESS_X = 61
        private const val PROGRESS_Y = 34
    }
}
