package ic2_120.client

import ic2_120.content.block.RubberFaceState
import ic2_120.content.block.RubberLogBlock
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext
import net.minecraft.world.level.BlockAndTintGetter
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.RotatedPillarBlock
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.client.resources.model.ModelBaker
import net.minecraft.client.resources.model.ModelState
import net.minecraft.client.resources.model.UnbakedModel
import net.minecraft.client.renderer.block.model.ItemOverrides
import net.minecraft.client.renderer.block.model.ItemTransforms
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.resources.model.Material
import net.minecraft.world.item.ItemStack
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import java.util.function.Function
import java.util.function.Supplier

/**
 * 橡胶树原木动态模型：根据 BlockState 的 rubber_north/south/east/west 在运行时选择每面纹理，
 * 无需 81 个静态模型 JSON 和 243 个 blockstate 变体。
 */
@Suppress("OVERRIDE_DEPRECATION")
class RubberLogModel : UnbakedModel, BakedModel, FabricBakedModel {

    private var sprites: Array<TextureAtlasSprite?> = arrayOfNulls(6)

    override fun getModelDependencies() = emptySet<ResourceLocation>()
    override fun setParents(modelLoader: Function<ResourceLocation, UnbakedModel>) {}

    override fun bake(
        baker: ModelBaker,
        textureGetter: Function<Material, TextureAtlasSprite>,
        rotationContainer: ModelState
    ): BakedModel {
        val atlas = InventoryMenu.BLOCK_ATLAS_TEXTURE
        val tex = { path: String -> Material(atlas, ResourceLocation.of("ic2", "block/resource/$path")) }
        sprites[0] = textureGetter.apply(tex("rubber_wood_wet_front"))
        sprites[1] = textureGetter.apply(tex("rubber_wood_dry_front"))
        sprites[2] = textureGetter.apply(tex("rubber_wood_wet_leftrightback"))
        sprites[3] = textureGetter.apply(tex("rubber_wood_dry_leftrightback"))
        sprites[4] = textureGetter.apply(tex("rubber_wood_wet_bottomtop"))
        sprites[5] = textureGetter.apply(tex("rubber_wood_dry_bottomtop"))
        return this
    }

    private fun spriteFor(face: RubberFaceState, anyWet: Boolean): TextureAtlasSprite = when (face) {
        RubberFaceState.NONE -> if (anyWet) sprites[2]!! else sprites[3]!!
        RubberFaceState.WET -> sprites[0]!!
        RubberFaceState.DRY -> sprites[1]!!
    }

    override fun getQuads(state: BlockState?, face: Direction?, random: RandomSource) = emptyList<net.minecraft.client.renderer.block.model.BakedQuad>()

    override fun useAmbientOcclusion() = true
    override fun hasDepth() = false
    override fun isSideLit() = true
    override fun isBuiltin() = false
    override fun getParticleSprite() = sprites[2]!!
    override fun getTransformation() = ItemTransforms.NONE
    override fun getOverrides() = ItemOverrides.EMPTY

    override fun isVanillaAdapter() = false

    override fun emitBlockQuads(
        blockView: BlockAndTintGetter,
        state: BlockState,
        pos: BlockPos,
        randomSupplier: Supplier<RandomSource>,
        context: RenderContext
    ) {
        if (state.block !is RubberLogBlock) return
        val block = state.block as RubberLogBlock
        val axis = state.get(RotatedPillarBlock.AXIS) ?: Direction.Axis.Y
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

    override fun emitItemQuads(stack: ItemStack?, randomSupplier: Supplier<RandomSource>?, context: RenderContext) {
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
