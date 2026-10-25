package ic2_120.client.renderers

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import ic2_120.content.block.machines.LeashKineticGeneratorBlockEntity
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.model.`object`.leash.LeashKnotModel
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.block.BlockModelRenderState
import net.minecraft.client.renderer.block.model.BlockDisplayContext
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.feature.ModelFeatureRenderer
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.Identifier
import net.minecraft.util.LightCoordsUtil
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3

/** Renders the animal hitch post through 26.2's block and entity submit APIs. */
class LeashKineticGeneratorBlockEntityRenderer(context: BlockEntityRendererProvider.Context)
    : BlockEntityRenderer<LeashKineticGeneratorBlockEntity, LeashKineticGeneratorBlockEntityRenderer.State> {
    companion object {
        private val LEASH_KNOT_TEXTURE = Identifier.withDefaultNamespace("textures/entity/lead_knot/lead_knot.png")
    }
    class State : BlockEntityRenderState() {
        var hasAnimal = false
        var angle = 0f
        var light = 0
        val fence = BlockModelRenderState()
    }
    private val knot = LeashKnotModel(context.bakeLayer(ModelLayers.LEASH_KNOT))
    private val blockModels = context.blockModelResolver()
    private val knotState = EntityRenderState()

    override fun createRenderState() = State()

    override fun extractRenderState(
        entity: LeashKineticGeneratorBlockEntity, state: State, partialTicks: Float,
        cameraPosition: Vec3, breakProgress: ModelFeatureRenderer.CrumblingOverlay?
    ) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress)
        state.hasAnimal = entity.sync.hasAnimal
        if (!state.hasAnimal) return
        state.angle = entity.sync.animalAngle
        val world = entity.level
        state.light = if (world != null) LightCoordsUtil.getLightCoords(world, entity.blockPos.above()) else state.lightCoords
        blockModels.update(state.fence, Blocks.OAK_FENCE.defaultBlockState(), BlockDisplayContext.create())
    }

    override fun submit(state: State, poseStack: PoseStack, nodes: SubmitNodeCollector, camera: CameraRenderState) {
        if (!state.hasAnimal) return
        poseStack.pushPose()
        poseStack.translate(0.5, 1.0, 0.5)
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.angle))
        poseStack.pushPose()
        poseStack.translate(-0.5, -0.5, -0.5)
        state.fence.submit(poseStack, nodes, state.light, OverlayTexture.NO_OVERLAY, 0)
        poseStack.popPose()
        poseStack.pushPose()
        poseStack.translate(0.0, 0.4, 0.0)
        poseStack.scale(-1f, -1f, 1f)
        nodes.submitModel(knot, knotState, poseStack, LEASH_KNOT_TEXTURE, state.light, OverlayTexture.NO_OVERLAY, 0, null)
        poseStack.popPose()
        poseStack.popPose()
    }
}
