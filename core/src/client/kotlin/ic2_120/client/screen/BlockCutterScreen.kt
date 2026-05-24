package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.t
import ic2_120.content.block.BlockCutterBlock
import ic2_120.content.screen.BlockCutterScreenHandler
import ic2_120.content.sync.BlockCutterSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = BlockCutterBlock::class)
class BlockCutterScreen(
    handler: BlockCutterScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<BlockCutterScreenHandler>(handler, playerInventory, title) {

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
        val progressFrac = (handler.sync.progress.coerceIn(0, BlockCutterSync.PROGRESS_MAX)
            .toFloat() / BlockCutterSync.PROGRESS_MAX).coerceIn(0f, 1f)
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val consumeRate = handler.sync.getSyncedConsumedAmount()

        // 电量条 (180,3)-(194,17) = 14×14，自下而上
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

        // 工作进度纹理 (180,51)-(227,70) = 47×19，自左向右
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

        // 刀片警告纹理 (180,21)-(210,47) = 30×26
        val bladeTooWeak = handler.sync.bladeTooWeak != 0
        val bladeMissing = !handler.getSlot(BlockCutterScreenHandler.SLOT_BLADE_INDEX).hasStack()
        if (bladeTooWeak) {
            context.drawTexture(
                TEXTURE, left + BLADE_WARN_X, top + BLADE_WARN_Y,
                BLADE_WARN_U.toFloat(), BLADE_WARN_V.toFloat(),
                BLADE_WARN_W, BLADE_WARN_H,
                TEXTURE_SIZE, TEXTURE_SIZE
            )
        }

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

        // 刀片警告悬停
        if (bladeTooWeak && relX in BLADE_WARN_X until BLADE_WARN_X + BLADE_WARN_W &&
            relY in BLADE_WARN_Y until BLADE_WARN_Y + BLADE_WARN_H
        ) {
            val warnText = if (bladeMissing) {
                Text.literal("机器无工作刀片！！！")
            } else {
                Text.literal("当前刀片工作硬度不足！！！")
            }
            context.drawTooltip(textRenderer, warnText, mouseX, mouseY)
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
        private val TEXTURE = Identifier.of("ic2", "textures/gui/guiblockcuttingmachine.png")
        private const val TEXTURE_SIZE = 256

        // 电量条 (180,3)-(194,17) = 14×14
        private const val ENERGY_BAR_U = 180
        private const val ENERGY_BAR_V = 3
        private const val ENERGY_BAR_W = 14
        private const val ENERGY_BAR_H = 14
        private const val ENERGY_BAR_X = 26
        private const val ENERGY_BAR_Y = 37

        // 工作进度 (180,51)-(227,70) = 47×19
        private const val PROGRESS_U = 180
        private const val PROGRESS_V = 51
        private const val PROGRESS_W = 47
        private const val PROGRESS_H = 19
        private const val PROGRESS_X = 55
        private const val PROGRESS_Y = 33

        // 刀片警告 (180,21)-(210,47) = 30×26
        private const val BLADE_WARN_U = 180
        private const val BLADE_WARN_V = 21
        private const val BLADE_WARN_W = 30
        private const val BLADE_WARN_H = 26
        private const val BLADE_WARN_X = 63
        private const val BLADE_WARN_Y = 27
    }
}
