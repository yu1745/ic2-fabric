package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.t
import ic2_120.content.block.ElectricFurnaceBlock
import ic2_120.content.screen.ElectricFurnaceScreenHandler
import ic2_120.content.sync.ElectricFurnaceSync
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

@ModScreen(block = ElectricFurnaceBlock::class)
class ElectricFurnaceScreen(
    handler: ElectricFurnaceScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<ElectricFurnaceScreenHandler>(handler, playerInventory, title) {

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
                    ButtonClickC2SPacket(handler.syncId, ElectricFurnaceScreenHandler.BUTTON_ID_COLLECT_XP)
                )
            }.dimensions(x + XP_BTN_X, y + XP_BTN_Y, XP_BTN_SIZE, XP_BTN_SIZE).build()
        )
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, backgroundWidth, backgroundHeight, TEXTURE_SIZE, TEXTURE_SIZE)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)

        val left = x
        val top = y

        context.drawText(textRenderer, title, left + (backgroundWidth - textRenderer.getWidth(title)) / 2, top + 6, 0x404040, false)

        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1)
        val energyFraction = (energy.toFloat() / cap).coerceIn(0f, 1f)
        val progressFrac = (handler.sync.progress.coerceIn(0, ElectricFurnaceSync.PROGRESS_MAX)
            .toFloat() / ElectricFurnaceSync.PROGRESS_MAX).coerceIn(0f, 1f)
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val consumeRate = handler.sync.getSyncedConsumedAmount()

        // 电量条 (178,2)-(191,15) = 13×13，自下而上
        if (energyFraction > 0f) {
            val fillHeight = (ENERGY_BAR_H * energyFraction).toInt().coerceAtLeast(1)
            context.enableScissor(
                left + ENERGY_BAR_X,
                top + ENERGY_BAR_Y + ENERGY_BAR_H - fillHeight,
                left + ENERGY_BAR_X + ENERGY_BAR_W,
                top + ENERGY_BAR_Y + ENERGY_BAR_H
            )
            context.drawTexture(
                TEXTURE, left + ENERGY_BAR_X, top + ENERGY_BAR_Y,
                ENERGY_BAR_U.toFloat(), ENERGY_BAR_V.toFloat(),
                ENERGY_BAR_W, ENERGY_BAR_H,
                TEXTURE_SIZE, TEXTURE_SIZE
            )
            context.disableScissor()
        }

        // 验证电炉纹理 (178,48)-(199,62) = 21×14
        context.drawTexture(
            TEXTURE, left + VERIFY_X, top + VERIFY_Y,
            VERIFY_U.toFloat(), VERIFY_V.toFloat(),
            VERIFY_W, VERIFY_H,
            TEXTURE_SIZE, TEXTURE_SIZE
        )

        // 工作进度纹理 (178,31)-(190,45) = 12×14，自左向右（渲染在验证纹理之上）
        if (progressFrac > 0f) {
            val arrowWidth = (PROGRESS_W * progressFrac).toInt().coerceAtLeast(1)
            context.enableScissor(
                left + PROGRESS_X,
                top + PROGRESS_Y,
                left + PROGRESS_X + arrowWidth,
                top + PROGRESS_Y + PROGRESS_H
            )
            context.drawTexture(
                TEXTURE, left + PROGRESS_X, top + PROGRESS_Y,
                PROGRESS_U.toFloat(), PROGRESS_V.toFloat(),
                PROGRESS_W, PROGRESS_H,
                TEXTURE_SIZE, TEXTURE_SIZE
            )
            context.disableScissor()
        }

        // uptips 纹理
        context.drawTexture(
            UPTIPS_TEXTURE, left + UPTIPS_X, top + UPTIPS_Y,
            0f, 0f, UPTIPS_SIZE, UPTIPS_SIZE,
            UPTIPS_SIZE, UPTIPS_SIZE
        )

        // 经验瓶纹理（按钮居中）
        context.drawItem(
            ItemStack(Items.EXPERIENCE_BOTTLE),
            left + XP_BTN_X + 2,
            top + XP_BTN_Y + 2
        )

        // 侧边文本
        val inputText = t("gui.ic2_120.input_eu", EnergyFormatUtils.formatEu(inputRate))
        val consumeText = t("gui.ic2_120.consume_eu", EnergyFormatUtils.formatEu(consumeRate))
        val sideTextWidth = maxOf(textRenderer.getWidth(inputText), textRenderer.getWidth(consumeText))
        val sideTextX = left - sideTextWidth - 4
        context.drawText(textRenderer, inputText, sideTextX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, consumeText, sideTextX, top + 20, 0xAAAAAA, false)

        drawMouseoverTooltip(context, mouseX, mouseY)

        // 悬停提示
        val relX = mouseX - left
        val relY = mouseY - top

        // 电量条悬停
        if (relX in ENERGY_BAR_X until ENERGY_BAR_X + ENERGY_BAR_W &&
            relY in ENERGY_BAR_Y until ENERGY_BAR_Y + ENERGY_BAR_H
        ) {
            context.drawTooltip(
                textRenderer,
                Text.literal("储能：${EnergyFormatUtils.formatEu(energy)} / ${EnergyFormatUtils.formatEu(cap)} EU"),
                mouseX, mouseY
            )
        }

        // XP 按钮悬停
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

        // uptips 悬停
        if (relX in UPTIPS_X until UPTIPS_X + UPTIPS_SIZE &&
            relY in UPTIPS_Y until UPTIPS_Y + UPTIPS_SIZE
        ) {
            context.drawTooltip(
                textRenderer,
                listOf(
                    Text.translatable("gui.ic2_120.electric_furnace.uptips"),
                    Text.literal(" §7").append(Text.translatable("item.ic2_120.overclocker_upgrade")),
                    Text.literal(" §7").append(Text.translatable("item.ic2_120.transformer_upgrade")),
                    Text.literal(" §7").append(Text.translatable("item.ic2_120.energy_storage_upgrade")),
                    Text.literal(" §7").append(Text.translatable("item.ic2_120.ejector_upgrade")),
                    Text.literal(" §7").append(Text.translatable("item.ic2_120.pulling_upgrade"))
                ),
                mouseX, mouseY
            )
        }
    }

    companion object {
        private val TEXTURE = Identifier.of("ic2", "textures/gui/scrapboxrecipes.png")
        private val UPTIPS_TEXTURE = Identifier.of("ic2", "textures/gui/uptips.png")
        private const val TEXTURE_SIZE = 256

        // 电量条 (178,2)-(191,15) = 13×13
        private const val ENERGY_BAR_U = 178
        private const val ENERGY_BAR_V = 2
        private const val ENERGY_BAR_W = 13
        private const val ENERGY_BAR_H = 13
        private const val ENERGY_BAR_X = 44
        private const val ENERGY_BAR_Y = 37

        // 验证电炉纹理 (178,47)-(199,62) = 21×15
        private const val VERIFY_U = 178
        private const val VERIFY_V = 47
        private const val VERIFY_W = 21
        private const val VERIFY_H = 15
        private const val VERIFY_X = 74
        private const val VERIFY_Y = 36

        // 工作进度纹理 (178,30)-(199,45) = 21×15
        private const val PROGRESS_U = 178
        private const val PROGRESS_V = 30
        private const val PROGRESS_W = 21
        private const val PROGRESS_H = 15
        private const val PROGRESS_X = 74
        private const val PROGRESS_Y = 36

        // uptips
        private const val UPTIPS_X = 4
        private const val UPTIPS_Y = 4
        private const val UPTIPS_SIZE = 16

        // XP 按钮
        private const val XP_BTN_X = 8
        private const val XP_BTN_Y = 60
        private const val XP_BTN_SIZE = 20
    }
}
