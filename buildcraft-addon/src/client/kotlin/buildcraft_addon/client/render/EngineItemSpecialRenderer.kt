package buildcraft_addon.client.render

import buildcraft_addon.content.block.CreativeEngineBlock
import buildcraft_addon.content.block.IronEngineBlock
import buildcraft_addon.content.block.RFEngineBlock
import buildcraft_addon.content.block.RedstoneEngineBlock
import buildcraft_addon.content.block.StoneEngineBlock
import buildcraft_addon.content.blockentity.CreativeEngineBlockEntity
import buildcraft_addon.content.blockentity.IronEngineBlockEntity
import buildcraft_addon.content.blockentity.RFEngineBlockEntity
import buildcraft_addon.content.blockentity.RedstoneEngineBlockEntity
import buildcraft_addon.content.blockentity.StoneEngineBlockEntity
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.serialization.MapCodec
import ic2_120.client.renderers.LegacyBlockEntityRenderer26
import ic2_120.client.renderers.LegacyMultiBufferSource26
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.special.SpecialModelRenderer
import net.minecraft.client.renderer.special.SpecialModelRenderers
import net.minecraft.core.BlockPos
import net.minecraft.resources.Identifier
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import org.joml.Vector3f
import org.joml.Vector3fc
import java.util.function.Consumer

/** Renders engine inventory items from the same geometry as their placed block entities. */
class EngineItemSpecialRenderer : SpecialModelRenderer<BlockState> {
    companion object {
        private val ID = Identifier.fromNamespaceAndPath("buildcraft_addon", "engine_item")
        fun register() { SpecialModelRenderers.ID_MAPPER.put(ID, Unbaked.MAP_CODEC) }
    }

    class Unbaked : SpecialModelRenderer.Unbaked<BlockState> {
        companion object { val MAP_CODEC: MapCodec<Unbaked> = MapCodec.unit(Unbaked()) }
        override fun bake(context: SpecialModelRenderer.BakingContext) = EngineItemSpecialRenderer()
        override fun type(): MapCodec<Unbaked> = MAP_CODEC
    }

    private val redstone = RedstoneEngineRenderer(null)
    private val stone = StoneEngineRenderer(null)
    private val iron = IronEngineRenderer(null)
    private val rf = RFEngineRenderer(null)
    private val creative = CreativeEngineRenderer(null)

    override fun extractArgument(stack: ItemStack): BlockState? =
        (stack.item as? BlockItem)?.block?.defaultBlockState()

    override fun getExtents(output: Consumer<Vector3fc>) {
        output.accept(Vector3f(0f, 0f, 0f))
        output.accept(Vector3f(1f, 1f, 1f))
    }

    override fun submit(
        state: BlockState?, poseStack: PoseStack, nodes: SubmitNodeCollector,
        light: Int, overlay: Int, hasFoil: Boolean, outlineColor: Int
    ) {
        if (state == null) return
        val pos = BlockPos.ZERO
        val batches: List<LegacyMultiBufferSource26.Batch> = when (state.block) {
            is RedstoneEngineBlock -> redstone.captureLegacy(RedstoneEngineBlockEntity(pos, state), light, overlay)
            is StoneEngineBlock -> stone.captureLegacy(StoneEngineBlockEntity(pos, state), light, overlay)
            is IronEngineBlock -> iron.captureLegacy(IronEngineBlockEntity(pos, state), light, overlay)
            is RFEngineBlock -> rf.captureLegacy(RFEngineBlockEntity(pos, state), light, overlay)
            is CreativeEngineBlock -> creative.captureLegacy(CreativeEngineBlockEntity(pos, state), light, overlay)
            else -> return
        }
        LegacyBlockEntityRenderer26.submitBatches(batches, poseStack, nodes)
    }
}
