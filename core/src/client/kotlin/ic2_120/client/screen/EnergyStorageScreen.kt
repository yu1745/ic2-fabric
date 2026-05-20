package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.t
import ic2_120.content.block.storage.EnergyStorageConfig
import ic2_120.content.screen.EnergyStorageScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(
    handlers = [
        "batbox", "cesu", "mfe", "mfsu",
        "batbox_chargepad", "cesu_chargepad", "mfe_chargepad", "mfsu_chargepad"
    ]
)
class EnergyStorageScreen(
    handler: EnergyStorageScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<EnergyStorageScreenHandler>(handler, playerInventory, title) {

    private val capacity: Long = resolveCapacity()
    private val useEquipmentSlots: Boolean = resolveUseEquipmentSlots()

    private fun resolveCapacity(): Long {
        return handler.context.get({ world, pos ->
            val block = world.getBlockState(pos).block
            val id = Registries.BLOCK.getId(block)
            EnergyStorageConfig.fromBlockPath(id.path)?.capacity ?: EnergyStorageConfig.BATBOX.capacity
        }, EnergyStorageConfig.BATBOX.capacity)
    }

    private fun resolveUseEquipmentSlots(): Boolean {
        return handler.context.get({ world, pos ->
            val block = world.getBlockState(pos).block
            val id = Registries.BLOCK.getId(block)
            EnergyStorageConfig.fromBlockPath(id.path)?.useEquipmentSlots ?: false
        }, false)
    }

    init {
        backgroundWidth = 179
        backgroundHeight = 196
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, backgroundWidth, backgroundHeight, TEXTURE_SIZE, TEXTURE_SIZE)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        val left = x
        val top = y
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val outputRate = handler.sync.getSyncedExtractedAmount()
        val cap = capacity
        val fraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f

        if (fraction > 0f) {
            val fillWidth = (ENERGY_BAR_W * fraction).toInt().coerceAtLeast(1)
            context.enableScissor(
                left + ENERGY_BAR_X,
                top + ENERGY_BAR_Y,
                left + ENERGY_BAR_X + fillWidth,
                top + ENERGY_BAR_Y + ENERGY_BAR_H
            )
            context.drawTexture(
                TEXTURE,
                left + ENERGY_BAR_X, top + ENERGY_BAR_Y,
                ENERGY_BAR_U.toFloat(), ENERGY_BAR_V.toFloat(),
                ENERGY_BAR_W, ENERGY_BAR_H,
                TEXTURE_SIZE, TEXTURE_SIZE
            )
            context.disableScissor()
        }

        val relX = mouseX - left
        val relY = mouseY - top
        if (relX in ENERGY_BAR_X until ENERGY_BAR_X + ENERGY_BAR_W &&
            relY in ENERGY_BAR_Y until ENERGY_BAR_Y + ENERGY_BAR_H
        ) {
            context.drawTooltip(
                textRenderer,
                Text.literal("${EnergyFormatUtils.formatEu(energy)} / ${EnergyFormatUtils.formatEu(cap)} EU"),
                mouseX, mouseY
            )
        }

        if (!useEquipmentSlots) {
            context.drawTexture(
                TEXTURE, left + OVERLAY_X, top + OVERLAY_Y,
                OVERLAY_U.toFloat(), OVERLAY_V.toFloat(),
                OVERLAY_W, OVERLAY_H,
                TEXTURE_SIZE, TEXTURE_SIZE
            )
        }

        val inputText = t("gui.ic2_120.input_eu", EnergyFormatUtils.formatEu(inputRate))
        val outputText = t("gui.ic2_120.output_eu", EnergyFormatUtils.formatEu(outputRate))
        val sideTextWidth = maxOf(textRenderer.getWidth(inputText), textRenderer.getWidth(outputText))
        val sideTextX = left - sideTextWidth - 4
        context.drawText(textRenderer, inputText, sideTextX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, outputText, sideTextX, top + 20, 0xAAAAAA, false)

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    companion object {
        private val TEXTURE = Identifier("ic2", "textures/gui/guielectricblock.png")
        private const val TEXTURE_SIZE = 256
        private const val ENERGY_BAR_U = 180
        private const val ENERGY_BAR_V = 4
        private const val ENERGY_BAR_W = 24
        private const val ENERGY_BAR_H = 16
        private const val ENERGY_BAR_X = 79
        private const val ENERGY_BAR_Y = 35
        private const val OVERLAY_U = 179
        private const val OVERLAY_V = 23
        private const val OVERLAY_W = 72
        private const val OVERLAY_H = 18
        private const val OVERLAY_X = 7
        private const val OVERLAY_Y = 83
    }
}