package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.FluidUtils
import ic2_120.content.block.nuclear.NuclearReactorBlock
import ic2_120.content.fluid.ModFluids
import ic2_120.content.screen.NuclearReactorScreenHandler
import ic2_120.content.sync.NuclearReactorSync
import ic2_120.registry.annotation.ModScreen
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.texture.Sprite
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = NuclearReactorBlock::class)
class NuclearReactorScreen(
    handler: NuclearReactorScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<NuclearReactorScreenHandler>(handler, playerInventory, title) {

    private val isThermal get() = handler.isThermalMode || handler.sync.isThermalMode == 1
    private var lockButton: ButtonWidget? = null

    private val coolantSprite by lazy {
        FluidRenderHandlerRegistry.INSTANCE.get(ModFluids.COOLANT_STILL)
            ?.getFluidSprites(null, null, ModFluids.COOLANT_STILL.defaultState)?.getOrNull(0)
    }
    private val hotCoolantSprite by lazy {
        FluidRenderHandlerRegistry.INSTANCE.get(ModFluids.HOT_COOLANT_STILL)
            ?.getFluidSprites(null, null, ModFluids.HOT_COOLANT_STILL.defaultState)?.getOrNull(0)
    }
    private val coolantColor by lazy { FluidUtils.getFluidColor(ModFluids.COOLANT_STILL) }
    private val hotCoolantColor by lazy { FluidUtils.getFluidColor(ModFluids.HOT_COOLANT_STILL) }

    init {
        val frameH = if (isThermal) {
            maxOf(TH_BG_H, handler.hotbarY + NuclearReactorScreenHandler.SLOT_SIZE + PANEL_BOTTOM_PADDING)
        } else {
            maxOf(NT_BG_H, handler.hotbarY + NuclearReactorScreenHandler.SLOT_SIZE + PANEL_BOTTOM_PADDING)
        }
        backgroundWidth = NuclearReactorScreenHandler.FRAME_WIDTH
        backgroundHeight = frameH
        titleY = -1000
        playerInventoryTitleY = -1000
    }

    override fun init() {
        super.init()
        val btnW = if (isThermal) TH_BTN_W else NT_BTN_W
        val btnH = if (isThermal) TH_BTN_H else NT_BTN_H
        lockButton = ButtonWidget.builder(
            Text.translatable("gui.ic2_120.nuclear_reactor.lock")
        ) {
            MinecraftClient.getInstance().player?.networkHandler?.sendPacket(
                ButtonClickC2SPacket(handler.syncId, NuclearReactorScreenHandler.BUTTON_ID_TOGGLE_LOCK)
            )
        }.dimensions(0, 0, btnW, btnH).build()
        addDrawableChild(lockButton!!)
    }

    private fun updateLockButton() {
        val btn = lockButton ?: return
        val isLocked = handler.sync.layoutLocked == 1
        btn.setPosition(
            x + (if (isThermal) TH_BTN_X else NT_BTN_X),
            y + (if (isThermal) TH_BTN_Y else NT_BTN_Y)
        )
        btn.message = Text.translatable(
            if (isLocked) "gui.ic2_120.nuclear_reactor.unlock"
            else "gui.ic2_120.nuclear_reactor.lock"
        )
    }

    private fun positionSlots() {
        val h = NuclearReactorScreenHandler
        for (i in 0 until handler.reactorSlotCount) {
            val col = i / h.GRID_ROWS
            val row = i % h.GRID_ROWS
            handler.slots[i].x = h.SLOT_GRID_X + col * h.SLOT_SIZE
            handler.slots[i].y = handler.slotGridY + row * h.SLOT_SIZE
        }
        if (isThermal) {
            val base = handler.reactorSlotCount
            handler.slots[base + 0].x = TH_SLOT_COOL_IN_X
            handler.slots[base + 0].y = TH_SLOT_COOL_IN_Y
            handler.slots[base + 1].x = TH_SLOT_COOL_OUT_X
            handler.slots[base + 1].y = TH_SLOT_COOL_OUT_Y
            handler.slots[base + 2].x = TH_SLOT_HOT_IN_X
            handler.slots[base + 2].y = TH_SLOT_HOT_IN_Y
            handler.slots[base + 3].x = TH_SLOT_HOT_OUT_X
            handler.slots[base + 3].y = TH_SLOT_HOT_OUT_Y
        }
        val playerStart = handler.playerInventorySlotStart
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                val idx = playerStart + row * 9 + col
                handler.slots[idx].x = h.PLAYER_INV_X + col * h.SLOT_SIZE
                handler.slots[idx].y = handler.playerInvY + row * h.SLOT_SIZE
            }
        }
        for (col in 0 until 9) {
            val idx = playerStart + 27 + col
            handler.slots[idx].x = h.PLAYER_INV_X + col * h.SLOT_SIZE
            handler.slots[idx].y = handler.hotbarY
        }
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        if (isThermal) {
            context.drawTexture(TEXTURE, x, y, TH_BG_U.toFloat(), TH_BG_V.toFloat(), TH_BG_W, TH_BG_H, TEX_W, TEX_H)
        } else {
            context.drawTexture(TEXTURE, x, y, NT_BG_U.toFloat(), NT_BG_V.toFloat(), NT_BG_W, NT_BG_H, TEX_W, TEX_H)
        }
        drawLockOverlays(context)
        drawHeatBar(context)
        if (isThermal) drawFluidBars(context)
    }

    private fun drawLockOverlays(context: DrawContext) {
        val cols = handler.reactorCols
        val offsetX = 1
        val offsetY = 1
        val h = NuclearReactorScreenHandler
        for (col in cols until 9) {
            for (row in 0 until h.GRID_ROWS) {
                val lx = x + h.SLOT_GRID_X + col * h.SLOT_SIZE + offsetX
                val ly = y + handler.slotGridY + row * h.SLOT_SIZE + offsetY
                context.drawTexture(TEXTURE, lx, ly, LOCK_U.toFloat(), LOCK_V.toFloat(), LOCK_W, LOCK_H, TEX_W, TEX_H)
            }
        }
    }

    private fun drawHeatBar(context: DrawContext) {
        val temp = handler.sync.temperature.coerceIn(0, NuclearReactorSync.HEAT_CAPACITY)
        val fraction = temp.toFloat() / NuclearReactorSync.HEAT_CAPACITY
        val fillW = (fraction * HEAT_W).toInt()
        if (fillW <= 0) return
        val barX = x + (if (isThermal) TH_HEAT_X else NT_HEAT_X)
        val barY = y + (if (isThermal) TH_HEAT_Y else NT_HEAT_Y)
        context.enableScissor(barX, barY, barX + fillW, barY + HEAT_H)
        context.drawTexture(TEXTURE, barX, barY, HEAT_U.toFloat(), HEAT_V.toFloat(), HEAT_W, HEAT_H, TEX_W, TEX_H)
        context.disableScissor()
    }

    private fun drawFluidBars(context: DrawContext) {
        // inputCoolant/outputHotCoolant 现在是 droplets，容量 = BUCKET * 16
        val inputFraction = if (COOLANT_TANK_DROPLETS > 0)
            handler.sync.inputCoolant.coerceAtLeast(0).toFloat() / COOLANT_TANK_DROPLETS else 0f
        val outputFraction = if (COOLANT_TANK_DROPLETS > 0)
            handler.sync.outputHotCoolant.coerceAtLeast(0).toFloat() / COOLANT_TANK_DROPLETS else 0f
        val fwL = TH_FLUID_L_X2 - TH_FLUID_L_X
        val fhL = TH_FLUID_L_Y2 - TH_FLUID_L_Y
        val fwR = TH_FLUID_R_X2 - TH_FLUID_R_X
        val fhR = TH_FLUID_R_Y2 - TH_FLUID_R_Y
        drawFluidTank(context, x + TH_FLUID_L_X, y + TH_FLUID_L_Y, fwL, fhL, coolantSprite, coolantColor, inputFraction)
        drawFluidTank(context, x + TH_FLUID_R_X, y + TH_FLUID_R_Y, fwR, fhR, hotCoolantSprite, hotCoolantColor, outputFraction)
    }

    private fun drawFluidTank(
        context: DrawContext, fx: Int, fy: Int, fw: Int, fh: Int,
        sprite: Sprite?, color: Int, fraction: Float
    ) {
        val fillH = (fraction.coerceIn(0f, 1f) * fh).toInt()
        if (fillH <= 0 || sprite == null || color == -1) return
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        val fillY = fy + fh - fillH
        context.enableScissor(fx, fillY, fx + fw, fy + fh)
        for (sy in fillY until (fy + fh) step 16) {
            val tileH = minOf(16, fy + fh - sy)
            for (sx in fx until (fx + fw) step 16) {
                val tileW = minOf(16, fx + fw - sx)
                context.drawSprite(sx, sy, 0, tileW, tileH, sprite, r, g, b, 1f)
            }
        }
        context.disableScissor()
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        positionSlots()
        updateLockButton()
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)
        drawTextOverlay(context)
        if (handler.sync.layoutLocked == 1) drawGhostItems(context)
        drawCustomTooltips(context, mouseX, mouseY)
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun drawTextOverlay(context: DrawContext) {
        val scale = 7f / 9f
        if (isThermal) {
            val heatOutput = handler.sync.thermalHeatOutput
            val text = "HU输出：$heatOutput HU/t"
            val tw = textRenderer.getWidth(text)
            val areaCenterX = (TH_TEXT_X1 + TH_TEXT_X2) / 2
            context.matrices.push()
            context.matrices.scale(scale, scale, 1f)
            context.drawText(textRenderer, text,
                ((x + areaCenterX - tw / 2) / scale).toInt(),
                ((y + TH_TEXT_Y) / scale).toInt(), 0xFFADD8E6.toInt(), false)
            context.matrices.pop()
        } else {
            val outputRate = handler.sync.getSyncedExtractedAmount()
            val text = "EU输出：${EnergyFormatUtils.formatEu(outputRate)} EU/t"
            val tw = textRenderer.getWidth(text)
            val areaCenterX = (NT_TEXT_X1 + NT_TEXT_X2) / 2
            context.matrices.push()
            context.matrices.scale(scale, scale, 1f)
            context.drawText(textRenderer, text,
                ((x + areaCenterX - tw / 2) / scale).toInt(),
                ((y + NT_TEXT_Y) / scale).toInt(), 0x55FF55, false)
            context.matrices.pop()
        }
    }

    private fun drawGhostItems(context: DrawContext) {
        val reactor = handler.reactor ?: return
        for (i in 0 until handler.reactorSlotCount) {
            val slot = handler.slots[i]
            if (slot.hasStack()) continue
            val lockedItem = reactor.getLockedItemForSlot(i) ?: continue
            val ghostStack = ItemStack(lockedItem)
            val sx = x + slot.x
            val sy = y + slot.y
            context.drawItem(ghostStack, sx, sy)
            context.fill(sx, sy, sx + 16, sy + 16, 0x90000000.toInt())
        }
    }

    private fun drawCustomTooltips(context: DrawContext, mouseX: Int, mouseY: Int) {
        if (isThermal) {
            if (mouseX in (x + TH_HEAT_X) until (x + TH_HEAT_X + HEAT_W) &&
                mouseY in (y + TH_HEAT_Y) until (y + TH_HEAT_Y + HEAT_H)
            ) {
                val temp = handler.sync.temperature.coerceIn(0, NuclearReactorSync.HEAT_CAPACITY)
                context.drawTooltip(textRenderer,
                    listOf(Text.translatable("gui.ic2_120.nuclear_reactor.core_temp", temp)),
                    mouseX, mouseY
                )
            }
            if (mouseX in (x + TH_FLUID_L_X) until (x + TH_FLUID_L_X2) &&
                mouseY in (y + TH_FLUID_L_Y) until (y + TH_FLUID_L_Y2)
            ) {
                val inputDroplets = handler.sync.inputCoolant.coerceAtLeast(0)
                val capMb = COOLANT_TANK_CAPACITY_MB
                if (inputDroplets > 0) {
                    context.drawTooltip(
                        textRenderer,
                        listOf(Text.translatable("gui.ic2_120.nuclear_reactor.coolant_input", inputDroplets / DROPLETS_PER_MB, capMb)),
                        mouseX, mouseY
                    )
                } else {
                    context.drawTooltip(textRenderer, listOf(Text.literal("空")), mouseX, mouseY)
                }
            }
            if (mouseX in (x + TH_FLUID_R_X) until (x + TH_FLUID_R_X2) &&
                mouseY in (y + TH_FLUID_R_Y) until (y + TH_FLUID_R_Y2)
            ) {
                val outputDroplets = handler.sync.outputHotCoolant.coerceAtLeast(0)
                val capMb = COOLANT_TANK_CAPACITY_MB
                if (outputDroplets > 0) {
                    context.drawTooltip(
                        textRenderer,
                        listOf(Text.translatable("gui.ic2_120.nuclear_reactor.hot_coolant", outputDroplets / DROPLETS_PER_MB, capMb)),
                        mouseX, mouseY
                    )
                } else {
                    context.drawTooltip(textRenderer, listOf(Text.literal("空")), mouseX, mouseY)
                }
            }
            if (mouseX in (x + TH_HOVER_X1) until (x + TH_HOVER_X2) &&
                mouseY in (y + TH_HOVER_Y1) until (y + TH_HOVER_Y2)
            ) {
                context.drawTooltip(textRenderer, buildThermalInfoLines(), mouseX, mouseY)
            }
        } else {
            if (mouseX in (x + NT_HEAT_X) until (x + NT_HEAT_X + HEAT_W) &&
                mouseY in (y + NT_HEAT_Y) until (y + NT_HEAT_Y + HEAT_H)
            ) {
                val temp = handler.sync.temperature.coerceIn(0, NuclearReactorSync.HEAT_CAPACITY)
                context.drawTooltip(textRenderer,
                    listOf(Text.translatable("gui.ic2_120.nuclear_reactor.core_temp", temp)),
                    mouseX, mouseY
                )
            }
            if (mouseX in (x + NT_HOVER_X1) until (x + NT_HOVER_X2) &&
                mouseY in (y + NT_HOVER_Y1) until (y + NT_HOVER_Y2)
            ) {
                context.drawTooltip(textRenderer, buildElectricInfoLines(), mouseX, mouseY)
            }
        }
    }

    private fun buildElectricInfoLines(): List<Text> {
        val lines = mutableListOf<Text>()
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = NuclearReactorSync.ENERGY_CAPACITY
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val outputRate = handler.sync.getSyncedExtractedAmount()
        val temp = handler.sync.temperature.coerceIn(0, NuclearReactorSync.HEAT_CAPACITY)
        val heatProduced = handler.sync.totalHeatProduced
        val heatDissipated = handler.sync.totalHeatDissipated
        lines.add(Text.translatable("gui.ic2_120.nuclear_reactor.energy_line",
            EnergyFormatUtils.formatEu(energy), EnergyFormatUtils.formatEu(cap)))
        lines.add(Text.translatable("gui.ic2_120.nuclear_reactor.gen_output_line",
            EnergyFormatUtils.formatEu(inputRate), EnergyFormatUtils.formatEu(outputRate)))
        lines.add(Text.translatable("gui.ic2_120.nuclear_reactor.core_temp", temp))
        lines.add(Text.translatable("gui.ic2_120.nuclear_reactor.produce_dissipate", heatProduced, heatDissipated))
        return lines
    }

    private fun buildThermalInfoLines(): List<Text> {
        val lines = mutableListOf<Text>()
        val temp = handler.sync.temperature.coerceIn(0, NuclearReactorSync.HEAT_CAPACITY)
        val heatProduced = handler.sync.totalHeatProduced
        val heatDissipated = handler.sync.totalHeatDissipated
        val actualDissipated = handler.sync.actualHeatDissipated
        val thermalOutput = handler.sync.thermalHeatOutput
        val inputDroplets = handler.sync.inputCoolant.coerceAtLeast(0)
        val outputDroplets = handler.sync.outputHotCoolant.coerceAtLeast(0)
        val inputMb = inputDroplets / DROPLETS_PER_MB
        val outputMb = outputDroplets / DROPLETS_PER_MB
        val capMb = COOLANT_TANK_CAPACITY_MB
        lines.add(Text.translatable("gui.ic2_120.nuclear_reactor.core_temp", temp))
        lines.add(Text.literal("(流体堆发热翻倍)"))
        lines.add(Text.translatable("gui.ic2_120.nuclear_reactor.thermal_output", thermalOutput / 20))
        lines.add(Text.translatable("gui.ic2_120.nuclear_reactor.produce_dissipate", heatProduced, heatDissipated))
        lines.add(Text.translatable("gui.ic2_120.nuclear_reactor.actual_dissipate", actualDissipated / 20))
        lines.add(Text.translatable("gui.ic2_120.nuclear_reactor.coolant_input", inputMb, capMb))
        lines.add(Text.literal("%.1f".format(inputDroplets.toFloat() / COOLANT_TANK_DROPLETS.toFloat() * 100) + "%"))
        lines.add(Text.translatable("gui.ic2_120.nuclear_reactor.hot_coolant", outputMb, capMb))
        lines.add(Text.literal("%.1f".format(outputDroplets.toFloat() / COOLANT_TANK_DROPLETS.toFloat() * 100) + "%"))
        return lines
    }

    companion object {
        private val TEXTURE = Identifier("ic2", "textures/gui/guinuclearpowergenerationequipment.png")
        private val DROPLETS_PER_MB = (FluidConstants.BUCKET / 1000).toInt()
        private val COOLANT_TANK_DROPLETS = (FluidConstants.BUCKET * NuclearReactorSync.COOLANT_TANK_CAPACITY_BUCKETS).toInt()
        private val COOLANT_TANK_CAPACITY_MB = (COOLANT_TANK_DROPLETS / DROPLETS_PER_MB)
        private const val TEX_W = 512
        private const val TEX_H = 512

        // Non-thermal background
        private const val NT_BG_U = 0
        private const val NT_BG_V = 0
        private const val NT_BG_W = 212
        private const val NT_BG_H = 296

        // Thermal background (248,100)-(460,397) = 212×297
        private const val TH_BG_U = 248
        private const val TH_BG_V = 100
        private const val TH_BG_W = 212
        private const val TH_BG_H = 297

        // Thermal GUI shift
        private const val TH_SHIFT_X = 248
        private const val TH_SHIFT_Y = 100

        // Heat bar texture UV
        private const val HEAT_U = 216
        private const val HEAT_V = 29
        private const val HEAT_W = 100
        private const val HEAT_H = 13

        // Lock overlay texture UV
        private const val LOCK_U = 217
        private const val LOCK_V = 6
        private const val LOCK_W = 15
        private const val LOCK_H = 15

        // Non-thermal element positions
        private const val NT_HEAT_X = 7
        private const val NT_HEAT_Y = 189
        private const val NT_TEXT_X1 = 109
        private const val NT_TEXT_X2 = 205
        private const val NT_TEXT_Y = 193
        private const val NT_HOVER_X1 = 7
        private const val NT_HOVER_Y1 = 215
        private const val NT_HOVER_X2 = 22
        private const val NT_HOVER_Y2 = 230
        private const val NT_BTN_X = 26
        private const val NT_BTN_Y = 5
        private const val NT_BTN_W = 50
        private const val NT_BTN_H = 18

        // Thermal element positions (texture coords - shift)
        private const val TH_HEAT_X = 255 - TH_SHIFT_X
        private const val TH_HEAT_Y = 290 - TH_SHIFT_Y
        private const val TH_TEXT_X1 = 357 - TH_SHIFT_X
        private const val TH_TEXT_X2 = 453 - TH_SHIFT_X
        private const val TH_TEXT_Y = 294 - TH_SHIFT_Y
        private const val TH_HOVER_X1 = 255 - TH_SHIFT_X
        private const val TH_HOVER_Y1 = 316 - TH_SHIFT_Y
        private const val TH_HOVER_X2 = 270 - TH_SHIFT_X
        private const val TH_HOVER_Y2 = 331 - TH_SHIFT_Y
        private const val TH_FLUID_L_X = 258 - TH_SHIFT_X
        private const val TH_FLUID_L_Y = 154 - TH_SHIFT_Y
        private const val TH_FLUID_L_X2 = 270 - TH_SHIFT_X
        private const val TH_FLUID_L_Y2 = 255 - TH_SHIFT_Y
        private const val TH_FLUID_R_X = 438 - TH_SHIFT_X
        private const val TH_FLUID_R_Y = 154 - TH_SHIFT_Y
        private const val TH_FLUID_R_X2 = 450 - TH_SHIFT_X
        private const val TH_FLUID_R_Y2 = 255 - TH_SHIFT_Y
        private const val TH_SLOT_COOL_IN_X = 256 - TH_SHIFT_X
        private const val TH_SLOT_COOL_IN_Y = 125 - TH_SHIFT_Y
        private const val TH_SLOT_COOL_OUT_X = 256 - TH_SHIFT_X
        private const val TH_SLOT_COOL_OUT_Y = 269 - TH_SHIFT_Y
        private const val TH_SLOT_HOT_IN_X = 436 - TH_SHIFT_X
        private const val TH_SLOT_HOT_IN_Y = 125 - TH_SHIFT_Y
        private const val TH_SLOT_HOT_OUT_X = 436 - TH_SHIFT_X
        private const val TH_SLOT_HOT_OUT_Y = 269 - TH_SHIFT_Y
        private const val TH_BTN_X = 259 - TH_SHIFT_X
        private const val TH_BTN_Y = 104 - TH_SHIFT_Y
        private const val TH_BTN_W = 50
        private const val TH_BTN_H = 14

        private const val PANEL_BOTTOM_PADDING = 8
    }
}
