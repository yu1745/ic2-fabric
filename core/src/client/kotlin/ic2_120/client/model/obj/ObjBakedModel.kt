package ic2_120.client.model.obj

import net.fabricmc.fabric.api.renderer.v1.RendererAccess
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext
import net.fabricmc.fabric.api.util.TriState
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.resources.model.ModelBaker
import net.minecraft.client.resources.model.ModelState
import net.minecraft.client.resources.model.UnbakedModel
import net.minecraft.client.renderer.block.model.BlockModel
import net.minecraft.client.renderer.block.model.ItemOverrides
import net.minecraft.client.renderer.block.model.ItemTransforms
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.resources.model.Material
import net.minecraft.world.item.ItemStack
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.level.BlockAndTintGetter
import org.joml.Vector3f
import org.joml.Vector4f
import java.util.function.Function
import java.util.function.Supplier

class ObjUnbakedModel(private val spec: ObjModelSpec) : UnbakedModel {

    private var transformation: ItemTransforms = ItemTransforms.NO_TRANSFORMS

    // 物品 overrides（电量档位、开关状态等）：借原版反序列化器解析，烘焙时交给 ItemOverrides
    private val overrideHolder: BlockModel? = spec.json.getAsJsonArray("overrides")?.let {
        BlockModel.fromString("{\"overrides\":$it}")
    }

    override fun getDependencies(): Collection<ResourceLocation> =
        listOfNotNull(spec.parent) + (overrideHolder?.overrides?.map { it.model } ?: emptyList())

    override fun resolveParents(modelLoader: Function<ResourceLocation, UnbakedModel>) {
        val parent = spec.parent?.let(modelLoader::apply) as? BlockModel ?: return
        parent.resolveParents(modelLoader)
        transformation = parent.transforms
    }

    override fun bake(
        baker: ModelBaker,
        textureGetter: Function<Material, TextureAtlasSprite>,
        rotationContainer: ModelState,
    ): BakedModel {
        val atlas = InventoryMenu.BLOCK_ATLAS
        val sprite = { slot: String ->
            spec.textures[slot]?.let { textureGetter.apply(Material(atlas, it)) }
                ?: textureGetter.apply(Material(atlas, MissingTextureAtlasSprite.getLocation()))
        }
        val renderer = RendererAccess.INSTANCE.renderer
            ?: error("ic2_120 OBJ 模型需要 Fabric Renderer API（Indigo 或 Indium）")
        val standard = renderer.materialFinder().find()
        val glow = renderer.materialFinder().emissive(true).disableDiffuse(true)
            .ambientOcclusion(TriState.FALSE).find()

        // blockstate 的 x/y 旋转：绕方块中心变换顶点与法线
        val matrix = rotationContainer.rotation.matrix
        val tmp = Vector4f()
        fun rotatePos(p: Vector3f): Vector3f {
            tmp.set(p.x - 0.5f, p.y - 0.5f, p.z - 0.5f, 1f).mul(matrix)
            return Vector3f(tmp.x + 0.5f, tmp.y + 0.5f, tmp.z + 0.5f)
        }
        fun rotateDir(n: Vector3f): Vector3f {
            tmp.set(n.x, n.y, n.z, 0f).mul(matrix)
            return Vector3f(tmp.x, tmp.y, tmp.z).normalize()
        }

        val builder = renderer.meshBuilder()
        val emitter = builder.emitter
        val sprites = HashMap<String, TextureAtlasSprite>()
        for (tri in spec.geometry.triangles) {
            val mat = spec.geometry.materials[tri.material]
            val sp = sprites.getOrPut(mat?.texture ?: "missing") { sprite(mat?.texture ?: "missing") }
            val pos = Array(3) { rotatePos(tri.pos[it]) }
            val faceNormal = Vector3f(pos[1]).sub(pos[0]).cross(Vector3f(pos[2]).sub(pos[0])).normalize()
            for (i in 0..3) {
                val k = minOf(i, 2)
                val uv = tri.uv[k]
                val v = if (spec.flipV) 1f - uv.y else uv.y
                emitter.pos(i, pos[k])
                emitter.uv(i, sp.getU(uv.x), sp.getV(v))
                val n = tri.normal?.let { rotateDir(it[k]) } ?: faceNormal
                emitter.normal(i, n)
                emitter.color(i, mat?.color ?: -1)
                if (mat?.emissive == true) emitter.lightmap(i, LightTexture.FULL_BRIGHT)
            }
            emitter.colorIndex(mat?.tintIndex ?: -1)
            emitter.nominalFace(Direction.getNearest(faceNormal.x.toDouble(), faceNormal.y.toDouble(), faceNormal.z.toDouble()))
            emitter.material(if (mat?.emissive == true) glow else standard)
            emitter.emit()
        }
        val particle = sprite("particle")
        val overrides = overrideHolder?.let { ItemOverrides(baker, it, it.overrides) } ?: ItemOverrides.EMPTY
        return ObjBakedModel(builder.build(), particle, transformation, overrides)
    }
}

@Suppress("OVERRIDE_DEPRECATION")
class ObjBakedModel(
    private val mesh: Mesh,
    private val particle: TextureAtlasSprite,
    private val transformation: ItemTransforms,
    private val overrides: ItemOverrides,
) : BakedModel, FabricBakedModel {

    // 供原版路径（方块实体渲染器里的 BlockModelRenderer、物品渲染回退）使用
    private val quadLists: Array<List<BakedQuad>> by lazy { ModelHelper.toQuadLists(mesh) }

    override fun isVanillaAdapter() = false

    override fun emitBlockQuads(
        blockView: BlockAndTintGetter,
        state: BlockState,
        pos: BlockPos,
        randomSupplier: Supplier<RandomSource>,
        context: RenderContext,
    ) = mesh.forEach { quad -> context.emitter.copyFrom(quad).emit() }

    override fun emitItemQuads(stack: ItemStack, randomSupplier: Supplier<RandomSource>, context: RenderContext) =
        mesh.forEach { quad -> context.emitter.copyFrom(quad).emit() }

    override fun getQuads(state: BlockState?, face: Direction?, random: RandomSource): List<BakedQuad> =
        quadLists[ModelHelper.toFaceIndex(face)]

    override fun useAmbientOcclusion() = true
    override fun isGui3d() = true
    override fun usesBlockLight() = true
    override fun isCustomRenderer() = false
    override fun getParticleIcon() = particle
    override fun getTransforms() = transformation
    override fun getOverrides(): ItemOverrides = overrides
}
