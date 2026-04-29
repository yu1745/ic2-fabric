package ic2_120.client

import ic2_120.content.block.RubberFaceState
import ic2_120.content.block.RubberLogBlock
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext
import net.minecraft.world.BlockRenderView
import net.minecraft.block.BlockState
import net.minecraft.block.PillarBlock
import net.minecraft.client.render.model.BakedModel
import net.minecraft.client.render.model.Baker
import net.minecraft.client.render.model.ModelBakeSettings
import net.minecraft.client.render.model.UnbakedModel
import net.minecraft.client.render.model.json.ModelOverrideList
import net.minecraft.client.render.model.json.ModelTransformation
import net.minecraft.client.texture.Sprite
import net.minecraft.client.util.SpriteIdentifier
import net.minecraft.item.ItemStack
import net.minecraft.screen.PlayerScreenHandler
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.random.Random
import java.util.function.Function
import java.util.function.Supplier

/**
 * 橡胶树原木动态模型：根据 BlockState 的 rubber_north/south/east/west 在运行时选择每面纹理，
 * 无需 81 个静态模型 JSON 和 243 个 blockstate 变体。
 */
@Suppress("OVERRIDE_DEPRECATION")
class RubberLogModel : UnbakedModel, BakedModel, FabricBakedModel {

    private var sprites: Array<Sprite?> = arrayOfNulls(6)

    override fun getModelDependencies() = emptySet<Identifier>()
    override fun setParents(modelLoader: Function<Identifier, UnbakedModel>) {}

    override fun bake(
        baker: Baker,
        textureGetter: Function<SpriteIdentifier, Sprite>,
        rotationContainer: ModelBakeSettings,
        modelId: Identifier
    ): BakedModel {
        val atlas = PlayerScreenHandler.BLOCK_ATLAS_TEXTURE
        val tex = { path: String -> SpriteIdentifier(atlas, Identifier("ic2", "block/resource/$path")) }
        sprites[0] = textureGetter.apply(tex("rubber_wood_wet_front"))
        sprites[1] = textureGetter.apply(tex("rubber_wood_dry_front"))
        sprites[2] = textureGetter.apply(tex("rubber_wood_wet_leftrightback"))
        sprites[3] = textureGetter.apply(tex("rubber_wood_dry_leftrightback"))
        sprites[4] = textureGetter.apply(tex("rubber_wood_wet_bottomtop"))
        sprites[5] = textureGetter.apply(tex("rubber_wood_dry_bottomtop"))
        return this
    }

    private fun spriteFor(face: RubberFaceState, anyWet: Boolean): Sprite = when (face) {
        RubberFaceState.NONE -> if (anyWet) sprites[2]!! else sprites[3]!!
        RubberFaceState.WET -> sprites[0]!!
        RubberFaceState.DRY -> sprites[1]!!
    }

    override fun getQuads(state: BlockState?, face: Direction?, random: Random) = emptyList<net.minecraft.client.render.model.BakedQuad>()

    override fun useAmbientOcclusion() = true
    override fun hasDepth() = false
    override fun isSideLit() = true
    override fun isBuiltin() = false
    override fun getParticleSprite() = sprites[2]!!
    override fun getTransformation() = ModelTransformation.NONE
    override fun getOverrides() = ModelOverrideList.EMPTY

    override fun isVanillaAdapter() = false

    override fun emitBlockQuads(
        blockView: BlockRenderView,
        state: BlockState,
        pos: BlockPos,
        randomSupplier: Supplier<Random>,
        context: RenderContext
    ) {
        if (state.block !is RubberLogBlock) return
        val block = state.block as RubberLogBlock
        val axis = state.get(PillarBlock.AXIS) ?: Direction.Axis.Y
        val n = block.getRubberState(state, Direction.NORTH)
        val s = block.getRubberState(state, Direction.SOUTH)
        val e = block.getRubberState(state, Direction.EAST)
        val w = block.getRubberState(state, Direction.WEST)
        val anyWet = n == RubberFaceState.WET || s == RubberFaceState.WET || e == RubberFaceState.WET || w == RubberFaceState.WET
        val bottomTop = if (anyWet) sprites[4]!! else sprites[5]!!

        val endFaces = when (axis) {
            Direction.Axis.Y -> setOf(Direction.UP, Direction.DOWN)
            Direction.Axis.X -> setOf(Direction.EAST, Direction.WEST)
            Direction.Axis.Z -> setOf(Direction.NORTH, Direction.SOUTH)
        }

        val emitter = context.emitter
        for (dir in Direction.values()) {
            val faceSprite = if (dir in endFaces) bottomTop else spriteFor(block.getRubberState(state, dir), anyWet)
            emitter.square(dir, 0f, 0f, 1f, 1f, 0f)
            emitter.spriteBake(faceSprite, MutableQuadView.BAKE_LOCK_UV)
            emitter.color(-1, -1, -1, -1)
            emitter.emit()
        }
    }

    override fun emitItemQuads(stack: ItemStack?, randomSupplier: Supplier<Random>?, context: RenderContext) {
        val emitter = context.emitter
        val sprite = sprites[2]!!
        for (dir in Direction.values()) {
            emitter.square(dir, 0f, 0f, 1f, 1f, 0f)
            emitter.spriteBake(sprite, MutableQuadView.BAKE_LOCK_UV)
            emitter.color(-1, -1, -1, -1)
            emitter.emit()
        }
    }
}
