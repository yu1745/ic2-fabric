package ic2_120.client.screen

import ic2_120.content.screen.TransformerScreenHandler
import ic2_120.content.sync.TransformerSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.components.Button
import net.minecraft.world.entity.player.Inventory
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

/**
 * 变压器的 GUI 界面。
 * 显示当前模式（升压/降压），提供固定升压/固定降压两个按钮。
 *
 * 所有电压等级的变压器（LV、MV、HV、EV）共用此 UI。
 */
@ModScreen(handlers = ["lv_transformer", "mv_transformer", "hv_transformer", "ev_transformer"])
class TransformerScreen(
    handler: TransformerScreenHandler,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<TransformerScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = 176
        backgroundHeight = 219
        titleY = 4
    }

    private lateinit var stepUpBtn: Button
    private lateinit var stepDownBtn: Button

    override fun init() {
        super.init()

        // 固定升压按钮
        stepUpBtn = addDrawableChild(Button.builder(
            Component.literal("固定升压")
        ) { btn ->
            stepDownBtn.setFocused(false)
            client?.player?.networkHandler?.sendPacket(
                ServerboundContainerButtonClickPacket(handler.syncId, TransformerScreenHandler.BUTTON_ID_STEP_UP)
            )
        }.dimensions(x + 21, y + 69, 74, 20).build())

        // 固定降压按钮
        stepDownBtn = addDrawableChild(Button.builder(
            Component.literal("固定降压")
        ) { btn ->
            stepUpBtn.setFocused(false)
            client?.player?.networkHandler?.sendPacket(
                ServerboundContainerButtonClickPacket(handler.syncId, TransformerScreenHandler.BUTTON_ID_STEP_DOWN)
            )
        }.dimensions(x + 21, y + 90, 74, 20).build())
    }

    override fun drawBackground(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, 176, 219, 256, 256)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        val currentMode = handler.sync.getMode()
        val isStepUp = currentMode == TransformerSync.Mode.STEP_UP

        val lowEu = handler.sync.getLowEuPerTick()
        val highEu = handler.sync.getHighEuPerTick()

        val (inputRate, outputRate) = if (isStepUp) {
            "$lowEu" to "$highEu"
        } else {
            "$highEu" to "$lowEu"
        }

        // 标题
        context.drawText(textRenderer, title, x + (176 - textRenderer.getWidth(title)) / 2, y + 4, 0x404040, false)

        // 文本：输入
        context.drawText(textRenderer, Component.literal("输入："), x + 21, y + 27, 0x404040, false)

        // 文本：输出
        context.drawText(textRenderer, Component.literal("输出："), x + 21, y + 43, 0x404040, false)

        // 文本：a（输入速率值）
        context.drawText(textRenderer, Component.literal("${inputRate}EU/t"), x + 51, y + 29, INPUT_OUTPUT_COLOR, false)

        // 文本：b（输出速率值）
        context.drawText(textRenderer, Component.literal("${outputRate}EU/t"), x + 51, y + 45, INPUT_OUTPUT_COLOR, false)

        // 扳手图标
        if (isStepUp) {
            context.drawTexture(WRENCH, x + 103, y + 70, 0f, 0f, 16, 16, 16, 16)
        } else {
            context.drawTexture(WRENCH, x + 103, y + 91, 0f, 0f, 16, 16, 16, 16)
        }

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath("ic2", "textures/gui/guitransfomer.png")
        private val WRENCH = ResourceLocation.fromNamespaceAndPath("ic2_120", "textures/item/tool/wrench.png")
        private const val INPUT_OUTPUT_COLOR = 0xFF90EE90.toInt()
    }
}
