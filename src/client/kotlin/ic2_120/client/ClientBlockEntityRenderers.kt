package ic2_120.client

import ic2_120.content.ModBlockEntities
import ic2_120.content.block.machines.WindGeneratorBlockEntity
import ic2_120.content.block.machines.WindKineticGeneratorBlockEntity
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories

object ClientBlockEntityRenderers {
    fun register() {
        BlockEntityRendererFactories.register(
            ModBlockEntities.getType(WindGeneratorBlockEntity::class),
            ::WindGeneratorBlockEntityRenderer
        )
        BlockEntityRendererFactories.register(
            ModBlockEntities.getType(WindKineticGeneratorBlockEntity::class),
            ::WindKineticGeneratorBlockEntityRenderer
        )
    }
}
