package ic2_120.client.renderers

import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.model.`object`.boat.BoatModel
import net.minecraft.client.renderer.entity.AbstractBoatRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.state.BoatRenderState
import net.minecraft.resources.Identifier

/** Vanilla boat animation and geometry with the IC2 boat texture. */
class Ic2BoatEntityRenderer(
    context: EntityRendererProvider.Context,
    texture: Identifier
) : AbstractBoatRenderer(context, texture) {
    private val boatModel: EntityModel<BoatRenderState> = BoatModel(context.bakeLayer(ModelLayers.OAK_BOAT))

    override fun model(): EntityModel<BoatRenderState> = boatModel
}
