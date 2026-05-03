package ic2_120.client.renderers

import ic2_120.content.block.machines.LeashKineticGeneratorBlockEntity
import net.minecraft.block.Blocks
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory
import net.minecraft.client.render.entity.model.LeashKnotEntityModel
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.Entity
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RotationAxis

class LeashKineticGeneratorBlockEntityRenderer(
    @Suppress("UNUSED_PARAMETER") context: BlockEntityRendererFactory.Context
) : BlockEntityRenderer<LeashKineticGeneratorBlockEntity> {

    companion object {
        private val LEASH_KNOT_TEXTURE = Identifier.ofVanilla("textures/entity/lead_knot.png")
    }

    private val leashKnotModel = LeashKnotEntityModel<Entity>(
        LeashKnotEntityModel.getTexturedModelData().createModel()
    )

    override fun render(
        entity: LeashKineticGeneratorBlockEntity,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        if (!entity.sync.hasAnimal) return

        val world = entity.world as? ClientWorld ?: return
        val renderPos = entity.pos.up()
        val renderLight = net.minecraft.client.render.WorldRenderer.getLightmapCoordinates(world, renderPos)

        val angle = entity.sync.animalAngle

        matrices.push()
        matrices.translate(0.5, 1.0, 0.5)
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-angle))

        // Render single oak fence post (centered, no connections in default state)
        matrices.push()
        matrices.translate(-0.5, -0.5, -0.5)
        MinecraftClient.getInstance().blockRenderManager.renderBlockAsEntity(
            Blocks.OAK_FENCE.defaultState,
            matrices,
            vertexConsumers,
            renderLight,
            OverlayTexture.DEFAULT_UV
        )
        matrices.pop()

        // Render leash knot on top of the fence post
        matrices.push()
        matrices.translate(0.0, 0.4, 0.0)
        matrices.scale(-1.0f, -1.0f, 1.0f)
        val vertexConsumer = vertexConsumers.getBuffer(leashKnotModel.getLayer(LEASH_KNOT_TEXTURE))
        leashKnotModel.render(matrices, vertexConsumer, renderLight, OverlayTexture.DEFAULT_UV)
        matrices.pop()

        matrices.pop()
    }
}
