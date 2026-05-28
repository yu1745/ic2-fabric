package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.t
import ic2_120.content.block.OreWashingPlantBlock
import ic2_120.content.screen.OreWashingPlantScreenHandler
import ic2_120.content.sync.OreWashingPlantSync
import ic2_120.registry.annotation.ModScreen
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.texture.SpriteAtlasTexture
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = OreWashingPlantBlock::class)
class OreWashingPlantScreen(
    handler: OreWashingPlantScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<OreWashingPlantScreenHandler>(handler, playerInventory, title) {

    private val waterSprite by lazy {
        MinecraftClient.getInstance()
            .getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
            .apply(WATER_STILL_ID)
    }

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
        val progressFrac = if (OreWashingPlantSync.PROGRESS_MAX > 0) {
            (handler.sync.progress.coerceIn(0, OreWashingPlantSync.PROGRESS_MAX).toFloat() / OreWashingPlantSync.PROGRESS_MAX).coerceIn(0f, 1f)
        } else 0f
        val waterAmountDroplets = handler.sync.waterAmount.toLong().coerceAtLeast(0)
        val waterCapacityDroplets = FluidConstants.BUCKET * 8
        val waterFraction = if (waterCapacityDroplets > 0) (waterAmountDroplets.toFloat() / waterCapacityDroplets).coerceIn(0f, 1f) else 0f
        val waterAmountMb = waterAmountDroplets / DROPLETS_PER_MB
        val waterCapacityMb = (waterCapacityDroplets / DROPLETS_PER_MB).toLong()
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val consumeRate = handler.sync.getSyncedConsumedAmount()

        // 电量条 (179,3)-(193,17) = 14x14，自下而上
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

        // 储水纹理 (64,23)-(76,70) = 12x47，自下而上
        if (waterAmountDroplets > 0) {
            val fillHeight = (WATER_H * waterFraction).toInt().coerceAtLeast(1)
            context.enableScissor(
                left + WATER_X,
                top + WATER_Y + WATER_H - fillHeight,
                left + WATER_X + WATER_W,
                top + WATER_Y + WATER_H
            )
            context.drawSprite(left + WATER_X, top + WATER_Y, 0, WATER_W, WATER_H, waterSprite, WATER_R, WATER_G, WATER_B, 1f)
            context.disableScissor()
        }

        // 容量标示纹理 (182,48)-(193,94) = 11x46，有流体时渲染在流体纹理之上
        if (waterAmountDroplets > 0) {
            context.drawTexture(
                TEXTURE, left + TANK_OVERLAY_X, top + TANK_OVERLAY_Y,
                TANK_OVERLAY_U.toFloat(), TANK_OVERLAY_V.toFloat(),
                TANK_OVERLAY_W, TANK_OVERLAY_H,
                TEXTURE_SIZE, TEXTURE_SIZE
            )
        }

        // 工作进度 (179,20)-(199,39) = 20x19，自左向右
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

        // 储水槽悬停
        if (relX in WATER_X until WATER_X + WATER_W &&
            relY in WATER_Y until WATER_Y + WATER_H
        ) {
            val lines = if (waterAmountDroplets > 0) listOf(Text.literal("水"), Text.literal("${"%,d".format(waterAmountMb)} / ${"%,d".format(waterCapacityMb)} mB"))
                        else listOf(Text.literal("空"))
            context.drawTooltip(textRenderer, lines, mouseX, mouseY)
        }

        // uptips 悬停
        if (relX in UPTIPS_X until UPTIPS_X + UPTIPS_SIZE &&
            relY in UPTIPS_Y until UPTIPS_Y + UPTIPS_SIZE
        ) {
            context.drawTooltip(
                textRenderer,
                listOf(
                    Text.translatable("gui.ic2_120.ore_washing_plant.uptips"),
                    Text.literal("§7").append(Text.translatable("item.ic2_120.overclocker_upgrade")),
                    Text.literal("§7").append(Text.translatable("item.ic2_120.transformer_upgrade")),
                    Text.literal("§7").append(Text.translatable("item.ic2_120.energy_storage_upgrade")),
                    Text.literal("§7").append(Text.translatable("item.ic2_120.ejector_upgrade")),
                    Text.literal("§7").append(Text.translatable("item.ic2_120.fluid_ejector_upgrade")),
                    Text.literal("§7").append(Text.translatable("item.ic2_120.fluid_pulling_upgrade"))
                ),
                mouseX, mouseY
            )
        }
    }

    companion object {
        private val TEXTURE = Identifier("ic2", "textures/gui/guiorewashingplant.png")
        private val UPTIPS_TEXTURE = Identifier("ic2", "textures/gui/uptips.png")
        private val WATER_STILL_ID = Identifier("minecraft", "block/water_still")
        private const val TEXTURE_SIZE = 256
        private val DROPLETS_PER_MB = (FluidConstants.BUCKET / 1000).toInt()

        // 电量条 (179,3)-(193,17) = 14x14
        private const val ENERGY_BAR_U = 179
        private const val ENERGY_BAR_V = 3
        private const val ENERGY_BAR_W = 14
        private const val ENERGY_BAR_H = 14
        private const val ENERGY_BAR_X = 9
        private const val ENERGY_BAR_Y = 42

        // 储水槽 (64,23)-(76,70) = 12x47
        private const val WATER_X = 64
        private const val WATER_Y = 23
        private const val WATER_W = 12
        private const val WATER_H = 47
        private const val WATER_R = 0.25f
        private const val WATER_G = 0.45f
        private const val WATER_B = 0.95f

        // 容量标示 (182,48)-(193,94) = 11x46
        private const val TANK_OVERLAY_U = 182
        private const val TANK_OVERLAY_V = 48
        private const val TANK_OVERLAY_W = 11
        private const val TANK_OVERLAY_H = 46
        private const val TANK_OVERLAY_X = 65
        private const val TANK_OVERLAY_Y = 24

        // 工作进度 (179,20)-(199,39) = 20x19
        private const val PROGRESS_U = 179
        private const val PROGRESS_V = 20
        private const val PROGRESS_W = 20
        private const val PROGRESS_H = 19
        private const val PROGRESS_X = 102
        private const val PROGRESS_Y = 37

        // uptips
        private const val UPTIPS_X = 4
        private const val UPTIPS_Y = 4
        private const val UPTIPS_SIZE = 16
    }
}
