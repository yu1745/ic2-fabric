package ic2_120.client.renderers

import ic2_120.content.block.machines.LeashKineticGeneratorBlockEntity
import net.minecraft.world.level.block.Blocks
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.model.LeashKnotModel
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.entity.Entity
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.BlockPos
import com.mojang.math.Axis

class LeashKineticGeneratorBlockEntityRenderer(
    @Suppress("UNUSED_PARAMETER") context: BlockEntityRendererProvider.Context
) : BlockEntityRenderer<LeashKineticGeneratorBlockEntity> {

    companion object {
        private val LEASH_KNOT_TEXTURE = ResourceLocation.fromNamespaceAndPath("textures/entity/lead_knot.png")
    }

    private val leashKnotModel = LeashKnotModel<Entity>(
        LeashKnotModel.getTexturedModelData().createModel()
    )

    override fun render(
        entity: LeashKineticGeneratorBlockEntity,
        tickDelta: Float,
        matrices: PoseStack,
        vertexConsumers: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        if (!entity.sync.hasAnimal) return

        val world = entity.level() as? ClientLevel ?: return
        val renderPos = entity.pos.above()
        val renderLight = net.minecraft.client.renderer.LevelRenderer.getLightmapCoordinates(world, renderPos)

        val angle = entity.sync.animalAngle

        matrices.pushPose()
        matrices.translate(0.5, 1.0, 0.5)
        matrices.mulPose(Axis.YP.rotationDegrees(-angle))

        // Render single oak fence post (centered, no connections in default state)
        matrices.pushPose()
        matrices.translate(-0.5, -0.5, -0.5)
        Minecraft.getInstance().blockRenderManager.renderBlockAsEntity(
            Blocks.OAK_FENCE.defaultBlockState(),
            matrices,
            vertexConsumers,
            renderLight,
            OverlayTexture.NO_OVERLAY
        )
        matrices.popPose()

        // Render leash knot on top of the fence post
        matrices.pushPose()
        matrices.translate(0.0, 0.4, 0.0)
        matrices.scale(-1.0f, -1.0f, 1.0f)
        val vertexConsumer = vertexConsumers.getBuffer(leashKnotModel.getLayer(LEASH_KNOT_TEXTURE))
        leashKnotModel.render(matrices, vertexConsumer, renderLight, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f)
        matrices.popPose()

        matrices.popPose()
    }
}
