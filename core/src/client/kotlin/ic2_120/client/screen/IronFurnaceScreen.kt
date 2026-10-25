package ic2_120.client.screen

import ic2_120.content.block.IronFurnaceBlock
import ic2_120.content.block.machines.IronFurnaceBlockEntity
import ic2_120.content.screen.IronFurnaceScreenHandler
import ic2_120.content.sync.IronFurnaceSync
import ic2_120.registry.annotation.ModScreen
import ic2_120.client.compat.*
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.components.Button
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.client.Minecraft

@ModScreen(block = IronFurnaceBlock::class)
class IronFurnaceScreen(
    val handler: IronFurnaceScreenHandler,
    playerInventory: Inventory,
    title: Component
) : LegacyContainerScreen26<IronFurnaceScreenHandler>(handler, playerInventory, title, 176, 166) {

    private lateinit var collectXpBtn: Button

    init {
    }

    override fun init() {
        super.init()

        collectXpBtn = addRenderableWidget(
            Button.builder(Component.empty()) { btn ->
                Minecraft.getInstance().connection?.send(
                    ServerboundContainerButtonClickPacket(menu.containerId, IronFurnaceScreenHandler.BUTTON_ID_COLLECT_XP)
                )
            }.bounds(leftPos + XP_BTN_X, topPos + XP_BTN_Y, XP_BTN_SIZE, XP_BTN_SIZE).build()
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

        val cookFrac = if (IronFurnaceSync.COOK_TIME_MAX > 0) {
            (menu.sync.cookTime.coerceIn(0, IronFurnaceSync.COOK_TIME_MAX).toFloat() / IronFurnaceSync.COOK_TIME_MAX)
                .coerceIn(0f, 1f)
        } else 0f
        val totalBurn = menu.sync.totalBurnTime.coerceAtLeast(1)
        val burnFrac = (menu.sync.burnTime.coerceIn(0, totalBurn).toFloat() / totalBurn).coerceIn(0f, 1f)

        // 燃烧值纹理 (180,5)-(193,18) = 13×13，自下而上
        if (burnFrac > 0f) {
            val flameHeight = (FLAME_H * burnFrac).toInt().coerceAtLeast(1)
            context.enableScissor(
                left + FLAME_X,
                top + FLAME_Y + FLAME_H - flameHeight,
                left + FLAME_X + FLAME_W,
                top + FLAME_Y + FLAME_H
            )
            context.blit(
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
            context.blit(
                TEXTURE, left + ARROW_X, top + ARROW_Y,
                ARROW_U.toFloat(), ARROW_V.toFloat(),
                ARROW_W, ARROW_H,
                TEXTURE_SIZE, TEXTURE_SIZE
            )
            context.disableScissor()
        }

        // 经验瓶纹理（按钮居中）
        context.renderItem(
            ItemStack(Items.EXPERIENCE_BOTTLE),
            left + XP_BTN_X + 2,
            top + XP_BTN_Y + 2
        )

        renderTooltip(context, mouseX, mouseY)

        // XP 按钮悬停提示
        val relX = mouseX - left
        val relY = mouseY - top
        if (relX in XP_BTN_X until XP_BTN_X + XP_BTN_SIZE &&
            relY in XP_BTN_Y until XP_BTN_Y + XP_BTN_SIZE
        ) {
            val xpRaw = menu.sync.experienceDisplay
            val xpWhole = xpRaw / 10
            val xpFrac = xpRaw % 10
            context.renderTooltip(
                font,
                Component.literal("XP: $xpWhole.$xpFrac"),
                mouseX, mouseY
            )
        }
    }

    companion object {
        private val TEXTURE = Identifier.fromNamespaceAndPath("ic2", "textures/gui/guiironfurnace.png")
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
