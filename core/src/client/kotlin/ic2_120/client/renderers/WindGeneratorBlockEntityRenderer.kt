package ic2_120.client.renderers

import ic2_120.compat.cachedState
import ic2_120.compat.world

import ic2_120.content.block.WindGeneratorBlock
import ic2_120.content.block.machines.WindGeneratorBlockEntity
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.rendertype.RenderTypes as RenderType
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.resources.Identifier
import net.minecraft.core.Direction
import com.mojang.math.Axis
import org.joml.Matrix4f

class WindGeneratorBlockEntityRenderer(
    context: BlockEntityRendererProvider.Context
) : LegacyBlockEntityRenderer26<WindGeneratorBlockEntity>() {

    companion object {
        private val WHITE_TEXTURE = Identifier.withDefaultNamespace("textures/misc/white.png")
        private const val BASE_SPEED_DEG_PER_TICK = 6.0f
        private const val SPEED_PER_EU = 3.0f
    }

    init {
        @Suppress("UNUSED_VARIABLE")
        val ignored = context
    }

    override fun renderLegacy(
        entity: WindGeneratorBlockEntity,
        tickDelta: Float,
        matrices: PoseStack,
        vertexConsumers: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        val world = entity.level ?: return
        val state = entity.cachedState
        val facing = state.getOptionalValue(BlockStateProperties.HORIZONTAL_FACING).orElse(Direction.NORTH)

        // 使用方块状态 ACTIVE 判断是否发电（sync 仅在有 GUI 时同步，方块状态始终同步）
        val generating = state.getValue(WindGeneratorBlock.ACTIVE)
        val outputEuPerTick = entity.sync.getSyncedInsertedAmount().coerceAtLeast(0L).toFloat()
        val speed = if (generating) BASE_SPEED_DEG_PER_TICK + outputEuPerTick * SPEED_PER_EU else 0.0f
        val angle = if (speed > 0.0f) ((world.gameTime + tickDelta) * speed) % 360.0f else 0.0f

        matrices.pushPose()
        matrices.translate(0.5, 0.5, 0.5)
        matrices.mulPose(Axis.YP.rotationDegrees(yawFromFacing(facing)))
        matrices.translate(0.0, 0.0, -0.505)
        matrices.mulPose(Axis.ZP.rotationDegrees(angle))

        val vc = vertexConsumers.getBuffer(RenderType.entityCutout(WHITE_TEXTURE))

        repeat(4) { i ->
            matrices.pushPose()
            matrices.mulPose(Axis.ZP.rotationDegrees((i * 90).toFloat()))
            drawCuboid(
                matrices,
                vc,
                light,
                overlay,
                minX = -0.045f, maxX = 0.045f,
                minY = 0.06f, maxY = 0.38f,
                minZ = -0.01f, maxZ = 0.01f
            )
            matrices.popPose()
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

        matrices.popPose()
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
        matrices: PoseStack,
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
        val entry = matrices.last()
        val pos = entry.pose()

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
        entry: PoseStack.Pose,
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
        entry: PoseStack.Pose,
        x: Float,
        y: Float,
        z: Float,
        light: Int,
        overlay: Int,
        nx: Float,
        ny: Float,
        nz: Float
    ) {
        vc.addVertex(pos, x, y, z)
            .setColor(0, 0, 0, 255)
            .setUv(0f, 0f)
            .setOverlay(overlay.takeUnless { it == 0 } ?: OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(entry, nx, ny, nz)
    }

    override fun shouldRenderOffScreen(): Boolean = true

    override fun getViewDistance(): Int = 256
}
