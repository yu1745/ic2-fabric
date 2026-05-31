package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.t
import ic2_120.content.block.SolidCannerBlock
import ic2_120.content.screen.SolidCannerScreenHandler
import ic2_120.content.sync.SolidCannerSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = SolidCannerBlock::class)
class SolidCannerScreen(
    handler: SolidCannerScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<SolidCannerScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = 176
        backgroundHeight = 166
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, backgroundWidth, backgroundHeight, TEXTURE_SIZE, TEXTURE_SIZE)
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
        val progressFrac = if (SolidCannerSync.PROGRESS_MAX > 0) {
            (handler.sync.progress.coerceIn(0, SolidCannerSync.PROGRESS_MAX).toFloat() / SolidCannerSync.PROGRESS_MAX).coerceIn(0f, 1f)
        } else 0f
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val consumeRate = handler.sync.getSyncedConsumedAmount()

        // 电量条 (179,3)-(193,17) = 14×14，自下而上
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

        // 进度条 (179,20)-(201,35) = 22×15，自左向右
        if (progressFrac > 0f) {
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

        // uptips 悬停
        if (relX in UPTIPS_X until UPTIPS_X + UPTIPS_SIZE &&
            relY in UPTIPS_Y until UPTIPS_Y + UPTIPS_SIZE
        ) {
            context.drawTooltip(
                textRenderer,
                listOf(
                    Text.translatable("gui.ic2_120.solid_canner.uptips"),
                    Text.literal(" §7").append(Text.translatable("item.ic2_120.overclocker_upgrade")),
                    Text.literal(" §7").append(Text.translatable("item.ic2_120.transformer_upgrade")),
                    Text.literal(" §7").append(Text.translatable("item.ic2_120.energy_storage_upgrade")),
                    Text.literal(" §7").append(Text.translatable("item.ic2_120.ejector_upgrade")),
                    Text.literal(" §7").append(Text.translatable("item.ic2_120.pulling_upgrade"))
                ),
                mouseX, mouseY
            )
        }
    }

    companion object {
        private val TEXTURE = Identifier("ic2", "textures/gui/guisolidcanner.png")
        private val UPTIPS_TEXTURE = Identifier("ic2", "textures/gui/uptips.png")
        private const val TEXTURE_SIZE = 256

        // 电量条 (179,3)-(193,17) = 14×14
        private const val ENERGY_BAR_U = 179
        private const val ENERGY_BAR_V = 3
        private const val ENERGY_BAR_W = 14
        private const val ENERGY_BAR_H = 14
        private const val ENERGY_BAR_X = 9
        private const val ENERGY_BAR_Y = 45

        // 进度条 (179,20)-(201,35) = 22×15
        private const val PROGRESS_U = 179
        private const val PROGRESS_V = 20
        private const val PROGRESS_W = 22
        private const val PROGRESS_H = 15
        private const val PROGRESS_X = 86
        private const val PROGRESS_Y = 36

        // uptips
        private const val UPTIPS_X = 4
        private const val UPTIPS_Y = 4
        private const val UPTIPS_SIZE = 16
    }
}
