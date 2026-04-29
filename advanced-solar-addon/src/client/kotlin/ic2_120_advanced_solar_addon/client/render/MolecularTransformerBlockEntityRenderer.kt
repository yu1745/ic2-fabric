package ic2_120_advanced_solar_addon.client.render

import ic2_120_advanced_solar_addon.content.block.MolecularTransformerBlock
import ic2_120_advanced_solar_addon.content.block.MolecularTransformerBlockEntity
import net.minecraft.client.render.LightmapTextureManager
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Identifier
import net.minecraft.util.math.RotationAxis

class MolecularTransformerBlockEntityRenderer(
    context: BlockEntityRendererFactory.Context
) : BlockEntityRenderer<MolecularTransformerBlockEntity> {

    companion object {
        private val TEXTURE = Identifier.of("ic2_120_advanced_solar_addon", "textures/models/texturemoleculartransformer.png")
        private val PLAZMA_TEXTURE = Identifier.of("ic2_120_advanced_solar_addon", "textures/models/plazma.png")
        private const val TW = 128f
        private const val TH = 64f

        // Convert model pixel coords to block coords.
        // Original MC 1.12 TESR: translate(x+0.5, y+1.5, z+0.5), rotate(180°, Z), scale(0.0625)
        // Result: bx = 0.5 - px/16, by = 1.5 - py/16, bz = 0.5 + pz/16
        private fun px(px: Float) = 0.5f - px / 16f
        private fun py(py: Float) = 1.5f - py / 16f
        private fun pz(pz: Float) = 0.5f + pz / 16f

        // coreBottom: texOff(0,0), pixel(-5,20,-5)-(5,23,5), size 10x3x10
        private val CORE_BOTTOM = floatArrayOf(px(5f), py(23f), pz(-5f), px(-5f), py(20f), pz(5f))
        // coreWorkZone: texOff(0,44), pixel(-3,12,-3)-(3,21,3), size 6x9x6
        private val CORE_WORK_ZONE = floatArrayOf(px(3f), py(21f), pz(-3f), px(-3f), py(12f), pz(3f))
        // coreTopElectr: texOff(25,44), pixel(-2,8,-1.4667)-(1,10,1.5333), size 3x2x3
        private val CORE_TOP_ELECTR = floatArrayOf(px(1f), py(10f), pz(-1.466667f), px(-2f), py(8f), pz(1.533333f))
        // coreTopPlate: texOff(0,30), pixel(-5,9,-4.5)-(4,12,4.5), size 9x3x9
        private val CORE_TOP_PLATE = floatArrayOf(px(4f), py(12f), pz(-4.5f), px(-5f), py(9f), pz(4.5f))
        // firstElTop: texOff(20,16), pixel(3,8,-5)-(7,11,5), size 4x3x10
        private val EL_TOP = floatArrayOf(px(7f), py(11f), pz(-5f), px(3f), py(8f), pz(5f))
        // firstElBottom: texOff(49,16), pixel(4,19,-3)-(7,24,3), size 3x5x6
        private val EL_BOTTOM = floatArrayOf(px(7f), py(24f), pz(-3f), px(4f), py(19f), pz(3f))
    }

    override fun render(
        entity: MolecularTransformerBlockEntity,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        val state = entity.world?.getBlockState(entity.pos) ?: entity.cachedState
        if (state.block !is MolecularTransformerBlock) return

        val isActive = try { state.get(MolecularTransformerBlock.ACTIVE) } catch (_: Exception) { false }
        val fullLight = LightmapTextureManager.MAX_LIGHT_COORDINATE
        val ov = overlay.takeUnless { it == 0 } ?: OverlayTexture.DEFAULT_UV

        val vc = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE))

        matrices.push()

        // Static model parts (no rotation)
        renderBox(vc, matrices, fullLight, ov, CORE_BOTTOM, 0, 0, 10, 3, 10)
        renderBox(vc, matrices, fullLight, ov, CORE_TOP_ELECTR, 25, 44, 3, 2, 3)
        renderBox(vc, matrices, fullLight, ov, CORE_TOP_PLATE, 0, 30, 9, 3, 9)

        // First electrode (no rotation)
        renderBox(vc, matrices, fullLight, ov, EL_TOP, 20, 16, 4, 3, 10)
        renderBox(vc, matrices, fullLight, ov, EL_BOTTOM, 49, 16, 3, 5, 6)

        // Second electrode: Y rotation -120°
        matrices.push()
        matrices.translate(0.5, 0.5, 0.5)
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-120f))
        matrices.translate(-0.5, -0.5, -0.5)
        renderBox(vc, matrices, fullLight, ov, EL_TOP, 20, 16, 4, 3, 10)
        renderBox(vc, matrices, fullLight, ov, EL_BOTTOM, 49, 16, 3, 5, 6)
        matrices.pop()

        // Third electrode: Y rotation +120°
        matrices.push()
        matrices.translate(0.5, 0.5, 0.5)
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(120f))
        matrices.translate(-0.5, -0.5, -0.5)
        renderBox(vc, matrices, fullLight, ov, EL_TOP, 20, 16, 4, 3, 10)
        renderBox(vc, matrices, fullLight, ov, EL_BOTTOM, 49, 16, 3, 5, 6)
        matrices.pop()

        // Translucent coreWorkZone
        val vcTrans = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE))
        renderBox(vcTrans, matrices, fullLight, ov, CORE_WORK_ZONE, 0, 44, 6, 9, 6)

        matrices.pop()

        // Active core effect
        if (isActive) {
            renderActiveCore(matrices, vertexConsumers, fullLight, ov)
        }
    }

    /**
     * Renders a box (single-sided, noCull handles adjacent block culling).
     * coords = [x1, y1, z1, x2, y2, z2] in block units.
     * texU/texV = texture offset, sizeX/Y/Z = box dimensions for UV calculation.
     * UV layout matches MC 1.12 ModelRenderer:
     *   Top(+Y):   (u+d, v)       size(w,d)
     *   Bottom(-Y):(u+d+w, v)     size(w,d)
     *   North(-Z): (u+d+w, v+d)   size(w,h)
     *   South(+Z): (u+d, v+d)     size(w,h)
     *   West(-X):  (u, v+d)       size(d,h)
     *   East(+X):  (u+d+w+d, v+d) size(d,h)
     *   where d=depth(sizeZ), w=width(sizeX), h=height(sizeY)
     */
    private fun renderBox(
        vc: VertexConsumer, matrices: MatrixStack,
        light: Int, overlay: Int,
        coords: FloatArray,
        texU: Int, texV: Int,
        sizeX: Int, sizeY: Int, sizeZ: Int
    ) {
        val entry = matrices.peek()

        val x1 = coords[0]; val y1 = coords[1]; val z1 = coords[2]
        val x2 = coords[3]; val y2 = coords[4]; val z2 = coords[5]
        val w = sizeX.toFloat(); val h = sizeY.toFloat(); val d = sizeZ.toFloat()

        // Top (+Y)
        quad(vc, entry, light, overlay,
            x1, y2, z2,  x2, y2, z2,  x2, y2, z1,  x1, y2, z1,
            (texU+d)/TW, texV/TH, (texU+d+w)/TW, (texV+d)/TH,
            0f, 1f, 0f)

        // Bottom (-Y)
        quad(vc, entry, light, overlay,
            x1, y1, z1,  x2, y1, z1,  x2, y1, z2,  x1, y1, z2,
            (texU+d+w)/TW, texV/TH, (texU+d+2*w)/TW, (texV+d)/TH,
            0f, -1f, 0f)

        // North (-Z)
        quad(vc, entry, light, overlay,
            x2, y2, z1,  x1, y2, z1,  x1, y1, z1,  x2, y1, z1,
            (texU+d+w)/TW, (texV+d)/TH, (texU+d)/TW, (texV+d+h)/TH,
            0f, 0f, -1f)

        // South (+Z)
        quad(vc, entry, light, overlay,
            x1, y2, z2,  x2, y2, z2,  x2, y1, z2,  x1, y1, z2,
            (texU+d)/TW, (texV+d)/TH, (texU+d+w)/TW, (texV+d+h)/TH,
            0f, 0f, 1f)

        // West (-X)
        quad(vc, entry, light, overlay,
            x1, y2, z1,  x1, y2, z2,  x1, y1, z2,  x1, y1, z1,
            texU/TW, (texV+d)/TH, (texU+d)/TW, (texV+d+h)/TH,
            -1f, 0f, 0f)

        // East (+X)
        quad(vc, entry, light, overlay,
            x2, y2, z2,  x2, y2, z1,  x2, y1, z1,  x2, y1, z2,
            (texU+d+w)/TW, (texV+d)/TH, (texU+d+w+d)/TW, (texV+d+h)/TH,
            1f, 0f, 0f)
    }

    private fun quad(
        vc: VertexConsumer,
        entry: MatrixStack.Entry,
        light: Int, overlay: Int,
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        u0: Float, v0: Float, u1: Float, v1: Float,
        nx: Float, ny: Float, nz: Float
    ) {
        vertex(vc, entry, x0, y0, z0, u0, v0, nx, ny, nz, light, overlay)
        vertex(vc, entry, x1, y1, z1, u1, v0, nx, ny, nz, light, overlay)
        vertex(vc, entry, x2, y2, z2, u1, v1, nx, ny, nz, light, overlay)
        vertex(vc, entry, x3, y3, z3, u0, v1, nx, ny, nz, light, overlay)
    }

    private fun vertex(
        vc: VertexConsumer,
        entry: MatrixStack.Entry,
        x: Float, y: Float, z: Float,
        u: Float, v: Float,
        nx: Float, ny: Float, nz: Float,
        light: Int, overlay: Int
    ) {
        vc.vertex(entry.positionMatrix, x, y, z)
            .color(255, 255, 255, 255)
            .texture(u, v)
            .overlay(overlay)
            .light(light)
            .normal(entry, nx, ny, nz)
    }

    private fun renderActiveCore(
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        val vc = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(PLAZMA_TEXTURE))
        renderBox(vc, matrices, light, overlay,
            floatArrayOf(px(1.5f), py(9f), pz(-1.5f), px(-1.5f), py(5f), pz(1.5f)),
            0, 0, 3, 4, 3)
    }

    override fun rendersOutsideBoundingBox(entity: MolecularTransformerBlockEntity): Boolean = true
}
