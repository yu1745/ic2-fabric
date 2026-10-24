package ic2_120.client.renderers

import ic2_120.content.entity.BrokenRubberBoatEntity
import ic2_120.content.entity.CarbonBoatEntity
import ic2_120.content.entity.ElectricBoatEntity
import ic2_120.content.entity.RubberBoatEntity
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.model.BoatModel
import net.minecraft.client.model.ChestBoatModel
import net.minecraft.client.model.ChestRaftModel
import net.minecraft.client.model.ListModel
import net.minecraft.client.model.RaftModel
import net.minecraft.client.model.WaterPatchModel
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.entity.vehicle.Boat
import org.joml.Quaternionf

/**
 * IC2 船实体渲染器：复用原版船模型与动画，按实体类型选择自定义贴图。
 */
class Ic2BoatEntityRenderer(
    context: EntityRendererProvider.Context,
    hasChest: Boolean = false
) : EntityRenderer<Boat>(context) {

    private val hasChestBoat = hasChest
    private val modelContext = context
    private val models = mutableMapOf<Boat.Type, ListModel<Boat>>()

    private fun createModel(type: Boat.Type): ListModel<Boat> {
        val layer = if (hasChestBoat) ModelLayers.createChestBoatModelName(type) else ModelLayers.createBoatModelName(type)
        val part = modelContext.bakeLayer(layer)
        return if (type == Boat.Type.BAMBOO) {
            if (hasChestBoat) ChestRaftModel(part) else RaftModel(part)
        } else {
            if (hasChestBoat) ChestBoatModel(part) else BoatModel(part)
        }
    }

    private fun getOrCreateModel(type: Boat.Type): ListModel<Boat> {
        return models.getOrPut(type) {
            runCatching { createModel(type) }.getOrElse {
                // 某些环境未烘焙 bamboo raft layer，回退到 oak 船模型避免资源重载崩溃。
                createModel(Boat.Type.OAK)
            }
        }
    }

    override fun getTextureLocation(boat: Boat): ResourceLocation = when (boat) {
        is BrokenRubberBoatEntity -> ResourceLocation.fromNamespaceAndPath("ic2", "textures/entity/boat/boat_rubber.png")
        is CarbonBoatEntity -> ResourceLocation.fromNamespaceAndPath("ic2", "textures/entity/boat/boat_carbon.png")
        is RubberBoatEntity -> ResourceLocation.fromNamespaceAndPath("ic2", "textures/entity/boat/boat_rubber.png")
        is ElectricBoatEntity -> ResourceLocation.fromNamespaceAndPath("ic2", "textures/entity/boat/boat_electric.png")
        else -> ResourceLocation.withDefaultNamespace("textures/entity/boat/oak.png")
    }

    override fun render(
        boat: Boat,
        entityYaw: Float,
        tickDelta: Float,
        matrices: PoseStack,
        vertexConsumers: MultiBufferSource,
        light: Int
    ) {
        matrices.pushPose()
        matrices.translate(0.0, 0.375, 0.0)
        matrices.mulPose(Axis.YP.rotationDegrees(180.0f - entityYaw))

        val wobbleTicks = boat.hurtTime - tickDelta
        var wobbleStrength = boat.damage - tickDelta
        if (wobbleStrength < 0.0f) wobbleStrength = 0.0f
        if (wobbleTicks > 0.0f) {
            matrices.mulPose(
                Axis.XP.rotationDegrees(
                    Mth.sin(wobbleTicks) * wobbleTicks * wobbleStrength / 10.0f * boat.hurtDir
                )
            )
        }

        val bubbleWobble = boat.getBubbleAngle(tickDelta)
        if (!Mth.equal(bubbleWobble, 0.0f)) {
            matrices.mulPose(Quaternionf().setAngleAxis((bubbleWobble * (Math.PI.toFloat() / 180.0f)).toDouble(), 1.0, 0.0, 1.0))
        }

        val model = getOrCreateModel(boat.variant)
        val texture = getTextureLocation(boat)
        matrices.scale(-1.0f, -1.0f, 1.0f)
        matrices.mulPose(Axis.YP.rotationDegrees(90.0f))
        model.setupAnim(boat, tickDelta, 0.0f, -0.1f, 0.0f, 0.0f)

        val vertexConsumer = vertexConsumers.getBuffer(model.renderType(texture))
        model.renderToBuffer(matrices, vertexConsumer, light, OverlayTexture.NO_OVERLAY)

        if (!boat.isUnderWater && model is WaterPatchModel) {
            val waterMask = vertexConsumers.getBuffer(RenderType.waterMask())
            model.waterPatch().render(matrices, waterMask, light, OverlayTexture.NO_OVERLAY)
        }

        matrices.popPose()
        super.render(boat, entityYaw, tickDelta, matrices, vertexConsumers, light)
    }
}
