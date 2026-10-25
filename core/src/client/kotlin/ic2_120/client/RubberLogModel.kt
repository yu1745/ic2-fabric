package ic2_120.client

import com.mojang.blaze3d.platform.Transparency
import ic2_120.content.block.RubberFaceState
import ic2_120.content.block.RubberLogBlock
import net.minecraft.client.model.geom.builders.UVPair
import net.minecraft.client.renderer.block.dispatch.BlockStateModel
import net.minecraft.client.renderer.block.dispatch.SingleVariant
import net.minecraft.client.resources.model.ModelBaker
import net.minecraft.client.resources.model.ResolvableModel
import net.minecraft.client.resources.model.SimpleModelWrapper
import net.minecraft.client.resources.model.geometry.BakedQuad
import net.minecraft.client.resources.model.geometry.QuadCollection
import net.minecraft.client.resources.model.sprite.Material
import net.minecraft.core.Direction
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.RotatedPillarBlock
import net.minecraft.world.level.block.state.BlockState
import org.joml.Vector3f

/** Bakes one set of quads per rubber log state using the 26.2 block model pipeline. */
class RubberLogModel(private val state: BlockState, private val sourceModel: Identifier) : BlockStateModel.UnbakedRoot {
    override fun resolveDependencies(resolver: ResolvableModel.Resolver) {
        resolver.markDependency(sourceModel)
    }

    override fun visualEqualityGroup(state: BlockState): Any = state

    override fun bake(state: BlockState, baker: ModelBaker): BlockStateModel {
        val block = state.block as RubberLogBlock
        val sides = listOf(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)
        val anyWet = sides.any { block.getRubberState(state, it) == RubberFaceState.WET }
        val axis = state.getValue(RotatedPillarBlock.AXIS)
        val endFaces = when (axis) {
            Direction.Axis.Y -> setOf(Direction.UP, Direction.DOWN)
            Direction.Axis.X -> setOf(Direction.EAST, Direction.WEST)
            Direction.Axis.Z -> setOf(Direction.NORTH, Direction.SOUTH)
        }
        val out = QuadCollection.Builder()
        val debug = net.minecraft.client.resources.model.ModelDebugName { sourceModel.toString() }
        var particle: Material.Baked? = null
        for (face in Direction.values()) {
            val path = when {
                face in endFaces -> if (anyWet) "rubber_wood_wet_bottomtop" else "rubber_wood_dry_bottomtop"
                block.getRubberState(state, face) == RubberFaceState.WET -> "rubber_wood_wet_front"
                block.getRubberState(state, face) == RubberFaceState.DRY -> "rubber_wood_dry_front"
                anyWet -> "rubber_wood_wet_leftrightback"
                else -> "rubber_wood_dry_leftrightback"
            }
            val baked = baker.materials().get(Material(Identifier.fromNamespaceAndPath("ic2", "block/resource/$path")), debug)
            if (particle == null) particle = baked
            val sprite = baked.sprite()
            val corners = faceCorners(face)
            val uv = arrayOf(0f to 1f, 1f to 1f, 1f to 0f, 0f to 0f)
            val packed = LongArray(4) { UVPair.pack(sprite.getU(uv[it].first), sprite.getV(uv[it].second)) }
            val info = baker.interner().materialInfo(BakedQuad.MaterialInfo.of(baked, Transparency.NONE, -1, true, 0))
            out.addCulledFace(face, BakedQuad(
                corners[0], corners[1], corners[2], corners[3],
                packed[0], packed[1], packed[2], packed[3], face, info
            ))
        }
        return SingleVariant(SimpleModelWrapper(out.build(), true, particle!!))
    }

    private fun faceCorners(face: Direction): Array<Vector3f> {
        val pts = when (face) {
            Direction.DOWN -> arrayOf(0f,0f,1f, 0f,0f,0f, 1f,0f,0f, 1f,0f,1f)
            Direction.UP -> arrayOf(0f,1f,0f, 0f,1f,1f, 1f,1f,1f, 1f,1f,0f)
            Direction.NORTH -> arrayOf(1f,0f,0f, 0f,0f,0f, 0f,1f,0f, 1f,1f,0f)
            Direction.SOUTH -> arrayOf(0f,0f,1f, 1f,0f,1f, 1f,1f,1f, 0f,1f,1f)
            Direction.WEST -> arrayOf(0f,0f,0f, 0f,0f,1f, 0f,1f,1f, 0f,1f,0f)
            Direction.EAST -> arrayOf(1f,0f,1f, 1f,0f,0f, 1f,1f,0f, 1f,1f,1f)
        }
        return Array(4) { i -> Vector3f(pts[i * 3], pts[i * 3 + 1], pts[i * 3 + 2]) }
    }
}
