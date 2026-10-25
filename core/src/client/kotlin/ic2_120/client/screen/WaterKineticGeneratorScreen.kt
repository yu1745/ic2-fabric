package ic2_120.client.screen

import ic2_120.client.t
import ic2_120.content.block.WaterKineticGeneratorBlock
import ic2_120.content.screen.WaterKineticGeneratorScreenHandler
import ic2_120.registry.annotation.ModScreen
import ic2_120.client.compat.*
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Inventory
import net.minecraft.network.chat.Component as McText
import net.minecraft.resources.Identifier

@ModScreen(block = WaterKineticGeneratorBlock::class)
class WaterKineticGeneratorScreen(
    val handler: WaterKineticGeneratorScreenHandler,
    playerInventory: Inventory,
    title: McText
) : LegacyContainerScreen26<WaterKineticGeneratorScreenHandler>(handler, playerInventory, title, 176, 166) {

    init {
    }

    override fun renderBg(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.blit(TEXTURE, leftPos, topPos, 0f, 0f, 176, 166, 256, 256)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)

        val left = leftPos
        val top = topPos
        val generatedKu = menu.sync.generatedKu.coerceAtLeast(0)
        val outputKu = menu.sync.outputKu.coerceAtLeast(0)
        val blocked = menu.sync.isStuck != 0
        val submerged = menu.sync.isSubmerged != 0
        val flowBonus = menu.sync.waterFlowBonus != 0
        val rotorLifetimeTenthsHours = menu.sync.rotorLifetimeTenthsHours.coerceAtLeast(0)

        // 标题居中于 y=6
        context.drawString(font, title, left + (176 - font.width(title)) / 2, top + 6, 0x404040, false)

        // 状态信息（左侧）
        val generatedText = McText.translatable("ic2_120.jade.water_ku_generated", generatedKu).string
        val outputText = McText.translatable("ic2_120.jade.water_ku_output", outputKu).string
        val statusText = McText.translatable(
            when {
                blocked -> "gui.ic2_120.water_kinetic.blocked"
                !submerged -> "gui.ic2_120.water_kinetic.not_submerged"
                else -> "gui.ic2_120.water_kinetic.submerged"
            }
        ).string
        val flowText = if (flowBonus) McText.translatable("gui.ic2_120.water_kinetic.flow_bonus").string else ""
        val lifetimeText = McText.translatable(
            "gui.ic2_120.water_kinetic.lifetime",
            String.format("%.1f", rotorLifetimeTenthsHours / 10.0)
        ).string

        val texts = listOfNotNull(generatedText, outputText, statusText, flowText.takeIf { it.isNotEmpty() }, lifetimeText)
        val sideTextWidth = texts.maxOf { font.width(it) }
        val sideTextX = left - sideTextWidth - 4
        var textY = top + 8
        val statusColor = when {
            blocked -> 0xD65A5A
            !submerged -> 0xD6A052
            else -> 0x6FA85E
        }
        for (text in texts) {
            val color = when (text) {
                statusText -> statusColor
                flowText -> 0x55AAFF
                else -> 0xAAAAAA
            }
            context.drawString(font, text, sideTextX, textY, color, false)
            textY += 12
        }

        renderTooltip(context, mouseX, mouseY)
    }

    companion object {
        private val TEXTURE = Identifier.fromNamespaceAndPath("ic2", "textures/gui/guiwaterkineticgenerator.png")
    }
}
