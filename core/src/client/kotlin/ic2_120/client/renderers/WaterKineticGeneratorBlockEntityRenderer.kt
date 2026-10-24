package ic2_120.client.renderers

import ic2_120.compat.cachedState
import ic2_120.compat.world

import ic2_120.content.block.machines.WaterKineticGeneratorBlockEntity
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.RenderType
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.world.item.ItemStack
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.Direction
import com.mojang.math.Axis
import org.joml.Matrix3f
import org.joml.Matrix4f

class WaterKineticGeneratorBlockEntityRenderer(
    context: BlockEntityRendererProvider.Context
) : BlockEntityRenderer<WaterKineticGeneratorBlockEntity> {
    companion object {
        private val WOOD_ROTOR_TEXTURE = ResourceLocation.fromNamespaceAndPath("ic2_120", "textures/item/rotor/wood_rotor_model.png")
        private val BRONZE_ROTOR_TEXTURE = ResourceLocation.fromNamespaceAndPath("ic2_120", "textures/item/rotor/bronze_rotor_model.png")
        private val IRON_ROTOR_TEXTURE = ResourceLocation.fromNamespaceAndPath("ic2_120", "textures/item/rotor/iron_rotor_model.png")
        private val STEEL_ROTOR_TEXTURE = ResourceLocation.fromNamespaceAndPath("ic2_120", "textures/item/rotor/steel_rotor_model.png")
        private val CARBON_ROTOR_TEXTURE = ResourceLocation.fromNamespaceAndPath("ic2_120", "textures/item/rotor/carbon_rotor_model.png")

        private const val PIXEL = 1.0f / 16.0f
        private const val BLADE_HALF_WIDTH = 6.0f * PIXEL
        private const val BLADE_THICKNESS_HALF = 0.5f * PIXEL
        private const val HUB_HALF_SIZE = 4.0f * PIXEL
        private const val BLADE_INNER_OFFSET = 1.0f * PIXEL
        private const val FRONT_OFFSET_FROM_CENTER = 1.0
        private const val BLADE_PITCH_DEGREES = 22.0f
        private const val SHAFT_HALF_WIDTH = 1.0f * PIXEL
        private const val SHAFT_LENGTH_INTO_BLOCK = 0.51f
    }

    init {
        @Suppress("UNUSED_VARIABLE")
        val ignored = context
    }

    private fun getRotorRadius(stack: ItemStack): Float {
        return when (BuiltInRegistries.ITEM.getKey(stack.item).path) {
            "wooden_rotor" -> 1.0f
            "bronze_rotor" -> 1.5f
            "iron_rotor" -> 1.5f
            "steel_rotor" -> 2.0f
            "carbon_rotor" -> 2.5f
            else -> 0.0f
        }
    }

    override fun render(
        entity: WaterKineticGeneratorBlockEntity,
        tickDelta: Float,
        matrices: PoseStack,
        vertexConsumers: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        val rotor = entity.getRotorStack()
        if (rotor.isEmpty) return
        val rotorTexture = textureForRotor(rotor) ?: return

        val world = entity.level ?: return
        val state = entity.cachedState
        val facing = state.getOptionalValue(BlockStateProperties.HORIZONTAL_FACING).orElse(Direction.NORTH)

        val angle = if (entity.isStuck) {
            entity.stuckAngle
        } else {
            ((world.gameTime + tickDelta) * 1.8f) % 360.0f
        }

        matrices.pushPose()
        matrices.translate(0.5, 0.5, 0.5)
        matrices.mulPose(Axis.YP.rotationDegrees(yawFromFacing(facing)))
        matrices.translate(0.0, 0.0, -FRONT_OFFSET_FROM_CENTER)
        matrices.mulPose(Axis.ZP.rotationDegrees(angle))

        val vc = vertexConsumers.getBuffer(RenderType.entityCutoutNoCull(rotorTexture))

        repeat(4) { i ->
            matrices.pushPose()
            matrices.mulPose(Axis.ZP.rotationDegrees((i * 90).toFloat()))
            matrices.mulPose(Axis.YP.rotationDegrees(BLADE_PITCH_DEGREES))
            drawCuboid(
                matrices,
                vc,
                light,
                overlay,
                minX = -BLADE_HALF_WIDTH, maxX = BLADE_HALF_WIDTH,
                minY = BLADE_INNER_OFFSET, maxY = getRotorRadius(rotor),
                minZ = -BLADE_THICKNESS_HALF, maxZ = BLADE_THICKNESS_HALF
            )
            matrices.popPose()
        }

        drawCuboid(
            matrices,
            vc,
            light,
            overlay,
            minX = -SHAFT_HALF_WIDTH, maxX = SHAFT_HALF_WIDTH,
            minY = -SHAFT_HALF_WIDTH, maxY = SHAFT_HALF_WIDTH,
            minZ = 0f, maxZ = SHAFT_LENGTH_INTO_BLOCK
        )

        drawCuboid(
            matrices,
            vc,
            light,
            overlay,
            minX = -HUB_HALF_SIZE, maxX = HUB_HALF_SIZE,
            minY = -HUB_HALF_SIZE, maxY = HUB_HALF_SIZE,
            minZ = -BLADE_THICKNESS_HALF, maxZ = BLADE_THICKNESS_HALF
        )

        matrices.popPose()
    }

    private fun textureForRotor(stack: ItemStack): ResourceLocation? =
        when (BuiltInRegistries.ITEM.getKey(stack.item).path) {
            "wooden_rotor" -> WOOD_ROTOR_TEXTURE
            "bronze_rotor" -> BRONZE_ROTOR_TEXTURE
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
        val normal = entry.normal()

        quad(vc, entry, light, overlay,
            minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ,
            0f, 0f, 1f
        )
        quad(vc, entry, light, overlay,
            maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ,
            0f, 0f, -1f
        )
        quad(vc, entry, light, overlay,
            minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ,
            -1f, 0f, 0f
        )
        quad(vc, entry, light, overlay,
            maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ,
            1f, 0f, 0f
        )
        quad(vc, entry, light, overlay,
            minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ,
            0f, 1f, 0f
        )
        quad(vc, entry, light, overlay,
            minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ,
            0f, -1f, 0f
        )
    }

    private fun quad(
        vc: VertexConsumer,
        entry: PoseStack.Pose,
        light: Int,
        overlay: Int,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        x4: Float, y4: Float, z4: Float,
        nx: Float, ny: Float, nz: Float
    ) {
        vertex(vc, entry, x1, y1, z1, 0f, 0f, light, overlay, nx, ny, nz)
        vertex(vc, entry, x2, y2, z2, 1f, 0f, light, overlay, nx, ny, nz)
        vertex(vc, entry, x3, y3, z3, 1f, 1f, light, overlay, nx, ny, nz)
        vertex(vc, entry, x4, y4, z4, 0f, 1f, light, overlay, nx, ny, nz)
    }

    private fun vertex(
        vc: VertexConsumer,
        entry: PoseStack.Pose,
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
        vc.addVertex(entry, x, y, z)
            .setColor(255, 255, 255, 255)
            .setUv(u, v)
            .setOverlay(overlay.takeUnless { it == 0 } ?: OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(entry, nx, ny, nz)
            
    }

    override fun shouldRenderOffScreen(blockEntity: WaterKineticGeneratorBlockEntity): Boolean = true

    override fun getViewDistance(): Int = 256
}
