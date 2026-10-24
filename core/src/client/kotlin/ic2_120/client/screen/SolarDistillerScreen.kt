package ic2_120.client.screen

import ic2_120.client.FluidUtils
import ic2_120.content.block.SolarDistillerBlock
import ic2_120.content.fluid.ModFluids
import ic2_120.content.screen.SolarDistillerScreenHandler
import ic2_120.content.sync.SolarDistillerSync
import ic2_120.registry.annotation.ModScreen
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.level.material.Fluids
import net.minecraft.network.chat.Component
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.minecraft.resources.ResourceLocation

@ModScreen(block = SolarDistillerBlock::class)
class SolarDistillerScreen(
    val handler: SolarDistillerScreenHandler,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<SolarDistillerScreenHandler>(handler, playerInventory, title) {

    private val waterFlowSprite: TextureAtlasSprite? by lazy {
        FluidRenderHandlerRegistry.INSTANCE.get(Fluids.WATER)
            ?.getFluidSprites(null, null, Fluids.WATER.defaultFluidState())
            ?.getOrNull(1)
    }

    private val waterColor: Int by lazy { FluidUtils.getFluidColor(Fluids.WATER) }

    private val distilledFlowSprite: TextureAtlasSprite? by lazy {
        FluidRenderHandlerRegistry.INSTANCE.get(ModFluids.DISTILLED_WATER_STILL)
            ?.getFluidSprites(null, null, ModFluids.DISTILLED_WATER_STILL.defaultFluidState())
            ?.getOrNull(1)
    }

    private val distilledColor: Int by lazy { FluidUtils.getFluidColor(ModFluids.DISTILLED_WATER_STILL) }

    init {
        imageWidth = 176
        imageHeight = 184
    }

    override fun renderBg(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.blit(TEXTURE, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, TEXTURE_SIZE, TEXTURE_SIZE)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)

        val left = leftPos
        val top = topPos

        context.drawString(font, title, left + (imageWidth - font.width(title)) / 2, top + 6, 0x404040, false)

        val waterMb = menu.sync.waterInput.coerceAtLeast(0)
        val distilledMb = menu.sync.distilledOutput.coerceAtLeast(0)
        val waterFraction = (waterMb.toFloat() / TANK_CAPACITY_DROPLETS).coerceIn(0f, 1f)
        val distilledFraction = (distilledMb.toFloat() / TANK_CAPACITY_DROPLETS).coerceIn(0f, 1f)

        // 水纹理渲染 — 使用流体本身 flowing 纹理 + 颜色
        if (waterMb > 0) {
            val fillHeight = (WATER_H * waterFraction).toInt().coerceAtLeast(1)
            drawFluidTank(context, left + WATER_X, top + WATER_Y, WATER_W, WATER_H, fillHeight,
                waterFlowSprite, waterColor)
        }

        // 蒸馏水纹理渲染 — 使用流体本身 flowing 纹理 + 颜色
        if (distilledMb > 0) {
            val fillHeight = (DISTILLED_H * distilledFraction).toInt().coerceAtLeast(1)
            drawFluidTank(context, left + DISTILLED_X, top + DISTILLED_Y, DISTILLED_W, DISTILLED_H, fillHeight,
                distilledFlowSprite, distilledColor)
        }

        // 工作状态纹理 (4,190)-(100,218) = 96×28
        if (menu.sync.isWorking != 0) {
            context.blit(
                TEXTURE, left + STATUS_X, top + STATUS_Y,
                STATUS_U.toFloat(), STATUS_V.toFloat(),
                STATUS_W, STATUS_H,
                TEXTURE_SIZE, TEXTURE_SIZE
            )
        }

        // uptips 纹理
        context.blit(
            UPTIPS_TEXTURE, left + UPTIPS_X, top + UPTIPS_Y,
            0f, 0f, UPTIPS_SIZE, UPTIPS_SIZE,
            UPTIPS_SIZE, UPTIPS_SIZE
        )

        // 水槽悬停提示
        val relX = mouseX - left
        val relY = mouseY - top
        if (relX in WATER_X until WATER_X + WATER_W && relY in WATER_Y until WATER_Y + WATER_H) {
            val waterLines = if (waterMb > 0) {
                listOf(Component.translatable("gui.ic2_120.solar_distiller.water_tooltip", "%,d".format(waterMb / DROPLETS_PER_MB), "%,d".format(TANK_CAPACITY_MB)))
            } else {
                listOf(Component.translatable("ic2.generic.text.empty"))
            }
            context.renderComponentTooltip(font, waterLines, mouseX, mouseY)
        }

        // 蒸馏水槽悬停提示
        if (relX in DISTILLED_X until DISTILLED_X + DISTILLED_W && relY in DISTILLED_Y until DISTILLED_Y + DISTILLED_H) {
            val distilledLines = if (distilledMb > 0) {
                listOf(Component.translatable("gui.ic2_120.solar_distiller.distilled_tooltip", "%,d".format(distilledMb / DROPLETS_PER_MB), "%,d".format(TANK_CAPACITY_MB)))
            } else {
                listOf(Component.translatable("ic2.generic.text.empty"))
            }
            context.renderComponentTooltip(font, distilledLines, mouseX, mouseY)
        }

        // uptips 悬停提示
        if (relX in UPTIPS_X until UPTIPS_X + UPTIPS_SIZE && relY in UPTIPS_Y until UPTIPS_Y + UPTIPS_SIZE) {
            context.renderComponentTooltip(
                font,
                listOf(
                    Component.translatable("gui.ic2_120.solar_distiller.uptips"),
                    Component.literal(" §7").append(Component.translatable("item.ic2_120.fluid_ejector_upgrade")),
                    Component.literal(" §7").append(Component.translatable("item.ic2_120.fluid_pulling_upgrade"))
                ),
                mouseX, mouseY
            )
        }

        renderTooltip(context, mouseX, mouseY)
    }

    /** 自下而上平铺流体 flowing 纹理 + 颜色着色 */
    private fun drawFluidTank(context: GuiGraphics, gx: Int, gy: Int, w: Int, h: Int, fillH: Int, sprite: TextureAtlasSprite?, color: Int) {
        if (sprite == null || color == -1) return
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        val fillY = gy + h - fillH
        context.enableScissor(gx, fillY, gx + w, gy + h)
        for (sy in fillY until (gy + h) step 16) {
            val tileH = minOf(16, gy + h - sy)
            for (sx in gx until (gx + w) step 16) {
                val tileW = minOf(16, gx + w - sx)
                context.blit(sx, sy, 0, tileW, tileH, sprite, r, g, b, 1f)
            }
        }
        context.disableScissor()
    }

    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath("ic2", "textures/gui/guisolardistiller.png")
        private val UPTIPS_TEXTURE = ResourceLocation.fromNamespaceAndPath("ic2", "textures/gui/uptips.png")
        private const val TEXTURE_SIZE = 256

        private val DROPLETS_PER_MB = (FluidConstants.BUCKET / 1000).toInt()
        private val TANK_CAPACITY_DROPLETS = (FluidConstants.BUCKET * SolarDistillerSync.TANK_CAPACITY_BUCKETS).toInt()
        private val TANK_CAPACITY_MB = TANK_CAPACITY_DROPLETS / DROPLETS_PER_MB

        // 水纹理区域 (37,44)-(90,61) = 53×17
        private const val WATER_X = 37
        private const val WATER_Y = 44
        private const val WATER_W = 53
        private const val WATER_H = 17

        // 蒸馏水纹理区域 (115,56)-(132,98) = 17×42
        private const val DISTILLED_X = 115
        private const val DISTILLED_Y = 56
        private const val DISTILLED_W = 17
        private const val DISTILLED_H = 42

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
