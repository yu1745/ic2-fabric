package ic2_120.client.renderers

import ic2_120.content.block.machines.TeleporterBlockEntity
import net.minecraft.client.render.LightmapTextureManager
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.util.Identifier
import org.joml.Matrix3f
import org.joml.Matrix4f
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

class TeleporterBlockEntityRenderer(
    context: BlockEntityRendererFactory.Context
) : BlockEntityRenderer<TeleporterBlockEntity> {

    companion object {
        private val WHITE_TEXTURE = Identifier("textures/misc/white.png")
        private const val SEGMENTS = 36
        private data class RenderCache(
            var lastProgress: Float = 0f,
            var lastTargetId: Int = -1,
            var sx: Double = 0.0,
            var sy: Double = 0.0,
            var sz: Double = 0.0
        )
        private val cacheByPos = HashMap<Long, RenderCache>()
    }

    init {
        @Suppress("UNUSED_VARIABLE")
        val ignored = context
    }

    override fun render(
        entity: TeleporterBlockEntity,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        val world = entity.world ?: return
        val key = entity.pos.asLong()
        if (!entity.clientCharging) {
            cacheByPos.remove(key)
            return
        }

        val side = entity.clientTeleportRange.toDouble().coerceIn(
            TeleporterBlockEntity.TELEPORT_RANGE_MIN.toDouble(),
            TeleporterBlockEntity.TELEPORT_RANGE_MAX.toDouble()
        )
        val half = side / 2.0
        val centerX = entity.pos.x + 0.5
        val centerY = entity.pos.y + 1.5
        val centerZ = entity.pos.z + 0.5

        val sourceBox = net.minecraft.util.math.Box(
            centerX - half, centerY - half, centerZ - half,
            centerX + half, centerY + half, centerZ + half
        )
        val targetId = entity.clientChargingEntityId
        val target: Entity = world.getEntityById(targetId) ?: run {
            cacheByPos.remove(key)
            return
        }
        if (!target.isAlive || target.hasVehicle() || target.hasPassengers()) {
            cacheByPos.remove(key)
            return
        }
        if (!sourceBox.intersects(target.boundingBox)) {
            cacheByPos.remove(key)
            return
        }

        val rawProgress = if (entity.clientChargeMax > 0) {
            (entity.clientChargeProgress + tickDelta) / entity.clientChargeMax.toFloat()
        } else {
            0f
        }.coerceIn(0f, 1f)
        val cache = cacheByPos.getOrPut(key) { RenderCache() }
        val progress = if (rawProgress < 0.05f) rawProgress else max(rawProgress, cache.lastProgress)
        cache.lastProgress = progress

        val time = world.time + tickDelta
        val pulse = (sin(time * 0.25f) * 0.5f + 0.5f)
        val radiusOuter = (1.1f - 0.85f * progress).coerceAtLeast(0.16f)
        val radiusInner = (radiusOuter - 0.10f).coerceAtLeast(0.08f)
        val ringY = 0.02f + 0.08f * pulse
        val alpha = (90 + 120 * (1f - progress)).toInt().coerceIn(60, 210)

        if (cache.lastTargetId != target.id) {
            cache.lastTargetId = target.id
            cache.sx = target.x
            cache.sy = target.y
            cache.sz = target.z
        } else {
            val lerp = 0.35
            cache.sx += (target.x - cache.sx) * lerp
            cache.sy += (target.y - cache.sy) * lerp
            cache.sz += (target.z - cache.sz) * lerp
        }

        matrices.push()
        matrices.translate(
            cache.sx - entity.pos.x,
            cache.sy - entity.pos.y,
            cache.sz - entity.pos.z
        )

        val vc = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(WHITE_TEXTURE))
        val fullLight = LightmapTextureManager.MAX_LIGHT_COORDINATE

        drawRingBand(matrices, vc, fullLight, overlay, radiusOuter, radiusInner, ringY, 80, 235, 255, alpha)
        drawRingBand(matrices, vc, fullLight, overlay, radiusOuter * 0.68f, radiusInner * 0.58f, ringY + 0.03f, 120, 250, 255, alpha - 20)

        if (progress > 0.7f) {
            val beamHeight = (target.height + 0.6f).toFloat()
            drawBeam(matrices, vc, fullLight, overlay, beamHeight, 70, 220, 255, (alpha + 20).coerceAtMost(230))
        }

        matrices.pop()
    }

    private fun drawRingBand(
        matrices: MatrixStack,
        vc: VertexConsumer,
        light: Int,
        overlay: Int,
        outerR: Float,
        innerR: Float,
        y: Float,
        red: Int,
        green: Int,
        blue: Int,
        alpha: Int
    ) {
        val entry = matrices.peek()
        val pos = entry.positionMatrix
        val normal = entry.normalMatrix
        val step = (2.0 * PI / SEGMENTS).toFloat()

        for (i in 0 until SEGMENTS) {
            val a0 = i * step
            val a1 = (i + 1) * step
            val ox0 = cos(a0) * outerR
            val oz0 = sin(a0) * outerR
            val ox1 = cos(a1) * outerR
            val oz1 = sin(a1) * outerR
            val ix0 = cos(a0) * innerR
            val iz0 = sin(a0) * innerR
            val ix1 = cos(a1) * innerR
            val iz1 = sin(a1) * innerR

            quadColor(
                vc, pos, normal, light, overlay,
                ox0.toFloat(), y, oz0.toFloat(),
                ox1.toFloat(), y, oz1.toFloat(),
                ix1.toFloat(), y, iz1.toFloat(),
                ix0.toFloat(), y, iz0.toFloat(),
                red, green, blue, alpha,
                0f, 1f, 0f
            )
        }
    }

    private fun drawBeam(
        matrices: MatrixStack,
        vc: VertexConsumer,
        light: Int,
        overlay: Int,
        height: Float,
        red: Int,
        green: Int,
        blue: Int,
        alpha: Int
    ) {
        val half = 0.06f
        val y0 = 0.03f
        val y1 = y0 + height
        val entry = matrices.peek()
        val pos = entry.positionMatrix
        val normal = entry.normalMatrix

        quadColor(vc, pos, normal, light, overlay, -half, y0, -half, -half, y1, -half, half, y1, -half, half, y0, -half, red, green, blue, alpha, 0f, 0f, -1f)
        quadColor(vc, pos, normal, light, overlay, half, y0, half, half, y1, half, -half, y1, half, -half, y0, half, red, green, blue, alpha, 0f, 0f, 1f)
        quadColor(vc, pos, normal, light, overlay, -half, y0, half, -half, y1, half, -half, y1, -half, -half, y0, -half, red, green, blue, alpha, -1f, 0f, 0f)
        quadColor(vc, pos, normal, light, overlay, half, y0, -half, half, y1, -half, half, y1, half, half, y0, half, red, green, blue, alpha, 1f, 0f, 0f)
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

    override fun rendersOutsideBoundingBox(blockEntity: TeleporterBlockEntity): Boolean = true
}
