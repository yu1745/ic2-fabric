package ic2_120.client

import ic2_120.Ic2_120
import ic2_120.content.block.RubberLogBlock
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier

private val RUBBER_LOG_ID = Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "rubber_log")
private val MODEL_ID = Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "block/rubber_log")

/** Assign a model for every rubber log state, avoiding hundreds of blockstate JSON variants. */
object RubberLogModelPlugin {
    fun register() {
        ModelLoadingPlugin.register { context ->
            val block = BuiltInRegistries.BLOCK.getValue(RUBBER_LOG_ID)
            if (block is RubberLogBlock) {
                context.registerBlockStateResolver(block) { resolver ->
                    for (state in block.stateDefinition.possibleStates) {
                        resolver.setModel(state, RubberLogModel(state, MODEL_ID))
                    }
                }
            }
        }
    }
}
