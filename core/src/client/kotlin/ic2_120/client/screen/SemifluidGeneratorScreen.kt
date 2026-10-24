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
import net.minecraft.client.resources.language.I18n
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.level.material.Fluid
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.tags.TagKey
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.resources.ResourceLocation

@ModScreen(block = SemifluidGeneratorBlock::class)
class SemifluidGeneratorScreen(
    val handler: SemifluidGeneratorScreenHandler,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<SemifluidGeneratorScreenHandler>(handler, playerInventory, title) {

    init {
        imageWidth = 176
        imageHeight = 161
    }

    override fun drawBackground(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.blit(TEXTURE, leftPos, topPos, 0f, 0f, 176, 161, 256, 256)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        val energy = menu.sync.energy.toLong().coerceAtLeast(0)
        val cap = SemifluidGeneratorSync.ENERGY_CAPACITY
        val energyFrac = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f

        val fuelDroplets = menu.sync.fuelAmount.coerceAtLeast(0)
        val fuelCapDroplets = 8 * FluidConstants.BUCKET
        val fuelFrac = if (fuelCapDroplets > 0) (fuelDroplets.toFloat() / fuelCapDroplets).coerceIn(0f, 1f) else 0f

        val inputRate = menu.sync.getSyncedInsertedAmount()
        val outputRate = menu.sync.getSyncedExtractedAmount()

        // 能量条位于 (120, 19) — 24×16 水平纹理来自 guiheatsourcegenerator.png (178,2)-(202,18)
        drawEnergyGauge(context, leftPos + 120, topPos + 19, energyFrac)

        // 燃料储量条：区域 (82,23)-(94,69) = 12×46
        drawFuelBar(context, leftPos + 82, topPos + 23, fuelFrac, menu.sync.fuelFluidRawId)

        // 标题文字居中于 y=6
        context.drawString(font, title, leftPos + (176 - font.width(title)) / 2, topPos + 6, 0x404040, false)

        // EU发电/输出文字显示在左侧
        val generateText = t("gui.ic2_120.generate_eu", EnergyFormatUtils.formatRaw(inputRate))
        val outputText = t("gui.ic2_120.output_eu", EnergyFormatUtils.formatRaw(outputRate))
        val sideTextWidth = maxOf(font.width(generateText), font.width(outputText))
        val sideTextX = leftPos - sideTextWidth - 4
        context.drawString(font, generateText, sideTextX, topPos + 8, 0xAAAAAA, false)
        context.drawString(font, outputText, sideTextX, topPos + 20, 0xAAAAAA, false)

        // 能量条悬停 (120,19)-(144,35) = 24×16
        if (mouseX in leftPos + 120 until leftPos + 144 && mouseY in topPos + 19 until topPos + 35) {
            context.renderComponentTooltip(
                font,
                Component.literal("储能：${EnergyFormatUtils.formatRaw(energy)} / ${EnergyFormatUtils.formatRaw(cap)} EU"),
                mouseX, mouseY
            )
        }

        // 燃料槽悬停 (82,23)-(94,69) = 12×46
        if (mouseX in leftPos + 82 until leftPos + 94 && mouseY in topPos + 23 until topPos + 69) {
            val fuelLines = mutableListOf<Component>()
            if (fuelDroplets > 0) {
                val fuelMb = fuelDroplets / DROPLETS_PER_MB
                val fuelCapMb = fuelCapDroplets / DROPLETS_PER_MB
                fuelLines.add(Component.translatable("gui.ic2_120.semifluid_generator.fuel_tooltip", "%,d".format(fuelMb), "%,d".format(fuelCapMb)))
            } else {
                fuelLines.add(Component.translatable("ic2.generic.text.empty"))
            }
            addSupportedFluidTooltipLines(fuelLines)
            context.renderComponentTooltip(font, fuelLines, mouseX, mouseY)
        }

        renderTooltip(context, mouseX, mouseY)
    }

    private fun drawEnergyGauge(context: GuiGraphics, gx: Int, gy: Int, fraction: Float) {
        val barW = 24
        val barH = 16
        val fillW = (fraction.coerceIn(0f, 1f) * barW).toInt()
        if (fillW <= 0) return
        context.enableScissor(gx, gy, gx + fillW, gy + barH)
        context.blit(TEXTURE, gx, gy, 178f, 2f, barW, barH, 256, 256)
        context.disableScissor()
    }

    private fun drawFuelBar(context: GuiGraphics, gx: Int, gy: Int, fraction: Float, fuelFluidRawId: Int) {
        val barW = 12
        val barH = 46
        val fillH = (fraction.coerceIn(0f, 1f) * barH).toInt()
        if (fillH <= 0) return

        // 获取流体 sprite 和颜色
        val fluid = if (fuelFluidRawId > 0) BuiltInRegistries.FLUID.get(fuelFluidRawId) else null
        val fluidHandler = fluid?.let { FluidRenderHandlerRegistry.INSTANCE.get(it) }
        val sprites = fluidHandler?.getFluidSprites(null, null, fluid.defaultFluidState().createLegacyBlock())
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
                    context.blit(sx, sy, 0, tileW, tileH, sprite, r, g, b, 1f)
                }
            }
            context.disableScissor()
        }

        // 容器标示纹理覆盖，有流体时才渲染
        if (fillH > 0) {
            context.enableScissor(gx, gy, gx + barW, gy + barH)
            context.blit(TEXTURE, gx + 1, gy, 179f, 23f, barW, barH, 256, 256)
            context.disableScissor()
        }
    }

    private fun addSupportedFluidTooltipLines(lines: MutableList<Component>) {
        lines.add(Component.translatable("gui.ic2_120.semifluid_generator.supported_fluids").withStyle(ChatFormatting.GOLD))
        addSupportedFluidGroup(
            lines,
            Component.translatable("gui.ic2_120.semifluid_generator.biofuel_equivalent", "32,000", "16").withStyle(ChatFormatting.GRAY),
            ModTags.Compat.Fluids.SEMIFLUID_BIOFUEL_EQUIVALENT
        )
        addSupportedFluidGroup(
            lines,
            Component.translatable("gui.ic2_120.semifluid_generator.creosote_equivalent", "3,200", "8").withStyle(ChatFormatting.GRAY),
            ModTags.Compat.Fluids.SEMIFLUID_CREOSOTE_EQUIVALENT
        )
    }

    private fun addSupportedFluidGroup(lines: MutableList<Component>, header: Component, tag: TagKey<Fluid>) {
        lines.add(header)
        val fluids = supportedFluids(tag)
        if (fluids.isEmpty()) {
            lines.add(Component.translatable("gui.ic2_120.semifluid_generator.no_supported_fluids").withStyle(ChatFormatting.DARK_GRAY))
            return
        }
        fluids.forEach { fluid ->
            lines.add(Component.literal("  - ").withStyle(ChatFormatting.DARK_GRAY).append(fluidDisplayText(fluid)))
        }
    }

    private fun supportedFluids(tag: TagKey<Fluid>): List<Fluid> {
        val fluids = mutableListOf<Fluid>()
        BuiltInRegistries.FLUID.forEach { fluid ->
            // Fluid.`is`(TagKey) 在 1.20.1 已 @Deprecated，改用未废弃的 FluidState.isIn。
            if (fluid.defaultFluidState().createLegacyBlock().`is`(tag) && !isFlowingVariant(fluid)) fluids.add(fluid)
        }
        return fluids.sortedBy { BuiltInRegistries.FLUID.getId(it).toString() }
    }

    private fun fluidDisplayText(fluid: Fluid): Component {
        val id = BuiltInRegistries.FLUID.getId(fluid)
        val fluidKey = "fluid.${id.namespace}.${id.path}"
        val blockKey = "block.${id.namespace}.${id.path}"
        val fluidName = when {
            I18n.hasTranslation(fluidKey) -> Component.translatable(fluidKey)
            I18n.hasTranslation(blockKey) -> Component.translatable(blockKey)
            else -> Component.literal(humanizeFluidPath(id.path))
        }.withStyle(ChatFormatting.DARK_AQUA)
        if (id.namespace == "ic2_120") return fluidName
        return Component.literal("[${id.namespace}] ").withStyle(ChatFormatting.DARK_GRAY)
            .append(fluidName)
    }

    private fun isFlowingVariant(fluid: Fluid): Boolean {
        val path = BuiltInRegistries.FLUID.getId(fluid).path
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
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath("ic2", "textures/gui/guiheatsourcegenerator.png")
        private const val DROPLETS_PER_MB = FluidConstants.BUCKET / 1000
    }
}
