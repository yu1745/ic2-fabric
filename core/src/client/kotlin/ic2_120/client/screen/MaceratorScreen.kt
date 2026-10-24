package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.t
import ic2_120.content.block.MaceratorBlock
import ic2_120.content.screen.MaceratorScreenHandler
import ic2_120.content.sync.MaceratorSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Inventory
import net.minecraft.network.chat.Component
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation

@ModScreen(block = MaceratorBlock::class)
class MaceratorScreen(
    val handler: MaceratorScreenHandler,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<MaceratorScreenHandler>(handler, playerInventory, title) {

    init {
        imageWidth = 176
        imageHeight = 166
    }

    override fun drawBackground(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.blit(TEXTURE, leftPos, topPos, 0f, 0f, 176, 166, 256, 256)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        // 升级提示图标位于 (4, 4) — 16×16 纹理来自 uptips.png (1,1) 至结束
        context.blit(UPTIPS, leftPos + 4, topPos + 4, 1f, 1f, 16, 16, 16, 16)

        val energy = menu.sync.energy.toLong().coerceAtLeast(0)
        val cap = menu.sync.energyCapacity.toLong().coerceAtLeast(1)
        val energyFrac = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val progressFrac = if (MaceratorSync.PROGRESS_MAX > 0) {
            (menu.sync.progress.coerceIn(0, MaceratorSync.PROGRESS_MAX)
                .toFloat() / MaceratorSync.PROGRESS_MAX).coerceIn(0f, 1f)
        } else 0f
        val inputRate = menu.sync.getSyncedInsertedAmount()
        val consumeRate = menu.sync.getSyncedConsumedAmount()

        // 能量条位于 (44, 36) — 14×14 纹理来自 scrapboxrecipes.png (178,2)-(191,15)
        drawEnergyGauge(context, leftPos + 43, topPos + 36, energyFrac)

        // 进度条位于 (76, 38) — 21×11 纹理来自 scrapboxrecipes.png (178,18)-(198,28)
        drawProgressGauge(context, leftPos + 74, topPos + 38, progressFrac)

        // 标题文字居中于 y=6
        context.drawString(font, title, leftPos + (176 - font.width(title)) / 2, topPos + 6, 0x404040, false)

        // EU输入/消耗文字显示在左侧
        val inputText = t("gui.ic2_120.input_eu", EnergyFormatUtils.formatRaw(inputRate))
        val consumeText = t("gui.ic2_120.consume_eu", EnergyFormatUtils.formatRaw(consumeRate))
        val sideTextWidth = maxOf(font.width(inputText), font.width(consumeText))
        val sideTextX = leftPos - sideTextWidth - 4
        context.drawString(font, inputText, sideTextX, topPos + 8, 0xAAAAAA, false)
        context.drawString(font, consumeText, sideTextX, topPos + 20, 0xAAAAAA, false)

        // 鼠标悬停能量条时显示电量信息
        if (mouseX in (leftPos + 44) until (leftPos + 44 + 14) && mouseY in (topPos + 36) until (topPos + 36 + 14)) {
            val energyTooltip = listOf(
                Component.translatable("gui.ic2_120.energy_tooltip",
                    EnergyFormatUtils.formatRaw(energy),
                    EnergyFormatUtils.formatRaw(cap))
            )
            context.renderComponentTooltip(font, energyTooltip, mouseX, mouseY)
        }

        // 鼠标悬停升级提示图标时显示支持的升级组件
        if (mouseX in (leftPos + 4) until (leftPos + 4 + 16) && mouseY in (topPos + 4) until (topPos + 4 + 16)) {
            val upgradeTooltip = mutableListOf<Component>()
            upgradeTooltip.add(Component.translatable("gui.ic2_120.upgrade_slots"))
            for (id in SUPPORTED_UPGRADES) {
                val item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", id))
                upgradeTooltip.add(item.description)
            }
            context.renderComponentTooltip(font, upgradeTooltip, mouseX, mouseY)
        }

        renderTooltip(context, mouseX, mouseY)
    }

    private fun drawEnergyGauge(context: GuiGraphics, gx: Int, gy: Int, fraction: Float) {
        val barW = 14
        val barH = 14
        val fillH = (fraction.coerceIn(0f, 1f) * barH).toInt()
        if (fillH <= 0) return
        context.enableScissor(gx, gy + barH - fillH, gx + barW, gy + barH)
        context.blit(TEXTURE, gx, gy, 177f, 1f, barW, barH, 256, 256)
        context.disableScissor()
    }

    private fun drawProgressGauge(context: GuiGraphics, gx: Int, gy: Int, fraction: Float) {
        val barW = 21
        val barH = 11
        val fillW = (fraction.coerceIn(0f, 1f) * barW).toInt()
        if (fillW <= 0) return
        context.enableScissor(gx, gy, gx + fillW, gy + barH)
        context.blit(TEXTURE, gx, gy, 178f, 18f, barW, barH, 256, 256)
        context.disableScissor()
    }

    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath("ic2", "textures/gui/scrapboxrecipes.png")
        private val UPTIPS = ResourceLocation.fromNamespaceAndPath("ic2", "textures/gui/uptips.png")
        private val SUPPORTED_UPGRADES = listOf(
            "overclocker_upgrade",
            "transformer_upgrade",
            "energy_storage_upgrade",
            "ejector_upgrade",
            "pulling_upgrade"
        )
    }
}
