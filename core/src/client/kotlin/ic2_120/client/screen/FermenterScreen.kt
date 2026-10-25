package ic2_120.client.screen

import ic2_120.client.FluidUtils
import ic2_120.content.block.FermenterBlock
import ic2_120.content.fluid.ModFluids
import ic2_120.content.screen.FermenterScreenHandler
import ic2_120.content.sync.FermenterSync
import ic2_120.registry.annotation.ModScreen
import ic2_120.client.compat.FluidRenderHandlerRegistry
import ic2_120.client.compat.*
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.level.material.Fluid
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.minecraft.resources.Identifier

@ModScreen(block = FermenterBlock::class)
class FermenterScreen(
    val handler: FermenterScreenHandler,
    playerInventory: Inventory,
    title: Component
) : LegacyContainerScreen26<FermenterScreenHandler>(handler, playerInventory, title, 175, 184) {

    init {
    }

    override fun renderBg(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.blit(TEXTURE, leftPos, topPos, 0f, 0f, 175, 184, 256, 256)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)

        val inputBiomassMb = menu.sync.inputBiomass.coerceAtLeast(0)
        val outputBiogasMb = menu.sync.outputBiogas.coerceAtLeast(0)
        val bufferedHeat = menu.sync.bufferedHeat.coerceAtLeast(0)
        val progress = menu.sync.progress.coerceIn(0, FermenterSync.PROCESS_INTERVAL_TICKS)
        val isWorking = menu.sync.isWorking != 0
        val fertilizerProgress = menu.sync.fertilizerProgress.coerceAtLeast(0)

        // 标题居中于 y=6
        context.drawString(font, title, leftPos + (175 - font.width(title)) / 2, topPos + 6, 0x404040, false)

        // 升级提示图标 (3,3) 16×16
        context.blit(UPTIPS, leftPos + 3, topPos + 3, 0f, 0f, 16, 16, 16, 16)

        // 生物质流体槽：区域 (37,48)-(85,78) = 48×30
        val inputFluid = menu.sync.fluidTypeToFluid(menu.sync.inputFluidType)
        val inputSprite = getFluidSprite(inputFluid)
        val inputColor = if (inputFluid != null) FluidUtils.getFluidColor(inputFluid) else -1
        drawFluidTank(context, leftPos + 37, topPos + 48, 48, 30, inputBiomassMb, TANK_CAPACITY_DROPLETS, inputSprite, inputColor)

        // 工作进度纹理：(178,3)-(219,7) = 41×4，工作进行时自左向右绘制
        if (isWorking) {
            val progFrac = progress.toFloat() / FermenterSync.PROCESS_INTERVAL_TICKS
            drawProgressH(context, leftPos + 41, topPos + 40, 41, 4, 178f, 3f, progFrac)
        }

        // 热量条：区域 (107,58)-(111,99) = 4×41
        val heatFrac = (bufferedHeat.toFloat() / 40000f).coerceIn(0f, 1f)
        drawHeatBar(context, leftPos + 107, topPos + 58, heatFrac)

        // 生物燃料流体槽：区域 (128,25)-(140,72) = 12×47
        val outputFluid = menu.sync.fluidTypeToFluid(menu.sync.outputFluidType)
        val outputSprite = getFluidSprite(outputFluid)
        val outputColor = if (outputFluid != null) FluidUtils.getFluidColor(outputFluid) else -1
        drawFluidTank(context, leftPos + 128, topPos + 25, 12, 47, outputBiogasMb, TANK_CAPACITY_DROPLETS, outputSprite, outputColor)

        // 容量标示覆盖层，有生物燃料时渲染
        if (outputBiogasMb > 0) {
            context.blit(TEXTURE, leftPos + 129, topPos + 26, 180f, 10f, 12, 47, 256, 256)
        }

        // 肥料进度纹理：(178,80)-(219,88) = 41×8，自左向右
        val fertFrac = (fertilizerProgress.toFloat() / FermenterSync.FERTILIZER_PER_BIOMASS_BUCKET_DROPLETS).coerceIn(0f, 1f)
        drawProgressH(context, leftPos + 37, topPos + 87, 41, 8, 178f, 80f, fertFrac)

        // 工具提示
        val biomassX = leftPos + 37; val biomassY = topPos + 48; val biomassW = 48; val biomassH = 30
        val heatX = leftPos + 107; val heatY = topPos + 58; val heatW = 4; val heatH = 41
        val biofuelX = leftPos + 128; val biofuelY = topPos + 25; val biofuelW = 12; val biofuelH = 47

        if (mouseX in biomassX until (biomassX + biomassW) && mouseY in biomassY until (biomassY + biomassH)) {
            val lines = if (inputBiomassMb > 0) listOf(Component.literal("生物质"), Component.literal("${"%,d".format(inputBiomassMb / DROPLETS_PER_MB)} / ${"%,d".format(TANK_CAPACITY_MB)} mB"))
                        else listOf(Component.literal("空"))
            context.renderComponentTooltip(font, lines, mouseX, mouseY)
        }
        if (mouseX in heatX until (heatX + heatW) && mouseY in heatY until (heatY + heatH)) {
            context.renderTooltip(font, Component.literal("热量: ${bufferedHeat} / 40000 HU"), mouseX, mouseY)
        }
        if (mouseX in biofuelX until (biofuelX + biofuelW) && mouseY in biofuelY until (biofuelY + biofuelH)) {
            val lines = if (outputBiogasMb > 0) listOf(Component.literal("生物燃料"), Component.literal("${"%,d".format(outputBiogasMb / DROPLETS_PER_MB)} / ${"%,d".format(TANK_CAPACITY_MB)} mB"))
                        else listOf(Component.literal("空"))
            context.renderComponentTooltip(font, lines, mouseX, mouseY)
        }
        if (mouseX in (leftPos + 3) until (leftPos + 19) && mouseY in (topPos + 3) until (topPos + 19)) {
            val upgradeTooltip = mutableListOf<Component>()
            upgradeTooltip.add(Component.translatable("gui.ic2_120.upgrade_slots"))
            for (id in SUPPORTED_UPGRADES) {
                val item = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", id))
                upgradeTooltip.add(item.defaultInstance.hoverName)
            }
            context.renderComponentTooltip(font, upgradeTooltip, mouseX, mouseY)
        }

        renderTooltip(context, mouseX, mouseY)
    }

    private fun getFluidSprite(fluid: Fluid?): TextureAtlasSprite? {
        if (fluid == null) return null
        val handler = FluidRenderHandlerRegistry.INSTANCE.get(fluid) ?: return null
        return handler.getFluidSprites(null, null, fluid.defaultFluidState())?.getOrNull(0)
    }

    /** 平铺流体纹理在槽内，自下而上 scissor 限制不超出区域，调用流体颜色渲染 */
    private fun drawFluidTank(
        context: GuiGraphics, gx: Int, gy: Int, tankW: Int, tankH: Int,
        amountMb: Int, capMb: Int, sprite: TextureAtlasSprite?, fluidColor: Int
    ) {
        val fillH = (amountMb.toFloat() / capMb * tankH).toInt().coerceIn(0, tankH)
        if (fillH <= 0) return
        val fillY = gy + tankH - fillH
        val r = ((fluidColor shr 16) and 0xFF) / 255f
        val g = ((fluidColor shr 8) and 0xFF) / 255f
        val b = (fluidColor and 0xFF) / 255f
        context.enableScissor(gx, fillY, gx + tankW, gy + tankH)
        if (sprite != null) {
            for (sy in fillY until (gy + tankH) step 16) {
                val h = minOf(16, gy + tankH - sy)
                for (sx in gx until (gx + tankW) step 16) {
                    val w = minOf(16, gx + tankW - sx)
                    context.blit(sx, sy, 0, w, h, sprite, r, g, b, 1f)
                }
            }
        }
        context.disableScissor()
    }

    /** 水平进度条自左向右填充 */
    private fun drawProgressH(context: GuiGraphics, gx: Int, gy: Int, w: Int, h: Int, u: Float, v: Float, frac: Float) {
        val fillW = (frac.coerceIn(0f, 1f) * w).toInt()
        if (fillW <= 0) return
        context.enableScissor(gx, gy, gx + fillW, gy + h)
        context.blit(TEXTURE, gx, gy, u, v, w, h, 256, 256)
        context.disableScissor()
    }

    /** 热量条：纹理 (199,13)-(203,54) = 4×41，自底向上填充 */
    private fun drawHeatBar(context: GuiGraphics, gx: Int, gy: Int, frac: Float) {
        val barW = 4
        val barH = 41
        val fillH = (frac.coerceIn(0f, 1f) * barH).toInt()
        if (fillH <= 0) return
        context.enableScissor(gx, gy + barH - fillH, gx + barW, gy + barH)
        context.blit(TEXTURE, gx, gy, 199f, 13f, barW, barH, 256, 256)
        context.disableScissor()
    }

    companion object {
        private val TEXTURE = Identifier.fromNamespaceAndPath("ic2", "textures/gui/guifermenter.png")
        private val UPTIPS = Identifier.fromNamespaceAndPath("ic2", "textures/gui/uptips.png")
        private val SUPPORTED_UPGRADES = listOf(
            "fluid_ejector_upgrade",
            "fluid_pulling_upgrade"
        )
        private val DROPLETS_PER_MB = (FluidConstants.BUCKET / 1000).toInt()
        private val TANK_CAPACITY_DROPLETS = (FluidConstants.BUCKET * FermenterSync.TANK_CAPACITY_BUCKETS).toInt()
        private val TANK_CAPACITY_MB = TANK_CAPACITY_DROPLETS / DROPLETS_PER_MB
    }
}
