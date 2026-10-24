package ic2_120.client.screen

import ic2_120.content.block.TeleporterBlock
import ic2_120.content.block.machines.TeleporterBlockEntity
import ic2_120.content.screen.TeleporterScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.components.Button
import net.minecraft.world.entity.player.Inventory
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket
import net.minecraft.network.chat.Component as McText
import net.minecraft.resources.ResourceLocation
import net.minecraft.client.Minecraft

@ModScreen(block = TeleporterBlock::class)
class TeleporterScreen(
    val handler: TeleporterScreenHandler,
    playerInventory: Inventory,
    title: McText
) : AbstractContainerScreen<TeleporterScreenHandler>(handler, playerInventory, title) {

    init {
        imageWidth = 176
        imageHeight = 162
        titleLabelY = 6
    }

    private lateinit var range1Btn: Button
    private lateinit var range3Btn: Button

    override fun init() {
        super.init()

        // 升级槽位（2个，无间隔）
        for (i in 0 until 2) {
            menu.slots[TeleporterScreenHandler.SLOT_UPGRADE_INDEX_START + i].x = UPGRADE_SLOT_X
            menu.slots[TeleporterScreenHandler.SLOT_UPGRADE_INDEX_START + i].y = UPGRADE_SLOT_Y + i * 18
        }

        // 玩家背包槽位
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                val idx = TeleporterScreenHandler.PLAYER_INV_START + row * 9 + col
                menu.slots[idx].x = PLAYER_INV_X + col * 18
                menu.slots[idx].y = PLAYER_INV_Y + row * 18
            }
        }

        // 快捷栏槽位
        for (col in 0 until 9) {
            val idx = TeleporterScreenHandler.PLAYER_INV_START + 27 + col
            menu.slots[idx].x = PLAYER_INV_X + col * 18
            menu.slots[idx].y = HOTBAR_Y
        }

        range1Btn = addRenderableWidget(Button.builder(
            McText.literal("")
        ) {
            Minecraft.getInstance().connection?.send(
                ServerboundContainerButtonClickPacket(menu.containerId, TeleporterScreenHandler.BUTTON_ID_RANGE_1)
            )
        }.bounds(leftPos + 26, topPos + 15, 45, 20).build())

        range3Btn = addRenderableWidget(Button.builder(
            McText.literal("")
        ) {
            Minecraft.getInstance().connection?.send(
                ServerboundContainerButtonClickPacket(menu.containerId, TeleporterScreenHandler.BUTTON_ID_RANGE_3)
            )
        }.bounds(leftPos + 26, topPos + 40, 45, 20).build())
    }

    override fun renderBg(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.blit(TEXTURE, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, TEXTURE_SIZE, TEXTURE_SIZE)

        // 升级槽背景
        for (i in 0 until 2) {
            context.blit(
                TEXTURE,
                leftPos + UPGRADE_SLOT_BG_X, topPos + UPGRADE_SLOT_BG_Y + i * 18,
                SLOT_BG_U.toFloat(), SLOT_BG_V.toFloat(),
                SLOT_BG_SIZE, SLOT_BG_SIZE,
                TEXTURE_SIZE, TEXTURE_SIZE
            )
        }

        // 升级提示图标
        context.blit(UPTIPS, leftPos + UPTIPS_X, topPos + UPTIPS_Y, 0f, 0f, UPTIPS_SIZE, UPTIPS_SIZE, UPTIPS_SIZE, UPTIPS_SIZE)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)

        val isRange1 = menu.sync.teleportRange == TeleporterBlockEntity.TELEPORT_RANGE_MIN

        // 标题
        context.drawString(font, title, leftPos + (imageWidth - font.width(title)) / 2, topPos + 6, 0x404040, false)

        // 玩家背包标签
        context.drawString(font, playerInventoryTitle, leftPos + 8, topPos + PLAYER_INV_Y - 12, 0x404040, false)

        // 按钮内居中显示模式文本（缩放到 45×20）
        drawButtonText(context, "范围1x1x1", leftPos + 26, topPos + 15, if (isRange1) 0xFFFFFF else MODE_INACTIVE_COLOR)
        drawButtonText(context, "范围3x3x3", leftPos + 26, topPos + 40, if (!isRange1) 0xFFFFFF else MODE_INACTIVE_COLOR)

        // 激活模式按钮旁渲染扳手
        val wrenchX = leftPos + 26 + 45 + 6
        val wrenchY = if (isRange1) {
            topPos + 15 + (20 - WRENCH_SIZE) / 2
        } else {
            topPos + 40 + (20 - WRENCH_SIZE) / 2
        }
        context.blit(WRENCH, wrenchX, wrenchY, 0f, 0f, WRENCH_SIZE, WRENCH_SIZE, WRENCH_SIZE, WRENCH_SIZE)

        // 升级提示悬停
        if (mouseX >= leftPos + UPTIPS_X && mouseX < leftPos + UPTIPS_X + UPTIPS_SIZE &&
            mouseY >= topPos + UPTIPS_Y && mouseY < topPos + UPTIPS_Y + UPTIPS_SIZE
        ) {
            context.renderComponentTooltip(font, menu.getUpgradeTooltips(), mouseX, mouseY)
        }

        renderTooltip(context, mouseX, mouseY)
    }

    private fun drawButtonText(
        context: GuiGraphics,
        text: String,
        bx: Int,
        by: Int,
        color: Int
    ) {
        val bw = 45
        val bh = 20
        val tw = font.width(text)
        val scale = 7f / font.lineHeight

        context.pose().pushPose()
        context.pose().translate((bx + bw / 2).toDouble(), (by + bh / 2).toDouble(), 0.0)
        context.pose().scale(scale, scale, 1f)
        context.drawString(font, text, -tw / 2, -font.lineHeight / 2, color, false)
        context.pose().popPose()
    }

    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath("ic2", "textures/gui/guiother.png")
        private val WRENCH = ResourceLocation.fromNamespaceAndPath("ic2_120", "textures/item/tool/wrench.png")
        private val UPTIPS = ResourceLocation.fromNamespaceAndPath("ic2", "textures/gui/uptips.png")
        private const val TEXTURE_SIZE = 256
        private const val WRENCH_SIZE = 16
        private const val UPTIPS_SIZE = 16

        // guiother.png 中槽位背景区域 (179,3)-(197,21)
        private const val SLOT_BG_U = 179
        private const val SLOT_BG_V = 3
        private const val SLOT_BG_SIZE = 18

        // 升级槽背景位置
        private const val UPGRADE_SLOT_BG_X = 152
        private const val UPGRADE_SLOT_BG_Y = 20

        // 升级槽位置（2槽，无间隔）
        private const val UPGRADE_SLOT_X = 153
        private const val UPGRADE_SLOT_Y = 21

        // 升级提示图标
        private const val UPTIPS_X = 4
        private const val UPTIPS_Y = 4

        // 玩家背包位置
        private const val PLAYER_INV_X = 8
        private const val PLAYER_INV_Y = 79
        private const val HOTBAR_Y = 137

        private const val MODE_INACTIVE_COLOR = 0xAAAAAA
    }
}
