package ic2_120.client

import ic2_120.Ic2_120
import ic2_120.content.block.RubberLogBlock
import net.fabricmc.fabric.api.client.model.loading.v1.BlockStateResolver
import net.fabricmc.fabric.api.client.model.loading.v1.DelegatingUnbakedModel
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier.OnLoad
import net.minecraft.client.render.model.UnbakedModel
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

private val RUBBER_LOG_ID = Identifier.of(Ic2_120.MOD_ID, "rubber_log")
private val MODEL_ID = Identifier.of(Ic2_120.MOD_ID, "block/rubber_log")

/**
 * 为橡胶树原木注册动态模型，替代 243 个 blockstate 变体和 81 个静态模型文件。
 */
object RubberLogModelPlugin {

    fun register() {
        ModelLoadingPlugin.register { ctx ->
            ctx.addModels(MODEL_ID)
            val block = Registries.BLOCK.get(RUBBER_LOG_ID)
            if (block is RubberLogBlock) {
                ctx.registerBlockStateResolver(block, BlockStateResolver { resolverCtx ->
                    val model = DelegatingUnbakedModel(MODEL_ID)
                    block.stateManager.states.forEach { state -> resolverCtx.setModel(state, model) }
                })
            }
            ctx.modifyModelOnLoad().register(ModelModifier.OnLoad { original, ctx ->
                val ctx0 = ctx as ModelModifier.OnLoad.Context
                if (RubberLogModelHelper.getModelId(ctx0) == MODEL_ID) RubberLogModel() else original
            })
        }
    }
}
