package ic2_120.client.screen

import ic2_120.client.t
import ic2_120.content.block.PatternStorageBlock
import ic2_120.content.network.NetworkManager
import ic2_120.content.screen.PatternStorageScreenHandler
import ic2_120.content.screen.GuiSize
import ic2_120.content.uu.UuTemplateEntry
import ic2_120.registry.annotation.ModScreen
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import ic2_120.client.compat.*
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.item.ItemStack
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import ic2_120.content.network.SelectTemplatePayload
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.player.Inventory

@ModScreen(block = PatternStorageBlock::class)
class PatternStorageScreen(
    val handler: PatternStorageScreenHandler, playerInventory: Inventory, title: Component
) : LegacyContainerScreen26<PatternStorageScreenHandler>(handler, playerInventory, title, 176, 166) {

    private var templates: List<UuTemplateEntry> = emptyList()
    private var selectedIndex = -1

    init {
    }

    override fun renderBg(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.blit(TEXTURE, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, TEX_SIZE, TEX_SIZE)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)

        val left = leftPos
        val top = topPos

        context.drawString(font, title, left + (imageWidth - font.width(title)) / 2, top + 6, 0x404040, false)

        val world = Minecraft.getInstance().level
        val storage = world?.let(menu::getPatternStorage)
        templates = storage?.getTemplatesSnapshot().orEmpty()
        selectedIndex = storage?.selectedTemplateIndex ?: -1

        // 模板物品纹理 (18,20) 18×18 居中
        drawTemplateItem(context, left, top)

        // 选中模板文本信息 (8,46)-(167,79) 7px
        drawTemplateInfo(context, left, top)

        // 悬停高亮
        val relX = mouseX - left
        val relY = mouseY - top

        // 读取水晶 (10,37)-(26,45)
        if (relX in READ_X1..READ_X2 && relY in READ_Y1..READ_Y2) {
            context.fill(left + READ_X1, top + READ_Y1, left + READ_X2 + 1, top + READ_Y2 + 1, 0x80FFFFFF.toInt())
            context.renderComponentTooltip(font, listOf(Component.translatable("gui.ic2_120.pattern_storage.import_crystal")), mouseX, mouseY)
        }
        // 写入水晶 (26,37)-(42,45)
        if (relX in WRITE_X1..WRITE_X2 && relY in WRITE_Y1..WRITE_Y2) {
            context.fill(left + WRITE_X1, top + WRITE_Y1, left + WRITE_X2 + 1, top + WRITE_Y2 + 1, 0x80FFFFFF.toInt())
            context.renderComponentTooltip(font, listOf(Component.translatable("gui.ic2_120.pattern_storage.write_crystal")), mouseX, mouseY)
        }
        // 向左 (7,19)-(16,37)
        if (templates.size > 1 && relX in NAV_L_X1..NAV_L_X2 && relY in NAV_Y1..NAV_Y2) {
            context.fill(left + NAV_L_X1, top + NAV_Y1, left + NAV_L_X2 + 1, top + NAV_Y2 + 1, 0x80FFFFFF.toInt())
        }
        // 向右 (36,19)-(45,37)
        if (templates.size > 1 && relX in NAV_R_X1..NAV_R_X2 && relY in NAV_Y1..NAV_Y2) {
            context.fill(left + NAV_R_X1, top + NAV_Y1, left + NAV_R_X2 + 1, top + NAV_Y2 + 1, 0x80FFFFFF.toInt())
        }

        renderTooltip(context, mouseX, mouseY)
    }

    private fun drawTemplateItem(context: GuiGraphics, left: Int, top: Int) {
        val template = templates.getOrNull(selectedIndex) ?: return
        val stack = templateToStack(template)
        if (stack.isEmpty) return
        context.renderItem(stack, left + TEMPLATE_ITEM_X, top + TEMPLATE_ITEM_Y)
    }

    private fun drawTemplateInfo(context: GuiGraphics, left: Int, top: Int) {
        val template = templates.getOrNull(selectedIndex)
        val text = if (template != null) {
            "${template.displayName().string} (${template.uuCostUb} uB)"
        } else {
            t("gui.ic2_120.pattern_storage.no_select_info")
        }
        val scale = 7f / 9f
        context.pose().pushPose()
        context.pose().scale(scale, scale, 1f)
        val sx = ((left + INFO_X1) / scale).toInt()
        val sy = ((top + INFO_Y + 2) / scale).toInt()
        context.drawString(font, text, sx, sy, 0xFFFFFF, false)
        context.pose().popPose()
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) {
            val relX = mouseX.toInt() - leftPos
            val relY = mouseY.toInt() - topPos

            if (relX in READ_X1..READ_X2 && relY in READ_Y1..READ_Y2) {
                Minecraft.getInstance().connection?.send(
                    ServerboundContainerButtonClickPacket(menu.containerId, PatternStorageScreenHandler.BUTTON_IMPORT_FROM_CRYSTAL))
                return true
            }
            if (relX in WRITE_X1..WRITE_X2 && relY in WRITE_Y1..WRITE_Y2) {
                Minecraft.getInstance().connection?.send(
                    ServerboundContainerButtonClickPacket(menu.containerId, PatternStorageScreenHandler.BUTTON_EXPORT_TO_CRYSTAL))
                return true
            }
            if (templates.size > 1 && relX in NAV_L_X1..NAV_L_X2 && relY in NAV_Y1..NAV_Y2) {
                navigateTemplate(-1)
                return true
            }
            if (templates.size > 1 && relX in NAV_R_X1..NAV_R_X2 && relY in NAV_Y1..NAV_Y2) {
                navigateTemplate(1)
                return true
            }
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    private fun navigateTemplate(delta: Int) {
        if (templates.isEmpty()) return
        val newIndex = ((selectedIndex + delta) % templates.size + templates.size) % templates.size
        ClientPlayNetworking.send(SelectTemplatePayload(menu.blockPos, newIndex))
    }

    private fun templateToStack(template: UuTemplateEntry): ItemStack {
        val id = Identifier.tryParse(template.itemId) ?: return ItemStack.EMPTY
        val item = BuiltInRegistries.ITEM.getOptional(id).orElse(null) ?: return ItemStack.EMPTY
        return if (item == net.minecraft.world.item.Items.AIR) ItemStack.EMPTY else ItemStack(item)
    }

    companion object {
        private val TEXTURE = Identifier.fromNamespaceAndPath("ic2", "textures/gui/guipatternstorage.png")
        private const val TEX_SIZE = 256

        private const val TEMPLATE_ITEM_X = 18
        private const val TEMPLATE_ITEM_Y = 20

        private const val NAV_Y1 = 19
        private const val NAV_Y2 = 37
        private const val NAV_L_X1 = 7
        private const val NAV_L_X2 = 16
        private const val NAV_R_X1 = 36
        private const val NAV_R_X2 = 45

        private const val READ_X1 = 10
        private const val READ_Y1 = 37
        private const val READ_X2 = 26
        private const val READ_Y2 = 45

        private const val WRITE_X1 = 26
        private const val WRITE_Y1 = 37
        private const val WRITE_X2 = 42
        private const val WRITE_Y2 = 45

        private const val INFO_X1 = 8
        private const val INFO_Y = 46
    }
}
