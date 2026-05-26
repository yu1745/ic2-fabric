package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.FluidUtils
import ic2_120.client.t
import ic2_120.content.block.FluidCannerBlock
import ic2_120.content.screen.FluidCannerScreenHandler
import ic2_120.content.sync.FluidCannerSync
import ic2_120.registry.annotation.ModScreen
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.fluid.Fluid
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = FluidCannerBlock::class)
class FluidCannerScreen(
    handler: FluidCannerScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<FluidCannerScreenHandler>(handler, playerInventory, title) {

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

        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1)
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val progressFrac = if (FluidCannerSync.PROGRESS_MAX > 0) {
            (handler.sync.progress.coerceIn(0, FluidCannerSync.PROGRESS_MAX).toFloat() / FluidCannerSync.PROGRESS_MAX).coerceIn(0f, 1f)
        } else 0f
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val consumeRate = handler.sync.getSyncedConsumedAmount()
        val fluidAmount = handler.sync.fluidAmountMb.toLong().coerceAtLeast(0)
        val fluidCapacity = handler.sync.fluidCapacityMb.toLong().coerceAtLeast(1)
        val fluidFraction = if (fluidCapacity > 0) (fluidAmount.toFloat() / fluidCapacity).coerceIn(0f, 1f) else 0f
        val fluidRawId = handler.sync.fluidRawId
        val isPourOut = handler.sync.lastPourOut != 0

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

        // 工作进度 (179,21)-(196,34) = 17x13，自左向右
        if (progressFrac > 0f) {
            val (px, py) = if (isPourOut) PROGRESS_POUROUT_X to PROGRESS_POUROUT_Y else PROGRESS_FILL_X to PROGRESS_FILL_Y
            val arrowWidth = (PROGRESS_W * progressFrac).toInt().coerceAtLeast(1)
            context.enableScissor(
                left + px,
                top + py,
                left + px + arrowWidth,
                top + py + PROGRESS_H
            )
            context.drawTexture(
                TEXTURE, left + px, top + py,
                PROGRESS_U.toFloat(), PROGRESS_V.toFloat(),
                PROGRESS_W, PROGRESS_H,
                TEXTURE_SIZE, TEXTURE_SIZE
            )
            context.disableScissor()
        }

        // 流体纹理渲染 (82,38)-(94,85) = 12x47，自下而上
        if (fluidAmount > 0 && fluidRawId != -1) {
            val fluid = Registries.FLUID.get(fluidRawId)
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
                context.drawSprite(left + TANK_X, top + TANK_Y, 0, TANK_W, TANK_H, sprite, cr, cg, cb, 1f)
                context.disableScissor()
            }
        }

        // 容量标示纹理 (180,43)-(192,90) = 12x47，始终渲染在流体纹理之上
        context.drawTexture(
            TEXTURE, left + TANK_X, top + TANK_Y,
            TANK_OVERLAY_U.toFloat(), TANK_OVERLAY_V.toFloat(),
            TANK_W, TANK_H,
            TEXTURE_SIZE, TEXTURE_SIZE
        )

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

        // 悬停提示
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

        // 流体槽悬停
        if (relX in TANK_X until TANK_X + TANK_W &&
            relY in TANK_Y until TANK_Y + TANK_H
        ) {
            val lines = if (fluidAmount > 0 && fluidRawId != -1) {
                val fluid = Registries.FLUID.get(fluidRawId)
                val fluidName = fluid.defaultState.blockState.block.name.string
                listOf(Text.literal(fluidName), Text.literal("${"%,d".format(fluidAmount)} / ${"%,d".format(fluidCapacity)} mB"))
            } else {
                listOf(Text.literal("空"))
            }
            context.drawTooltip(textRenderer, lines, mouseX, mouseY)
        }

        // uptips 悬停
        if (relX in UPTIPS_X until UPTIPS_X + UPTIPS_SIZE &&
            relY in UPTIPS_Y until UPTIPS_Y + UPTIPS_SIZE
        ) {
            context.drawTooltip(
                textRenderer,
                listOf(
                    Text.translatable("gui.ic2_120.fluid_canner.uptips"),
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
        private val TEXTURE = Identifier("ic2", "textures/gui/guifluidcanner.png")
        private val UPTIPS_TEXTURE = Identifier("ic2", "textures/gui/uptips.png")
        private const val TEXTURE_SIZE = 256

        // 电量条 (179,3)-(193,17) = 14x14
        private const val ENERGY_BAR_U = 179
        private const val ENERGY_BAR_V = 3
        private const val ENERGY_BAR_W = 14
        private const val ENERGY_BAR_H = 14
        private const val ENERGY_BAR_X = 9
        private const val ENERGY_BAR_Y = 35

        // 工作进度 (179,21)-(196,34) = 17x13
        private const val PROGRESS_U = 179
        private const val PROGRESS_V = 21
        private const val PROGRESS_W = 17
        private const val PROGRESS_H = 13
        private const val PROGRESS_POUROUT_X = 61
        private const val PROGRESS_POUROUT_Y = 36
        private const val PROGRESS_FILL_X = 99
        private const val PROGRESS_FILL_Y = 55

        // 流体槽 (82,38)-(94,85) = 12x47
        private const val TANK_X = 82
        private const val TANK_Y = 38
        private const val TANK_W = 12
        private const val TANK_H = 47

        // 容量标示 (180,43)-(192,90) = 12x47
        private const val TANK_OVERLAY_U = 180
        private const val TANK_OVERLAY_V = 43

        // uptips
        private const val UPTIPS_X = 4
        private const val UPTIPS_Y = 4
        private const val UPTIPS_SIZE = 16
    }
}

private fun getFluidStillSprite(fluid: Fluid): net.minecraft.client.texture.Sprite? {
    val handler = FluidRenderHandlerRegistry.INSTANCE.get(fluid) ?: return null
    return handler.getFluidSprites(null, null, fluid.defaultState).getOrNull(0)
}
