package ic2_120.client.renderers

import ic2_120.content.block.ComposeDebugBlockEntity
import net.minecraft.client.render.LightmapTextureManager
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Identifier
import net.minecraft.util.math.RotationAxis
import org.joml.Matrix3f
import org.joml.Matrix4f
import kotlin.math.PI
import kotlin.math.sin

class ComposeDebugBlockEntityRenderer(
    context: BlockEntityRendererFactory.Context
) : BlockEntityRenderer<ComposeDebugBlockEntity> {

    companion object {
        private val WHITE_TEXTURE = Identifier("textures/misc/white.png")
        private const val RING_SEGMENTS = 24
    }

    init {
        @Suppress("UNUSED_VARIABLE")
        val ignored = context
    }

    override fun render(
        entity: ComposeDebugBlockEntity,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        val world = entity.world ?: return
        val time = world.time + tickDelta
        val bob = sin(time * 0.08f) * 0.09f
        val spinDeg = time * 3.0f

        val vc = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(WHITE_TEXTURE))
        val fullLight = LightmapTextureManager.MAX_LIGHT_COORDINATE

        matrices.push()
        matrices.translate(0.5, 0.0, 0.5)

        drawHalo(matrices, vc, spinDeg, bob, fullLight, overlay)
        drawArrow(matrices, vc, spinDeg, bob, fullLight, overlay)

        matrices.pop()
    }

    private fun drawHalo(
        matrices: MatrixStack,
        vc: VertexConsumer,
        spinDeg: Float,
        bob: Float,
        light: Int,
        overlay: Int
    ) {
        val radius = 0.58f
        val ringHeight = 0.055f
        val ringThickness = 0.030f
        val segmentArcHalf = ((2.0 * PI * radius) / RING_SEGMENTS * 0.46).toFloat()
        val y = 0.14f + bob * 0.35f

        repeat(RING_SEGMENTS) { i ->
            val angle = spinDeg + (360.0f * i / RING_SEGMENTS)
            matrices.push()
            matrices.translate(0.0, y.toDouble(), 0.0)
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle))
            matrices.translate(radius.toDouble(), 0.0, 0.0)
            drawCuboid(
                matrices, vc, light, overlay,
                minX = -ringThickness * 0.5f, maxX = ringThickness * 0.5f,
                minY = -ringHeight * 0.5f, maxY = ringHeight * 0.5f,
                minZ = -segmentArcHalf, maxZ = segmentArcHalf,
                red = 80, green = 235, blue = 255, alpha = 150
            )
            matrices.pop()
        }
    }

    private fun drawArrow(
        matrices: MatrixStack,
        vc: VertexConsumer,
        spinDeg: Float,
        bob: Float,
        light: Int,
        overlay: Int
    ) {
        val baseY = 1.28f + bob
        matrices.push()
        matrices.translate(0.0, baseY.toDouble(), 0.0)
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-spinDeg * 1.2f))

        drawCuboid(
            matrices, vc, light, overlay,
            minX = -0.035f, maxX = 0.035f,
            minY = -0.20f, maxY = 0.06f,
            minZ = -0.035f, maxZ = 0.035f,
            red = 95, green = 245, blue = 255, alpha = 170
        )

        drawPyramid(
            matrices, vc, light, overlay,
            tipY = 0.33f,
            baseY = 0.06f,
            baseHalf = 0.12f,
            red = 125, green = 250, blue = 255, alpha = 195
        )
        matrices.pop()
    }

    private fun drawPyramid(
        matrices: MatrixStack,
        vc: VertexConsumer,
        light: Int,
        overlay: Int,
        tipY: Float,
        baseY: Float,
        baseHalf: Float,
        red: Int,
        green: Int,
        blue: Int,
        alpha: Int
    ) {
        val entry = matrices.peek()
        val pos = entry.positionMatrix
        val normal = entry.normalMatrix

        val bx1 = -baseHalf
        val bz1 = -baseHalf
        val bx2 = baseHalf
        val bz2 = -baseHalf
        val bx3 = baseHalf
        val bz3 = baseHalf
        val bx4 = -baseHalf
        val bz4 = baseHalf

        // 四个三角侧面（用退化四边形提交）
        quadColor(vc, pos, normal, light, overlay, 0f, tipY, 0f, bx1, baseY, bz1, bx2, baseY, bz2, 0f, tipY, 0f, red, green, blue, alpha, 0f, 1f, -1f)
        quadColor(vc, pos, normal, light, overlay, 0f, tipY, 0f, bx2, baseY, bz2, bx3, baseY, bz3, 0f, tipY, 0f, red, green, blue, alpha, 1f, 1f, 0f)
        quadColor(vc, pos, normal, light, overlay, 0f, tipY, 0f, bx3, baseY, bz3, bx4, baseY, bz4, 0f, tipY, 0f, red, green, blue, alpha, 0f, 1f, 1f)
        quadColor(vc, pos, normal, light, overlay, 0f, tipY, 0f, bx4, baseY, bz4, bx1, baseY, bz1, 0f, tipY, 0f, red, green, blue, alpha, -1f, 1f, 0f)

        quadColor(vc, pos, normal, light, overlay, bx1, baseY, bz4, bx2, baseY, bz3, bx3, baseY, bz2, bx4, baseY, bz1, red, green, blue, alpha, 0f, -1f, 0f)
    }

    private fun drawCuboid(
        matrices: MatrixStack,
        vc: VertexConsumer,
        light: Int,
        overlay: Int,
        minX: Float,
        maxX: Float,
        minY: Float,
        maxY: Float,
        minZ: Float,
        maxZ: Float,
        red: Int,
        green: Int,
        blue: Int,
        alpha: Int
    ) {
        val entry = matrices.peek()
        val pos = entry.positionMatrix
        val normal = entry.normalMatrix

        quadColor(vc, pos, normal, light, overlay, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, red, green, blue, alpha, 0f, 0f, 1f)
        quadColor(vc, pos, normal, light, overlay, maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, red, green, blue, alpha, 0f, 0f, -1f)
        quadColor(vc, pos, normal, light, overlay, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, red, green, blue, alpha, -1f, 0f, 0f)
        quadColor(vc, pos, normal, light, overlay, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, red, green, blue, alpha, 1f, 0f, 0f)
        quadColor(vc, pos, normal, light, overlay, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ, red, green, blue, alpha, 0f, 1f, 0f)
        quadColor(vc, pos, normal, light, overlay, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, red, green, blue, alpha, 0f, -1f, 0f)
    }

    private fun quadColor(
        vc: VertexConsumer,
        pos: Matrix4f,
        normal: Matrix3f,
        light: Int,
        overlay: Int,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        x4: Float, y4: Float, z4: Float,
        red: Int, green: Int, blue: Int, alpha: Int,
        nx: Float, ny: Float, nz: Float
    ) {
        vertex(vc, pos, normal, x1, y1, z1, light, overlay, red, green, blue, alpha, nx, ny, nz)
        vertex(vc, pos, normal, x2, y2, z2, light, overlay, red, green, blue, alpha, nx, ny, nz)
        vertex(vc, pos, normal, x3, y3, z3, light, overlay, red, green, blue, alpha, nx, ny, nz)
        vertex(vc, pos, normal, x4, y4, z4, light, overlay, red, green, blue, alpha, nx, ny, nz)
    }

    private fun vertex(
        vc: VertexConsumer,
        pos: Matrix4f,
        normal: Matrix3f,
        x: Float,
        y: Float,
        z: Float,
        light: Int,
        overlay: Int,
        red: Int,
        green: Int,
        blue: Int,
        alpha: Int,
        nx: Float,
        ny: Float,
        nz: Float
    ) {
        vc.vertex(pos, x, y, z)
            .color(red, green, blue, alpha)
            .texture(0f, 0f)
            .overlay(overlay.takeUnless { it == 0 } ?: OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(normal, nx, ny, nz)
            .next()
    }

    override fun rendersOutsideBoundingBox(blockEntity: ComposeDebugBlockEntity): Boolean = true
}
