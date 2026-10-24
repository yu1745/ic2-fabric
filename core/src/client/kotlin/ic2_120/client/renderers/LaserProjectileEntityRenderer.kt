package ic2_120.client.renderers

import ic2_120.config.Ic2Config
import ic2_120.content.entity.LaserProjectileEntity
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.RenderType
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import kotlin.math.atan2

/**
 * 采矿镭射枪弹射体渲染器。
 * 将弹射体渲染为沿飞行方向、长度为 VISUAL_LENGTH 的发光棱柱体（子弹形态）。
 */
class LaserProjectileEntityRenderer(
    context: EntityRendererProvider.Context
) : EntityRenderer<LaserProjectileEntity>(context) {

    companion object {
        private val WHITE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/misc/white.png")

        /** 弹体截面半径 */
        private val RADIUS: Float get() = Ic2Config.current.miningLaser.bulletRadius.toFloat()
        /** 截面边数 */
        private const val SIDES = 6

        /** 外层辉光半径倍数 */
        private const val GLOW_RADIUS_MULT = 2.5f
        /** 外层辉光透明度 */
        private const val GLOW_ALPHA = 40
    }

    init {
        @Suppress("UNUSED_VARIABLE")
        val ignored = context
    }

    override fun getTexture(entity: LaserProjectileEntity): ResourceLocation = WHITE_TEXTURE

    override fun render(
        entity: LaserProjectileEntity,
        yaw: Float,
        tickDelta: Float,
        matrices: PoseStack,
        vertexConsumers: MultiBufferSource,
        light: Int
    ) {
        // 零距离渲染会占满屏幕，只有飞出一定距离后才显示
        val renderX = entity.prevX + (entity.x - entity.prevX) * tickDelta
        val renderY = entity.prevY + (entity.y - entity.prevY) * tickDelta
        val renderZ = entity.prevZ + (entity.z - entity.prevZ) * tickDelta
        val cam = this.dispatcher.camera.pos
        val distSq = (renderX - cam.x) * (renderX - cam.x) +
                     (renderY - cam.y) * (renderY - cam.y) +
                     (renderZ - cam.z) * (renderZ - cam.z)
        val minDist = LaserProjectileEntity.VISUAL_LENGTH * 0.3f
        if (distSq < minDist * minDist) return

        val col = entity.color
        val r = ((col shr 16) and 0xFF) / 255f
        val g = ((col shr 8) and 0xFF) / 255f
        val b = (col and 0xFF) / 255f

        // 计算飞行方向
        val vel = entity.velocity
        val speed = vel.length()
        if (speed < 0.001) return

        val dir = vel.multiply(1.0 / speed)
        val length = LaserProjectileEntity.VISUAL_LENGTH.toFloat()

        // 计算偏航和俯仰来对齐弹体
        val dirYaw = Math.toDegrees(atan2(dir.x, dir.z)).toFloat()
        val dirPitch = Math.toDegrees(atan2(-dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z))).toFloat()

        matrices.pushPose()
        matrices.translate(
            renderX - entity.x,
            renderY - entity.y,
            renderZ - entity.z
        )

        // 弹体尾端对齐实体位置（头部朝飞行方向前方）
        // 先旋转到飞行方向
        matrices.mulPose(com.mojang.math.Axis.YP.rotationDegrees(dirYaw))
        matrices.mulPose(com.mojang.math.Axis.XP.rotationDegrees(dirPitch))

        val fullLight = LightTexture.FULL_BRIGHT
        val overlay = OverlayTexture.NO_OVERLAY

        // --- 外层辉光 ---
        val glowVc = vertexConsumers.getBuffer(RenderType.entityTranslucent(WHITE_TEXTURE))
        drawPrism(matrices, glowVc, fullLight, overlay, length, RADIUS * GLOW_RADIUS_MULT, r, g, b, GLOW_ALPHA)

        // --- 核心弹体 ---
        val coreVc = vertexConsumers.getBuffer(RenderType.entityTranslucent(WHITE_TEXTURE))
        drawPrism(matrices, coreVc, fullLight, overlay, length, RADIUS, r, g, b, 220)

        // --- 头部亮点 ---
        val tipVc = vertexConsumers.getBuffer(RenderType.entityTranslucent(WHITE_TEXTURE))
        drawTip(matrices, tipVc, fullLight, overlay, length, RADIUS, r, g, b, 220)

        matrices.popPose()
    }

    /**
     * 沿 Z 轴绘制从 z=0 到 z=length 的棱柱体。
     * 在局部坐标系中，Z+ 为飞行方向。
     */
    private fun drawPrism(
        matrices: PoseStack,
        vc: VertexConsumer,
        light: Int,
        overlay: Int,
        length: Float,
        radius: Float,
        r: Float, g: Float, b: Float, a: Int
    ) {
        val entry = matrices.last()
        val step = (2.0 * Math.PI / SIDES).toFloat()

        for (i in 0 until SIDES) {
            val angle0 = i * step
            val angle1 = (i + 1) * step

            val cos0 = Math.cos(angle0.toDouble()).toFloat()
            val sin0 = Math.sin(angle0.toDouble()).toFloat()
            val cos1 = Math.cos(angle1.toDouble()).toFloat()
            val sin1 = Math.sin(angle1.toDouble()).toFloat()

            val nx = (cos0 + cos1) * 0.5f
            val nz = (sin0 + sin1) * 0.5f

            // 侧面 (从尾端 z=0 到 头端 z=length)
            vertex(vc, entry, cos0 * radius, sin0 * radius, 0f, light, overlay, r, g, b, a, nx, 0f, nz)
            vertex(vc, entry, cos1 * radius, sin1 * radius, 0f, light, overlay, r, g, b, a, nx, 0f, nz)
            vertex(vc, entry, cos1 * radius, sin1 * radius, length, light, overlay, r, g, b, a, nx, 0f, nz)
            vertex(vc, entry, cos0 * radius, sin0 * radius, length, light, overlay, r, g, b, a, nx, 0f, nz)
        }
    }

    /**
     * 绘制头端锥形亮点，制造子弹头部高光效果。
     */
    private fun drawTip(
        matrices: PoseStack,
        vc: VertexConsumer,
        light: Int,
        overlay: Int,
        length: Float,
        radius: Float,
        r: Float, g: Float, b: Float, a: Int
    ) {
        val entry = matrices.last()
        val step = (2.0 * Math.PI / SIDES).toFloat()
        val tipZ = length + radius * 0.6f // 锥尖比弹体再伸出一点

        for (i in 0 until SIDES) {
            val angle0 = i * step
            val angle1 = (i + 1) * step

            val cos0 = Math.cos(angle0.toDouble()).toFloat()
            val sin0 = Math.sin(angle0.toDouble()).toFloat()
            val cos1 = Math.cos(angle1.toDouble()).toFloat()
            val sin1 = Math.sin(angle1.toDouble()).toFloat()

            // 头端面 → 锥尖
            vertex(vc, entry, cos0 * radius, sin0 * radius, length, light, overlay, 1f, 1f, 1f, (a * 0.7f).toInt(), 0f, 0f, 1f)
            vertex(vc, entry, cos1 * radius, sin1 * radius, length, light, overlay, 1f, 1f, 1f, (a * 0.7f).toInt(), 0f, 0f, 1f)
            vertex(vc, entry, 0f, 0f, tipZ, light, overlay, r, g, b, a, 0f, 0f, 1f)
            vertex(vc, entry, 0f, 0f, tipZ, light, overlay, r, g, b, a, 0f, 0f, 1f)
        }
    }

    private fun vertex(
        vc: VertexConsumer,
        entry: PoseStack.Pose,
        x: Float, y: Float, z: Float,
        light: Int,
        overlay: Int,
        r: Float, g: Float, b: Float, a: Int,
        nx: Float, ny: Float, nz: Float
    ) {
        vc.addVertex(entry.pose(), x, y, z)
            .setColor(r, g, b, a / 255f)
            .setUv(0f, 0f)
            .setOverlay(overlay)
            .setLight(light)
            .setNormal(entry, nx, ny, nz)
    }
}
