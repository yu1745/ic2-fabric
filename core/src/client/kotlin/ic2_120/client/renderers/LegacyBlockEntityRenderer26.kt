package ic2_120.client.renderers

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState
import net.minecraft.client.renderer.feature.ModelFeatureRenderer
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.Vec3

/** Captures custom vertices during extraction and submits immutable geometry in the 26.2 render stage. */
abstract class LegacyBlockEntityRenderer26<T : BlockEntity> : BlockEntityRenderer<T, LegacyBlockEntityRenderer26.State> {
    class State : BlockEntityRenderState() {
        var batches: List<LegacyMultiBufferSource26.Batch> = emptyList()
    }

    protected abstract fun renderLegacy(
        entity: T, partialTicks: Float, poseStack: PoseStack,
        vertexConsumers: LegacyMultiBufferSource26, light: Int, overlay: Int
    )

    /** Capture static item geometry using the same renderer as the placed block entity. */
    fun captureLegacy(entity: T, light: Int, overlay: Int): List<LegacyMultiBufferSource26.Batch> {
        val output = LegacyMultiBufferSource26()
        renderLegacy(entity, 0f, PoseStack(), output, light, overlay)
        return output.finish()
    }

    override fun createRenderState() = State()

    override fun extractRenderState(
        entity: T, state: State, partialTicks: Float, cameraPosition: Vec3,
        breakProgress: ModelFeatureRenderer.CrumblingOverlay?
    ) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress)
        val output = LegacyMultiBufferSource26()
        renderLegacy(entity, partialTicks, PoseStack(), output, state.lightCoords, OverlayTexture.NO_OVERLAY)
        state.batches = output.finish()
    }

    override fun submit(state: State, poseStack: PoseStack, nodes: SubmitNodeCollector, camera: CameraRenderState) {
        submitBatches(state.batches, poseStack, nodes)
    }

    companion object {
        fun submitBatches(batches: List<LegacyMultiBufferSource26.Batch>, poseStack: PoseStack, nodes: SubmitNodeCollector) {
        for (batch in batches) {
            nodes.submitCustomGeometry(poseStack, batch.renderType) { pose, buffer ->
                for (vertex in batch.vertices) {
                    buffer.addVertex(pose, vertex.x, vertex.y, vertex.z)
                        .setColor(vertex.color).setUv(vertex.u, vertex.v)
                        .setOverlay(vertex.overlay).setLight(vertex.light)
                        .setNormal(pose, vertex.nx, vertex.ny, vertex.nz)
                }
            }
        }
        }
    }
}

/** Replaces the removed MultiBufferSource interface for custom block entity geometry. */
class LegacyMultiBufferSource26 {
    data class Vertex(
        val x: Float, val y: Float, val z: Float, val color: Int,
        val u: Float, val v: Float, val overlay: Int, val light: Int,
        val nx: Float, val ny: Float, val nz: Float
    )
    data class Batch(val renderType: RenderType, val vertices: List<Vertex>)

    private val buffers = LinkedHashMap<RenderType, CaptureConsumer>()
    fun getBuffer(type: RenderType): VertexConsumer = buffers.getOrPut(type) { CaptureConsumer() }
    fun finish(): List<Batch> = buffers.map { (type, consumer) -> Batch(type, consumer.finish()) }

    private class CaptureConsumer : VertexConsumer {
        private val vertices = ArrayList<Vertex>()
        private var x = 0f; private var y = 0f; private var z = 0f
        private var color = -1; private var u = 0f; private var v = 0f
        private var overlay = 0; private var light = 15728880
        private var nx = 0f; private var ny = 1f; private var nz = 0f
        private var pending = false
        private fun flush() {
            if (pending) vertices.add(Vertex(x, y, z, color, u, v, overlay, light, nx, ny, nz))
            pending = false
        }
        fun finish(): List<Vertex> { flush(); return vertices.toList() }
        override fun addVertex(x: Float, y: Float, z: Float): VertexConsumer {
            flush(); this.x = x; this.y = y; this.z = z
            color = -1; u = 0f; v = 0f; overlay = 0; light = 15728880
            nx = 0f; ny = 1f; nz = 0f; pending = true
            return this
        }
        override fun setColor(r: Int, g: Int, b: Int, a: Int): VertexConsumer {
            color = ((a and 255) shl 24) or ((r and 255) shl 16) or ((g and 255) shl 8) or (b and 255)
            return this
        }
        override fun setColor(color: Int): VertexConsumer { this.color = color; return this }
        override fun setUv(u: Float, v: Float): VertexConsumer { this.u = u; this.v = v; return this }
        override fun setUv1(u: Int, v: Int): VertexConsumer { overlay = (v shl 16) or (u and 65535); return this }
        override fun setUv2(u: Int, v: Int): VertexConsumer { light = (v shl 16) or (u and 65535); return this }
        override fun setNormal(x: Float, y: Float, z: Float): VertexConsumer { nx = x; ny = y; nz = z; return this }
        override fun setLineWidth(width: Float): VertexConsumer = this
    }
}

typealias MultiBufferSource = LegacyMultiBufferSource26
