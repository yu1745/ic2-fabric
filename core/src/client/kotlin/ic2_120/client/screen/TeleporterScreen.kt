package ic2_120.client.screen

import ic2_120.content.block.TeleporterBlock
import ic2_120.content.block.machines.TeleporterBlockEntity
import ic2_120.content.screen.TeleporterScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket
import net.minecraft.text.Text as McText
import net.minecraft.util.Identifier

@ModScreen(block = TeleporterBlock::class)
class TeleporterScreen(
    handler: TeleporterScreenHandler,
    playerInventory: PlayerInventory,
    title: McText
) : HandledScreen<TeleporterScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = 176
        backgroundHeight = 162
        titleY = 6
    }

    private lateinit var range1Btn: ButtonWidget
    private lateinit var range3Btn: ButtonWidget

    override fun init() {
        super.init()

        // 升级槽位（2个，无间隔）
        for (i in 0 until 2) {
            handler.slots[TeleporterScreenHandler.SLOT_UPGRADE_INDEX_START + i].x = UPGRADE_SLOT_X
            handler.slots[TeleporterScreenHandler.SLOT_UPGRADE_INDEX_START + i].y = UPGRADE_SLOT_Y + i * 18
        }

        // 玩家背包槽位
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                val idx = TeleporterScreenHandler.PLAYER_INV_START + row * 9 + col
                handler.slots[idx].x = PLAYER_INV_X + col * 18
                handler.slots[idx].y = PLAYER_INV_Y + row * 18
            }
        }

        // 快捷栏槽位
        for (col in 0 until 9) {
            val idx = TeleporterScreenHandler.PLAYER_INV_START + 27 + col
            handler.slots[idx].x = PLAYER_INV_X + col * 18
            handler.slots[idx].y = HOTBAR_Y
        }

        range1Btn = addDrawableChild(ButtonWidget.builder(
            McText.literal("")
        ) {
            client?.player?.networkHandler?.sendPacket(
                ButtonClickC2SPacket(handler.syncId, TeleporterScreenHandler.BUTTON_ID_RANGE_1)
            )
        }.dimensions(x + 26, y + 15, 45, 20).build())

        range3Btn = addDrawableChild(ButtonWidget.builder(
            McText.literal("")
        ) {
            client?.player?.networkHandler?.sendPacket(
                ButtonClickC2SPacket(handler.syncId, TeleporterScreenHandler.BUTTON_ID_RANGE_3)
            )
        }.dimensions(x + 26, y + 40, 45, 20).build())
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, backgroundWidth, backgroundHeight, TEXTURE_SIZE, TEXTURE_SIZE)

        // 升级槽背景
        for (i in 0 until 2) {
            context.drawTexture(
                TEXTURE,
                x + UPGRADE_SLOT_BG_X, y + UPGRADE_SLOT_BG_Y + i * 18,
                SLOT_BG_U.toFloat(), SLOT_BG_V.toFloat(),
                SLOT_BG_SIZE, SLOT_BG_SIZE,
                TEXTURE_SIZE, TEXTURE_SIZE
            )
        }

        // 升级提示图标
        context.drawTexture(UPTIPS, x + UPTIPS_X, y + UPTIPS_Y, 0f, 0f, UPTIPS_SIZE, UPTIPS_SIZE, UPTIPS_SIZE, UPTIPS_SIZE)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        val isRange1 = handler.sync.teleportRange == TeleporterBlockEntity.TELEPORT_RANGE_MIN

        // 标题
        context.drawText(textRenderer, title, x + (backgroundWidth - textRenderer.getWidth(title)) / 2, y + 6, 0x404040, false)

        // 玩家背包标签
        context.drawText(textRenderer, playerInventoryTitle, x + 8, y + PLAYER_INV_Y - 12, 0x404040, false)

        // 按钮内居中显示模式文本（缩放到 45×20）
        drawButtonText(context, "范围1x1x1", x + 26, y + 15, if (isRange1) 0xFFFFFF else MODE_INACTIVE_COLOR)
        drawButtonText(context, "范围3x3x3", x + 26, y + 40, if (!isRange1) 0xFFFFFF else MODE_INACTIVE_COLOR)

        // 激活模式按钮旁渲染扳手
        val wrenchX = x + 26 + 45 + 6
        val wrenchY = if (isRange1) {
            y + 15 + (20 - WRENCH_SIZE) / 2
        } else {
            y + 40 + (20 - WRENCH_SIZE) / 2
        }
        context.drawTexture(WRENCH, wrenchX, wrenchY, 0f, 0f, WRENCH_SIZE, WRENCH_SIZE, WRENCH_SIZE, WRENCH_SIZE)

        // 升级提示悬停
        if (mouseX >= x + UPTIPS_X && mouseX < x + UPTIPS_X + UPTIPS_SIZE &&
            mouseY >= y + UPTIPS_Y && mouseY < y + UPTIPS_Y + UPTIPS_SIZE
        ) {
            context.drawTooltip(textRenderer, handler.getUpgradeTooltips(), mouseX, mouseY)
        }

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun drawButtonText(
        context: DrawContext,
        text: String,
        bx: Int,
        by: Int,
        color: Int
    ) {
        val bw = 45
        val bh = 20
        val tw = textRenderer.getWidth(text)
        val scale = 7f / textRenderer.fontHeight

        context.matrices.push()
        context.matrices.translate((bx + bw / 2).toDouble(), (by + bh / 2).toDouble(), 0.0)
        context.matrices.scale(scale, scale, 1f)
        context.drawText(textRenderer, text, -tw / 2, -textRenderer.fontHeight / 2, color, false)
        context.matrices.pop()
    }

    companion object {
        private val TEXTURE = Identifier("ic2", "textures/gui/guiother.png")
        private val WRENCH = Identifier("ic2", "textures/item/tool/wrench.png")
        private val UPTIPS = Identifier("ic2", "textures/gui/uptips.png")
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
