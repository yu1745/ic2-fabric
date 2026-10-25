package ic2_120.client.model.obj

import ic2_120.Ic2_120
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel
import net.minecraft.client.renderer.block.dispatch.BlockStateModel
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

/**
 * 外置 OBJ 模型加载：扫描 `assets/ic2_120/models/` 下声明了 `"loader": "ic2_120:obj"` 的 JSON，
 * 以对应模型 id（如 `ic2_120:block/machine3d/macerator`）解析为 [ObjUnbakedModel]。
 *
 * 资源重载时在后台线程完成文件读取与 OBJ 解析，模型加载阶段只做查表。
 */
object ObjModelLoadingPlugin {

    private val LOGGER = LoggerFactory.getLogger("ic2_120/obj")
    private val extraKeys = HashMap<Identifier, ExtraModelKey<BlockStateModel>>()

    fun extraModelKey(id: Identifier): ExtraModelKey<BlockStateModel>? = extraKeys[id]

    fun register() {
        PreparableModelLoadingPlugin.register({ store, executor -> load(store.resourceManager(), executor) }) { specs, ctx ->
            ctx.modifyModelOnLoad().register { model, context ->
                specs[context.id()]?.let(::ObjUnbakedModel) ?: model
            }
            for (id in specs.keys) {
                // Extra block-state models require block-atlas sprites. Ordinary item OBJ
                // models bake through their item definitions and may use the item atlas.
                if (!id.path.startsWith("block/") && id.path != "item/cell3d/shell") continue
                val key = extraKeys.getOrPut(id) { ExtraModelKey.create { id.toString() } }
                ctx.addModel(key, SimpleUnbakedExtraModel.blockStateModel(id))
            }
        }
    }

    private fun load(manager: ResourceManager, executor: Executor): CompletableFuture<Map<Identifier, ObjModelSpec>> =
        CompletableFuture.supplyAsync({
            val read = { id: Identifier -> manager.getResource(id).orElseThrow().openAsReader().use { it.readText() } }
            val out = HashMap<Identifier, ObjModelSpec>()
            val found = manager.listResources("models") { it.namespace == Ic2_120.MOD_ID && it.path.endsWith(".json") }
            for ((file, resource) in found) {
                val text = resource.openAsReader().use { it.readText() }
                if (!text.contains(ObjParser.LOADER_ID)) continue
                val modelId = Identifier.fromNamespaceAndPath(file.namespace, file.path.removePrefix("models/").removeSuffix(".json"))
                try {
                    val json = ObjParser.parseJson(text)
                    if (json.get("loader")?.asString != ObjParser.LOADER_ID) continue
                    out[modelId] = ObjParser.parseSpec(json, read)
                } catch (e: Exception) {
                    LOGGER.error("加载 OBJ 模型 {} 失败", modelId, e)
                }
            }
            ic2_120.client.renderers.Machine3dBlockEntityRenderer.reload(manager)
            LOGGER.info("已加载 {} 个 OBJ 模型", out.size)
            out
        }, executor)
}
