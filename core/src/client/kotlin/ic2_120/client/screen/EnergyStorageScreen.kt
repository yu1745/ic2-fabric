package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.t
import ic2_120.content.block.storage.EnergyStorageConfig
import ic2_120.content.screen.EnergyStorageScreenHandler
import ic2_120.registry.annotation.ModScreen
import ic2_120.client.compat.*
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Inventory
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.client.Minecraft

@ModScreen(
    handlers = [
        "batbox", "cesu", "mfe", "mfsu",
        "batbox_chargepad", "cesu_chargepad", "mfe_chargepad", "mfsu_chargepad"
    ]
)
class EnergyStorageScreen(
    val handler: EnergyStorageScreenHandler,
    playerInventory: Inventory,
    title: Component
) : LegacyContainerScreen26<EnergyStorageScreenHandler>(handler, playerInventory, title, 179, 196) {

   private val capacity: Long = resolveCapacity()
   private val useEquipmentSlots: Boolean = resolveUseEquipmentSlots()

    private fun resolveCapacity(): Long {
        return menu.context.evaluate({ world, pos ->
            val block = world.getBlockState(pos).block
            val id = BuiltInRegistries.BLOCK.getKey(block)
            EnergyStorageConfig.fromBlockPath(id.path)?.capacity ?: EnergyStorageConfig.BATBOX.capacity
        }, EnergyStorageConfig.BATBOX.capacity)
    }

    private fun resolveUseEquipmentSlots(): Boolean {
        return menu.context.evaluate({ world, pos ->
            val block = world.getBlockState(pos).block
            val id = BuiltInRegistries.BLOCK.getKey(block)
            EnergyStorageConfig.fromBlockPath(id.path)?.useEquipmentSlots ?: false
        }, false)
    }

    init {
    }

   override fun init() {
       super.init()
       if (menu.supportsRedstoneMode) {
           addRenderableWidget(
               net.minecraft.client.gui.components.Button.builder(Component.empty()) {
                   Minecraft.getInstance().connection?.send(
                       net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket(
                           menu.containerId, EnergyStorageScreenHandler.BUTTON_REDSTONE_MODE
                       )
                   )
               }.bounds(leftPos + imageWidth - 24, topPos + 4, 18, 14).build()
           )
       }
   }

   /** 服务端切换后聊天栏提示模式名；GUI 内鼠标悬停也显示当前模式。 */
   override fun renderTooltip(context: GuiGraphics, mouseX: Int, mouseY: Int) {
       super.renderTooltip(context, mouseX, mouseY)
       val btnX = leftPos + imageWidth - 24
       val btnY = topPos + 4
       if (menu.supportsRedstoneMode && mouseX in btnX until btnX + 18 && mouseY in btnY until btnY + 14) {
           context.renderTooltip(
               font,
               Component.translatable("gui.ic2_120.eu_storage.redstone_mode${menu.currentRedstoneMode}"),
               mouseX, mouseY
           )
       }
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
        val inputRate = menu.sync.getSyncedInsertedAmount()
        val outputRate = menu.sync.getSyncedExtractedAmount()
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
            context.blit(
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
            context.renderTooltip(
                font,
                Component.literal("${EnergyFormatUtils.formatRaw(energy)} / ${EnergyFormatUtils.formatRaw(cap)} EU"),
                mouseX, mouseY
            )
        }

        // 红石模式按钮：原版红石粉图标（居中于 18x14 按钮）
        if (menu.supportsRedstoneMode) {
            context.renderItem(
                net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.REDSTONE),
                leftPos + imageWidth - 24 + (18 - 16) / 2,
                topPos + 4 + (14 - 16) / 2
            )
        }

        if (!useEquipmentSlots) {
            context.blit(
                TEXTURE, left + OVERLAY_X, top + OVERLAY_Y,
                OVERLAY_U.toFloat(), OVERLAY_V.toFloat(),
                OVERLAY_W, OVERLAY_H,
                TEXTURE_SIZE, TEXTURE_SIZE
            )
        }

        val inputText = t("gui.ic2_120.input_eu", EnergyFormatUtils.formatRaw(inputRate))
        val outputText = t("gui.ic2_120.output_eu", EnergyFormatUtils.formatRaw(outputRate))
        val sideTextWidth = maxOf(font.width(inputText), font.width(outputText))
        val sideTextX = left - sideTextWidth - 4
        context.drawString(font, inputText, sideTextX, top + 4, 0xAAAAAA, false)
        context.drawString(font, outputText, sideTextX, top + 16, 0xAAAAAA, false)

        renderTooltip(context, mouseX, mouseY)
    }

    companion object {
        private val TEXTURE = Identifier.fromNamespaceAndPath("ic2", "textures/gui/guielectricblock.png")
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
