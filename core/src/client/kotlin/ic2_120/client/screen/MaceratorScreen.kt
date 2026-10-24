package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.t
import ic2_120.content.block.MaceratorBlock
import ic2_120.content.screen.MaceratorScreenHandler
import ic2_120.content.sync.MaceratorSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

@ModScreen(block = MaceratorBlock::class)
class MaceratorScreen(
    handler: MaceratorScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<MaceratorScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = 176
        backgroundHeight = 166
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, 176, 166, 256, 256)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        // 升级提示图标位于 (4, 4) — 16×16 纹理来自 uptips.png (1,1) 至结束
        context.drawTexture(UPTIPS, x + 4, y + 4, 1f, 1f, 16, 16, 16, 16)

        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1)
        val energyFrac = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val progressFrac = if (MaceratorSync.PROGRESS_MAX > 0) {
            (handler.sync.progress.coerceIn(0, MaceratorSync.PROGRESS_MAX)
                .toFloat() / MaceratorSync.PROGRESS_MAX).coerceIn(0f, 1f)
        } else 0f
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val consumeRate = handler.sync.getSyncedConsumedAmount()

        // 能量条位于 (44, 36) — 14×14 纹理来自 scrapboxrecipes.png (178,2)-(191,15)
        drawEnergyGauge(context, x + 43, y + 36, energyFrac)

        // 进度条位于 (76, 38) — 21×11 纹理来自 scrapboxrecipes.png (178,18)-(198,28)
        drawProgressGauge(context, x + 74, y + 38, progressFrac)

        // 标题文字居中于 y=6
        context.drawText(textRenderer, title, x + (176 - textRenderer.getWidth(title)) / 2, y + 6, 0x404040, false)

        // EU输入/消耗文字显示在左侧
        val inputText = t("gui.ic2_120.input_eu", EnergyFormatUtils.formatRaw(inputRate))
        val consumeText = t("gui.ic2_120.consume_eu", EnergyFormatUtils.formatRaw(consumeRate))
        val sideTextWidth = maxOf(textRenderer.getWidth(inputText), textRenderer.getWidth(consumeText))
        val sideTextX = x - sideTextWidth - 4
        context.drawText(textRenderer, inputText, sideTextX, y + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, consumeText, sideTextX, y + 20, 0xAAAAAA, false)

        // 鼠标悬停能量条时显示电量信息
        if (mouseX in (x + 44) until (x + 44 + 14) && mouseY in (y + 36) until (y + 36 + 14)) {
            val energyTooltip = listOf(
                Text.translatable("gui.ic2_120.energy_tooltip",
                    EnergyFormatUtils.formatRaw(energy),
                    EnergyFormatUtils.formatRaw(cap))
            )
            context.drawTooltip(textRenderer, energyTooltip, mouseX, mouseY)
        }

        // 鼠标悬停升级提示图标时显示支持的升级组件
        if (mouseX in (x + 4) until (x + 4 + 16) && mouseY in (y + 4) until (y + 4 + 16)) {
            val upgradeTooltip = mutableListOf<Text>()
            upgradeTooltip.add(Text.translatable("gui.ic2_120.upgrade_slots"))
            for (id in SUPPORTED_UPGRADES) {
                val item = Registries.ITEM.get(Identifier("ic2_120", id))
                upgradeTooltip.add(item.name)
            }
            context.drawTooltip(textRenderer, upgradeTooltip, mouseX, mouseY)
        }

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun drawEnergyGauge(context: DrawContext, gx: Int, gy: Int, fraction: Float) {
        val barW = 14
        val barH = 14
        val fillH = (fraction.coerceIn(0f, 1f) * barH).toInt()
        if (fillH <= 0) return
        context.enableScissor(gx, gy + barH - fillH, gx + barW, gy + barH)
        context.drawTexture(TEXTURE, gx, gy, 177f, 1f, barW, barH, 256, 256)
        context.disableScissor()
    }

    private fun drawProgressGauge(context: DrawContext, gx: Int, gy: Int, fraction: Float) {
        val barW = 21
        val barH = 11
        val fillW = (fraction.coerceIn(0f, 1f) * barW).toInt()
        if (fillW <= 0) return
        context.enableScissor(gx, gy, gx + fillW, gy + barH)
        context.drawTexture(TEXTURE, gx, gy, 178f, 18f, barW, barH, 256, 256)
        context.disableScissor()
    }

    companion object {
        private val TEXTURE = Identifier("ic2", "textures/gui/scrapboxrecipes.png")
        private val UPTIPS = Identifier("ic2", "textures/gui/uptips.png")
        private val SUPPORTED_UPGRADES = listOf(
            "overclocker_upgrade",
            "transformer_upgrade",
            "energy_storage_upgrade",
            "ejector_upgrade",
            "pulling_upgrade"
        )
    }
}
