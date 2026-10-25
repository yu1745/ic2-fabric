package ic2_120.client.compat

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.item.ItemStack
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import org.joml.Matrix3x2fStack
import java.util.Optional

typealias GuiGraphics = GuiGraphicsExtractor

fun GuiGraphics.blit(texture: Identifier, x: Int, y: Int, u: Float, v: Float, width: Int, height: Int, textureWidth: Int, textureHeight: Int) {
    blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, textureWidth, textureHeight)
}

fun GuiGraphics.drawString(font: Font, text: Component, x: Int, y: Int, color: Int, shadow: Boolean) {
    text(font, text, x, y, color, shadow)
}

fun GuiGraphics.drawString(font: Font, text: String, x: Int, y: Int, color: Int, shadow: Boolean) {
    text(font, text, x, y, color, shadow)
}

fun GuiGraphics.drawString(font: Font, text: Component, x: Int, y: Int, color: Int) {
    text(font, text, x, y, color, false)
}

fun GuiGraphics.drawString(font: Font, text: String, x: Int, y: Int, color: Int) {
    text(font, text, x, y, color, false)
}

fun GuiGraphics.drawCenteredString(font: Font, text: String, x: Int, y: Int, color: Int) {
    centeredText(font, text, x, y, color)
}

fun GuiGraphics.drawString(font: Font, text: FormattedCharSequence, x: Int, y: Int, color: Int, shadow: Boolean) {
    text(font, text, x, y, color, shadow)
}

fun GuiGraphics.renderTooltip(font: Font, text: Component, x: Int, y: Int) {
    setTooltipForNextFrame(font, text, x, y)
}

fun GuiGraphics.renderTooltip(font: Font, stack: ItemStack, x: Int, y: Int) {
    setTooltipForNextFrame(font, stack, x, y)
}

fun GuiGraphics.renderComponentTooltip(font: Font, lines: List<Component>, x: Int, y: Int) {
    setTooltipForNextFrame(font, lines, Optional.empty(), x, y)
}

fun GuiGraphics.renderItem(stack: ItemStack, x: Int, y: Int) {
    item(stack, x, y)
}

fun GuiGraphics.renderItemDecorations(font: Font, stack: ItemStack, x: Int, y: Int) {
    itemDecorations(font, stack, x, y)
}

fun GuiGraphics.renderOutline(x: Int, y: Int, width: Int, height: Int, color: Int) {
    fill(x, y, x + width, y + 1, color)
    fill(x, y + height - 1, x + width, y + height, color)
    fill(x, y, x + 1, y + height, color)
    fill(x + width - 1, y, x + width, y + height, color)
}

fun GuiGraphics.blit(x: Int, y: Int, z: Int, width: Int, height: Int, sprite: TextureAtlasSprite, r: Float, g: Float, b: Float, a: Float) {
    val color = ((a.coerceIn(0f, 1f) * 255).toInt() shl 24) or
        ((r.coerceIn(0f, 1f) * 255).toInt() shl 16) or
        ((g.coerceIn(0f, 1f) * 255).toInt() shl 8) or
        (b.coerceIn(0f, 1f) * 255).toInt()
    blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height, color)
}

fun Matrix3x2fStack.pushPose() { pushMatrix() }
fun Matrix3x2fStack.popPose() { popMatrix() }
fun Matrix3x2fStack.translate(x: Double, y: Double, z: Double) { translate(x.toFloat(), y.toFloat()) }
fun Matrix3x2fStack.scale(x: Float, y: Float, z: Float) { scale(x, y) }
