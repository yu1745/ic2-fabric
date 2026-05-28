package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.t
import ic2_120.content.block.CropmatronBlock
import ic2_120.content.screen.CropmatronScreenHandler
import ic2_120.content.sync.CropmatronSync
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.texture.SpriteAtlasTexture
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = CropmatronBlock::class)
class CropmatronScreen(
    handler: CropmatronScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<CropmatronScreenHandler>(handler, playerInventory, title) {

    private val waterSprite by lazy {
        MinecraftClient.getInstance()
            .getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
            .apply(WATER_STILL_ID)
    }
    private val weedExSprite by lazy {
        MinecraftClient.getInstance()
            .getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
            .apply(WEED_EX_STILL_ID)
    }

    init {
        backgroundWidth = 179
        backgroundHeight = 191
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, backgroundWidth, backgroundHeight, TEXTURE_SIZE, TEXTURE_SIZE)

        // 肥料纹理 (181,23)-(193,34) = 12x11，居中渲染至7个肥料槽
        for (i in 0 until 7) {
            context.drawTexture(
                TEXTURE,
                x + FERTILIZER_SLOTS_X + i * 18 + FERTILIZER_ICON_OFFSET_X,
                y + FERTILIZER_SLOTS_Y + FERTILIZER_ICON_OFFSET_Y,
                FERTILIZER_ICON_U.toFloat(), FERTILIZER_ICON_V.toFloat(),
                FERTILIZER_ICON_W, FERTILIZER_ICON_H,
                TEXTURE_SIZE, TEXTURE_SIZE
            )
        }
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
        val waterAmount = handler.sync.waterAmount.toLong().coerceAtLeast(0)
        val weedExAmount = handler.sync.weedExAmount.toLong().coerceAtLeast(0)
        val waterFraction = (waterAmount.toFloat() / WATER_TANK_DROPLETS).coerceIn(0f, 1f)
        val weedExFraction = (weedExAmount.toFloat() / WEED_EX_TANK_DROPLETS).coerceIn(0f, 1f)
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val consumeRate = handler.sync.getSyncedConsumedAmount()

        // 电量条 (180,3)-(194,16) = 14x13，自下而上
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

        // 水槽 (11,25)-(35,72) = 24x47，自下而上
        if (waterAmount > 0) {
            val fillHeight = (WATER_TANK_H * waterFraction).toInt().coerceAtLeast(1)
            context.enableScissor(
                left + WATER_TANK_X,
                top + WATER_TANK_Y + WATER_TANK_H - fillHeight,
                left + WATER_TANK_X + WATER_TANK_W,
                top + WATER_TANK_Y + WATER_TANK_H
            )
            context.drawSprite(
                left + WATER_TANK_X, top + WATER_TANK_Y, 0,
                WATER_TANK_W, WATER_TANK_H, waterSprite,
                WATER_R, WATER_G, WATER_B, 1f
            )
            context.disableScissor()
        }

        // Weed-EX 槽 (105,25)-(129,72) = 24x47，自下而上
        if (weedExAmount > 0) {
            val fillHeight = (WEED_EX_TANK_H * weedExFraction).toInt().coerceAtLeast(1)
            context.enableScissor(
                left + WEED_EX_TANK_X,
                top + WEED_EX_TANK_Y + WEED_EX_TANK_H - fillHeight,
                left + WEED_EX_TANK_X + WEED_EX_TANK_W,
                top + WEED_EX_TANK_Y + WEED_EX_TANK_H
            )
            context.drawSprite(
                left + WEED_EX_TANK_X, top + WEED_EX_TANK_Y, 0,
                WEED_EX_TANK_W, WEED_EX_TANK_H, weedExSprite,
                WEED_EX_R, WEED_EX_G, WEED_EX_B, 1f
            )
            context.disableScissor()
        }

        // 水槽容量标示 (182,41)-(194,88) = 12×47 → (23,26)，有流体时渲染
        if (waterAmount > 0) {
            context.drawTexture(TEXTURE, left + 23, top + 26, 182f, 41f, 12, 47, TEXTURE_SIZE, TEXTURE_SIZE)
        }
        // Weed-EX 槽容量标示 (182,41)-(194,88) = 12×47 → (117,26)，有流体时渲染
        if (weedExAmount > 0) {
            context.drawTexture(TEXTURE, left + 117, top + 26, 182f, 41f, 12, 47, TEXTURE_SIZE, TEXTURE_SIZE)
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
                Text.literal("${EnergyFormatUtils.formatRaw(energy)} / ${EnergyFormatUtils.formatRaw(cap)} EU"),
                mouseX, mouseY
            )
        }

        // 水槽悬停
        if (relX in WATER_TANK_X until WATER_TANK_X + WATER_TANK_W &&
            relY in WATER_TANK_Y until WATER_TANK_Y + WATER_TANK_H
        ) {
            val lines = if (waterAmount > 0) listOf(Text.literal("水"), Text.literal("${"%,d".format(waterAmount / DROPLETS_PER_MB)} / ${"%,d".format(WATER_TANK_DROPLETS / DROPLETS_PER_MB)} mB"))
                        else listOf(Text.literal("空"))
            context.drawTooltip(textRenderer, lines, mouseX, mouseY)
        }

        // Weed-EX 槽悬停
        if (relX in WEED_EX_TANK_X until WEED_EX_TANK_X + WEED_EX_TANK_W &&
            relY in WEED_EX_TANK_Y until WEED_EX_TANK_Y + WEED_EX_TANK_H
        ) {
            val lines = if (weedExAmount > 0) listOf(Text.translatable("fluid.ic2_120.weed_ex"), Text.literal("${"%,d".format(weedExAmount / DROPLETS_PER_MB)} / ${"%,d".format(WEED_EX_TANK_DROPLETS / DROPLETS_PER_MB)} mB"))
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
                    Text.translatable("gui.ic2_120.cropmatron.uptips"),
                    Text.literal("§7").append(Text.translatable("item.ic2_120.overclocker_upgrade")),
                    Text.literal("§7").append(Text.translatable("item.ic2_120.transformer_upgrade")),
                    Text.literal("§7").append(Text.translatable("item.ic2_120.energy_storage_upgrade")),
                    Text.literal("§7").append(Text.translatable("item.ic2_120.fluid_ejector_upgrade")),
                    Text.literal("§7").append(Text.translatable("item.ic2_120.fluid_pulling_upgrade"))
                ),
                mouseX, mouseY
            )
        }
    }

    companion object {
        private val DROPLETS_PER_MB = (FluidConstants.BUCKET / 1000).toInt()
        private val WATER_TANK_DROPLETS = (FluidConstants.BUCKET * CropmatronSync.WATER_TANK_CAPACITY_BUCKETS).toInt()
        private val WEED_EX_TANK_DROPLETS = (FluidConstants.BUCKET * CropmatronSync.WEED_EX_TANK_CAPACITY_BUCKETS).toInt()
        private val TEXTURE = Identifier("ic2", "textures/gui/guiregulation.png")
        private val UPTIPS_TEXTURE = Identifier("ic2", "textures/gui/uptips.png")
        private val WATER_STILL_ID = Identifier("minecraft", "block/water_still")
        private val WEED_EX_STILL_ID = Identifier("ic2", "block/fluid/weed_ex_still")
        private const val TEXTURE_SIZE = 256

        // 电量条 (180,3)-(194,16) = 14x13
        private const val ENERGY_BAR_U = 180
        private const val ENERGY_BAR_V = 3
        private const val ENERGY_BAR_W = 14
        private const val ENERGY_BAR_H = 13
        private const val ENERGY_BAR_X = 135
        private const val ENERGY_BAR_Y = 81

        // 水槽 (11,25)-(35,72) = 24x47
        private const val WATER_TANK_X = 11
        private const val WATER_TANK_Y = 25
        private const val WATER_TANK_W = 24
        private const val WATER_TANK_H = 47
        private const val WATER_R = 0.25f
        private const val WATER_G = 0.45f
        private const val WATER_B = 0.95f

        // Weed-EX 槽 (105,25)-(129,72) = 24x47
        private const val WEED_EX_TANK_X = 105
        private const val WEED_EX_TANK_Y = 25
        private const val WEED_EX_TANK_W = 24
        private const val WEED_EX_TANK_H = 47
        private const val WEED_EX_R = 1f
        private const val WEED_EX_G = 1f
        private const val WEED_EX_B = 1f

        // 肥料图标 (181,23)-(193,34) = 12x11
        private const val FERTILIZER_ICON_U = 181
        private const val FERTILIZER_ICON_V = 23
        private const val FERTILIZER_ICON_W = 12
        private const val FERTILIZER_ICON_H = 11
        private const val FERTILIZER_SLOTS_X = 8
        private const val FERTILIZER_SLOTS_Y = 79
        private const val FERTILIZER_ICON_OFFSET_X = 2
        private const val FERTILIZER_ICON_OFFSET_Y = 2

        // uptips
        private const val UPTIPS_X = 4
        private const val UPTIPS_Y = 4
        private const val UPTIPS_SIZE = 16
    }
}
