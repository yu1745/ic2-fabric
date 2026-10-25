package ic2_120.client.screen

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.MouseButtonInfo
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.AbstractContainerMenu

/** Maps the old screen hooks onto the 26.2 extraction pass. */
abstract class LegacyContainerScreen26<T : AbstractContainerMenu>(
    menu: T, inventory: Inventory, title: Component, width: Int, height: Int
) : AbstractContainerScreen<T>(menu, inventory, title, width, height) {
    open fun renderBg(context: GuiGraphicsExtractor, delta: Float, mouseX: Int, mouseY: Int) {}

    open fun render(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {}

    open fun renderBackground(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {}

    open fun renderTooltip(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {}

    open fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        super.mouseClicked(MouseButtonEvent(mouseX, mouseY, MouseButtonInfo(button, 0)), false)

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean =
        mouseClicked(event.x, event.y, event.button())

    open fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean =
        super.mouseDragged(MouseButtonEvent(mouseX, mouseY, MouseButtonInfo(button, 0)), deltaX, deltaY)

    override fun mouseDragged(event: MouseButtonEvent, deltaX: Double, deltaY: Double): Boolean =
        mouseDragged(event.x, event.y, event.button(), deltaX, deltaY)

    open fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean =
        super.mouseReleased(MouseButtonEvent(mouseX, mouseY, MouseButtonInfo(button, 0)))

    override fun mouseReleased(event: MouseButtonEvent): Boolean =
        mouseReleased(event.x, event.y, event.button())

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        renderBg(context, delta, mouseX, mouseY)
        super.extractRenderState(context, mouseX, mouseY, delta)
        render(context, mouseX, mouseY, delta)
    }
}
