package ic2_120.client.renderers

import ic2_120.content.entity.BrokenRubberBoatEntity
import ic2_120.content.entity.CarbonBoatEntity
import ic2_120.content.entity.ElectricBoatEntity
import ic2_120.content.entity.RubberBoatEntity
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.model.BoatModel
import net.minecraft.client.model.ChestBoatModel
import net.minecraft.client.model.ChestRaftModel
import net.minecraft.client.model.ListModel
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.model.WaterPatchModel
import net.minecraft.client.model.RaftModel
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.world.entity.vehicle.Boat
import net.minecraft.util.Mth
import com.mojang.math.Axis
import net.minecraft.resources.ResourceLocation
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
        if (type == Boat.Type.BAMBOO) {
            return if (hasChestBoat) {
                ChestRaftModel(modelContext.getPart(ModelLayers.createChestRaft(type)))
            } else {
                RaftModel(modelContext.getPart(ModelLayers.createRaft(type)))
            }
        }
        return if (hasChestBoat) {
            ChestBoatModel(modelContext.getPart(ModelLayers.createChestBoat(type)))
        } else {
            BoatModel(modelContext.getPart(ModelLayers.createBoat(type)))
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

    override fun getTexture(boat: Boat): ResourceLocation = when (boat) {
        is BrokenRubberBoatEntity -> ResourceLocation.of("ic2", "textures/entity/boat/boat_rubber.png")
        is CarbonBoatEntity -> ResourceLocation.of("ic2", "textures/entity/boat/boat_carbon.png")
        is RubberBoatEntity -> ResourceLocation.of("ic2", "textures/entity/boat/boat_rubber.png")
        is ElectricBoatEntity -> ResourceLocation.of("ic2", "textures/entity/boat/boat_electric.png")
        else -> ResourceLocation.ofVanilla("textures/entity/boat/oak.png")
    }

    override fun render(
        boat: Boat,
        entityYaw: Float,
        tickDelta: Float,
        matrices: PoseStack,
        vertexConsumers: MultiBufferSource,
        light: Int
    ) {
        matrices.push()
        matrices.translate(0.0, 0.375, 0.0)
        matrices.multiply(Axis.POSITIVE_Y.rotationDegrees(180.0f - entityYaw))

        val wobbleTicks = boat.damageWobbleTicks.toFloat() - tickDelta
        var wobbleStrength = boat.damageWobbleStrength - tickDelta
        if (wobbleStrength < 0.0f) wobbleStrength = 0.0f
        if (wobbleTicks > 0.0f) {
            matrices.multiply(
                Axis.POSITIVE_X.rotationDegrees(
                    Mth.sin(wobbleTicks) * wobbleTicks * wobbleStrength / 10.0f * boat.damageWobbleSide.toFloat()
                )
            )
        }

        val bubbleWobble = boat.interpolateBubbleWobble(tickDelta)
        if (!Mth.approximatelyEquals(bubbleWobble, 0.0f)) {
            matrices.multiply(Quaternionf().setAngleAxis((bubbleWobble * (Math.PI.toFloat() / 180.0f)).toDouble(), 1.0, 0.0, 1.0))
        }

        val model = getOrCreateModel(boat.variant)
        val texture = getTexture(boat)
        matrices.scale(-1.0f, -1.0f, 1.0f)
        matrices.multiply(Axis.POSITIVE_Y.rotationDegrees(90.0f))
        model.setAngles(boat, tickDelta, 0.0f, -0.1f, 0.0f, 0.0f)

        val vertexConsumer = vertexConsumers.getBuffer(model.getLayer(texture))
        model.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV, -1)

        if (!boat.isSubmergedInWater && model is WaterPatchModel) {
            val waterMask = vertexConsumers.getBuffer(RenderType.getWaterMask())
            model.waterPatch.render(matrices, waterMask, light, OverlayTexture.DEFAULT_UV)
        }

        matrices.pop()
        super.render(boat, entityYaw, tickDelta, matrices, vertexConsumers, light)
    }
}
