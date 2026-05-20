package ic2_120.client.screen

import ic2_120.content.block.BlastFurnaceBlock
import ic2_120.content.screen.BlastFurnaceScreenHandler
import ic2_120.content.sync.BlastFurnaceSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = BlastFurnaceBlock::class)
class BlastFurnaceScreen(
    handler: BlastFurnaceScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<BlastFurnaceScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = 176
        backgroundHeight = 166
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, 176, 166, 256, 256)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)

        val temperature = handler.sync.temperature.coerceIn(0, BlastFurnaceSync.TEMP_MAX)
        val tempFrac = temperature.toFloat() / BlastFurnaceSync.TEMP_MAX

        val progress = handler.sync.progress.coerceAtLeast(0)
        val progressMax = getProgressMax(temperature)
        val progressFrac = if (progressMax > 0 && progress > 0) progress.toFloat() / progressMax else 0f

        val airAmount = handler.sync.airAmountMb.coerceIn(0, BlastFurnaceSync.AIR_CAPACITY_MB)
        val airFrac = airAmount.toFloat() / BlastFurnaceSync.AIR_CAPACITY_MB

        val huInput = handler.sync.huInput.coerceAtLeast(0)

        val canWork = temperature >= BlastFurnaceSync.TEMP_WORK_MIN
        val isWorking = progress > 0

        // 标题居中于 y=6
        context.drawText(textRenderer, title, x + (176 - textRenderer.getWidth(title)) / 2, y + 6, 0x404040, false)

        // 升级提示图标位于 (35, 4)，16×16
        context.drawTexture(UPTIPS, x + 35, y + 4, 0f, 0f, 16, 16, 16, 16)

        // 热量条（温度）：源 (180,22)-(202,29) 22×7→目标 (71,70)-(82,77) 11×7，自左向右填充
        drawHeatGauge(context, x + 71, y + 70, tempFrac)

        // 热量输入指示器：源 (180,4)-(193,17) 13×13，目标 (96,67)，HU 达标时渲染
        if (huInput >= getHuPerTick(temperature)) {
            context.drawTexture(TEXTURE, x + 96, y + 67, 180f, 4f, 13, 13, 256, 256)
        }

        // 温度达标指示器（可工作）：源 (179,33)-(206,60) 27×27，目标 (75,34)-(102,61)
        if (canWork) {
            context.drawTexture(TEXTURE, x + 75, y + 34, 179f, 33f, 27, 27, 256, 256)
        }

        // 工作状态覆盖：源 (179,64)-(206,91) 27×27，目标 (75,34)-(102,61)，自底向上填充
        if (isWorking) {
            drawWorkingState(context, x + 75, y + 34, progressFrac)
        }

        // 空气流体渲染（次一层）
        if (airAmount > 0) {
            drawAirFluid(context, x + 11, y + 11, airFrac)
        }

        // 容量标示纹理：源 (181,95)-(192,141) 11×46，目标 (12,11)-(23,57)，有空气时渲染（顶层）
        if (airAmount > 0) {
            context.drawTexture(TEXTURE, x + 12, y + 11, 181f, 95f, 11, 46, 256, 256)
        }

        // 缺失压缩空气纹理：源 (206,3)-(228,25) 22×22，目标 (109,38)-(131,60)，无空气时渲染
        if (airAmount <= 0) {
            context.drawTexture(TEXTURE, x + 109, y + 38, 206f, 3f, 22, 22, 256, 256)
        }

        // 空气表工具提示：区域 (11,11)-(23,58)，仅当空气 > 0 时显示
        if (airAmount > 0 && mouseX in (x + 11) until (x + 23) && mouseY in (y + 11) until (y + 58)) {
            val airTooltip = Text.translatable("gui.ic2_120.blast_furnace.air_tooltip",
                airAmount, BlastFurnaceSync.AIR_CAPACITY_MB)
            context.drawTooltip(textRenderer, airTooltip, mouseX, mouseY)
        }

        // 温度条工具提示：区域 (71,70)-(92,77)
        if (mouseX in (x + 71) until (x + 92) && mouseY in (y + 70) until (y + 77)) {
            val heatTooltip = Text.translatable("gui.ic2_120.blast_furnace.heat_tooltip",
                temperature, BlastFurnaceSync.TEMP_MAX)
            context.drawTooltip(textRenderer, heatTooltip, mouseX, mouseY)
        }

        // 升级提示工具提示
        if (mouseX in (x + 35) until (x + 35 + 16) && mouseY in (y + 4) until (y + 4 + 16)) {
            val upgradeTooltip = mutableListOf<Text>()
            upgradeTooltip.add(Text.translatable("gui.ic2_120.upgrade_slots"))
            for (id in SUPPORTED_UPGRADES) {
                val item = Registries.ITEM.get(Identifier.of("ic2_120", id))
                upgradeTooltip.add(item.name)
            }
            context.drawTooltip(textRenderer, upgradeTooltip, mouseX, mouseY)
        }

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun getProgressMax(temp: Int): Int = when {
        temp >= 1601 -> BlastFurnaceSync.PROGRESS_MAX_FAST
        temp >= 1501 -> BlastFurnaceSync.PROGRESS_MAX_MEDIUM
        else -> BlastFurnaceSync.PROGRESS_MAX_SLOW
    }

    private fun getHuPerTick(temp: Int): Int = when {
        temp >= 1601 -> 40
        temp >= 1501 -> 60
        temp >= 1401 -> 80
        else -> 100
    }

    /** 压缩空气色块渲染：RGB(53,81,92)，区域 (11,11)-(23,58) 12×47，自底向上填充。 */
    private fun drawAirFluid(context: DrawContext, gx: Int, gy: Int, fraction: Float) {
        val barW = 12
        val barH = 47
        val fillH = (fraction * barH).toInt()
        if (fillH <= 0) return
        val topY = gy + barH - fillH
        context.fill(gx, topY, gx + barW, gy + barH, AIR_COLOR)
    }

    /** 热量条：21×7（从 22×7 源纹理采样），自左向右填充。 */
    private fun drawHeatGauge(context: DrawContext, gx: Int, gy: Int, fraction: Float) {
        val barW = 21
        val barH = 7
        val fillW = (fraction * barW).toInt()
        if (fillW <= 0) return
        context.enableScissor(gx, gy, gx + fillW, gy + barH)
        context.drawTexture(TEXTURE, gx, gy, 180f, 22f, barW, barH, 256, 256)
        context.disableScissor()
    }

    /** 工作状态覆盖：27×27，自底向上填充。 */
    private fun drawWorkingState(context: DrawContext, gx: Int, gy: Int, fraction: Float) {
        val barW = 27
        val barH = 27
        val fillH = (fraction * barH).toInt()
        if (fillH <= 0) return
        context.enableScissor(gx, gy + barH - fillH, gx + barW, gy + barH)
        context.drawTexture(TEXTURE, gx, gy, 179f, 64f, barW, barH, 256, 256)
        context.disableScissor()
    }

    companion object {
        private val TEXTURE = Identifier.of("ic2", "textures/gui/guiblockcutter.png")
        private val UPTIPS = Identifier.of("ic2", "textures/gui/uptips.png")
        private const val AIR_COLOR = 0xFF35515C.toInt()
        private val SUPPORTED_UPGRADES = listOf(
            "ejector_upgrade",
            "pulling_upgrade"
        )
    }
}
