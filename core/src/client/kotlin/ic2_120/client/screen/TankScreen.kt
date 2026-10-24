package ic2_120.client.screen

import ic2_120.client.FluidUtils
import ic2_120.content.screen.TankScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Inventory
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

@ModScreen(handler = "tank")
class TankScreen(
    val handler: TankScreenHandler,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<TankScreenHandler>(handler, playerInventory, title) {

    init {
        imageWidth = 176
        imageHeight = 166
    }

    override fun drawBackground(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.blit(TEXTURE, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, TEXTURE_SIZE, TEXTURE_SIZE)

        // 与压缩机相同的升级提示图标，复用上游 uptips 资源。
        context.blit(
            UPTIPS_TEXTURE, leftPos + UPTIPS_X, topPos + UPTIPS_Y,
            0f, 0f, UPTIPS_SIZE, UPTIPS_SIZE,
            UPTIPS_SIZE, UPTIPS_SIZE
        )

        // 槽位纹理 (179,3)-(197,21) = 18x18
        context.blit(
            TEXTURE, leftPos + SLOT_X, topPos + SLOT_Y,
            SLOT_U.toFloat(), SLOT_V.toFloat(),
            SLOT_SIZE, SLOT_SIZE,
            TEXTURE_SIZE, TEXTURE_SIZE
        )

        // 流体全铺渲染至 (79,27)-(97,45) = 18x18，使用 FluidUtils 着色
        val fluidId = menu.sync.fluidId ?: return
        val fluid = BuiltInRegistries.FLUID.getValue(fluidId)
        val renderHandler = FluidRenderHandlerRegistry.INSTANCE.get(fluid) ?: return
        val sprites = renderHandler.getFluidSprites(null, null, fluid.defaultFluidState().createLegacyBlock())
        val sprite = sprites[0]
        val amount = menu.sync.fluidAmount
        if (amount <= 0) return
        val color = FluidUtils.getFluidColor(fluid)
        val (r, g, b) = if (color != -1) {
            Triple(((color shr 16) and 0xFF) / 255f, ((color shr 8) and 0xFF) / 255f, (color and 0xFF) / 255f)
        } else {
            Triple(1f, 1f, 1f)
        }
        context.blit(
            leftPos + FLUID_X, topPos + FLUID_Y, 0,
            FLUID_W, FLUID_H, sprite,
            r, g, b, 1f
        )
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        context.drawString(font, title, leftPos + (imageWidth - font.width(title)) / 2, topPos + 6, 0x404040, false)

        renderTooltip(context, mouseX, mouseY)

        val relX = mouseX - leftPos
        val relY = mouseY - topPos

        if (relX in UPTIPS_X until UPTIPS_X + UPTIPS_SIZE &&
            relY in UPTIPS_Y until UPTIPS_Y + UPTIPS_SIZE
        ) {
            context.renderComponentTooltip(
                font,
                listOf(
                    Component.translatable("gui.ic2_120.tank.level_change"),
                    Component.translatable("gui.ic2_120.tank.level_change_1s", formatRate(menu.sync.levelChange1s)),
                    Component.translatable("gui.ic2_120.tank.level_change_5s", formatRate(menu.sync.levelChange5s)),
                    Component.translatable("gui.ic2_120.tank.level_change_15s", formatRate(menu.sync.levelChange15s)),
                    Component.translatable("gui.ic2_120.tank.level_change_30s", formatRate(menu.sync.levelChange30s)),
                    Component.translatable("gui.ic2_120.tank.level_change_60s", formatRate(menu.sync.levelChange60s))
                ),
                mouseX, mouseY
            )
        }

        if (relX in FLUID_X until FLUID_X + FLUID_W &&
            relY in FLUID_Y until FLUID_Y + FLUID_H
        ) {
            context.renderOutline(
                leftPos + FLUID_X, topPos + FLUID_Y,
                FLUID_W, FLUID_H,
                0xFFFFFFFF.toInt()
            )

            val fluidId = menu.sync.fluidId
            val amount = menu.sync.fluidAmount
            val capacity = menu.sync.capacity

            val lines = if (fluidId != null && amount > 0) {
                val fluid = BuiltInRegistries.FLUID.getValue(fluidId)
                val fluidName = net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes.getName(
                    net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(fluid)
                ).string
                listOf(Component.literal(fluidName), Component.literal("${"%,d".format(amount)} / ${"%,d".format(capacity)} mB"))
            } else {
                listOf(Component.literal("空"))
            }

            context.renderComponentTooltip(font, lines, mouseX, mouseY)
        }
    }

    private fun formatRate(rate: Int): String = "%+d mB/s".format(rate)

    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath("ic2", "textures/gui/guiother.png")
        private val UPTIPS_TEXTURE = ResourceLocation.fromNamespaceAndPath("ic2", "textures/gui/uptips.png")
        private const val TEXTURE_SIZE = 256
        private const val UPTIPS_SIZE = 16
        private const val UPTIPS_X = 4
        private const val UPTIPS_Y = 4

        // 槽位纹理 (179,3)-(197,21) = 18x18, 渲染至 79,28
        private const val SLOT_U = 179
        private const val SLOT_V = 3
        private const val SLOT_SIZE = 18
        private const val SLOT_X = 79
        private const val SLOT_Y = 28

        // 流体渲染 (80,29)-(96,45) = 16x16
        private const val FLUID_X = 80
        private const val FLUID_Y = 29
        private const val FLUID_W = 16
        private const val FLUID_H = 16
    }

}
