package ic2_120.client.screen

import ic2_120.content.block.SolarDistillerBlock
import ic2_120.content.screen.SolarDistillerScreenHandler
import ic2_120.content.sync.SolarDistillerSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.texture.SpriteAtlasTexture
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = SolarDistillerBlock::class)
class SolarDistillerScreen(
    handler: SolarDistillerScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<SolarDistillerScreenHandler>(handler, playerInventory, title) {

    private val waterSprite by lazy {
        MinecraftClient.getInstance()
            .getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
            .apply(WATER_STILL_ID)
    }

    init {
        backgroundWidth = 176
        backgroundHeight = 184
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

        val waterMb = handler.sync.waterInputMb.coerceAtLeast(0)
        val distilledMb = handler.sync.distilledOutputMb.coerceAtLeast(0)
        val waterFraction = (waterMb.toFloat() / SolarDistillerSync.TANK_CAPACITY_MB).coerceIn(0f, 1f)
        val distilledFraction = (distilledMb.toFloat() / SolarDistillerSync.TANK_CAPACITY_MB).coerceIn(0f, 1f)

        // 水纹理渲染（自下而上，带蓝色着色）
        if (waterMb > 0) {
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

        // 蒸馏水纹理渲染（自下而上，带蓝色着色）
        if (distilledMb > 0) {
            val fillHeight = (DISTILLED_H * distilledFraction).toInt().coerceAtLeast(1)
            context.enableScissor(
                left + DISTILLED_X,
                top + DISTILLED_Y + DISTILLED_H - fillHeight,
                left + DISTILLED_X + DISTILLED_W,
                top + DISTILLED_Y + DISTILLED_H
            )
            context.drawSprite(left + DISTILLED_X, top + DISTILLED_Y, 0, DISTILLED_W, DISTILLED_H, waterSprite, DISTILLED_R, DISTILLED_G, DISTILLED_B, 1f)
            context.disableScissor()
        }

        // 工作状态纹理 (4,190)-(100,218) = 96×28
        if (handler.sync.isWorking != 0) {
            context.drawTexture(
                TEXTURE, left + STATUS_X, top + STATUS_Y,
                STATUS_U.toFloat(), STATUS_V.toFloat(),
                STATUS_W, STATUS_H,
                TEXTURE_SIZE, TEXTURE_SIZE
            )
        }

        // uptips 纹理
        context.drawTexture(
            UPTIPS_TEXTURE, left + UPTIPS_X, top + UPTIPS_Y,
            0f, 0f, UPTIPS_SIZE, UPTIPS_SIZE,
            UPTIPS_SIZE, UPTIPS_SIZE
        )

        // 水悬停提示
        val relX = mouseX - left
        val relY = mouseY - top
        if (waterMb > 0 && relX in WATER_X until WATER_X + WATER_W && relY in WATER_Y until WATER_Y + WATER_H) {
            context.drawTooltip(
                textRenderer,
                listOf(Text.translatable("gui.ic2_120.solar_distiller.water_tooltip", waterMb, SolarDistillerSync.TANK_CAPACITY_MB)),
                mouseX, mouseY
            )
        }

        // 蒸馏水悬停提示
        if (distilledMb > 0 && relX in DISTILLED_X until DISTILLED_X + DISTILLED_W && relY in DISTILLED_Y until DISTILLED_Y + DISTILLED_H) {
            context.drawTooltip(
                textRenderer,
                listOf(Text.translatable("gui.ic2_120.solar_distiller.distilled_tooltip", distilledMb, SolarDistillerSync.TANK_CAPACITY_MB)),
                mouseX, mouseY
            )
        }

        // uptips 悬停提示
        if (relX in UPTIPS_X until UPTIPS_X + UPTIPS_SIZE && relY in UPTIPS_Y until UPTIPS_Y + UPTIPS_SIZE) {
            context.drawTooltip(
                textRenderer,
                listOf(
                    Text.translatable("gui.ic2_120.solar_distiller.uptips"),
                    Text.literal(" §7").append(Text.translatable("item.ic2_120.fluid_ejector_upgrade")),
                    Text.literal(" §7").append(Text.translatable("item.ic2_120.fluid_pulling_upgrade"))
                ),
                mouseX, mouseY
            )
        }

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    companion object {
        private val TEXTURE = Identifier("ic2", "textures/gui/guisolardistiller.png")
        private val UPTIPS_TEXTURE = Identifier("ic2", "textures/gui/uptips.png")
        private val WATER_STILL_ID = Identifier("minecraft", "block/water_flow")
        private const val TEXTURE_SIZE = 256

        // 水纹理区域 (37,44)-(90,61) = 53×17
        private const val WATER_X = 37
        private const val WATER_Y = 44
        private const val WATER_W = 53
        private const val WATER_H = 17
        private const val WATER_R = 0.25f
        private const val WATER_G = 0.45f
        private const val WATER_B = 0.95f

        // 蒸馏水纹理区域 (115,56)-(132,98) = 17×42
        private const val DISTILLED_X = 115
        private const val DISTILLED_Y = 56
        private const val DISTILLED_W = 17
        private const val DISTILLED_H = 42
        private const val DISTILLED_R = 0.45f
        private const val DISTILLED_G = 0.65f
        private const val DISTILLED_B = 0.98f

        // 工作状态纹理 (4,190)-(100,218) = 96×28
        private const val STATUS_U = 4
        private const val STATUS_V = 190
        private const val STATUS_W = 96
        private const val STATUS_H = 28
        private const val STATUS_X = 37
        private const val STATUS_Y = 27

        // uptips 纹理
        private const val UPTIPS_X = 4
        private const val UPTIPS_Y = 4
        private const val UPTIPS_SIZE = 16
    }
}
