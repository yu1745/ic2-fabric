package ic2_120_industrial_upgrade.client.screen

import ic2_120.client.FluidUtils
import ic2_120_industrial_upgrade.content.fluid.NeutronFluid
import ic2_120_industrial_upgrade.content.screen.NeutronFabricatorScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Inventory
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

/**
 * 中子制造机客户端 GUI。
 *
 * 复用 IC2 经典物质制造机的 GUI 背景贴图（中子制造机即物质制造机的改版，
 * 仅产出流体由 UU 物质改为中子流体，界面布局一致）。
 *
 * 渲染：背景、标题、能量进度文本、中子流体槽（动态填充 + 着色）、充电槽、玩家背包。
 */
@ModScreen(handler = "neutron_fabricator")
class NeutronFabricatorScreen(
    handler: NeutronFabricatorScreenHandler,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<NeutronFabricatorScreenHandler>(handler, playerInventory, title) {

    init {
        imageWidth = 176
        imageHeight = 166
        // 隐藏父类 AbstractContainerScreen 默认绘制的容器 title（由本类 render 手动居中绘制，避免重复）
        titleX = -1000
        titleLabelY = -1000
        // 玩家背包标题位置（贴图默认）
        playerInventoryTitleY = imageHeight - 94
    }

    override fun drawBackground(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.blit(TEXTURE, x, y, 0f, 0f, imageWidth, imageHeight, TEX_SIZE, TEX_SIZE)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        val left = x
        val top = y

        // 标题居中
        context.drawString(
            textRenderer, title,
            left + (imageWidth - textRenderer.getWidth(title)) / 2, top + 6,
            0x404040, false
        )

        // 中子流体槽渲染
        drawFluidTank(context, left, top)

        // 流体槽容量标示纹理（物质制造机贴图的左侧液面刻度），有流体时叠加
        if (handler.sync.fluidAmount > 0) {
            context.blit(
                TEXTURE, left + TANK_OVERLAY_X, top + TANK_OVERLAY_Y,
                TANK_OVERLAY_U.toFloat(), TANK_OVERLAY_V.toFloat(),
                TANK_OVERLAY_W, TANK_OVERLAY_H, TEX_SIZE, TEX_SIZE
            )
        }

        // 能量进度文本（百分比）
        val pct = handler.sync.progress.coerceIn(0, 100)
        context.drawString(
            textRenderer,
            Component.translatable("gui.ic2_120.matter_generator.progress_pct", pct),
            left + 12, top + 43, 0x000000, false
        )

        // 悬停提示
        val relX = mouseX - left
        val relY = mouseY - top

        // 流体槽悬停
        if (relX in TANK_X until TANK_X + TANK_W && relY in TANK_Y until TANK_Y + TANK_H) {
            val amt = handler.sync.fluidAmount.coerceAtLeast(0)
            val cap = handler.sync.fluidCapacity.coerceAtLeast(1)
            val lines = if (amt > 0) {
                listOf(
                    Component.translatable("fluid.ic2_120_industrial_upgrade.neutron"),
                    Component.literal("${"%,d".format(amt / DROPLETS_PER_MB)} / ${"%,d".format(cap / DROPLETS_PER_MB)} mB")
                )
            } else {
                listOf(Component.literal("空"))
            }
            context.renderComponentTooltip(textRenderer, lines, mouseX, mouseY)
        }

        renderTooltip(context, mouseX, mouseY)
    }

    /** 绘制中子流体槽内的流体（按填充比例着色平铺） */
    private fun drawFluidTank(context: GuiGraphics, left: Int, top: Int) {
        val amt = handler.sync.fluidAmount.coerceAtLeast(0)
        if (amt <= 0) return
        val cap = handler.sync.fluidCapacity.coerceAtLeast(1)
        val fraction = (amt.toFloat() / cap).coerceIn(0f, 1f)
        val fillH = (TANK_H * fraction).toInt().coerceAtLeast(1)
        val sx = left + TANK_X
        val sy = top + TANK_Y
        val sprite = neutronSprite ?: return
        val color = FluidUtils.getFluidColor(NeutronFluid.NEUTRON_STILL)
        if (color == -1) return
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        val fillY = sy + TANK_H - fillH
        context.enableScissor(sx, fillY, sx + TANK_W, sy + TANK_H)
        for (cy in fillY until (sy + TANK_H) step 16) {
            val tileH = minOf(16, sy + TANK_H - cy)
            for (cx in sx until (sx + TANK_W) step 16) {
                val tileW = minOf(16, sx + TANK_W - cx)
                context.drawSpriteM(cx, cy, 0, tileW, tileH, sprite, r, g, b, 1f)
            }
        }
        context.disableScissor()
    }

    companion object {
        private val DROPLETS_PER_MB = (FluidConstants.BUCKET / 1000).toInt()
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath("ic2", "textures/gui/guimattergenerator.png")
        private const val TEX_SIZE = 256

        // 流体槽区域（与 core 物质制造机贴图布局一致）
        private const val TANK_X = 100
        private const val TANK_Y = 26
        private const val TANK_W = 12
        private const val TANK_H = 47

        // 流体槽左侧容量刻度贴图
        private const val TANK_OVERLAY_U = 181
        private const val TANK_OVERLAY_V = 6
        private const val TANK_OVERLAY_W = 11
        private const val TANK_OVERLAY_H = 46
        private const val TANK_OVERLAY_X = 101
        private const val TANK_OVERLAY_Y = 27

        private val neutronSprite by lazy {
            FluidRenderHandlerRegistry.INSTANCE.getValue(NeutronFluid.NEUTRON_STILL)
                ?.getFluidSprites(null, null, NeutronFluid.NEUTRON_STILL.defaultBlockState())?.getOrNull(0)
        }
    }
}
