package ic2_120.client.renderers

import ic2_120.compat.cachedState
import ic2_120.compat.world

import ic2_120.content.block.machines.ManualKineticGeneratorBlockEntity
import ic2_120.content.item.CrankMaterial
import ic2_120.content.item.CrankHandleItem
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.rendertype.RenderTypes as RenderType
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.core.Direction
import com.mojang.math.Axis
import org.joml.Matrix3f
import org.joml.Matrix4f

class ManualKineticGeneratorBlockEntityRenderer(
    context: BlockEntityRendererProvider.Context
) : LegacyBlockEntityRenderer26<ManualKineticGeneratorBlockEntity>() {

    companion object {
        private val WOOD_TEXTURE = Identifier.fromNamespaceAndPath("ic2_120", "textures/item/rotor/wood_rotor_model.png")
        private val IRON_TEXTURE = Identifier.fromNamespaceAndPath("ic2_120", "textures/item/rotor/iron_rotor_model.png")
        private val STEEL_TEXTURE = Identifier.fromNamespaceAndPath("ic2_120", "textures/item/rotor/steel_rotor_model.png")
        private val CARBON_TEXTURE = Identifier.fromNamespaceAndPath("ic2_120", "textures/item/rotor/carbon_rotor_model.png")
        
        private const val PIXEL = 1.0f / 16.0f
        private const val SHAFT_HALF = 1.0f * PIXEL
        private const val SHAFT_LENGTH = 2.0f * PIXEL
        private const val ARM_HALF = 1.0f * PIXEL
        private const val ARM_LENGTH = 5.0f * PIXEL

        private const val HANDLE_HALF = 1.0f * PIXEL
        private const val HANDLE_LENGTH = 6.0f * PIXEL
    }

    init {
        @Suppress("UNUSED_VARIABLE")
        val ignored = context
    }

    override fun renderLegacy(
        entity: ManualKineticGeneratorBlockEntity,
        tickDelta: Float,
        matrices: PoseStack,
        vertexConsumers: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        val crankStack = entity.getCrankStack()
        if (crankStack.isEmpty) return

        val world = entity.level ?: return
        val state = entity.cachedState
        val facing = state.getOptionalValue(BlockStateProperties.FACING).orElse(Direction.NORTH)
        
        val material = (crankStack.item as? CrankHandleItem)?.material ?: return
        val texture = getTextureForMaterial(material)
        val vc = vertexConsumers.getBuffer(RenderType.entityCutout(texture))

        val angle = entity.clientTurnAngle + if (entity.sync.isTurning) tickDelta * 15f else 0f

        matrices.pushPose()
        matrices.translate(0.5, 0.5, 0.5)
        
        rotateForFacing(matrices, facing)
        
        matrices.translate(0.0, 0.5, 0.0)
        matrices.mulPose(Axis.YP.rotationDegrees(angle))

        drawShaft(matrices, vc, light, overlay)
        drawArm(matrices, vc, light, overlay)
        drawHandle(matrices, vc, light, overlay)

        matrices.popPose()
    }

    private fun getTextureForMaterial(material: CrankMaterial): Identifier =
        when (material) {
            CrankMaterial.WOOD -> WOOD_TEXTURE
            CrankMaterial.IRON -> IRON_TEXTURE
            CrankMaterial.STEEL -> STEEL_TEXTURE
            CrankMaterial.CARBON -> CARBON_TEXTURE
        }

    private fun rotateForFacing(matrices: PoseStack, facing: Direction) {
        when (facing) {
            Direction.NORTH -> { }
            Direction.SOUTH -> matrices.mulPose(Axis.YP.rotationDegrees(180f))
            Direction.WEST -> matrices.mulPose(Axis.YP.rotationDegrees(90f))
            Direction.EAST -> matrices.mulPose(Axis.YP.rotationDegrees(-90f))
            else -> { }
        }
    }

    private fun drawShaft(
        matrices: PoseStack,
        vc: VertexConsumer,
        light: Int,
        overlay: Int
    ) {
        drawCuboid(
            matrices, vc, light, overlay,
            -SHAFT_HALF, -SHAFT_LENGTH, -SHAFT_HALF,
            SHAFT_HALF, ARM_HALF, SHAFT_HALF
        )
    }

    private fun drawArm(
        matrices: PoseStack,
        vc: VertexConsumer,
        light: Int,
        overlay: Int
    ) {
        val totalLength = ARM_LENGTH + SHAFT_HALF + HANDLE_HALF
        matrices.pushPose()
        matrices.translate(0f, 0f, -ARM_LENGTH * 0.5f + (HANDLE_HALF - SHAFT_HALF) * 0.5f)
        drawCuboid(
            matrices, vc, light, overlay,
            -ARM_HALF, -ARM_HALF, -totalLength * 0.5f,
            ARM_HALF, ARM_HALF, totalLength * 0.5f
        )
        matrices.popPose()
    }

    private fun drawHandle(
        matrices: PoseStack,
        vc: VertexConsumer,
        light: Int,
        overlay: Int
    ) {
        matrices.pushPose()
        matrices.translate(0f, 0f, -ARM_LENGTH)
        drawCuboid(
            matrices, vc, light, overlay,
            -HANDLE_HALF, -ARM_HALF, -HANDLE_HALF,
            HANDLE_HALF, HANDLE_LENGTH, HANDLE_HALF
        )
        matrices.popPose()
    }

    private fun drawCuboid(
        matrices: PoseStack,
        vc: VertexConsumer,
        light: Int,
        overlay: Int,
        minX: Float,
        minY: Float,
        minZ: Float,
        maxX: Float,
        maxY: Float,
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
}