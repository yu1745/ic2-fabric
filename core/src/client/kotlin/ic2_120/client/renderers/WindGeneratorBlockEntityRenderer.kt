package ic2_120.client.renderers

import ic2_120.content.block.WindGeneratorBlock
import ic2_120.content.block.machines.WindGeneratorBlockEntity
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.state.property.Properties
import net.minecraft.util.Identifier
import net.minecraft.util.math.Direction
import net.minecraft.util.math.RotationAxis
import org.joml.Matrix4f

class WindGeneratorBlockEntityRenderer(
    context: BlockEntityRendererFactory.Context
) : BlockEntityRenderer<WindGeneratorBlockEntity> {

    companion object {
        private val WHITE_TEXTURE = Identifier.ofVanilla("textures/misc/white.png")
        private const val BASE_SPEED_DEG_PER_TICK = 6.0f
        private const val SPEED_PER_EU = 3.0f
    }

    init {
        @Suppress("UNUSED_VARIABLE")
        val ignored = context
    }

    override fun render(
        entity: WindGeneratorBlockEntity,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        val world = entity.world ?: return
        val state = entity.cachedState
        val facing = state.getOrEmpty(Properties.HORIZONTAL_FACING).orElse(Direction.NORTH)

        // 使用方块状态 ACTIVE 判断是否发电（sync 仅在有 GUI 时同步，方块状态始终同步）
        val generating = state.get(WindGeneratorBlock.ACTIVE)
        val outputEuPerTick = entity.sync.getSyncedInsertedAmount().coerceAtLeast(0L).toFloat()
        val speed = if (generating) BASE_SPEED_DEG_PER_TICK + outputEuPerTick * SPEED_PER_EU else 0.0f
        val angle = if (speed > 0.0f) ((world.time + tickDelta) * speed) % 360.0f else 0.0f

        matrices.push()
        matrices.translate(0.5, 0.5, 0.5)
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yawFromFacing(facing)))
        matrices.translate(0.0, 0.0, -0.505)
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle))

        val vc = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(WHITE_TEXTURE))

        repeat(4) { i ->
            matrices.push()
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((i * 90).toFloat()))
            drawCuboid(
                matrices,
                vc,
                light,
                overlay,
                minX = -0.045f, maxX = 0.045f,
                minY = 0.06f, maxY = 0.38f,
                minZ = -0.01f, maxZ = 0.01f
            )
            matrices.pop()
        }

        drawCuboid(
            matrices,
            vc,
            light,
            overlay,
            minX = -0.05f, maxX = 0.05f,
            minY = -0.05f, maxY = 0.05f,
            minZ = -0.02f, maxZ = 0.02f
        )

        matrices.pop()
    }

    private fun yawFromFacing(facing: Direction): Float =
        when (facing) {
            Direction.NORTH -> 0.0f
            Direction.SOUTH -> 180.0f
            Direction.WEST -> 90.0f
            Direction.EAST -> -90.0f
            else -> 0.0f
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
        maxZ: Float
    ) {
        val entry = matrices.peek()
        val pos = entry.positionMatrix

        quad(vc, pos, entry, light, overlay,
            minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ,
            0f, 0f, 1f
        )
        quad(vc, pos, entry, light, overlay,
            maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ,
            0f, 0f, -1f
        )
        quad(vc, pos, entry, light, overlay,
            minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ,
            -1f, 0f, 0f
        )
        quad(vc, pos, entry, light, overlay,
            maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ,
            1f, 0f, 0f
        )
        quad(vc, pos, entry, light, overlay,
            minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ,
            0f, 1f, 0f
        )
        quad(vc, pos, entry, light, overlay,
            minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ,
            0f, -1f, 0f
        )
    }

    private fun quad(
        vc: VertexConsumer,
        pos: Matrix4f,
        entry: MatrixStack.Entry,
        light: Int,
        overlay: Int,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        x4: Float, y4: Float, z4: Float,
        nx: Float, ny: Float, nz: Float
    ) {
        vertex(vc, pos, entry, x1, y1, z1, light, overlay, nx, ny, nz)
        vertex(vc, pos, entry, x2, y2, z2, light, overlay, nx, ny, nz)
        vertex(vc, pos, entry, x3, y3, z3, light, overlay, nx, ny, nz)
        vertex(vc, pos, entry, x4, y4, z4, light, overlay, nx, ny, nz)
    }

    private fun vertex(
        vc: VertexConsumer,
        pos: Matrix4f,
        entry: MatrixStack.Entry,
        x: Float,
        y: Float,
        z: Float,
        light: Int,
        overlay: Int,
        nx: Float,
        ny: Float,
        nz: Float
    ) {
        vc.vertex(pos, x, y, z)
            .color(0, 0, 0, 255)
            .texture(0f, 0f)
            .overlay(overlay.takeUnless { it == 0 } ?: OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry, nx, ny, nz)
    }

    override fun rendersOutsideBoundingBox(blockEntity: WindGeneratorBlockEntity): Boolean = true
}
