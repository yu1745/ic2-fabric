package ic2_120.client.screen

import ic2_120.client.FluidUtils
import ic2_120.content.screen.TankScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(handler = "tank")
class TankScreen(
    handler: TankScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<TankScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = 176
        backgroundHeight = 166
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, backgroundWidth, backgroundHeight, TEXTURE_SIZE, TEXTURE_SIZE)

        // 与压缩机相同的升级提示图标，复用上游 uptips 资源。
        context.drawTexture(
            UPTIPS_TEXTURE, x + UPTIPS_X, y + UPTIPS_Y,
            0f, 0f, UPTIPS_SIZE, UPTIPS_SIZE,
            UPTIPS_SIZE, UPTIPS_SIZE
        )

        // 槽位纹理 (179,3)-(197,21) = 18x18
        context.drawTexture(
            TEXTURE, x + SLOT_X, y + SLOT_Y,
            SLOT_U.toFloat(), SLOT_V.toFloat(),
            SLOT_SIZE, SLOT_SIZE,
            TEXTURE_SIZE, TEXTURE_SIZE
        )

        // 流体全铺渲染至 (79,27)-(97,45) = 18x18，使用 FluidUtils 着色
        val fluidId = handler.sync.fluidId ?: return
        val fluid = Registries.FLUID.get(fluidId)
        val renderHandler = FluidRenderHandlerRegistry.INSTANCE.get(fluid) ?: return
        val sprites = renderHandler.getFluidSprites(null, null, fluid.defaultState)
        val sprite = sprites[0]
        val amount = handler.sync.fluidAmount
        if (amount <= 0) return
        val color = FluidUtils.getFluidColor(fluid)
        val (r, g, b) = if (color != -1) {
            Triple(((color shr 16) and 0xFF) / 255f, ((color shr 8) and 0xFF) / 255f, (color and 0xFF) / 255f)
        } else {
            Triple(1f, 1f, 1f)
        }
        context.drawSprite(
            x + FLUID_X, y + FLUID_Y, 0,
            FLUID_W, FLUID_H, sprite,
            r, g, b, 1f
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        context.drawText(textRenderer, title, x + (backgroundWidth - textRenderer.getWidth(title)) / 2, y + 6, 0x404040, false)

        drawMouseoverTooltip(context, mouseX, mouseY)

        val relX = mouseX - x
        val relY = mouseY - y

        if (relX in UPTIPS_X until UPTIPS_X + UPTIPS_SIZE &&
            relY in UPTIPS_Y until UPTIPS_Y + UPTIPS_SIZE
        ) {
            context.drawTooltip(
                textRenderer,
                listOf(
                    Text.translatable("gui.ic2_120.tank.level_change"),
                    Text.translatable("gui.ic2_120.tank.level_change_1s", formatRate(handler.sync.levelChange1s)),
                    Text.translatable("gui.ic2_120.tank.level_change_5s", formatRate(handler.sync.levelChange5s)),
                    Text.translatable("gui.ic2_120.tank.level_change_15s", formatRate(handler.sync.levelChange15s)),
                    Text.translatable("gui.ic2_120.tank.level_change_30s", formatRate(handler.sync.levelChange30s)),
                    Text.translatable("gui.ic2_120.tank.level_change_60s", formatRate(handler.sync.levelChange60s))
                ),
                mouseX, mouseY
            )
        }

        if (relX in FLUID_X until FLUID_X + FLUID_W &&
            relY in FLUID_Y until FLUID_Y + FLUID_H
        ) {
            context.drawBorder(
                x + FLUID_X, y + FLUID_Y,
                FLUID_W, FLUID_H,
                0xFFFFFFFF.toInt()
            )

            val fluidId = handler.sync.fluidId
            val amount = handler.sync.fluidAmount
            val capacity = handler.sync.capacity

            val lines = if (fluidId != null && amount > 0) {
                val fluid = Registries.FLUID.get(fluidId)
                val fluidName = net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes.getName(
                    net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(fluid)
                ).string
                listOf(Text.literal(fluidName), Text.literal("${"%,d".format(amount)} / ${"%,d".format(capacity)} mB"))
            } else {
                listOf(Text.literal("空"))
            }

            context.drawTooltip(textRenderer, lines, mouseX, mouseY)
        }
    }

    private fun formatRate(rate: Int): String = "%+d mB/s".format(rate)

    companion object {
        private val TEXTURE = Identifier("ic2", "textures/gui/guiother.png")
        private val UPTIPS_TEXTURE = Identifier("ic2", "textures/gui/uptips.png")
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
