package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.FluidUtils
import ic2_120.client.t
import ic2_120.content.block.PumpBlock
import ic2_120.content.screen.PumpScreenHandler
import ic2_120.content.sync.PumpSync
import ic2_120.registry.annotation.ModScreen
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.level.material.Fluid
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

@ModScreen(block = PumpBlock::class)
class PumpScreen(
    val handler: PumpScreenHandler,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<PumpScreenHandler>(handler, playerInventory, title) {

    init {
        imageWidth = 176
        imageHeight = 166
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

        val energy = menu.sync.energy.toLong().coerceAtLeast(0)
        val cap = menu.sync.energyCapacity.toLong().coerceAtLeast(1)
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val progressFrac = if (PumpSync.PROGRESS_MAX > 0) {
            (menu.sync.progress.coerceIn(0, PumpSync.PROGRESS_MAX).toFloat() / PumpSync.PROGRESS_MAX).coerceIn(0f, 1f)
        } else 0f
        val inputRate = menu.sync.getSyncedInsertedAmount()
        val consumeRate = menu.sync.getSyncedConsumedAmount()
        val fluidAmount = menu.sync.fluidAmount.toLong().coerceAtLeast(0)
        val fluidCapacity = FluidConstants.BUCKET * 8
        val fluidFraction = if (fluidCapacity > 0) (fluidAmount.toFloat() / fluidCapacity).coerceIn(0f, 1f) else 0f
        val fluidRawId = menu.sync.fluidRawId

        // 电量条 (179,3)-(193,17) = 14x14，自下而上
        if (energyFraction > 0f) {
            val fillHeight = (ENERGY_BAR_H * energyFraction).toInt().coerceAtLeast(1)
            context.enableScissor(
                left + ENERGY_BAR_X,
                top + ENERGY_BAR_Y + ENERGY_BAR_H - fillHeight,
                left + ENERGY_BAR_X + ENERGY_BAR_W,
                top + ENERGY_BAR_Y + ENERGY_BAR_H
            )
            context.blit(
                TEXTURE, left + ENERGY_BAR_X, top + ENERGY_BAR_Y,
                ENERGY_BAR_U.toFloat(), ENERGY_BAR_V.toFloat(),
                ENERGY_BAR_W, ENERGY_BAR_H,
                TEXTURE_SIZE, TEXTURE_SIZE
            )
            context.disableScissor()
        }

        // 工作进度 (179,25)-(201,39) = 22x14，自左向右
        if (progressFrac > 0f) {
            val arrowWidth = (PROGRESS_W * progressFrac).toInt().coerceAtLeast(1)
            context.enableScissor(
                left + PROGRESS_X,
                top + PROGRESS_Y,
                left + PROGRESS_X + arrowWidth,
                top + PROGRESS_Y + PROGRESS_H
            )
            context.blit(
                TEXTURE, left + PROGRESS_X, top + PROGRESS_Y,
                PROGRESS_U.toFloat(), PROGRESS_V.toFloat(),
                PROGRESS_W, PROGRESS_H,
                TEXTURE_SIZE, TEXTURE_SIZE
            )
            context.disableScissor()
        }

        // 流体纹理 (100,26)-(112,73) = 12x47，自下而上
        if (fluidAmount > 0 && fluidRawId != -1) {
            val fluid = BuiltInRegistries.FLUID.byId(fluidRawId)
            val sprite = getFluidStillSprite(fluid)
            if (sprite != null) {
                val fillHeight = (TANK_H * fluidFraction).toInt().coerceAtLeast(1)
                context.enableScissor(
                    left + TANK_X,
                    top + TANK_Y + TANK_H - fillHeight,
                    left + TANK_X + TANK_W,
                    top + TANK_Y + TANK_H
                )
                val tintColor = FluidUtils.getFluidColor(fluid)
                val (cr, cg, cb) = if (tintColor != -1) {
                    Triple(((tintColor shr 16) and 0xFF) / 255f, ((tintColor shr 8) and 0xFF) / 255f, (tintColor and 0xFF) / 255f)
                } else {
                    Triple(1f, 1f, 1f)
                }
                context.blit(left + TANK_X, top + TANK_Y, 0, TANK_W, TANK_H, sprite, cr, cg, cb, 1f)
                context.disableScissor()
            }
        }

        // 容量标示 (183,49)-(194,96) = 11x47，有流体时渲染在流体纹理之上
        if (fluidAmount > 0) {
            context.blit(
                TEXTURE, left + TANK_OVERLAY_X, top + TANK_OVERLAY_Y,
                TANK_OVERLAY_U.toFloat(), TANK_OVERLAY_V.toFloat(),
                TANK_OVERLAY_W, TANK_OVERLAY_H,
                TEXTURE_SIZE, TEXTURE_SIZE
            )
        }

        // uptips 纹理
        context.blit(
            UPTIPS_TEXTURE, left + UPTIPS_X, top + UPTIPS_Y,
            0f, 0f, UPTIPS_SIZE, UPTIPS_SIZE,
            UPTIPS_SIZE, UPTIPS_SIZE
        )

        // 侧边文本
        val inputText = t("gui.ic2_120.input_eu", EnergyFormatUtils.formatRaw(inputRate))
        val consumeText = t("gui.ic2_120.consume_eu", EnergyFormatUtils.formatRaw(consumeRate))
        val sideTextWidth = maxOf(font.width(inputText), font.width(consumeText))
        val sideTextX = left - sideTextWidth - 4
        context.drawString(font, inputText, sideTextX, top + 8, 0xAAAAAA, false)
        context.drawString(font, consumeText, sideTextX, top + 20, 0xAAAAAA, false)

        renderTooltip(context, mouseX, mouseY)

        val relX = mouseX - left
        val relY = mouseY - top

        // 电量条悬停
        if (relX in ENERGY_BAR_X until ENERGY_BAR_X + ENERGY_BAR_W &&
            relY in ENERGY_BAR_Y until ENERGY_BAR_Y + ENERGY_BAR_H
        ) {
            context.renderTooltip(
                font,
                Component.literal("储能：${EnergyFormatUtils.formatRaw(energy)} / ${EnergyFormatUtils.formatRaw(cap)} EU"),
                mouseX, mouseY
            )
        }

        // 流体槽悬停
        if (relX in TANK_X until TANK_X + TANK_W &&
            relY in TANK_Y until TANK_Y + TANK_H
        ) {
            val lines = if (fluidAmount > 0 && fluidRawId != -1) {
                val fluid = BuiltInRegistries.FLUID.byId(fluidRawId)
                val fluidName = fluid.defaultFluidState().createLegacyBlock().block.name.string
                listOf(Component.literal(fluidName), Component.literal("${"%,d".format(fluidAmount / DROPLETS_PER_MB)} / ${"%,d".format(fluidCapacity / DROPLETS_PER_MB)} mB"))
            } else {
                listOf(Component.literal("空"))
            }
            context.renderComponentTooltip(font, lines, mouseX, mouseY)
        }

        // uptips 悬停
        if (relX in UPTIPS_X until UPTIPS_X + UPTIPS_SIZE &&
            relY in UPTIPS_Y until UPTIPS_Y + UPTIPS_SIZE
        ) {
            context.renderComponentTooltip(
                font,
                listOf(
                    Component.translatable("gui.ic2_120.pump.uptips"),
                    Component.literal("§7").append(Component.translatable("item.ic2_120.overclocker_upgrade")),
                    Component.literal("§7").append(Component.translatable("item.ic2_120.transformer_upgrade")),
                    Component.literal("§7").append(Component.translatable("item.ic2_120.energy_storage_upgrade")),
                    Component.literal("§7").append(Component.translatable("item.ic2_120.ejector_upgrade")),
                    Component.literal("§7").append(Component.translatable("item.ic2_120.pulling_upgrade")),
                    Component.literal("§7").append(Component.translatable("item.ic2_120.fluid_ejector_upgrade")),
                    Component.literal("§7").append(Component.translatable("item.ic2_120.fluid_pulling_upgrade"))
                ),
                mouseX, mouseY
            )
        }
    }

    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath("ic2", "textures/gui/guipump.png")
        private val UPTIPS_TEXTURE = ResourceLocation.fromNamespaceAndPath("ic2", "textures/gui/uptips.png")
        private const val TEXTURE_SIZE = 256

        private val DROPLETS_PER_MB = (FluidConstants.BUCKET / 1000).toInt()

        // 电量条 (179,3)-(193,17) = 14x14
        private const val ENERGY_BAR_U = 179
        private const val ENERGY_BAR_V = 3
        private const val ENERGY_BAR_W = 14
        private const val ENERGY_BAR_H = 14
        private const val ENERGY_BAR_X = 10
        private const val ENERGY_BAR_Y = 65

        // 工作进度 (179,25)-(201,39) = 22x14
        private const val PROGRESS_U = 179
        private const val PROGRESS_V = 24
        private const val PROGRESS_W = 22
        private const val PROGRESS_H = 15
        private const val PROGRESS_X = 60
        private const val PROGRESS_Y = 41

        // 流体槽 (100,26)-(112,73) = 12x47
        private const val TANK_X = 100
        private const val TANK_Y = 26
        private const val TANK_W = 12
        private const val TANK_H = 47

        // 容量标示 (183,49)-(194,96) = 11x47
        private const val TANK_OVERLAY_U = 183
        private const val TANK_OVERLAY_V = 49
        private const val TANK_OVERLAY_W = 11
        private const val TANK_OVERLAY_H = 47
        private const val TANK_OVERLAY_X = 101
        private const val TANK_OVERLAY_Y = 27

        // uptips
        private const val UPTIPS_X = 4
        private const val UPTIPS_Y = 4
        private const val UPTIPS_SIZE = 16
    }
}

private fun getFluidStillSprite(fluid: Fluid): TextureAtlasSprite? {
    val handler = FluidRenderHandlerRegistry.INSTANCE.get(fluid) ?: return null
    return handler.getFluidSprites(null, null, fluid.defaultFluidState()).getOrNull(0)
}
