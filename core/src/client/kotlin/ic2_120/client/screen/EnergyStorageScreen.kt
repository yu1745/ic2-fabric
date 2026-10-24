package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.t
import ic2_120.content.block.storage.EnergyStorageConfig
import ic2_120.content.screen.EnergyStorageScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Container
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

@ModScreen(
    handlers = [
        "batbox", "cesu", "mfe", "mfsu",
        "batbox_chargepad", "cesu_chargepad", "mfe_chargepad", "mfsu_chargepad"
    ]
)
class EnergyStorageScreen(
    handler: EnergyStorageScreenHandler,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<EnergyStorageScreenHandler>(handler, playerInventory, title) {

   private val capacity: Long = resolveCapacity()
   private val useEquipmentSlots: Boolean = resolveUseEquipmentSlots()

    private fun resolveCapacity(): Long {
        return handler.context.get({ world, pos ->
            val block = world.getBlockState(pos).block
            val id = BuiltInRegistries.BLOCK.getId(block)
            EnergyStorageConfig.fromBlockPath(id.path)?.capacity ?: EnergyStorageConfig.BATBOX.capacity
        }, EnergyStorageConfig.BATBOX.capacity)
    }

    private fun resolveUseEquipmentSlots(): Boolean {
        return handler.context.get({ world, pos ->
            val block = world.getBlockState(pos).block
            val id = BuiltInRegistries.BLOCK.getId(block)
            EnergyStorageConfig.fromBlockPath(id.path)?.useEquipmentSlots ?: false
        }, false)
    }

    init {
        backgroundWidth = 179
        backgroundHeight = 196
    }

   override fun init() {
       super.init()
       if (handler.supportsRedstoneMode) {
           addDrawableChild(
               net.minecraft.client.gui.components.Button.builder(Component.empty()) {
                   client?.player?.networkHandler?.sendPacket(
                       net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket(
                           handler.syncId, EnergyStorageScreenHandler.BUTTON_REDSTONE_MODE
                       )
                   )
               }.dimensions(x + backgroundWidth - 24, y + 4, 18, 14).build()
           )
       }
   }

   /** 服务端切换后聊天栏提示模式名；GUI 内鼠标悬停也显示当前模式。 */
   override fun drawMouseoverTooltip(context: GuiGraphics, mouseX: Int, mouseY: Int) {
       super.drawMouseoverTooltip(context, mouseX, mouseY)
       val btnX = x + backgroundWidth - 24
       val btnY = y + 4
       if (handler.supportsRedstoneMode && mouseX in btnX until btnX + 18 && mouseY in btnY until btnY + 14) {
           context.drawTooltip(
               textRenderer,
               Component.translatable("gui.ic2_120.eu_storage.redstone_mode${handler.currentRedstoneMode}"),
               mouseX, mouseY
           )
       }
   }

    override fun drawBackground(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, backgroundWidth, backgroundHeight, TEXTURE_SIZE, TEXTURE_SIZE)
    }

   override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
       renderBackground(context)
       super.render(context, mouseX, mouseY, delta)

       val left = x
        val top = y

        context.drawText(textRenderer, title, left + (backgroundWidth - textRenderer.getWidth(title)) / 2, top + 6, 0x404040, false)

        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val outputRate = handler.sync.getSyncedExtractedAmount()
        val cap = capacity
        val fraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f

        if (fraction > 0f) {
            val fillWidth = (ENERGY_BAR_W * fraction).toInt().coerceAtLeast(1)
            context.enableScissor(
                left + ENERGY_BAR_X,
                top + ENERGY_BAR_Y,
                left + ENERGY_BAR_X + fillWidth,
                top + ENERGY_BAR_Y + ENERGY_BAR_H
            )
            context.drawTexture(
                TEXTURE,
                left + ENERGY_BAR_X, top + ENERGY_BAR_Y,
                ENERGY_BAR_U.toFloat(), ENERGY_BAR_V.toFloat(),
                ENERGY_BAR_W, ENERGY_BAR_H,
                TEXTURE_SIZE, TEXTURE_SIZE
            )
            context.disableScissor()
        }

        val relX = mouseX - left
        val relY = mouseY - top
        if (relX in ENERGY_BAR_X until ENERGY_BAR_X + ENERGY_BAR_W &&
            relY in ENERGY_BAR_Y until ENERGY_BAR_Y + ENERGY_BAR_H
        ) {
            context.drawTooltip(
                textRenderer,
                Component.literal("${EnergyFormatUtils.formatRaw(energy)} / ${EnergyFormatUtils.formatRaw(cap)} EU"),
                mouseX, mouseY
            )
        }

        // 红石模式按钮：原版红石粉图标（居中于 18x14 按钮）
        if (handler.supportsRedstoneMode) {
            context.drawItem(
                net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.REDSTONE),
                x + backgroundWidth - 24 + (18 - 16) / 2,
                y + 4 + (14 - 16) / 2
            )
        }

        if (!useEquipmentSlots) {
            context.drawTexture(
                TEXTURE, left + OVERLAY_X, top + OVERLAY_Y,
                OVERLAY_U.toFloat(), OVERLAY_V.toFloat(),
                OVERLAY_W, OVERLAY_H,
                TEXTURE_SIZE, TEXTURE_SIZE
            )
        }

        val inputText = t("gui.ic2_120.input_eu", EnergyFormatUtils.formatRaw(inputRate))
        val outputText = t("gui.ic2_120.output_eu", EnergyFormatUtils.formatRaw(outputRate))
        val sideTextWidth = maxOf(textRenderer.getWidth(inputText), textRenderer.getWidth(outputText))
        val sideTextX = left - sideTextWidth - 4
        context.drawText(textRenderer, inputText, sideTextX, top + 4, 0xAAAAAA, false)
        context.drawText(textRenderer, outputText, sideTextX, top + 16, 0xAAAAAA, false)

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath("ic2", "textures/gui/guielectricblock.png")
        private const val TEXTURE_SIZE = 256
        private const val ENERGY_BAR_U = 180
        private const val ENERGY_BAR_V = 4
        private const val ENERGY_BAR_W = 24
        private const val ENERGY_BAR_H = 16
        private const val ENERGY_BAR_X = 79
        private const val ENERGY_BAR_Y = 35
        private const val OVERLAY_U = 179
        private const val OVERLAY_V = 23
        private const val OVERLAY_W = 72
        private const val OVERLAY_H = 18
        private const val OVERLAY_X = 7
        private const val OVERLAY_Y = 83
    }
}
