package ic2_120.client

import ic2_120.client.renderers.ComposeDebugBlockEntityRenderer
import ic2_120.client.renderers.TeleporterBlockEntityRenderer
import ic2_120.client.renderers.TransmissionBlockEntityRenderer
import ic2_120.client.renderers.WindGeneratorBlockEntityRenderer
import ic2_120.client.renderers.WindKineticGeneratorBlockEntityRenderer
import ic2_120.content.block.ComposeDebugBlockEntity
import ic2_120.content.block.machines.TeleporterBlockEntity
import ic2_120.content.block.transmission.TransmissionBlockEntity
import ic2_120.content.block.machines.WindGeneratorBlockEntity
import ic2_120.content.block.machines.WindKineticGeneratorBlockEntity
import ic2_120.registry.type
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories

object ClientBlockEntityRenderers {
    fun register() {
        BlockEntityRendererFactories.register(
            WindGeneratorBlockEntity::class.type(),
            ::WindGeneratorBlockEntityRenderer
        )
        BlockEntityRendererFactories.register(
            WindKineticGeneratorBlockEntity::class.type(),
            ::WindKineticGeneratorBlockEntityRenderer
        )
        BlockEntityRendererFactories.register(
            TransmissionBlockEntity::class.type(),
            ::TransmissionBlockEntityRenderer
        )
        BlockEntityRendererFactories.register(
            ComposeDebugBlockEntity::class.type(),
            ::ComposeDebugBlockEntityRenderer
        )
        BlockEntityRendererFactories.register(
            TeleporterBlockEntity::class.type(),
            ::TeleporterBlockEntityRenderer
        )
    }
}
