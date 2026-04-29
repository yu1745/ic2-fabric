package ic2_120.client.renderers

import ic2_120.content.entity.BrokenRubberBoatEntity
import ic2_120.content.entity.CarbonBoatEntity
import ic2_120.content.entity.ElectricBoatEntity
import ic2_120.content.entity.RubberBoatEntity
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.entity.EntityRenderer
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.render.entity.model.BoatEntityModel
import net.minecraft.client.render.entity.model.ChestBoatEntityModel
import net.minecraft.client.render.entity.model.ChestRaftEntityModel
import net.minecraft.client.render.entity.model.CompositeEntityModel
import net.minecraft.client.render.entity.model.EntityModelLayers
import net.minecraft.client.render.entity.model.ModelWithWaterPatch
import net.minecraft.client.render.entity.model.RaftEntityModel
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.vehicle.BoatEntity
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.RotationAxis
import net.minecraft.util.Identifier
import org.joml.Quaternionf

/**
 * IC2 船实体渲染器：复用原版船模型与动画，按实体类型选择自定义贴图。
 */
class Ic2BoatEntityRenderer(
    context: EntityRendererFactory.Context,
    hasChest: Boolean = false
) : EntityRenderer<BoatEntity>(context) {

    private val hasChestBoat = hasChest
    private val modelContext = context
    private val models = mutableMapOf<BoatEntity.Type, CompositeEntityModel<BoatEntity>>()

    private fun createModel(type: BoatEntity.Type): CompositeEntityModel<BoatEntity> {
        if (type == BoatEntity.Type.BAMBOO) {
            return if (hasChestBoat) {
                ChestRaftEntityModel(modelContext.getPart(EntityModelLayers.createChestRaft(type)))
            } else {
                RaftEntityModel(modelContext.getPart(EntityModelLayers.createRaft(type)))
            }
        }
        return if (hasChestBoat) {
            ChestBoatEntityModel(modelContext.getPart(EntityModelLayers.createChestBoat(type)))
        } else {
            BoatEntityModel(modelContext.getPart(EntityModelLayers.createBoat(type)))
        }
    }

    private fun getOrCreateModel(type: BoatEntity.Type): CompositeEntityModel<BoatEntity> {
        return models.getOrPut(type) {
            runCatching { createModel(type) }.getOrElse {
                // 某些环境未烘焙 bamboo raft layer，回退到 oak 船模型避免资源重载崩溃。
                createModel(BoatEntity.Type.OAK)
            }
        }
    }

    override fun getTexture(boat: BoatEntity): Identifier = when (boat) {
        is BrokenRubberBoatEntity -> Identifier("ic2", "textures/entity/boat/boat_rubber.png")
        is CarbonBoatEntity -> Identifier("ic2", "textures/entity/boat/boat_carbon.png")
        is RubberBoatEntity -> Identifier("ic2", "textures/entity/boat/boat_rubber.png")
        is ElectricBoatEntity -> Identifier("ic2", "textures/entity/boat/boat_electric.png")
        else -> Identifier("textures/entity/boat/oak.png")
    }

    override fun render(
        boat: BoatEntity,
        entityYaw: Float,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int
    ) {
        matrices.push()
        matrices.translate(0.0, 0.375, 0.0)
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - entityYaw))

        val wobbleTicks = boat.damageWobbleTicks.toFloat() - tickDelta
        var wobbleStrength = boat.damageWobbleStrength - tickDelta
        if (wobbleStrength < 0.0f) wobbleStrength = 0.0f
        if (wobbleTicks > 0.0f) {
            matrices.multiply(
                RotationAxis.POSITIVE_X.rotationDegrees(
                    MathHelper.sin(wobbleTicks) * wobbleTicks * wobbleStrength / 10.0f * boat.damageWobbleSide.toFloat()
                )
            )
        }

        val bubbleWobble = boat.interpolateBubbleWobble(tickDelta)
        if (!MathHelper.approximatelyEquals(bubbleWobble, 0.0f)) {
            matrices.multiply(Quaternionf().setAngleAxis((bubbleWobble * (Math.PI.toFloat() / 180.0f)).toDouble(), 1.0, 0.0, 1.0))
        }

        val model = getOrCreateModel(boat.variant)
        val texture = getTexture(boat)
        matrices.scale(-1.0f, -1.0f, 1.0f)
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0f))
        model.setAngles(boat, tickDelta, 0.0f, -0.1f, 0.0f, 0.0f)

        val vertexConsumer = vertexConsumers.getBuffer(model.getLayer(texture))
        model.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV, 1.0f, 1.0f, 1.0f, 1.0f)

        if (!boat.isSubmergedInWater && model is ModelWithWaterPatch) {
            val waterMask = vertexConsumers.getBuffer(RenderLayer.getWaterMask())
            model.waterPatch.render(matrices, waterMask, light, OverlayTexture.DEFAULT_UV)
        }

        matrices.pop()
        super.render(boat, entityYaw, tickDelta, matrices, vertexConsumers, light)
    }
}
