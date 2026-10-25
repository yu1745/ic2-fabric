package ic2_120.client.renderers

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.serialization.MapCodec
import ic2_120.Ic2_120
import ic2_120.content.block.transmission.TransmissionBlockEntity
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

/** Uses the same shaft/gear geometry for inventory items as for placed block entities. */
class TransmissionItemSpecialRenderer : SpecialModelRenderer<BlockState> {
    companion object {
        private val ID = Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "transmission_item")
        fun register() { SpecialModelRenderers.ID_MAPPER.put(ID, Unbaked.MAP_CODEC) }
    }

    class Unbaked : SpecialModelRenderer.Unbaked<BlockState> {
        companion object { val MAP_CODEC: MapCodec<Unbaked> = MapCodec.unit(Unbaked()) }
        override fun bake(context: SpecialModelRenderer.BakingContext) = TransmissionItemSpecialRenderer()
        override fun type(): MapCodec<Unbaked> = MAP_CODEC
    }

    private val renderer = TransmissionBlockEntityRenderer(null)

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
        val dummy = TransmissionBlockEntity(BlockPos.ZERO, state)
        val captured = LegacyMultiBufferSource26()
        renderer.renderLegacy(dummy, 0f, PoseStack(), captured, light, overlay)
        LegacyBlockEntityRenderer26.submitBatches(captured.finish(), poseStack, nodes)
    }
}
