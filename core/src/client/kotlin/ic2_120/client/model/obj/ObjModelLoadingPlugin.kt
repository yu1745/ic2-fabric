package ic2_120.client.model.obj

import ic2_120.Ic2_120
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin
import net.minecraft.resource.ResourceManager
import net.minecraft.util.Identifier
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

    fun register() {
        PreparableModelLoadingPlugin.register(::load) { specs, ctx ->
            // 全部登记为额外模型：方块实体渲染器可直接按 id 取用运动件
            ctx.addModels(specs.keys)
            ctx.resolveModel().register { c -> specs[c.id()]?.let(::ObjUnbakedModel) }
        }
    }

    private fun load(manager: ResourceManager, executor: Executor): CompletableFuture<Map<Identifier, ObjModelSpec>> =
        CompletableFuture.supplyAsync({
            val read = { id: Identifier -> manager.getResourceOrThrow(id).reader.use { it.readText() } }
            val out = HashMap<Identifier, ObjModelSpec>()
            val found = manager.findResources("models") { it.namespace == Ic2_120.MOD_ID && it.path.endsWith(".json") }
            for ((file, resource) in found) {
                val text = resource.reader.use { it.readText() }
                if (!text.contains(ObjParser.LOADER_ID)) continue
                val modelId = Identifier(file.namespace, file.path.removePrefix("models/").removeSuffix(".json"))
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
