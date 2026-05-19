package ic2_120.client.screen

import ic2_120.client.FluidUtils
import ic2_120.content.block.FermenterBlock
import ic2_120.content.fluid.ModFluids
import ic2_120.content.screen.FermenterScreenHandler
import ic2_120.content.sync.FermenterSync
import ic2_120.registry.annotation.ModScreen
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = FermenterBlock::class)
class FermenterScreen(
    handler: FermenterScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<FermenterScreenHandler>(handler, playerInventory, title) {

    private val biomassColor = FluidUtils.getFluidColor(ModFluids.BIOMASS_STILL)
    private val biomassSprite by lazy {
        FluidRenderHandlerRegistry.INSTANCE.get(ModFluids.BIOMASS_STILL)
            ?.getFluidSprites(null, null, ModFluids.BIOMASS_STILL.defaultState)?.getOrNull(0)
    }
    private val biofuelColor = FluidUtils.getFluidColor(ModFluids.BIOFUEL_STILL)
    private val biofuelSprite by lazy {
        FluidRenderHandlerRegistry.INSTANCE.get(ModFluids.BIOFUEL_STILL)
            ?.getFluidSprites(null, null, ModFluids.BIOFUEL_STILL.defaultState)?.getOrNull(0)
    }

    init {
        backgroundWidth = 175
        backgroundHeight = 184
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, 175, 184, 256, 256)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        val inputBiomassMb = handler.sync.inputBiomassMb.coerceAtLeast(0)
        val outputBiogasMb = handler.sync.outputBiogasMb.coerceAtLeast(0)
        val bufferedHeat = handler.sync.bufferedHeat.coerceAtLeast(0)
        val progress = handler.sync.progress.coerceIn(0, FermenterSync.PROCESS_INTERVAL_TICKS)
        val isWorking = handler.sync.isWorking != 0
        val fertilizerProgress = handler.sync.fertilizerProgress.coerceAtLeast(0)

        // 标题居中于 y=6
        context.drawText(textRenderer, title, x + (175 - textRenderer.getWidth(title)) / 2, y + 6, 0x404040, false)

        // 升级提示图标 (3,3) 16×16
        context.drawTexture(UPTIPS, x + 3, y + 3, 0f, 0f, 16, 16, 16, 16)

        // 生物质流体槽：区域 (37,48)-(85,78) = 48×30
        val biomassX = x + 37; val biomassY = y + 48; val biomassW = 48; val biomassH = 30
        drawFluidTank(context, biomassX, biomassY, biomassW, biomassH, inputBiomassMb, 10000, biomassSprite, biomassColor)

        // 工作进度纹理：(178,3)-(219,7) = 41×4，工作进行时自左向右绘制
        if (isWorking) {
            val progFrac = progress.toFloat() / FermenterSync.PROCESS_INTERVAL_TICKS
            drawProgressH(context, x + 41, y + 40, 41, 4, 178f, 3f, progFrac)
        }

        // 热量条：区域 (107,58)-(111,99) = 4×41
        val heatX = x + 107; val heatY = y + 58; val heatW = 4; val heatH = 41
        val heatFrac = (bufferedHeat.toFloat() / 40000f).coerceIn(0f, 1f)
        drawHeatBar(context, heatX, heatY, heatFrac)

        // 生物燃料流体槽：区域 (128,25)-(140,72) = 12×47
        val biofuelX = x + 128; val biofuelY = y + 25; val biofuelW = 12; val biofuelH = 47
        drawFluidTank(context, biofuelX, biofuelY, biofuelW, biofuelH, outputBiogasMb, 10000, biofuelSprite, biofuelColor)

        // 容量标示覆盖层，有生物燃料时渲染
        if (outputBiogasMb > 0) {
            context.drawTexture(TEXTURE, biofuelX, biofuelY, 180f, 10f, biofuelW, biofuelH, 256, 256)
        }

        // 肥料进度纹理：(178,80)-(219,88) = 41×8，自左向右
        val fertFrac = (fertilizerProgress.toFloat() / FermenterSync.FERTILIZER_PER_BIOMASS_BUCKET_MB).coerceIn(0f, 1f)
        drawProgressH(context, x + 37, y + 87, 41, 8, 178f, 80f, fertFrac)

        // 工具提示
        if (mouseX in biomassX until (biomassX + biomassW) && mouseY in biomassY until (biomassY + biomassH)) {
            context.drawTooltip(textRenderer, Text.literal("生物质: ${inputBiomassMb} mB"), mouseX, mouseY)
        }
        if (mouseX in heatX until (heatX + heatW) && mouseY in heatY until (heatY + heatH)) {
            context.drawTooltip(textRenderer, Text.literal("热量: ${bufferedHeat} / 40000 HU"), mouseX, mouseY)
        }
        if (mouseX in biofuelX until (biofuelX + biofuelW) && mouseY in biofuelY until (biofuelY + biofuelH)) {
            context.drawTooltip(textRenderer, Text.literal("生物燃料: ${outputBiogasMb} mB"), mouseX, mouseY)
        }
        if (mouseX in (x + 3) until (x + 19) && mouseY in (y + 3) until (y + 19)) {
            val upgradeTooltip = mutableListOf<Text>()
            upgradeTooltip.add(Text.translatable("gui.ic2_120.upgrade_slots"))
            for (id in SUPPORTED_UPGRADES) {
                val item = Registries.ITEM.get(Identifier("ic2_120", id))
                upgradeTooltip.add(item.name)
            }
            context.drawTooltip(textRenderer, upgradeTooltip, mouseX, mouseY)
        }

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    /** 平铺流体纹理在槽内，scissor 限制不超出区域 */
    private fun drawFluidTank(
        context: DrawContext, gx: Int, gy: Int, tankW: Int, tankH: Int,
        amountMb: Int, capMb: Int, sprite: net.minecraft.client.texture.Sprite?, color: Int
    ) {
        val fillH = (amountMb.toFloat() / capMb * tankH).toInt().coerceIn(0, tankH)
        if (fillH <= 0) return
        val topY = gy + tankH - fillH
        context.enableScissor(gx, gy, gx + tankW, gy + tankH)
        if (sprite != null) {
            for (sy in topY until (gy + tankH) step 16) {
                val h = minOf(16, gy + tankH - sy)
                for (sx in gx until (gx + tankW) step 16) {
                    val w = minOf(16, gx + tankW - sx)
                    context.drawSprite(sx, sy, 0, w, h, sprite)
                }
            }
        }
        context.fill(gx, topY, gx + tankW, gy + tankH, color)
        context.disableScissor()
    }

    /** 水平进度条自左向右填充 */
    private fun drawProgressH(context: DrawContext, gx: Int, gy: Int, w: Int, h: Int, u: Float, v: Float, frac: Float) {
        val fillW = (frac.coerceIn(0f, 1f) * w).toInt()
        if (fillW <= 0) return
        context.enableScissor(gx, gy, gx + fillW, gy + h)
        context.drawTexture(TEXTURE, gx, gy, u, v, w, h, 256, 256)
        context.disableScissor()
    }

    /** 热量条：纹理 (199,13)-(203,54) = 4×41，自底向上填充 */
    private fun drawHeatBar(context: DrawContext, gx: Int, gy: Int, frac: Float) {
        val barW = 4
        val barH = 41
        val fillH = (frac.coerceIn(0f, 1f) * barH).toInt()
        if (fillH <= 0) return
        context.enableScissor(gx, gy + barH - fillH, gx + barW, gy + barH)
        context.drawTexture(TEXTURE, gx, gy, 199f, 13f, barW, barH, 256, 256)
        context.disableScissor()
    }

    companion object {
        private val TEXTURE = Identifier("ic2", "textures/gui/guifermenter.png")
        private val UPTIPS = Identifier("ic2", "textures/gui/uptips.png")
        private val SUPPORTED_UPGRADES = listOf(
            "fluid_ejector_upgrade",
            "fluid_pulling_upgrade"
        )
    }
}
