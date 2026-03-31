package ic2_120.client.renderers

import ic2_120.content.block.machines.WindKineticGeneratorBlockEntity
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.LightmapTextureManager
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.state.property.Properties
import net.minecraft.util.Identifier
import net.minecraft.util.math.Direction
import net.minecraft.util.math.RotationAxis
import org.joml.Matrix3f
import org.joml.Matrix4f

class WindKineticGeneratorBlockEntityRenderer(
    context: BlockEntityRendererFactory.Context
) : BlockEntityRenderer<WindKineticGeneratorBlockEntity> {
    companion object {
        private val WOOD_ROTOR_TEXTURE = Identifier("ic2", "textures/item/rotor/wood_rotor_model.png")
        private val IRON_ROTOR_TEXTURE = Identifier("ic2", "textures/item/rotor/iron_rotor_model.png")
        private val STEEL_ROTOR_TEXTURE = Identifier("ic2", "textures/item/rotor/steel_rotor_model.png")
        private val CARBON_ROTOR_TEXTURE = Identifier("ic2", "textures/item/rotor/carbon_rotor_model.png")

        private const val PIXEL = 1.0f / 16.0f
        // private const val ROTOR_RADIUS = 6.0f                  // 直径 12 blocks
        private const val BLADE_HALF_WIDTH = 6.0f * PIXEL      // 叶片宽 
        private const val BLADE_THICKNESS_HALF = 0.5f * PIXEL  // 叶片厚 1px
        private const val HUB_HALF_SIZE = 4.0f * PIXEL         // 中心轴帽 
        private const val BLADE_INNER_OFFSET = 1.0f * PIXEL    // 叶片从中心外 1px 开始
        private const val FRONT_OFFSET_FROM_CENTER = 1.0        // 相对方块中心前移 0.5 格（方块前表面在 0.5）
        private const val BLADE_PITCH_DEGREES = 22.0f          // 叶片螺距角（攻角），符合物理的风机桨叶倾角
        private const val SHAFT_HALF_WIDTH = 1.0f * PIXEL      // 轴宽 2px（圆柱截面近似）
        private const val SHAFT_LENGTH_INTO_BLOCK = 0.51f       // 轴深入机器内部的长度（格）
    }

    init {
        // Context 目前未使用，保留构造签名以符合工厂注册接口。
        @Suppress("UNUSED_VARIABLE")
        val ignored = context
    }

    private fun getRotorRadius(stack: ItemStack): Float {
        return when (Registries.ITEM.getId(stack.item).path) {
            "wooden_rotor" -> 2.0f
            "iron_rotor" -> 3.0f
            "steel_rotor" -> 4.0f
            "carbon_rotor" -> 5.0f
            else -> 0.0f
        }
    }

    override fun render(
        entity: WindKineticGeneratorBlockEntity,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        val rotor = entity.getRotorStack()
        if (rotor.isEmpty) return
        val rotorTexture = textureForRotor(rotor) ?: return

        val world = entity.world ?: return
        val state = entity.cachedState
        val facing = state.getOrEmpty(Properties.HORIZONTAL_FACING).orElse(Direction.NORTH)

        val angle = if (entity.isStuck) {
            entity.stuckAngle
        } else {
            ((world.time + tickDelta) * 1.8f) % 360.0f
        }

        matrices.push()
        matrices.translate(0.5, 0.5, 0.5)
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yawFromFacing(facing)))
        // 转子中心位于方块正面外 0.5 格。
        matrices.translate(0.0, 0.0, -FRONT_OFFSET_FROM_CENTER)
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle))

        val vc = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(rotorTexture))
        val rotorLight = LightmapTextureManager.MAX_LIGHT_COORDINATE

        // 四个扇叶：每片都是薄立方体，沿旋转轴每 90 度排布；绕 X 轴施加螺距角使叶片具有攻角。
        repeat(4) { i ->
            matrices.push()
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((i * 90).toFloat()))
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(BLADE_PITCH_DEGREES))
            drawCuboid(
                matrices,
                vc,
                rotorLight,
                overlay,
                minX = -BLADE_HALF_WIDTH, maxX = BLADE_HALF_WIDTH,
                minY = BLADE_INNER_OFFSET, maxY = getRotorRadius(rotor),
                minZ = -BLADE_THICKNESS_HALF, maxZ = BLADE_THICKNESS_HALF
            )
            matrices.pop()
        }

        // 旋转轴：从螺旋桨中心延伸入机器内部
        drawCuboid(
            matrices,
            vc,
            rotorLight,
            overlay,
            minX = -SHAFT_HALF_WIDTH, maxX = SHAFT_HALF_WIDTH,
            minY = -SHAFT_HALF_WIDTH, maxY = SHAFT_HALF_WIDTH,
            minZ = 0f, maxZ = SHAFT_LENGTH_INTO_BLOCK
        )

        // 中心轴帽
        drawCuboid(
            matrices,
            vc,
            rotorLight,
            overlay,
            minX = -HUB_HALF_SIZE, maxX = HUB_HALF_SIZE,
            minY = -HUB_HALF_SIZE, maxY = HUB_HALF_SIZE,
            minZ = -BLADE_THICKNESS_HALF, maxZ = BLADE_THICKNESS_HALF
        )

        matrices.pop()
    }

    private fun textureForRotor(stack: ItemStack): Identifier? =
        when (Registries.ITEM.getId(stack.item).path) {
            "wooden_rotor" -> WOOD_ROTOR_TEXTURE
            "iron_rotor" -> IRON_ROTOR_TEXTURE
            "steel_rotor" -> STEEL_ROTOR_TEXTURE
            "carbon_rotor" -> CARBON_ROTOR_TEXTURE
            else -> null
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
        val normal = entry.normalMatrix

        // Front (+Z)
        quad(vc, pos, normal, light, overlay,
            minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ,
            0f, 0f, 1f
        )
        // Back (-Z)
        quad(vc, pos, normal, light, overlay,
            maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ,
            0f, 0f, -1f
        )
        // Left (-X)
        quad(vc, pos, normal, light, overlay,
            minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ,
            -1f, 0f, 0f
        )
        // Right (+X)
        quad(vc, pos, normal, light, overlay,
            maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ,
            1f, 0f, 0f
        )
        // Top (+Y)
        quad(vc, pos, normal, light, overlay,
            minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ,
            0f, 1f, 0f
        )
        // Bottom (-Y)
        quad(vc, pos, normal, light, overlay,
            minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ,
            0f, -1f, 0f
        )
    }

    private fun quad(
        vc: VertexConsumer,
        pos: Matrix4f,
        normal: Matrix3f,
        light: Int,
        overlay: Int,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        x4: Float, y4: Float, z4: Float,
        nx: Float, ny: Float, nz: Float
    ) {
        vertex(vc, pos, normal, x1, y1, z1, 0f, 0f, light, overlay, nx, ny, nz)
        vertex(vc, pos, normal, x2, y2, z2, 1f, 0f, light, overlay, nx, ny, nz)
        vertex(vc, pos, normal, x3, y3, z3, 1f, 1f, light, overlay, nx, ny, nz)
        vertex(vc, pos, normal, x4, y4, z4, 0f, 1f, light, overlay, nx, ny, nz)
    }

    private fun vertex(
        vc: VertexConsumer,
        pos: Matrix4f,
        normal: Matrix3f,
        x: Float,
        y: Float,
        z: Float,
        u: Float,
        v: Float,
        light: Int,
        overlay: Int,
        nx: Float,
        ny: Float,
        nz: Float
    ) {
        vc.vertex(pos, x, y, z)
            .color(255, 255, 255, 255)
            .texture(u, v)
            .overlay(overlay.takeUnless { it == 0 } ?: OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(normal, nx, ny, nz)
            .next()
    }

    override fun rendersOutsideBoundingBox(blockEntity: WindKineticGeneratorBlockEntity): Boolean = true
}
