package ic2_120_advanced_solar_addon.client.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.serialization.MapCodec
import ic2_120.client.renderers.LegacyBlockEntityRenderer26
import ic2_120.client.renderers.LegacyMultiBufferSource26
import ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon
import ic2_120_advanced_solar_addon.content.block.MolecularTransformerBlock
import ic2_120_advanced_solar_addon.content.block.MolecularTransformerBlockEntity
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

/** Uses the placed transformer's geometry for its inventory item. */
class MolecularTransformerItemSpecialRenderer : SpecialModelRenderer<BlockState> {
    companion object {
        private val ID = Identifier.fromNamespaceAndPath(IC2AdvancedSolarAddon.MOD_ID, "molecular_transformer_item")
        fun register() { SpecialModelRenderers.ID_MAPPER.put(ID, Unbaked.MAP_CODEC) }
    }

    class Unbaked : SpecialModelRenderer.Unbaked<BlockState> {
        companion object { val MAP_CODEC: MapCodec<Unbaked> = MapCodec.unit(Unbaked()) }
        override fun bake(context: SpecialModelRenderer.BakingContext) = MolecularTransformerItemSpecialRenderer()
        override fun type(): MapCodec<Unbaked> = MAP_CODEC
    }

    private val renderer = MolecularTransformerBlockEntityRenderer(null)

    override fun extractArgument(stack: ItemStack): BlockState? =
        (stack.item as? BlockItem)?.block?.defaultBlockState()?.setValue(MolecularTransformerBlock.ACTIVE, false)

    override fun getExtents(output: Consumer<Vector3fc>) {
        output.accept(Vector3f(0f, 0f, 0f))
        output.accept(Vector3f(1f, 1f, 1f))
    }

    override fun submit(
        state: BlockState?, poseStack: PoseStack, nodes: SubmitNodeCollector,
        light: Int, overlay: Int, hasFoil: Boolean, outlineColor: Int
    ) {
        if (state == null) return
        val dummy = MolecularTransformerBlockEntity(BlockPos.ZERO, state)
        val captured = LegacyMultiBufferSource26()
        renderer.renderLegacy(dummy, 0f, PoseStack(), captured, light, overlay)
        LegacyBlockEntityRenderer26.submitBatches(captured.finish(), poseStack, nodes)
    }
}
