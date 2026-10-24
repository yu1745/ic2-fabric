package ic2_120.client.model.obj

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.resources.ResourceLocation
import org.joml.Vector2f
import org.joml.Vector3f

/**
 * OBJ + MTL 的最小解析结果。
 *
 * 约定（与 tools/model3d 导出器一致）：
 * - 坐标为方块空间 [0,1]，Y 向上，正面朝北（-Z）；
 * - UV 以贴图左上为原点（v 向下），1.0 = 一张贴图；
 * - 多边形面会被拆成三角形，三角形以"重复第 4 个顶点"的方式输出为四边形。
 */
class ObjGeometry(val triangles: List<Triangle>, val materials: Map<String, ObjMaterial>)

class Triangle(
    val material: String,
    val pos: Array<Vector3f>,
    val uv: Array<Vector2f>,
    val normal: Array<Vector3f>?,
)

/**
 * @param texture 贴图槽位名（`map_Kd #slot`），由模型 JSON 的 `textures` 解析为实际贴图
 * @param color Kd 染色（ARGB，A 恒为 0xFF）
 * @param emissive Ke 非零时视为自发光（不受光照影响）
 * @param tintIndex 非标准 `tintindex` 行；-1 不染色
 */
class ObjMaterial(val texture: String, val color: Int, val emissive: Boolean, val tintIndex: Int = -1)

/**
 * 模型 JSON 中的 OBJ 声明：
 * ```json
 * { "loader": "ic2_120:obj", "model": "ic2_120:models/block/x.obj",
 *   "parent": "minecraft:block/block", "textures": { "steel": "ic2_120:block/..." } }
 * ```
 * `parent` 只用于继承 `display` 变换；`flip_v` 可选（默认 false）；`overrides` 与原版物品模型同义。
 */
class ObjModelSpec(
    val geometry: ObjGeometry,
    val textures: Map<String, ResourceLocation>,
    val parent: ResourceLocation?,
    val json: JsonObject,
    val flipV: Boolean,
)

object ObjParser {

    const val LOADER_ID = "ic2_120:obj"

    fun parseSpec(json: JsonObject, readText: (ResourceLocation) -> String): ObjModelSpec {
        val objId = ResourceLocation(json.get("model").asString)
        val geometry = parseObj(readText(objId), objId, readText)
        val textures = buildMap {
            json.getAsJsonObject("textures")?.entrySet()?.forEach { (k, v) -> put(k, ResourceLocation(v.asString)) }
        }
        val parent = json.get("parent")?.asString?.let(::ResourceLocation)
        val flipV = json.get("flip_v")?.asBoolean ?: false
        return ObjModelSpec(geometry, textures, parent, json, flipV)
    }

    fun parseObj(text: String, objId: ResourceLocation, readText: (ResourceLocation) -> String): ObjGeometry {
        val positions = ArrayList<Vector3f>()
        val uvs = ArrayList<Vector2f>()
        val normals = ArrayList<Vector3f>()
        val triangles = ArrayList<Triangle>()
        val materials = HashMap<String, ObjMaterial>()
        var current = ""

        for (raw in text.lineSequence()) {
            val line = raw.substringBefore('#').trim()
            if (line.isEmpty()) continue
            val p = line.split(Regex("\\s+"))
            when (p[0]) {
                "v" -> positions += Vector3f(p[1].toFloat(), p[2].toFloat(), p[3].toFloat())
                "vt" -> uvs += Vector2f(p[1].toFloat(), p[2].toFloat())
                "vn" -> normals += Vector3f(p[1].toFloat(), p[2].toFloat(), p[3].toFloat())
                "usemtl" -> current = p[1]
                "mtllib" -> {
                    val dir = objId.path.substringBeforeLast('/', "")
                    val mtlId = ResourceLocation(objId.namespace, if (dir.isEmpty()) p[1] else "$dir/${p[1]}")
                    materials += parseMtl(readText(mtlId))
                }
                "f" -> {
                    val corners = p.drop(1).map { c ->
                        val idx = c.split('/')
                        Triple(
                            index(idx[0], positions.size),
                            idx.getOrNull(1)?.takeIf { it.isNotEmpty() }?.let { index(it, uvs.size) },
                            idx.getOrNull(2)?.takeIf { it.isNotEmpty() }?.let { index(it, normals.size) },
                        )
                    }
                    for (i in 1 until corners.size - 1) {
                        val tri = arrayOf(corners[0], corners[i], corners[i + 1])
                        triangles += Triangle(
                            current,
                            Array(3) { positions[tri[it].first] },
                            Array(3) { tri[it].second?.let(uvs::get) ?: Vector2f() },
                            if (tri.all { it.third != null }) Array(3) { normals[tri[it].third!!] } else null,
                        )
                    }
                }
            }
        }
        return ObjGeometry(triangles, materials)
    }

    private fun index(s: String, size: Int): Int {
        val i = s.toInt()
        return if (i < 0) size + i else i - 1
    }

    fun parseMtl(text: String): Map<String, ObjMaterial> {
        val out = HashMap<String, ObjMaterial>()
        var name: String? = null
        var tex = "missing"
        var color = -1
        var emissive = false
        var tint = -1
        fun flush() {
            name?.let { out[it] = ObjMaterial(tex, color, emissive, tint) }
        }
        for (raw in text.lineSequence()) {
            val p = raw.trim().split(Regex("\\s+"))
            when (p[0]) {
                "newmtl" -> {
                    flush(); name = p[1]; tex = "missing"; color = -1; emissive = false; tint = -1
                }
                "Kd" -> {
                    val c = p.drop(1).take(3).map { (it.toFloat().coerceIn(0f, 1f) * 255 + 0.5f).toInt() }
                    color = (0xFF shl 24) or (c[0] shl 16) or (c[1] shl 8) or c[2]
                }
                "Ke" -> emissive = p.drop(1).take(3).any { it.toFloat() > 0f }
                "map_Kd" -> tex = p[1].removePrefix("#")
                // 非标准扩展：物品/方块着色器的 tintindex（如流体单元按所含流体染色）
                "tintindex" -> tint = p[1].toInt()
            }
        }
        flush()
        return out
    }

    fun parseJson(text: String): JsonObject = JsonParser.parseString(text).asJsonObject
}
