package ic2_120.client

import ic2_120.client.renderers.Ic2BoatEntityRenderer
import ic2_120.client.renderers.LaserProjectileEntityRenderer
import ic2_120.content.entity.ModEntities
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.minecraft.resources.Identifier

object ClientEntityRenderers {

    fun register() {
        EntityRendererRegistry.register(ModEntities.BROKEN_RUBBER_BOAT) { context ->
            Ic2BoatEntityRenderer(context, Identifier.fromNamespaceAndPath("ic2", "textures/entity/boat/boat_rubber.png"))
        }
        EntityRendererRegistry.register(ModEntities.CARBON_BOAT) { context ->
            Ic2BoatEntityRenderer(context, Identifier.fromNamespaceAndPath("ic2", "textures/entity/boat/boat_carbon.png"))
        }
        EntityRendererRegistry.register(ModEntities.RUBBER_BOAT) { context ->
            Ic2BoatEntityRenderer(context, Identifier.fromNamespaceAndPath("ic2", "textures/entity/boat/boat_rubber.png"))
        }
        EntityRendererRegistry.register(ModEntities.ELECTRIC_BOAT) { context ->
            Ic2BoatEntityRenderer(context, Identifier.fromNamespaceAndPath("ic2", "textures/entity/boat/boat_electric.png"))
        }
        EntityRendererRegistry.register(ModEntities.LASER_PROJECTILE) { context ->
            LaserProjectileEntityRenderer(context)
        }
    }
}
