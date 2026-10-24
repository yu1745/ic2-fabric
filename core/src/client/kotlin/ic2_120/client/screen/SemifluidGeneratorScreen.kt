package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.FluidUtils
import ic2_120.client.t
import ic2_120.content.block.SemifluidGeneratorBlock
import ic2_120.content.recipes.ModTags
import ic2_120.content.screen.SemifluidGeneratorScreenHandler
import ic2_120.content.sync.SemifluidGeneratorSync
import ic2_120.registry.annotation.ModScreen
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.minecraft.client.resource.language.I18n
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.fluid.Fluid
import net.minecraft.registry.Registries
import net.minecraft.registry.tag.TagKey
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier

@ModScreen(block = SemifluidGeneratorBlock::class)
class SemifluidGeneratorScreen(
    handler: SemifluidGeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<SemifluidGeneratorScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = 176
        backgroundHeight = 161
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, 176, 161, 256, 256)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = SemifluidGeneratorSync.ENERGY_CAPACITY
        val energyFrac = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f

        val fuelDroplets = handler.sync.fuelAmount.coerceAtLeast(0)
        val fuelCapDroplets = 8 * FluidConstants.BUCKET
        val fuelFrac = if (fuelCapDroplets > 0) (fuelDroplets.toFloat() / fuelCapDroplets).coerceIn(0f, 1f) else 0f

        val inputRate = handler.sync.getSyncedInsertedAmount()
        val outputRate = handler.sync.getSyncedExtractedAmount()

        // 能量条位于 (120, 19) — 24×16 水平纹理来自 guiheatsourcegenerator.png (178,2)-(202,18)
        drawEnergyGauge(context, x + 120, y + 19, energyFrac)

        // 燃料储量条：区域 (82,23)-(94,69) = 12×46
        drawFuelBar(context, x + 82, y + 23, fuelFrac, handler.sync.fuelFluidRawId)

        // 标题文字居中于 y=6
        context.drawText(textRenderer, title, x + (176 - textRenderer.getWidth(title)) / 2, y + 6, 0x404040, false)

        // EU发电/输出文字显示在左侧
        val generateText = t("gui.ic2_120.generate_eu", EnergyFormatUtils.formatRaw(inputRate))
        val outputText = t("gui.ic2_120.output_eu", EnergyFormatUtils.formatRaw(outputRate))
        val sideTextWidth = maxOf(textRenderer.getWidth(generateText), textRenderer.getWidth(outputText))
        val sideTextX = x - sideTextWidth - 4
        context.drawText(textRenderer, generateText, sideTextX, y + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, outputText, sideTextX, y + 20, 0xAAAAAA, false)

        // 能量条悬停 (120,19)-(144,35) = 24×16
        if (mouseX in x + 120 until x + 144 && mouseY in y + 19 until y + 35) {
            context.drawTooltip(
                textRenderer,
                Text.literal("储能：${EnergyFormatUtils.formatRaw(energy)} / ${EnergyFormatUtils.formatRaw(cap)} EU"),
                mouseX, mouseY
            )
        }

        // 燃料槽悬停 (82,23)-(94,69) = 12×46
        if (mouseX in x + 82 until x + 94 && mouseY in y + 23 until y + 69) {
            val fuelLines = mutableListOf<Text>()
            if (fuelDroplets > 0) {
                val fuelMb = fuelDroplets / DROPLETS_PER_MB
                val fuelCapMb = fuelCapDroplets / DROPLETS_PER_MB
                fuelLines.add(Text.translatable("gui.ic2_120.semifluid_generator.fuel_tooltip", "%,d".format(fuelMb), "%,d".format(fuelCapMb)))
            } else {
                fuelLines.add(Text.translatable("ic2.generic.text.empty"))
            }
            addSupportedFluidTooltipLines(fuelLines)
            context.drawTooltip(textRenderer, fuelLines, mouseX, mouseY)
        }

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun drawEnergyGauge(context: DrawContext, gx: Int, gy: Int, fraction: Float) {
        val barW = 24
        val barH = 16
        val fillW = (fraction.coerceIn(0f, 1f) * barW).toInt()
        if (fillW <= 0) return
        context.enableScissor(gx, gy, gx + fillW, gy + barH)
        context.drawTexture(TEXTURE, gx, gy, 178f, 2f, barW, barH, 256, 256)
        context.disableScissor()
    }

    private fun drawFuelBar(context: DrawContext, gx: Int, gy: Int, fraction: Float, fuelFluidRawId: Int) {
        val barW = 12
        val barH = 46
        val fillH = (fraction.coerceIn(0f, 1f) * barH).toInt()
        if (fillH <= 0) return

        // 获取流体 sprite 和颜色
        val fluid = if (fuelFluidRawId > 0) Registries.FLUID.get(fuelFluidRawId) else null
        val fluidHandler = fluid?.let { FluidRenderHandlerRegistry.INSTANCE.get(it) }
        val sprites = fluidHandler?.getFluidSprites(null, null, fluid.defaultState)
        val sprite = sprites?.getOrNull(0)
        val color = fluid?.let { FluidUtils.getFluidColor(it) } ?: -1

        if (sprite != null && color != -1) {
            val r = ((color shr 16) and 0xFF) / 255f
            val g = ((color shr 8) and 0xFF) / 255f
            val b = (color and 0xFF) / 255f
            val fillY = gy + barH - fillH
            context.enableScissor(gx, fillY, gx + barW, gy + barH)
            for (sy in fillY until (gy + barH) step 16) {
                val tileH = minOf(16, gy + barH - sy)
                for (sx in gx until (gx + barW) step 16) {
                    val tileW = minOf(16, gx + barW - sx)
                    context.drawSprite(sx, sy, 0, tileW, tileH, sprite, r, g, b, 1f)
                }
            }
            context.disableScissor()
        }

        // 容器标示纹理覆盖，有流体时才渲染
        if (fillH > 0) {
            context.enableScissor(gx, gy, gx + barW, gy + barH)
            context.drawTexture(TEXTURE, gx + 1, gy, 179f, 23f, barW, barH, 256, 256)
            context.disableScissor()
        }
    }

    private fun addSupportedFluidTooltipLines(lines: MutableList<Text>) {
        lines.add(Text.translatable("gui.ic2_120.semifluid_generator.supported_fluids").formatted(Formatting.GOLD))
        addSupportedFluidGroup(
            lines,
            Text.translatable("gui.ic2_120.semifluid_generator.biofuel_equivalent", "32,000", "16").formatted(Formatting.GRAY),
            ModTags.Compat.Fluids.SEMIFLUID_BIOFUEL_EQUIVALENT
        )
        addSupportedFluidGroup(
            lines,
            Text.translatable("gui.ic2_120.semifluid_generator.creosote_equivalent", "3,200", "8").formatted(Formatting.GRAY),
            ModTags.Compat.Fluids.SEMIFLUID_CREOSOTE_EQUIVALENT
        )
    }

    private fun addSupportedFluidGroup(lines: MutableList<Text>, header: Text, tag: TagKey<Fluid>) {
        lines.add(header)
        val fluids = supportedFluids(tag)
        if (fluids.isEmpty()) {
            lines.add(Text.translatable("gui.ic2_120.semifluid_generator.no_supported_fluids").formatted(Formatting.DARK_GRAY))
            return
        }
        fluids.forEach { fluid ->
            lines.add(Text.literal("  - ").formatted(Formatting.DARK_GRAY).append(fluidDisplayText(fluid)))
        }
    }

    private fun supportedFluids(tag: TagKey<Fluid>): List<Fluid> {
        val fluids = mutableListOf<Fluid>()
        Registries.FLUID.forEach { fluid ->
            // Fluid.isIn(TagKey) 在 1.20.1 已 @Deprecated，改用未废弃的 FluidState.isIn。
            if (fluid.defaultState.isIn(tag) && !isFlowingVariant(fluid)) fluids.add(fluid)
        }
        return fluids.sortedBy { Registries.FLUID.getId(it).toString() }
    }

    private fun fluidDisplayText(fluid: Fluid): Text {
        val id = Registries.FLUID.getId(fluid)
        val fluidKey = "fluid.${id.namespace}.${id.path}"
        val blockKey = "block.${id.namespace}.${id.path}"
        val fluidName = when {
            I18n.hasTranslation(fluidKey) -> Text.translatable(fluidKey)
            I18n.hasTranslation(blockKey) -> Text.translatable(blockKey)
            else -> Text.literal(humanizeFluidPath(id.path))
        }.formatted(Formatting.DARK_AQUA)
        if (id.namespace == "ic2_120") return fluidName
        return Text.literal("[${id.namespace}] ").formatted(Formatting.DARK_GRAY)
            .append(fluidName)
    }

    private fun isFlowingVariant(fluid: Fluid): Boolean {
        val path = Registries.FLUID.getId(fluid).path
        return path.startsWith("flowing_") || path.endsWith("_flowing")
    }

    private fun humanizeFluidPath(path: String): String {
        return path
            .removePrefix("flowing_")
            .removeSuffix("_fluid")
            .replace('_', ' ')
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }

    companion object {
        private val TEXTURE = Identifier("ic2", "textures/gui/guiheatsourcegenerator.png")
        private const val DROPLETS_PER_MB = FluidConstants.BUCKET / 1000
    }
}
