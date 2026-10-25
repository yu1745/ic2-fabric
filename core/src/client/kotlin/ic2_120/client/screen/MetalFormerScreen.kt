package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.t
import ic2_120.content.block.MetalFormerBlock
import ic2_120.content.screen.MetalFormerScreenHandler
import ic2_120.content.sync.MetalFormerSync
import ic2_120.registry.annotation.ModScreen
import ic2_120.client.compat.*
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.components.Button
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.client.Minecraft

@ModScreen(block = MetalFormerBlock::class)
class MetalFormerScreen(
    val handler: MetalFormerScreenHandler,
    playerInventory: Inventory,
    title: Component
) : LegacyContainerScreen26<MetalFormerScreenHandler>(handler, playerInventory, title, 176, 166) {

    init {
    }

    override fun init() {
        super.init()
        addRenderableWidget(
            Button.builder(Component.empty()) {
                Minecraft.getInstance().connection?.send(
                    ServerboundContainerButtonClickPacket(menu.containerId, MetalFormerScreenHandler.BUTTON_ID_MODE_CYCLE)
                )
            }.bounds(leftPos + MODE_BUTTON_X, topPos + MODE_BUTTON_Y, MODE_BUTTON_SIZE, MODE_BUTTON_SIZE).build()
        )
    }

    override fun renderBg(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.blit(TEXTURE, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, TEXTURE_SIZE, TEXTURE_SIZE)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)

        val left = leftPos
        val top = topPos

        context.drawString(font, title, left + (imageWidth - font.width(title)) / 2, top + 6, 0x404040, false)

        val energy = menu.sync.energy.toLong().coerceAtLeast(0)
        val cap = menu.sync.energyCapacity.toLong().coerceAtLeast(1)
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val progressFrac = if (MetalFormerSync.PROGRESS_MAX > 0) {
            (menu.sync.progress.coerceIn(0, MetalFormerSync.PROGRESS_MAX).toFloat() / MetalFormerSync.PROGRESS_MAX).coerceIn(0f, 1f)
        } else 0f
        val inputRate = menu.sync.getSyncedInsertedAmount()
        val consumeRate = menu.sync.getSyncedConsumedAmount()

        // 电量条 (179,3)-(193,17) = 14x14，自下而上
        if (energyFraction > 0f) {
            val fillHeight = (ENERGY_BAR_H * energyFraction).toInt().coerceAtLeast(1)
            context.enableScissor(
                left + ENERGY_BAR_X,
                top + ENERGY_BAR_Y + ENERGY_BAR_H - fillHeight,
                left + ENERGY_BAR_X + ENERGY_BAR_W,
                top + ENERGY_BAR_Y + ENERGY_BAR_H
            )
            context.blit(
                TEXTURE, left + ENERGY_BAR_X, top + ENERGY_BAR_Y,
                ENERGY_BAR_U.toFloat(), ENERGY_BAR_V.toFloat(),
                ENERGY_BAR_W, ENERGY_BAR_H,
                TEXTURE_SIZE, TEXTURE_SIZE
            )
            context.disableScissor()
        }

        // 工作进度 (179,21)-(225,30) = 46x9，自左向右
        if (progressFrac > 0f) {
            val arrowWidth = (PROGRESS_W * progressFrac).toInt().coerceAtLeast(1)
            context.enableScissor(
                left + PROGRESS_X,
                top + PROGRESS_Y,
                left + PROGRESS_X + arrowWidth,
                top + PROGRESS_Y + PROGRESS_H
            )
            context.blit(
                TEXTURE, left + PROGRESS_X, top + PROGRESS_Y,
                PROGRESS_U.toFloat(), PROGRESS_V.toFloat(),
                PROGRESS_W, PROGRESS_H,
                TEXTURE_SIZE, TEXTURE_SIZE
            )
            context.disableScissor()
        }

        // 模式切换按钮上的物品贴图
        val currentMode = menu.sync.getMode()
        val modeItemId = when (currentMode) {
            MetalFormerSync.Mode.ROLLING -> "forge_hammer"
            MetalFormerSync.Mode.CUTTING -> "cutter"
            MetalFormerSync.Mode.EXTRUDING -> "copper_cable"
        }
        val modeStack = ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", modeItemId)))
        context.renderItem(modeStack, left + MODE_BUTTON_X + 2, top + MODE_BUTTON_Y + 2)

        // uptips 纹理
        context.blit(
            UPTIPS_TEXTURE, left + UPTIPS_X, top + UPTIPS_Y,
            0f, 0f, UPTIPS_SIZE, UPTIPS_SIZE,
            UPTIPS_SIZE, UPTIPS_SIZE
        )

        // 侧边文本
        val inputText = t("gui.ic2_120.input_eu", EnergyFormatUtils.formatRaw(inputRate))
        val consumeText = t("gui.ic2_120.consume_eu", EnergyFormatUtils.formatRaw(consumeRate))
        val sideTextWidth = maxOf(font.width(inputText), font.width(consumeText))
        val sideTextX = left - sideTextWidth - 4
        context.drawString(font, inputText, sideTextX, top + 8, 0xAAAAAA, false)
        context.drawString(font, consumeText, sideTextX, top + 20, 0xAAAAAA, false)

        renderTooltip(context, mouseX, mouseY)

        // 悬停提示
        val relX = mouseX - left
        val relY = mouseY - top

        // 电量条悬停
        if (relX in ENERGY_BAR_X until ENERGY_BAR_X + ENERGY_BAR_W &&
            relY in ENERGY_BAR_Y until ENERGY_BAR_Y + ENERGY_BAR_H
        ) {
            context.renderTooltip(
                font,
                Component.literal("储能：${EnergyFormatUtils.formatRaw(energy)} / ${EnergyFormatUtils.formatRaw(cap)} EU"),
                mouseX, mouseY
            )
        }

        // uptips 悬停
        if (relX in UPTIPS_X until UPTIPS_X + UPTIPS_SIZE &&
            relY in UPTIPS_Y until UPTIPS_Y + UPTIPS_SIZE
        ) {
            context.renderComponentTooltip(
                font,
                listOf(
                    Component.translatable("gui.ic2_120.metal_former.uptips"),
                    Component.literal("§7").append(Component.translatable("item.ic2_120.overclocker_upgrade")),
                    Component.literal("§7").append(Component.translatable("item.ic2_120.transformer_upgrade")),
                    Component.literal("§7").append(Component.translatable("item.ic2_120.energy_storage_upgrade")),
                    Component.literal("§7").append(Component.translatable("item.ic2_120.ejector_upgrade")),
                    Component.literal("§7").append(Component.translatable("item.ic2_120.pulling_upgrade"))
                ),
                mouseX, mouseY
            )
        }
    }

    companion object {
        private val TEXTURE = Identifier.fromNamespaceAndPath("ic2", "textures/gui/guimetalformer.png")
        private val UPTIPS_TEXTURE = Identifier.fromNamespaceAndPath("ic2", "textures/gui/uptips.png")
        private const val TEXTURE_SIZE = 256

        // 电量条 (179,3)-(193,17) = 14x14
        private const val ENERGY_BAR_U = 179
        private const val ENERGY_BAR_V = 3
        private const val ENERGY_BAR_W = 14
        private const val ENERGY_BAR_H = 14
        private const val ENERGY_BAR_X = 17
        private const val ENERGY_BAR_Y = 36

        // 工作进度 (179,21)-(225,30) = 46x9
        private const val PROGRESS_U = 179
        private const val PROGRESS_V = 21
        private const val PROGRESS_W = 46
        private const val PROGRESS_H = 9
        private const val PROGRESS_X = 52
        private const val PROGRESS_Y = 39

        // 模式切换按钮
        private const val MODE_BUTTON_X = 65
        private const val MODE_BUTTON_Y = 57
        private const val MODE_BUTTON_SIZE = 20

        // uptips
        private const val UPTIPS_X = 40
        private const val UPTIPS_Y = 4
        private const val UPTIPS_SIZE = 16
    }
}
