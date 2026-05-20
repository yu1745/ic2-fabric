package ic2_120.client.screen

import ic2_120.content.block.IronFurnaceBlock
import ic2_120.content.block.machines.IronFurnaceBlockEntity
import ic2_120.content.screen.IronFurnaceScreenHandler
import ic2_120.content.sync.IronFurnaceSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = IronFurnaceBlock::class)
class IronFurnaceScreen(
    handler: IronFurnaceScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<IronFurnaceScreenHandler>(handler, playerInventory, title) {

    private lateinit var collectXpBtn: ButtonWidget

    init {
        backgroundWidth = 176
        backgroundHeight = 166
    }

    override fun init() {
        super.init()

        collectXpBtn = addDrawableChild(
            ButtonWidget.builder(Text.empty()) { btn ->
                client?.player?.networkHandler?.sendPacket(
                    ButtonClickC2SPacket(handler.syncId, IronFurnaceScreenHandler.BUTTON_ID_COLLECT_XP)
                )
            }.dimensions(x + XP_BTN_X, y + XP_BTN_Y, XP_BTN_SIZE, XP_BTN_SIZE).build()
        )
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, backgroundWidth, backgroundHeight, TEXTURE_SIZE, TEXTURE_SIZE)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        val left = x
        val top = y

        context.drawText(textRenderer, title, left + (backgroundWidth - textRenderer.getWidth(title)) / 2, top + 6, 0x404040, false)

        val cookFrac = if (IronFurnaceSync.COOK_TIME_MAX > 0) {
            (handler.sync.cookTime.coerceIn(0, IronFurnaceSync.COOK_TIME_MAX).toFloat() / IronFurnaceSync.COOK_TIME_MAX)
                .coerceIn(0f, 1f)
        } else 0f
        val totalBurn = handler.sync.totalBurnTime.coerceAtLeast(1)
        val burnFrac = (handler.sync.burnTime.coerceIn(0, totalBurn).toFloat() / totalBurn).coerceIn(0f, 1f)

        // 燃烧值纹理 (180,5)-(193,18) = 13×13，自下而上
        if (burnFrac > 0f) {
            val flameHeight = (FLAME_H * burnFrac).toInt().coerceAtLeast(1)
            context.enableScissor(
                left + FLAME_X,
                top + FLAME_Y + FLAME_H - flameHeight,
                left + FLAME_X + FLAME_W,
                top + FLAME_Y + FLAME_H
            )
            context.drawTexture(
                TEXTURE, left + FLAME_X, top + FLAME_Y,
                FLAME_U.toFloat(), FLAME_V.toFloat(),
                FLAME_W, FLAME_H,
                TEXTURE_SIZE, TEXTURE_SIZE
            )
            context.disableScissor()
        }

        // 工作进度纹理 (180,23)-(201,37) = 21×14，自左向右
        if (cookFrac > 0f) {
            val arrowWidth = (ARROW_W * cookFrac).toInt().coerceAtLeast(1)
            context.enableScissor(
                left + ARROW_X,
                top + ARROW_Y,
                left + ARROW_X + arrowWidth,
                top + ARROW_Y + ARROW_H
            )
            context.drawTexture(
                TEXTURE, left + ARROW_X, top + ARROW_Y,
                ARROW_U.toFloat(), ARROW_V.toFloat(),
                ARROW_W, ARROW_H,
                TEXTURE_SIZE, TEXTURE_SIZE
            )
            context.disableScissor()
        }

        // 经验瓶纹理（按钮居中）
        context.drawItem(
            ItemStack(Items.EXPERIENCE_BOTTLE),
            left + XP_BTN_X + 2,
            top + XP_BTN_Y + 2
        )

        drawMouseoverTooltip(context, mouseX, mouseY)

        // XP 按钮悬停提示
        val relX = mouseX - left
        val relY = mouseY - top
        if (relX in XP_BTN_X until XP_BTN_X + XP_BTN_SIZE &&
            relY in XP_BTN_Y until XP_BTN_Y + XP_BTN_SIZE
        ) {
            val xpRaw = handler.sync.experienceDisplay
            val xpWhole = xpRaw / 10
            val xpFrac = xpRaw % 10
            context.drawTooltip(
                textRenderer,
                Text.literal("XP: $xpWhole.$xpFrac"),
                mouseX, mouseY
            )
        }
    }

    companion object {
        private val TEXTURE = Identifier("ic2", "textures/gui/guiironfurnace.png")
        private const val TEXTURE_SIZE = 256

        // 燃烧值纹理 (180,5)-(193,18) = 13×13
        private const val FLAME_U = 180
        private const val FLAME_V = 5
        private const val FLAME_W = 13
        private const val FLAME_H = 13
        private const val FLAME_X = 57
        private const val FLAME_Y = 37

        // 工作进度纹理 (180,23)-(201,37) = 21×14
        private const val ARROW_U = 180
        private const val ARROW_V = 23
        private const val ARROW_W = 21
        private const val ARROW_H = 14
        private const val ARROW_X = 81
        private const val ARROW_Y = 36

        // XP 收集按钮
        private const val XP_BTN_X = 8
        private const val XP_BTN_Y = 60
        private const val XP_BTN_SIZE = 20
    }
}
