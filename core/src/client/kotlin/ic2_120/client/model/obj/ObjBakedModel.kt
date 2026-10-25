package ic2_120.client.model.obj

import com.mojang.blaze3d.platform.Transparency
import net.minecraft.client.model.geom.builders.UVPair
import net.minecraft.client.renderer.block.dispatch.ModelState
import net.minecraft.client.resources.model.ModelBaker
import net.minecraft.client.resources.model.ModelDebugName
import net.minecraft.client.resources.model.UnbakedModel
import net.minecraft.client.resources.model.geometry.BakedQuad
import net.minecraft.client.resources.model.geometry.QuadCollection
import net.minecraft.client.resources.model.geometry.UnbakedGeometry
import net.minecraft.client.resources.model.sprite.Material
import net.minecraft.client.resources.model.sprite.TextureSlots
import net.minecraft.core.Direction
import net.minecraft.resources.Identifier
import org.joml.Vector3f

/** OBJ geometry for the 26.2 model pipeline. The vanilla baker now consumes quad records. */
class ObjUnbakedModel(private val spec: ObjModelSpec) : UnbakedModel {
    override fun parent(): Identifier? = spec.parent

    override fun textureSlots(): TextureSlots.Data {
        val slots = TextureSlots.Data.Builder()
        for ((slot, id) in spec.textures) slots.addTexture(slot, Material(id))
        return slots.build()
    }

    override fun geometry(): UnbakedGeometry = ObjGeometryBaker(spec)
}

private class ObjGeometryBaker(private val spec: ObjModelSpec) : UnbakedGeometry {
    override fun bake(slots: TextureSlots, baker: ModelBaker, state: ModelState, name: ModelDebugName): QuadCollection {
        val out = QuadCollection.Builder()
        val transform = state.transformation().matrix
        for (tri in spec.geometry.triangles) {
            val material = spec.geometry.materials[tri.material]
            val slot = material?.texture ?: "particle"
            val baked = baker.materials().resolveSlot(slots, slot, name)
            val sprite = baked.sprite()
            val positions = Array(4) { index ->
                val point = Vector3f(tri.pos[minOf(index, 2)]).sub(0.5f, 0.5f, 0.5f)
                transform.transformPosition(point).add(0.5f, 0.5f, 0.5f)
            }
            val n = Vector3f(positions[1]).sub(positions[0])
                .cross(Vector3f(positions[2]).sub(positions[0])).normalize()
            val facing = Direction.getApproximateNearest(n.x, n.y, n.z)
            val uv = LongArray(4) { index ->
                val raw = tri.uv[minOf(index, 2)]
                UVPair.pack(sprite.getU(raw.x), sprite.getV(if (spec.flipV) 1f - raw.y else raw.y))
            }
            val transparency = if (baked.forceTranslucent()) Transparency.TRANSLUCENT
                else sprite.contents().computeTransparency(0f, 0f, 1f, 1f)
            val info = baker.interner().materialInfo(
                BakedQuad.MaterialInfo.of(baked, transparency, material?.tintIndex ?: -1, true, if (material?.emissive == true) 15 else 0)
            )
            out.addUnculledFace(BakedQuad(
                positions[0], positions[1], positions[2], positions[3],
                uv[0], uv[1], uv[2], uv[3], facing, info
            ))
        }
        return out.build()
    }
}
