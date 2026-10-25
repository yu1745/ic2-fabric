package ic2_120.client.renderers

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import ic2_120.config.Ic2Config
import ic2_120.content.entity.LaserProjectileEntity
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.Identifier
import net.minecraft.util.LightCoordsUtil
import kotlin.math.atan2

/** Mining laser beam rendered with the 26.2 entity render-state pipeline. */
class LaserProjectileEntityRenderer(context: EntityRendererProvider.Context)
    : EntityRenderer<LaserProjectileEntity, LaserProjectileEntityRenderer.State>(context) {

    companion object {
        private val WHITE_TEXTURE = Identifier.withDefaultNamespace("textures/misc/white.png")
        private const val SIDES = 6
        private const val GLOW_RADIUS_MULT = 2.5f
        private const val GLOW_ALPHA = 40
    }

    class State : EntityRenderState() {
        var visible = false
        var yaw = 0f
        var pitch = 0f
        var length = 0f
        var radius = 0f
        var r = 1f
        var g = 1f
        var b = 1f
    }

    override fun createRenderState() = State()

    override fun extractRenderState(entity: LaserProjectileEntity, state: State, partialTicks: Float) {
        super.extractRenderState(entity, state, partialTicks)
        val minDist = LaserProjectileEntity.VISUAL_LENGTH * 0.3f
        val vel = entity.deltaMovement
        val speed = vel.length()
        state.visible = state.distanceToCameraSq >= minDist * minDist && speed >= 0.001
        if (!state.visible) return
        val color = entity.color
        state.r = ((color shr 16) and 255) / 255f
        state.g = ((color shr 8) and 255) / 255f
        state.b = (color and 255) / 255f
        val dir = vel.scale(1.0 / speed)
        state.yaw = Math.toDegrees(atan2(dir.x, dir.z)).toFloat()
        state.pitch = Math.toDegrees(atan2(-dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z))).toFloat()
        state.length = LaserProjectileEntity.VISUAL_LENGTH.toFloat()
        state.radius = Ic2Config.current.miningLaser.bulletRadius.toFloat()
    }

    override fun submit(state: State, matrices: PoseStack, nodes: SubmitNodeCollector, camera: CameraRenderState) {
        if (!state.visible) return
        matrices.pushPose()
        matrices.mulPose(Axis.YP.rotationDegrees(state.yaw))
        matrices.mulPose(Axis.XP.rotationDegrees(state.pitch))
        nodes.submitCustomGeometry(matrices, RenderTypes.entityTranslucent(WHITE_TEXTURE)) { pose, consumer ->
            drawPrism(pose, consumer, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                state.length, state.radius * GLOW_RADIUS_MULT, state.r, state.g, state.b, GLOW_ALPHA)
            drawPrism(pose, consumer, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                state.length, state.radius, state.r, state.g, state.b, 220)
            drawTip(pose, consumer, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                state.length, state.radius, state.r, state.g, state.b, 220)
        }
        matrices.popPose()
        super.submit(state, matrices, nodes, camera)
    }

    /**
     * 沿 Z 轴绘制从 z=0 到 z=length 的棱柱体。
     * 在局部坐标系中，Z+ 为飞行方向。
     */
    private fun drawPrism(
        entry: PoseStack.Pose,
        vc: VertexConsumer,
        light: Int,
        overlay: Int,
        length: Float,
        radius: Float,
        r: Float, g: Float, b: Float, a: Int
    ) {
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
        entry: PoseStack.Pose,
        vc: VertexConsumer,
        light: Int,
        overlay: Int,
        length: Float,
        radius: Float,
        r: Float, g: Float, b: Float, a: Int
    ) {
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
