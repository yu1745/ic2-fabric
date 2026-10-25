package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.t
import ic2_120.content.block.BlockCutterBlock
import ic2_120.content.screen.BlockCutterScreenHandler
import ic2_120.content.sync.BlockCutterSync
import ic2_120.registry.annotation.ModScreen
import ic2_120.client.compat.*
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Inventory
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

@ModScreen(block = BlockCutterBlock::class)
class BlockCutterScreen(
    val handler: BlockCutterScreenHandler,
    playerInventory: Inventory,
    title: Component
) : LegacyContainerScreen26<BlockCutterScreenHandler>(handler, playerInventory, title, 176, 166) {

    init {
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
        val progressFrac = (menu.sync.progress.coerceIn(0, BlockCutterSync.PROGRESS_MAX)
            .toFloat() / BlockCutterSync.PROGRESS_MAX).coerceIn(0f, 1f)
        val inputRate = menu.sync.getSyncedInsertedAmount()
        val consumeRate = menu.sync.getSyncedConsumedAmount()

        // 电量条 (180,3)-(194,17) = 14×14，自下而上
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

        // 升级提示图标 (4,4) 16×16
        context.blit(UPTIPS, left + UPGRADE_ICON_X, top + UPGRADE_ICON_Y, 0f, 0f, 16, 16, 16, 16)

        // 工作进度纹理 (180,51)-(227,70) = 47×19，自左向右
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

        // 刀片警告纹理 (180,21)-(210,47) = 30×26
        val bladeTooWeak = menu.sync.bladeTooWeak != 0
        val bladeMissing = !menu.getSlot(BlockCutterScreenHandler.SLOT_BLADE_INDEX) .hasItem()
        if (bladeTooWeak) {
            context.blit(
                TEXTURE, left + BLADE_WARN_X, top + BLADE_WARN_Y,
                BLADE_WARN_U.toFloat(), BLADE_WARN_V.toFloat(),
                BLADE_WARN_W, BLADE_WARN_H,
                TEXTURE_SIZE, TEXTURE_SIZE
            )
        }

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

        // 刀片警告悬停
        if (bladeTooWeak && relX in BLADE_WARN_X until BLADE_WARN_X + BLADE_WARN_W &&
            relY in BLADE_WARN_Y until BLADE_WARN_Y + BLADE_WARN_H
        ) {
            val warnText = if (bladeMissing) {
                Component.literal("机器无工作刀片！！！")
            } else {
                Component.literal("当前刀片工作硬度不足！！！")
            }
            context.renderTooltip(font, warnText, mouseX, mouseY)
        }

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

        // 升级提示悬停
        if (relX in UPGRADE_ICON_X until UPGRADE_ICON_X + 16 &&
            relY in UPGRADE_ICON_Y until UPGRADE_ICON_Y + 16
        ) {
            val upgradeTooltip = mutableListOf<Component>()
            upgradeTooltip.add(Component.translatable("gui.ic2_120.upgrade_slots"))
            for (id in SUPPORTED_UPGRADES) {
                val item = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", id))
                upgradeTooltip.add(item.defaultInstance.hoverName)
            }
            context.renderComponentTooltip(font, upgradeTooltip, mouseX, mouseY)
        }
    }

    companion object {
        private val TEXTURE = Identifier.fromNamespaceAndPath("ic2", "textures/gui/guiblockcuttingmachine.png")
        private val UPTIPS = Identifier.fromNamespaceAndPath("ic2", "textures/gui/uptips.png")
        private const val TEXTURE_SIZE = 256

        // 升级提示图标
        private const val UPGRADE_ICON_X = 4
        private const val UPGRADE_ICON_Y = 4

        // 电量条 (180,3)-(194,17) = 14×14
        private const val ENERGY_BAR_U = 180
        private const val ENERGY_BAR_V = 3
        private const val ENERGY_BAR_W = 14
        private const val ENERGY_BAR_H = 14
        private const val ENERGY_BAR_X = 26
        private const val ENERGY_BAR_Y = 36

        // 工作进度 (180,51)-(227,70) = 47×19
        private const val PROGRESS_U = 180
        private const val PROGRESS_V = 51
        private const val PROGRESS_W = 47
        private const val PROGRESS_H = 19
        private const val PROGRESS_X = 55
        private const val PROGRESS_Y = 33

        // 刀片警告 (180,21)-(210,47) = 30×26
        private const val BLADE_WARN_U = 180
        private const val BLADE_WARN_V = 21
        private const val BLADE_WARN_W = 30
        private const val BLADE_WARN_H = 26
        private const val BLADE_WARN_X = 63
        private const val BLADE_WARN_Y = 27

        private val SUPPORTED_UPGRADES = listOf(
            "overclocker_upgrade",
            "energy_storage_upgrade",
            "transformer_upgrade",
            "ejector_upgrade",
            "pulling_upgrade"
        )
    }
}
