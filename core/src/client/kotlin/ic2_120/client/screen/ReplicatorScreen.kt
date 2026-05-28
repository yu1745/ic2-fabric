package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.FluidUtils
import ic2_120.client.t
import ic2_120.content.block.ReplicatorBlock
import ic2_120.content.network.SelectTemplatePayload
import ic2_120.content.screen.ReplicatorScreenHandler
import ic2_120.content.sync.ReplicatorSync
import ic2_120.content.uu.UuTemplateEntry
import ic2_120.content.uu.findUniqueAdjacentPatternStorage
import ic2_120.registry.annotation.ModScreen
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.fluid.Fluid
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import ic2_120.content.fluid.ModFluids
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.minecraft.util.Identifier

@ModScreen(block = ReplicatorBlock::class)
class ReplicatorScreen(
    handler: ReplicatorScreenHandler, playerInventory: PlayerInventory, title: Text
) : HandledScreen<ReplicatorScreenHandler>(handler, playerInventory, title) {

    private var showTemplateInfo = false
    private var lastStatus = -1
    private var templates: List<UuTemplateEntry> = emptyList()
    private var selectedIndex = -1
    private var templateInfoText = ""
    private var cancelBtn: ButtonWidget? = null
    private var singleBtn: ButtonWidget? = null
    private var repeatBtn: ButtonWidget? = null

    init {
        backgroundWidth = 176
        backgroundHeight = 184
    }

    override fun init() {
        super.init()
        cancelBtn = ButtonWidget.builder(Text.empty()) {
            MinecraftClient.getInstance().player?.networkHandler?.sendPacket(
                ButtonClickC2SPacket(handler.syncId, ReplicatorScreenHandler.BUTTON_CANCEL)
            )
        }.dimensions(0, 0, 18, 18).build()
        singleBtn = ButtonWidget.builder(Text.empty()) {
            MinecraftClient.getInstance().player?.networkHandler?.sendPacket(
                ButtonClickC2SPacket(handler.syncId, ReplicatorScreenHandler.BUTTON_MODE_SINGLE)
            )
        }.dimensions(0, 0, 18, 18).build()
        repeatBtn = ButtonWidget.builder(Text.empty()) {
            MinecraftClient.getInstance().player?.networkHandler?.sendPacket(
                ButtonClickC2SPacket(handler.syncId, ReplicatorScreenHandler.BUTTON_MODE_REPEAT)
            )
        }.dimensions(0, 0, 18, 18).build()
        addDrawableChild(cancelBtn!!)
        addDrawableChild(singleBtn!!)
        addDrawableChild(repeatBtn!!)
    }

    private fun updateButtons() {
        cancelBtn?.setPosition(x + 71, y + 81)
        singleBtn?.setPosition(x + 88, y + 81)
        repeatBtn?.setPosition(x + 105, y + 81)
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, backgroundWidth, backgroundHeight, TEX_SIZE, TEX_SIZE)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        updateButtons()
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)

        val left = x
        val top = y

        context.drawText(textRenderer, title, left + (backgroundWidth - textRenderer.getWidth(title)) / 2, top + 6, 0x404040, false)

        val world = client?.world
        val storage = world?.let { findUniqueAdjacentPatternStorage(it, handler.blockPos) }
        templates = storage?.getTemplatesSnapshot().orEmpty()
        selectedIndex = storage?.selectedTemplateIndex ?: -1

        val status = handler.sync.status
        if (status != lastStatus) {
            if (status == ReplicatorSync.STATUS_RUNNING || status == ReplicatorSync.STATUS_COMPLETE) {
                showTemplateInfo = false
            }
            lastStatus = status
        }

        // 模式切换纹理 (198,3)-(250,21) = 52×18 渲染至 (71,81)
        context.drawTexture(TEXTURE, left + MODE_TEX_X, top + MODE_TEX_Y,
            MODE_TEX_U.toFloat(), MODE_TEX_V.toFloat(), MODE_TEX_W, MODE_TEX_H, TEX_SIZE, TEX_SIZE)

        // 电量条 (180,3)-(194,18) = 14×15 渲染至 (134,85)
        drawEnergyBar(context, left, top)

        // 流体槽 (31,34)-(43,81) = 12×47
        drawFluidTank(context, left, top)

        // 容量标示纹理 (181,25)-(192,71) = 11×46 → (32,35)，有流体时渲染
        if (handler.sync.fluidAmount > 0) {
            context.drawTexture(TEXTURE, left + TANK_OVERLAY_X, top + TANK_OVERLAY_Y,
                TANK_OVERLAY_U.toFloat(), TANK_OVERLAY_V.toFloat(), TANK_OVERLAY_W, TANK_OVERLAY_H, TEX_SIZE, TEX_SIZE)
        }

        // 复制产物详情区域 (91,17) 18×18
        drawProductDetail(context, left, top)

        // 状态文本区域 (50,37)-(144,51) 居中
        drawStatusText(context, left, top)

        // uptips (4,4) 16×16
        context.drawTexture(UPTIPS_TEXTURE, left + 4, top + 4, 0f, 0f, 16, 16, 16, 16)

        // 悬停高亮和提示
        val relX = mouseX - left
        val relY = mouseY - top

        // 向左键 (79,16)-(89,34)
        if (templates.size > 1 && relX in NAV_LEFT_X1..NAV_LEFT_X2 && relY in NAV_Y1..NAV_Y2) {
            context.fill(left + NAV_LEFT_X1, top + NAV_Y1, left + NAV_LEFT_X2 + 1, top + NAV_Y2 + 1, 0x80FFFFFF.toInt())
        }
        // 向右键 (109,16)-(118,34)
        if (templates.size > 1 && relX in NAV_RIGHT_X1..NAV_RIGHT_X2 && relY in NAV_Y1..NAV_Y2) {
            context.fill(left + NAV_RIGHT_X1, top + NAV_Y1, left + NAV_RIGHT_X2 + 1, top + NAV_Y2 + 1, 0x80FFFFFF.toInt())
        }

        // 电量条悬停
        if (relX in ENERGY_X until ENERGY_X + ENERGY_W && relY in ENERGY_Y until ENERGY_Y + ENERGY_H) {
            val energy = handler.sync.energy.toLong().coerceAtLeast(0)
            val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1)
            context.drawTooltip(textRenderer,
                listOf(Text.literal("储能：${"%,d".format(energy)} / ${"%,d".format(cap)} EU")),
                mouseX, mouseY)
        }

        // 流体槽悬停
        if (relX in TANK_X until TANK_X + TANK_W && relY in TANK_Y until TANK_Y + TANK_H) {
            val amt = handler.sync.fluidAmount.coerceAtLeast(0)
            val cap = handler.sync.fluidCapacity.coerceAtLeast(1)
            val lines = if (amt > 0) listOf(Text.literal("UU物质"), Text.literal("${"%,d".format(amt / DROPLETS_PER_MB)} / ${"%,d".format(cap / DROPLETS_PER_MB)} mB"))
                        else listOf(Text.literal("空"))
            context.drawTooltip(textRenderer, lines, mouseX, mouseY)
        }

        // 模式按钮悬停
        if (relX in 71..88 && relY in 81..98) {
            context.drawTooltip(textRenderer, listOf(Text.translatable("gui.ic2_120.replicator.cancel")), mouseX, mouseY)
        }
        if (relX in 88..105 && relY in 81..98) {
            context.drawTooltip(textRenderer, listOf(Text.translatable("gui.ic2_120.replicator.mode_single")), mouseX, mouseY)
        }
        if (relX in 105..122 && relY in 81..98) {
            context.drawTooltip(textRenderer, listOf(Text.translatable("gui.ic2_120.replicator.mode_repeat")), mouseX, mouseY)
        }

        // uptips悬停
        if (relX in 4 until 20 && relY in 4 until 20) {
            context.drawTooltip(textRenderer, listOf(
                Text.translatable("gui.ic2_120.replicator.uptips"),
                Text.literal("§7").append(Text.translatable("item.ic2_120.overclocker_upgrade")),
                Text.literal("§7").append(Text.translatable("item.ic2_120.transformer_upgrade")),
                Text.literal("§7").append(Text.translatable("item.ic2_120.energy_storage_upgrade")),
                Text.literal("§7").append(Text.translatable("item.ic2_120.ejector_upgrade")),
                Text.literal("§7").append(Text.translatable("item.ic2_120.fluid_ejector_upgrade")),
                Text.literal("§7").append(Text.translatable("item.ic2_120.fluid_pulling_upgrade"))
            ), mouseX, mouseY)
        }

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun drawEnergyBar(context: DrawContext, left: Int, top: Int) {
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1)
        val fraction = (energy.toFloat() / cap).coerceIn(0f, 1f)
        if (fraction <= 0f) return
        val fillH = (ENERGY_H * fraction).toInt().coerceAtLeast(1)
        context.enableScissor(
            left + ENERGY_X, top + ENERGY_Y + ENERGY_H - fillH,
            left + ENERGY_X + ENERGY_W, top + ENERGY_Y + ENERGY_H
        )
        context.drawTexture(TEXTURE, left + ENERGY_X, top + ENERGY_Y,
            ENERGY_U.toFloat(), ENERGY_V.toFloat(), ENERGY_W, ENERGY_H, TEX_SIZE, TEX_SIZE)
        context.disableScissor()
    }

    private fun drawFluidTank(context: DrawContext, left: Int, top: Int) {
        val amt = handler.sync.fluidAmount.coerceAtLeast(0)
        if (amt <= 0) return
        val cap = handler.sync.fluidCapacity.coerceAtLeast(1)
        val fraction = (amt.toFloat() / cap).coerceIn(0f, 1f)
        val fillH = (TANK_H * fraction).toInt().coerceAtLeast(1)
        val sx = left + TANK_X
        val sy = top + TANK_Y
        val sprite = uuMatterSprite ?: return
        val color = FluidUtils.getFluidColor(ModFluids.UU_MATTER_STILL)
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
                context.drawSprite(cx, cy, 0, tileW, tileH, sprite, r, g, b, 1f)
            }
        }
        context.disableScissor()
    }

    private fun drawProductDetail(context: DrawContext, left: Int, top: Int) {
        val template = templates.getOrNull(selectedIndex) ?: return
        val stack = templateToStack(template)
        if (!stack.isEmpty) {
            context.drawItem(stack, left + PRODUCT_X, top + PRODUCT_Y)
        }
    }

    private fun drawStatusText(context: DrawContext, left: Int, top: Int) {
        val text = if (showTemplateInfo) {
            templateInfoText
        } else {
            val base = statusText(handler.sync.status)
            if (handler.sync.status == ReplicatorSync.STATUS_RUNNING) {
                val pct = if (handler.sync.progressMaxUb > 0)
                    (handler.sync.progressUb.toFloat() / handler.sync.progressMaxUb * 100).toInt().coerceIn(0, 100)
                else 0
                "$base($pct%)"
            } else base
        }
        if (text.isEmpty()) return
        val tw = textRenderer.getWidth(text)
        val areaCenterX = (STATUS_X1 + STATUS_X2) / 2
        val color = if (showTemplateInfo) 0xFFFFFF else statusColor(handler.sync.status)
        context.drawText(textRenderer, text, left + areaCenterX - tw / 2, top + STATUS_Y, color, false)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) {
            val relX = mouseX.toInt() - x
            val relY = mouseY.toInt() - y
            if (templates.size > 1) {
                if (relX in NAV_LEFT_X1..NAV_LEFT_X2 && relY in NAV_Y1..NAV_Y2) {
                    navigateTemplate(-1)
                    return true
                }
                if (relX in NAV_RIGHT_X1..NAV_RIGHT_X2 && relY in NAV_Y1..NAV_Y2) {
                    navigateTemplate(1)
                    return true
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    private fun navigateTemplate(delta: Int) {
        if (templates.isEmpty()) return
        val newIndex = ((selectedIndex + delta) % templates.size + templates.size) % templates.size
        ClientPlayNetworking.send(SelectTemplatePayload(handler.blockPos, newIndex))
        // 更新本地显示
        val template = templates.getOrNull(newIndex)
        if (template != null) {
            templateInfoText = "${template.displayName().string} (${template.uuCostUb} uB)"
        }
        showTemplateInfo = true
    }

    private fun templateToStack(template: UuTemplateEntry): ItemStack {
        val id = Identifier.tryParse(template.itemId) ?: return ItemStack.EMPTY
        val item = Registries.ITEM.getOrEmpty(id).orElse(null) ?: return ItemStack.EMPTY
        return if (item == net.minecraft.item.Items.AIR) ItemStack.EMPTY else ItemStack(item)
    }

    private fun statusText(status: Int): String = when (status) {
        ReplicatorSync.STATUS_NO_REDSTONE -> t("gui.ic2_120.replicator.status_no_redstone")
        ReplicatorSync.STATUS_NO_STORAGE -> t("gui.ic2_120.status_no_storage")
        ReplicatorSync.STATUS_NO_TEMPLATE -> t("gui.ic2_120.replicator.status_no_template")
        ReplicatorSync.STATUS_NO_FLUID -> t("gui.ic2_120.replicator.status_no_fluid")
        ReplicatorSync.STATUS_NO_OUTPUT -> t("gui.ic2_120.replicator.status_no_output")
        ReplicatorSync.STATUS_NO_ENERGY -> t("gui.ic2_120.status_no_energy")
        ReplicatorSync.STATUS_RUNNING -> t("gui.ic2_120.replicator.status_running")
        ReplicatorSync.STATUS_COMPLETE -> t("gui.ic2_120.replicator.status_complete")
        else -> t("gui.ic2_120.status_idle")
    }

    private fun statusColor(status: Int): Int = when (status) {
        ReplicatorSync.STATUS_COMPLETE -> 0x55FF55
        ReplicatorSync.STATUS_RUNNING -> 0x55AAFF
        ReplicatorSync.STATUS_NO_REDSTONE, ReplicatorSync.STATUS_NO_STORAGE, ReplicatorSync.STATUS_NO_TEMPLATE,
        ReplicatorSync.STATUS_NO_FLUID, ReplicatorSync.STATUS_NO_OUTPUT, ReplicatorSync.STATUS_NO_ENERGY -> 0xFF5555
        else -> 0xAAAAAA
    }

    companion object {
        private val DROPLETS_PER_MB = (FluidConstants.BUCKET / 1000).toInt()
        private val TEXTURE = Identifier.of("ic2", "textures/gui/guirguireplicator.png")
        private val UPTIPS_TEXTURE = Identifier.of("ic2", "textures/gui/uptips.png")
        private const val TEX_SIZE = 256

        private const val MODE_TEX_U = 198
        private const val MODE_TEX_V = 3
        private const val MODE_TEX_W = 52
        private const val MODE_TEX_H = 18
        private const val MODE_TEX_X = 71
        private const val MODE_TEX_Y = 81

        private const val ENERGY_U = 180
        private const val ENERGY_V = 3
        private const val ENERGY_W = 14
        private const val ENERGY_H = 15
        private const val ENERGY_X = 133
        private const val ENERGY_Y = 83

        private const val TANK_X = 31
        private const val TANK_Y = 34
        private const val TANK_W = 12
        private const val TANK_H = 47

        private const val TANK_OVERLAY_U = 181
        private const val TANK_OVERLAY_V = 25
        private const val TANK_OVERLAY_W = 11
        private const val TANK_OVERLAY_H = 46
        private const val TANK_OVERLAY_X = 32
        private const val TANK_OVERLAY_Y = 35

        private const val PRODUCT_X = 91
        private const val PRODUCT_Y = 17

        private const val STATUS_X1 = 50
        private const val STATUS_X2 = 144
        private const val STATUS_Y = 40

        private const val NAV_Y1 = 16
        private const val NAV_Y2 = 34
        private const val NAV_LEFT_X1 = 79
        private const val NAV_LEFT_X2 = 89
        private const val NAV_RIGHT_X1 = 109
        private const val NAV_RIGHT_X2 = 118

        private val uuMatterSprite by lazy {
            FluidRenderHandlerRegistry.INSTANCE.get(ModFluids.UU_MATTER_STILL)
                ?.getFluidSprites(null, null, ModFluids.UU_MATTER_STILL.defaultState)?.getOrNull(0)
        }
    }
}
