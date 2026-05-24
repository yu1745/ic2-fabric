package ic2_120.client.screen

import com.mojang.blaze3d.systems.RenderSystem
import ic2_120.client.FluidUtils
import ic2_120.client.t
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.SteamKineticGeneratorBlock
import ic2_120.content.screen.GuiSize
import ic2_120.content.screen.SteamKineticGeneratorScreenHandler
import ic2_120.content.sync.SteamKineticGeneratorSync
import ic2_120.registry.annotation.ModScreen
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.texture.Sprite
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.fluid.Fluid
import net.minecraft.fluid.Fluids
import net.minecraft.text.Text as McText
import net.minecraft.util.Identifier

/**
 * 蒸汽动能发电机 GUI — 1:1 对齐 ic2_origin GuiSteamKineticGenerator (176×166)。
 *
 * 纹理: ic2:textures/gui/guisteamkineticgenerator.png (256×256)
 *
 * ic2_origin 元素位置:
 *   - 蒸馏水罐: TankGauge Plain at (75, 21, 26×26)
 *   - 涡轮槽: SlotGrid at (80, 26)
 *   - 排气警告图标: Image at (36, 20, 30×26) uv=(176,0)-(206,26)
 *   - 冷凝警告图标: Image at (110, 20, 30×26) uv=(176,0)-(206,26)
 *   - 状态文本: TextLabel at (8, 51, 160×13)
 *   - 输出文本: TextLabel at (8, 68, 160×13)
 */
@ModScreen(block = SteamKineticGeneratorBlock::class)
class SteamKineticGeneratorScreen(
    handler: SteamKineticGeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: McText
) : HandledScreen<SteamKineticGeneratorScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = 176; backgroundHeight = 166
        titleX = 999
    }

    private val waterSprite: Sprite? by lazy {
        val handler = FluidRenderHandlerRegistry.INSTANCE.get(Fluids.WATER) ?: return@lazy null
        handler.getFluidSprites(null, null, Fluids.WATER.defaultState)?.getOrNull(0)
    }

    // ==== 背景 — 先填充再纹理 ====
    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        renderBackground(context, mouseX, mouseY, delta)
        val left = x; val top = y
        val sync = handler.sync

        // 蒸馏水罐填充 (TankGauge Plain at 75,21, 26×26) - fluid sprite + color rendering
        val waterFrac = if (SteamKineticGeneratorSync.DISTILLED_WATER_TANK_CAPACITY > 0)
            sync.distilledWaterAmount.toFloat() / SteamKineticGeneratorSync.DISTILLED_WATER_TANK_CAPACITY else 0f
        val tx = left + 75; val ty = top + 21; val tw = 26; val th = 26
        val fh = (waterFrac.coerceIn(0f, 1f) * th).toInt()
        if (fh > 0) {
            val fillY = ty + th - fh
            val color = FluidUtils.getFluidColor(Fluids.WATER)
            val r = ((color shr 16) and 0xFF) / 255f
            val g = ((color shr 8) and 0xFF) / 255f
            val b = (color and 0xFF) / 255f
            context.enableScissor(tx, fillY, tx + tw, ty + th)
            val sprite = waterSprite
            if (sprite != null) {
                for (sy in fillY until (ty + th) step 16) {
                    val tileH = minOf(16, ty + th - sy)
                    for (sx in tx until (tx + tw) step 16) {
                        val tileW = minOf(16, tx + tw - sx)
                        context.drawSprite(sx, sy, 0, tileW, tileH, sprite, r, g, b, 1f)
                    }
                }
            }
            context.disableScissor()
        }

        // 纹理
        context.drawTexture(TEXTURE, left, top, 0f, 0f, backgroundWidth, backgroundHeight, 256, 256)

        // 警告图标
        val iconU = 176; val iconV = 0; val iconW = 30; val iconH = 26
        if (sync.waterBlocked != 0) context.drawTexture(TEXTURE, left + 110, top + 20, iconU.toFloat(), iconV.toFloat(), iconW, iconH, 256, 256)

        // 玩家背包槽位边框
        GuiBackground.drawPlayerInventorySlotBorders(context, left, top, PLAYER_INV_Y, HOTBAR_Y, SLOT_SIZE)
    }

    // ==== 前景 ====
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x; val top = y
        val sync = handler.sync
        val hasTurbine = sync.hasTurbine != 0
        val waterBlocked = sync.waterBlocked != 0
        val kuOutput = sync.kuOutput.coerceAtLeast(0)

        // 状态文本 (TextLabel at 8,51)
        val statusColor = if (hasTurbine && !waterBlocked) 0xFF20EE7E.toInt() else 0xFFE44444.toInt()
        val statusKey = when {
            !hasTurbine -> "gui.ic2_120.kinetic_no_turbine"
            waterBlocked -> "gui.ic2_120.kinetic_water_blocked"
            kuOutput > 0 -> "gui.ic2_120.kinetic_active"
            else -> "gui.ic2_120.kinetic_waiting"
        }
        drawCentered(context, t(statusKey), left + 8, top + 51, 160, 13, statusColor)

        // 输出文本 (TextLabel at 8,68)
        drawCentered(context, t("gui.ic2_120.kinetic_output", kuOutput), left + 8, top + 68, 160, 13, 0xFF20EE7E.toInt())

        // uptips 纹理 (4,4)
        context.drawTexture(UPTIPS_TEXTURE, left + 4, top + 4, 0f, 0f, UPTIPS_SIZE, UPTIPS_SIZE, UPTIPS_SIZE, UPTIPS_SIZE)

        // 物品渲染（涡轮 + 背包 + hotbar）
        super.render(context, mouseX, mouseY, delta)
        drawMouseoverTooltip(context, mouseX, mouseY)

        // uptips 悬停提示
        val relX = mouseX - left
        val relY = mouseY - top
        if (relX in 4 until 4 + UPTIPS_SIZE && relY in 4 until 4 + UPTIPS_SIZE) {
            context.drawTooltip(
                textRenderer,
                listOf(
                    McText.translatable("gui.ic2_120.steam_kinetic_generator.uptips"),
                    McText.literal("§7").append(McText.translatable("item.ic2_120.pulling_upgrade")),
                    McText.literal("§7").append(McText.translatable("item.ic2_120.fluid_ejector_upgrade")),
                    McText.literal("§7").append(McText.translatable("item.ic2_120.fluid_pulling_upgrade"))
                ),
                mouseX, mouseY
            )
        }
    }

    private fun drawCentered(ctx: DrawContext, text: String, ax: Int, ay: Int, aw: Int, ah: Int, color: Int) {
        val tw = textRenderer.getWidth(text)
        ctx.drawText(textRenderer, text, ax + (aw - tw) / 2, ay + (ah - 8) / 2, color, true)
    }

    companion object {
        private val TEXTURE = Identifier.of("ic2", "textures/gui/guisteamkineticgenerator.png")
        private val UPTIPS_TEXTURE = Identifier.of("ic2", "textures/gui/uptips.png")
        private const val PLAYER_INV_Y = 84
        private const val HOTBAR_Y = 142
        private const val SLOT_SIZE = 18
        private const val UPTIPS_SIZE = 16
    }
}
