package ic2_120.client.renderers

import ic2_120.content.block.pipes.BasePipeBlock
import ic2_120.content.block.pipes.PipeBlockEntity
import ic2_120.content.block.pipes.PipeMaterial
import ic2_120.content.block.pipes.PumpAttachmentBlock
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory
import net.minecraft.client.texture.Sprite
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.fluid.Fluid
import net.minecraft.registry.Registries
import net.minecraft.screen.PlayerScreenHandler
import net.minecraft.state.property.Properties
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import org.joml.Matrix3f
import org.joml.Matrix4f

class PipeBlockEntityRenderer(
    context: BlockEntityRendererFactory.Context
) : BlockEntityRenderer<PipeBlockEntity> {

    companion object {
        private val PIPE_TEXTURE = Identifier.of("ic2_120", "textures/block/transport/pipe_white.png")

        private const val BRONZE_COLOR_R = 133
        private const val BRONZE_COLOR_G = 66
        private const val BRONZE_COLOR_B = 0
        private const val CARBON_COLOR_R = 25
        private const val CARBON_COLOR_G = 25
        private const val CARBON_COLOR_B = 25

        // 法兰宽度（比管道半径多出的部分）
        private const val FLANGE_EXTRA = 1.0f / 16.0f
        // 法兰厚度
        private const val FLANGE_THICKNESS = 1.0f / 16.0f
        // 观察窗边框厚度（不使用半透明，几何留孔）
        private const val WINDOW_FRAME = 1.0f / 16.0f
        // 流体与管壁/玻璃的安全间距，避免 Z-fighting 闪烁
        private const val FLUID_WALL_GAP = 0.9f / 16.0f
        // 流体核心与延申臂连接重叠，避免分段渲染产生细缝
        private const val FLUID_JOIN_OVERLAP = 0.015f / 16.0f
        // 跨方块边界重叠，避免两根相邻管道在边界处出现断缝
        private const val FLUID_BLOCK_JOIN_OVERLAP = 0.04f / 16.0f
        // 观察窗玻璃厚度（很薄，避免厚重）
        // 玻璃窗基底厚度；增大更有实体感，过大可能更容易与流体/骨架产生层叠感。
        private const val GLASS_THICKNESS = 0.35f / 16.0f
        // 玻璃窗相对边框的内缩偏移；增大可减少边缘穿插/闪烁，但会缩小可视窗口。
        private const val GLASS_INSET = 0.001f
        // 玻璃基底颜色（RGB）；提高可让玻璃更“冷白”，降低则更暗/更偏灰。
        private const val GLASS_COLOR_R = 220
        private const val GLASS_COLOR_G = 242
        private const val GLASS_COLOR_B = 255
        // 玻璃基底透明度；值越低越接近“几乎不可见”的原版玻璃观感。
        private const val GLASS_ALPHA = 30
        // 颗粒层透明度；值越高颗粒感越明显，但过高会遮挡流体细节。
        private const val GLASS_PARTICLE_ALPHA = 48
        // 颗粒层颜色（RGB）；用于控制颗粒偏蓝/偏灰的质感。
        private const val GLASS_PARTICLE_R = 176
        private const val GLASS_PARTICLE_G = 206
        private const val GLASS_PARTICLE_B = 228
        // 单颗粒斑点尺寸范围（位于玻璃表面平面内）。
        private const val GLASS_PARTICLE_MIN = 0.10f / 16.0f
        private const val GLASS_PARTICLE_MAX = 0.26f / 16.0f
        // 斑点法线方向厚度（非常薄），避免“正反体”体积感。
        private const val GLASS_PARTICLE_DEPTH = 0.045f / 16.0f
        // 颗粒层相对基础玻璃的内缩增量；增大可减少与基础玻璃重叠导致的闪烁。
        private const val GLASS_PARTICLE_INSET_EXTRA = 0.0008f
        // 颗粒层厚度比例；越大颗粒分布的纵深越厚。
        private const val GLASS_PARTICLE_THICKNESS_SCALE = 0.7f
        // 颗粒密度系数与上下限。
        private const val GLASS_PARTICLE_DENSITY_SCALE = 22.0f
        private const val GLASS_PARTICLE_DENSITY_MIN = 4
        private const val GLASS_PARTICLE_DENSITY_MAX = 20
        // 颗粒透明度随机波动范围（基于 GLASS_PARTICLE_ALPHA 的倍率）。
        private const val GLASS_PARTICLE_ALPHA_VARIATION_MIN = 0.72f
        private const val GLASS_PARTICLE_ALPHA_VARIATION_MAX = 1.0f

        private val WHITE_TEXTURE = Identifier("textures/misc/white.png")
    }

    init {
        @Suppress("UNUSED_VARIABLE")
        val ignored = context
    }

    override fun render(
        entity: PipeBlockEntity,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        val state = entity.cachedState
        if (!state.get(BasePipeBlock.TRANSPARENT)) return

        val block = state.block as? BasePipeBlock ?: return
        val world = entity.world ?: return

        val radius = block.size.radius.toFloat()
        val min = 0.5f - radius
        val max = 0.5f + radius

        val (matR, matG, matB) = when (block.material) {
            PipeMaterial.BRONZE -> Triple(BRONZE_COLOR_R, BRONZE_COLOR_G, BRONZE_COLOR_B)
            PipeMaterial.CARBON -> Triple(CARBON_COLOR_R, CARBON_COLOR_G, CARBON_COLOR_B)
        }

        // 解析流体
        val fluid = entity.currentFluidId?.let { idStr ->
            Identifier.tryParse(idStr)?.let { id ->
                if (Registries.FLUID.containsId(id)) Registries.FLUID.get(id) else null
            }
        }

        // --- 渲染管道外壳（Cutout，不使用半透明） ---
        // pipe_white 贴图上的透明区域即“观察窗”，只有这些区域能看到内部流体。
        val shellVc = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(PIPE_TEXTURE))
        val entry = matrices.peek()
        val pos = entry.positionMatrix
        val norm = entry.normalMatrix

        // 连续四角骨架：从截面看仅四角不透明，其余全部留空。
        drawContinuousCornerShell(
            state, pos, norm, shellVc, light, overlay,
            min, max, matR, matG, matB
        )
        if (block is PumpAttachmentBlock && state.contains(Properties.FACING)) {
            drawPumpAttachmentPlate(
                pos, norm, shellVc, light, overlay,
                state.get(Properties.FACING),
                matR, matG, matB
            )
        }

        // --- 渲染内部流体 ---
        if (fluid != null) {
            val fluidSprites = FluidRenderHandlerRegistry.INSTANCE
                .get(fluid)
                ?.getFluidSprites(world, entity.pos, fluid.defaultState)
            val fluidSprite = fluidSprites?.firstOrNull()
            val fluidColor = getFluidTintColor(fluid)
            val fluidVc = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE))

            val inset = FLUID_WALL_GAP
            val fMin = min + inset
            val fMax = max - inset

            if (fluidSprite != null) {
                // 核心（流体贴图）
                drawCuboidFluid(pos, norm, fluidVc, light, overlay,
                    fMin, fMin, fMin, fMax, fMax, fMax,
                    fluidSprite, fluidColor)

                // 臂（流体贴图）
                for (dir in Direction.entries) {
                    if (!state.get(BasePipeBlock.propertyFor(dir))) continue
                    drawArmFluid(pos, norm, fluidVc, light, overlay,
                        radius, inset, dir, fluidSprite, fluidColor, fMin, fMax)
                }
            }
        }

        // --- 渲染观察窗玻璃（纯代码着色，不使用图案贴图） ---
        val glassVc = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(WHITE_TEXTURE))
        val connectionCount = Direction.entries.count { state.get(BasePipeBlock.propertyFor(it)) }
        val cutCenter = connectionCount > 2
        drawWindowGlass(
            state, pos, norm, glassVc, light, overlay, min, max, cutCenter,
            GLASS_INSET, GLASS_THICKNESS, GLASS_COLOR_R, GLASS_COLOR_G, GLASS_COLOR_B, GLASS_ALPHA
        )
        drawWindowGlassParticles(
            state, pos, norm, glassVc, light, overlay, min, max, cutCenter,
            GLASS_INSET + GLASS_PARTICLE_INSET_EXTRA, GLASS_THICKNESS * GLASS_PARTICLE_THICKNESS_SCALE,
            GLASS_PARTICLE_R, GLASS_PARTICLE_G, GLASS_PARTICLE_B, GLASS_PARTICLE_ALPHA
        )

        // 透明版本不再绘制法兰，避免打断观察窗连续性。
    }

    // ========== 管壁渲染 ==========

    private fun drawContinuousCornerShell(
        state: net.minecraft.block.BlockState,
        pos: Matrix4f, norm: Matrix3f, vc: VertexConsumer,
        light: Int, overlay: Int,
        min: Float, max: Float,
        r: Int, g: Int, b: Int
    ) {
        val activeX = state.get(BasePipeBlock.WEST) || state.get(BasePipeBlock.EAST)
        val activeY = state.get(BasePipeBlock.DOWN) || state.get(BasePipeBlock.UP)
        val activeZ = state.get(BasePipeBlock.NORTH) || state.get(BasePipeBlock.SOUTH)
        val hasAnyAxis = activeX || activeY || activeZ

        if (activeX || !hasAnyAxis) {
            drawCornerRibsOnAxis(
                axis = Direction.Axis.X,
                from = if (state.get(BasePipeBlock.WEST)) 0.0f else min,
                to = if (state.get(BasePipeBlock.EAST)) 1.0f else max,
                min = min,
                max = max,
                pos = pos,
                norm = norm,
                vc = vc,
                light = light,
                overlay = overlay,
                r = r, g = g, b = b
            )
        }
        if (activeY || !hasAnyAxis) {
            drawCornerRibsOnAxis(
                axis = Direction.Axis.Y,
                from = if (state.get(BasePipeBlock.DOWN)) 0.0f else min,
                to = if (state.get(BasePipeBlock.UP)) 1.0f else max,
                min = min,
                max = max,
                pos = pos,
                norm = norm,
                vc = vc,
                light = light,
                overlay = overlay,
                r = r, g = g, b = b
            )
        }
        if (activeZ || !hasAnyAxis) {
            drawCornerRibsOnAxis(
                axis = Direction.Axis.Z,
                from = if (state.get(BasePipeBlock.NORTH)) 0.0f else min,
                to = if (state.get(BasePipeBlock.SOUTH)) 1.0f else max,
                min = min,
                max = max,
                pos = pos,
                norm = norm,
                vc = vc,
                light = light,
                overlay = overlay,
                r = r, g = g, b = b
            )
        }
    }

    private fun drawPumpAttachmentPlate(
        pos: Matrix4f,
        norm: Matrix3f,
        vc: VertexConsumer,
        light: Int,
        overlay: Int,
        facing: Direction,
        r: Int,
        g: Int,
        b: Int
    ) {
        val min = 2.0f / 16.0f
        val max = 14.0f / 16.0f
        val t = 2.0f / 16.0f
        when (facing) {
            Direction.NORTH -> drawCuboidColor(pos, norm, vc, light, overlay, min, min, 0.0f, max, max, t, r, g, b, 255)
            Direction.SOUTH -> drawCuboidColor(pos, norm, vc, light, overlay, min, min, 1.0f - t, max, max, 1.0f, r, g, b, 255)
            Direction.WEST -> drawCuboidColor(pos, norm, vc, light, overlay, 0.0f, min, min, t, max, max, r, g, b, 255)
            Direction.EAST -> drawCuboidColor(pos, norm, vc, light, overlay, 1.0f - t, min, min, 1.0f, max, max, r, g, b, 255)
            Direction.DOWN -> drawCuboidColor(pos, norm, vc, light, overlay, min, 0.0f, min, max, t, max, r, g, b, 255)
            Direction.UP -> drawCuboidColor(pos, norm, vc, light, overlay, min, 1.0f - t, min, max, 1.0f, max, r, g, b, 255)
        }
    }

    private fun drawWindowGlass(
        state: net.minecraft.block.BlockState,
        pos: Matrix4f, norm: Matrix3f, vc: VertexConsumer,
        light: Int, overlay: Int,
        min: Float, max: Float,
        cutCenter: Boolean,
        paneInset: Float,
        paneThickness: Float,
        paneR: Int,
        paneG: Int,
        paneB: Int,
        paneA: Int
    ) {
        val activeX = state.get(BasePipeBlock.WEST) || state.get(BasePipeBlock.EAST)
        val activeY = state.get(BasePipeBlock.DOWN) || state.get(BasePipeBlock.UP)
        val activeZ = state.get(BasePipeBlock.NORTH) || state.get(BasePipeBlock.SOUTH)
        val hasAnyAxis = activeX || activeY || activeZ

        if (activeX || !hasAnyAxis) {
            drawGlassOnAxis(
                axis = Direction.Axis.X,
                from = if (state.get(BasePipeBlock.WEST)) 0.0f else min,
                to = if (state.get(BasePipeBlock.EAST)) 1.0f else max,
                min = min,
                max = max,
                pos = pos,
                norm = norm,
                vc = vc,
                light = light,
                overlay = overlay,
                cutCenter = cutCenter,
                paneInset = paneInset,
                paneThickness = paneThickness,
                paneR = paneR,
                paneG = paneG,
                paneB = paneB,
                paneA = paneA
            )
        }
        if (activeY || !hasAnyAxis) {
            drawGlassOnAxis(
                axis = Direction.Axis.Y,
                from = if (state.get(BasePipeBlock.DOWN)) 0.0f else min,
                to = if (state.get(BasePipeBlock.UP)) 1.0f else max,
                min = min,
                max = max,
                pos = pos,
                norm = norm,
                vc = vc,
                light = light,
                overlay = overlay,
                cutCenter = cutCenter,
                paneInset = paneInset,
                paneThickness = paneThickness,
                paneR = paneR,
                paneG = paneG,
                paneB = paneB,
                paneA = paneA
            )
        }
        if (activeZ || !hasAnyAxis) {
            drawGlassOnAxis(
                axis = Direction.Axis.Z,
                from = if (state.get(BasePipeBlock.NORTH)) 0.0f else min,
                to = if (state.get(BasePipeBlock.SOUTH)) 1.0f else max,
                min = min,
                max = max,
                pos = pos,
                norm = norm,
                vc = vc,
                light = light,
                overlay = overlay,
                cutCenter = cutCenter,
                paneInset = paneInset,
                paneThickness = paneThickness,
                paneR = paneR,
                paneG = paneG,
                paneB = paneB,
                paneA = paneA
            )
        }
    }

    private fun drawGlassOnAxis(
        axis: Direction.Axis,
        from: Float,
        to: Float,
        min: Float,
        max: Float,
        pos: Matrix4f,
        norm: Matrix3f,
        vc: VertexConsumer,
        light: Int,
        overlay: Int,
        cutCenter: Boolean,
        paneInset: Float,
        paneThickness: Float,
        paneR: Int,
        paneG: Int,
        paneB: Int,
        paneA: Int
    ) {
        if (to <= from) return
        val edge = WINDOW_FRAME.coerceAtMost((max - min) * 0.5f - 0.0001f)
        if (edge <= 0.0f) return
        val lo2 = min + edge + paneInset
        val hi2 = max - edge - paneInset
        if (hi2 <= lo2) return
        val from2 = from + paneInset
        val to2 = to - paneInset
        if (to2 <= from2) return
        val segments = mutableListOf<Pair<Float, Float>>()
        if (cutCenter) {
            if (from2 < min) segments.add(from2 to min - paneInset)
            if (to2 > max) segments.add(max + paneInset to to2)
            if (segments.isEmpty()) return
        } else {
            segments.add(from2 to to2)
        }

        for ((segFrom, segTo) in segments) {
            if (segTo <= segFrom) continue
            drawGlassSegment(
                axis, segFrom, segTo, min, max, lo2, hi2, pos, norm, vc, light, overlay,
                paneThickness, paneR, paneG, paneB, paneA
            )
        }
    }

    private fun drawGlassSegment(
        axis: Direction.Axis,
        from: Float,
        to: Float,
        min: Float,
        max: Float,
        lo2: Float,
        hi2: Float,
        pos: Matrix4f,
        norm: Matrix3f,
        vc: VertexConsumer,
        light: Int,
        overlay: Int,
        paneThickness: Float,
        paneR: Int,
        paneG: Int,
        paneB: Int,
        paneA: Int
    ) {
        val t = paneThickness
        when (axis) {
            Direction.Axis.X -> {
                drawCuboidColor(pos, norm, vc, light, overlay, from, min, lo2, to, min + t, hi2, paneR, paneG, paneB, paneA)
                drawCuboidColor(pos, norm, vc, light, overlay, from, max - t, lo2, to, max, hi2, paneR, paneG, paneB, paneA)
                drawCuboidColor(pos, norm, vc, light, overlay, from, lo2, min, to, hi2, min + t, paneR, paneG, paneB, paneA)
                drawCuboidColor(pos, norm, vc, light, overlay, from, lo2, max - t, to, hi2, max, paneR, paneG, paneB, paneA)
            }
            Direction.Axis.Y -> {
                drawCuboidColor(pos, norm, vc, light, overlay, min, from, lo2, min + t, to, hi2, paneR, paneG, paneB, paneA)
                drawCuboidColor(pos, norm, vc, light, overlay, max - t, from, lo2, max, to, hi2, paneR, paneG, paneB, paneA)
                drawCuboidColor(pos, norm, vc, light, overlay, lo2, from, min, hi2, to, min + t, paneR, paneG, paneB, paneA)
                drawCuboidColor(pos, norm, vc, light, overlay, lo2, from, max - t, hi2, to, max, paneR, paneG, paneB, paneA)
            }
            Direction.Axis.Z -> {
                drawCuboidColor(pos, norm, vc, light, overlay, min, lo2, from, min + t, hi2, to, paneR, paneG, paneB, paneA)
                drawCuboidColor(pos, norm, vc, light, overlay, max - t, lo2, from, max, hi2, to, paneR, paneG, paneB, paneA)
                drawCuboidColor(pos, norm, vc, light, overlay, lo2, min, from, hi2, min + t, to, paneR, paneG, paneB, paneA)
                drawCuboidColor(pos, norm, vc, light, overlay, lo2, max - t, from, hi2, max, to, paneR, paneG, paneB, paneA)
            }
        }
    }

    private fun drawWindowGlassParticles(
        state: net.minecraft.block.BlockState,
        pos: Matrix4f, norm: Matrix3f, vc: VertexConsumer,
        light: Int, overlay: Int,
        min: Float, max: Float,
        cutCenter: Boolean,
        paneInset: Float,
        paneThickness: Float,
        particleR: Int,
        particleG: Int,
        particleB: Int,
        particleA: Int
    ) {
        val activeX = state.get(BasePipeBlock.WEST) || state.get(BasePipeBlock.EAST)
        val activeY = state.get(BasePipeBlock.DOWN) || state.get(BasePipeBlock.UP)
        val activeZ = state.get(BasePipeBlock.NORTH) || state.get(BasePipeBlock.SOUTH)
        val hasAnyAxis = activeX || activeY || activeZ

        if (activeX || !hasAnyAxis) {
            drawParticlesOnAxis(
                Direction.Axis.X,
                if (state.get(BasePipeBlock.WEST)) 0.0f else min,
                if (state.get(BasePipeBlock.EAST)) 1.0f else max,
                min, max, cutCenter, paneInset, paneThickness,
                pos, norm, vc, light, overlay, particleR, particleG, particleB, particleA
            )
        }
        if (activeY || !hasAnyAxis) {
            drawParticlesOnAxis(
                Direction.Axis.Y,
                if (state.get(BasePipeBlock.DOWN)) 0.0f else min,
                if (state.get(BasePipeBlock.UP)) 1.0f else max,
                min, max, cutCenter, paneInset, paneThickness,
                pos, norm, vc, light, overlay, particleR, particleG, particleB, particleA
            )
        }
        if (activeZ || !hasAnyAxis) {
            drawParticlesOnAxis(
                Direction.Axis.Z,
                if (state.get(BasePipeBlock.NORTH)) 0.0f else min,
                if (state.get(BasePipeBlock.SOUTH)) 1.0f else max,
                min, max, cutCenter, paneInset, paneThickness,
                pos, norm, vc, light, overlay, particleR, particleG, particleB, particleA
            )
        }
    }

    private fun drawParticlesOnAxis(
        axis: Direction.Axis,
        from: Float,
        to: Float,
        min: Float,
        max: Float,
        cutCenter: Boolean,
        paneInset: Float,
        paneThickness: Float,
        pos: Matrix4f,
        norm: Matrix3f,
        vc: VertexConsumer,
        light: Int,
        overlay: Int,
        particleR: Int,
        particleG: Int,
        particleB: Int,
        particleA: Int
    ) {
        if (to <= from) return
        val edge = WINDOW_FRAME.coerceAtMost((max - min) * 0.5f - 0.0001f)
        if (edge <= 0.0f) return
        val lo2 = min + edge + paneInset
        val hi2 = max - edge - paneInset
        if (hi2 <= lo2) return
        val from2 = from + paneInset
        val to2 = to - paneInset
        if (to2 <= from2) return

        val segments = mutableListOf<Pair<Float, Float>>()
        if (cutCenter) {
            if (from2 < min) segments.add(from2 to min - paneInset)
            if (to2 > max) segments.add(max + paneInset to to2)
        } else {
            segments.add(from2 to to2)
        }

        for ((segFrom, segTo) in segments) {
            if (segTo <= segFrom) continue
            drawParticlesForGlassSegment(
                axis, segFrom, segTo, min, max, lo2, hi2, paneThickness,
                pos, norm, vc, light, overlay, particleR, particleG, particleB, particleA
            )
        }
    }

    private fun drawParticlesForGlassSegment(
        axis: Direction.Axis,
        from: Float,
        to: Float,
        min: Float,
        max: Float,
        lo2: Float,
        hi2: Float,
        paneThickness: Float,
        pos: Matrix4f,
        norm: Matrix3f,
        vc: VertexConsumer,
        light: Int,
        overlay: Int,
        particleR: Int,
        particleG: Int,
        particleB: Int,
        particleA: Int
    ) {
        val density = ((to - from) * GLASS_PARTICLE_DENSITY_SCALE).toInt()
            .coerceIn(GLASS_PARTICLE_DENSITY_MIN, GLASS_PARTICLE_DENSITY_MAX)
        when (axis) {
            Direction.Axis.X -> {
                drawParticleSpotsOnPane(pos, norm, vc, light, overlay, from, min, lo2, to, min + paneThickness, hi2, Direction.Axis.Y, density, particleR, particleG, particleB, particleA, 0x31A2)
                drawParticleSpotsOnPane(pos, norm, vc, light, overlay, from, max - paneThickness, lo2, to, max, hi2, Direction.Axis.Y, density, particleR, particleG, particleB, particleA, 0x31A3)
                drawParticleSpotsOnPane(pos, norm, vc, light, overlay, from, lo2, min, to, hi2, min + paneThickness, Direction.Axis.Z, density, particleR, particleG, particleB, particleA, 0x31A4)
                drawParticleSpotsOnPane(pos, norm, vc, light, overlay, from, lo2, max - paneThickness, to, hi2, max, Direction.Axis.Z, density, particleR, particleG, particleB, particleA, 0x31A5)
            }
            Direction.Axis.Y -> {
                drawParticleSpotsOnPane(pos, norm, vc, light, overlay, min, from, lo2, min + paneThickness, to, hi2, Direction.Axis.X, density, particleR, particleG, particleB, particleA, 0x42B2)
                drawParticleSpotsOnPane(pos, norm, vc, light, overlay, max - paneThickness, from, lo2, max, to, hi2, Direction.Axis.X, density, particleR, particleG, particleB, particleA, 0x42B3)
                drawParticleSpotsOnPane(pos, norm, vc, light, overlay, lo2, from, min, hi2, to, min + paneThickness, Direction.Axis.Z, density, particleR, particleG, particleB, particleA, 0x42B4)
                drawParticleSpotsOnPane(pos, norm, vc, light, overlay, lo2, from, max - paneThickness, hi2, to, max, Direction.Axis.Z, density, particleR, particleG, particleB, particleA, 0x42B5)
            }
            Direction.Axis.Z -> {
                drawParticleSpotsOnPane(pos, norm, vc, light, overlay, min, lo2, from, min + paneThickness, hi2, to, Direction.Axis.X, density, particleR, particleG, particleB, particleA, 0x53C2)
                drawParticleSpotsOnPane(pos, norm, vc, light, overlay, max - paneThickness, lo2, from, max, hi2, to, Direction.Axis.X, density, particleR, particleG, particleB, particleA, 0x53C3)
                drawParticleSpotsOnPane(pos, norm, vc, light, overlay, lo2, min, from, hi2, min + paneThickness, to, Direction.Axis.Y, density, particleR, particleG, particleB, particleA, 0x53C4)
                drawParticleSpotsOnPane(pos, norm, vc, light, overlay, lo2, max - paneThickness, from, hi2, max, to, Direction.Axis.Y, density, particleR, particleG, particleB, particleA, 0x53C5)
            }
        }
    }

    private fun drawParticleSpotsOnPane(
        pos: Matrix4f,
        norm: Matrix3f,
        vc: VertexConsumer,
        light: Int,
        overlay: Int,
        minX: Float,
        minY: Float,
        minZ: Float,
        maxX: Float,
        maxY: Float,
        maxZ: Float,
        normalAxis: Direction.Axis,
        count: Int,
        r: Int,
        g: Int,
        b: Int,
        alpha: Int,
        seedBase: Int
    ) {
        if (maxX <= minX || maxY <= minY || maxZ <= minZ) return
        repeat(count) { i ->
            val seed0 = seedBase xor (i * -0x61C88647)
            val sizeU = GLASS_PARTICLE_MIN + (GLASS_PARTICLE_MAX - GLASS_PARTICLE_MIN) * hashToUnit(seed0 xor 0x13579B)
            val sizeV = GLASS_PARTICLE_MIN + (GLASS_PARTICLE_MAX - GLASS_PARTICLE_MIN) * hashToUnit(seed0 xor 0x97531A)
            val depth = GLASS_PARTICLE_DEPTH

            var x0: Float
            var y0: Float
            var z0: Float
            var x1: Float
            var y1: Float
            var z1: Float
            when (normalAxis) {
                Direction.Axis.X -> {
                    val hx = ((maxX - minX) * 0.5f).coerceAtMost(depth * 0.5f)
                    val hy = sizeU * 0.5f
                    val hz = sizeV * 0.5f
                    val cx = (minX + maxX) * 0.5f
                    val cy = lerp(minY + hy, maxY - hy, hashToUnit(seed0 xor 0x2468AC))
                    val cz = lerp(minZ + hz, maxZ - hz, hashToUnit(seed0 xor 0x55AA11))
                    x0 = cx - hx; y0 = cy - hy; z0 = cz - hz
                    x1 = cx + hx; y1 = cy + hy; z1 = cz + hz
                }
                Direction.Axis.Y -> {
                    val hx = sizeU * 0.5f
                    val hy = ((maxY - minY) * 0.5f).coerceAtMost(depth * 0.5f)
                    val hz = sizeV * 0.5f
                    val cx = lerp(minX + hx, maxX - hx, hashToUnit(seed0 xor 0x2468AC))
                    val cy = (minY + maxY) * 0.5f
                    val cz = lerp(minZ + hz, maxZ - hz, hashToUnit(seed0 xor 0x55AA11))
                    x0 = cx - hx; y0 = cy - hy; z0 = cz - hz
                    x1 = cx + hx; y1 = cy + hy; z1 = cz + hz
                }
                Direction.Axis.Z -> {
                    val hx = sizeU * 0.5f
                    val hy = sizeV * 0.5f
                    val hz = ((maxZ - minZ) * 0.5f).coerceAtMost(depth * 0.5f)
                    val cx = lerp(minX + hx, maxX - hx, hashToUnit(seed0 xor 0x2468AC))
                    val cy = lerp(minY + hy, maxY - hy, hashToUnit(seed0 xor 0x55AA11))
                    val cz = (minZ + maxZ) * 0.5f
                    x0 = cx - hx; y0 = cy - hy; z0 = cz - hz
                    x1 = cx + hx; y1 = cy + hy; z1 = cz + hz
                }
            }
            val alphaRange = GLASS_PARTICLE_ALPHA_VARIATION_MAX - GLASS_PARTICLE_ALPHA_VARIATION_MIN
            val a = (alpha * (GLASS_PARTICLE_ALPHA_VARIATION_MIN + hashToUnit(seed0 xor 0x998877) * alphaRange))
                .toInt()
                .coerceIn(8, 255)
            drawCuboidColor(pos, norm, vc, light, overlay, x0, y0, z0, x1, y1, z1, r, g, b, a)
        }
    }

    private fun hashToUnit(seed: Int): Float {
        var x = seed
        x = x xor (x ushr 16)
        x *= -0x7A143595
        x = x xor (x ushr 13)
        x *= -0x3D4D51CB
        x = x xor (x ushr 16)
        return ((x and 0x7fffffff).toFloat() / Int.MAX_VALUE.toFloat()).coerceIn(0.0f, 1.0f)
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    // ========== 流体渲染 ==========

    private fun drawArmFluid(
        pos: Matrix4f, norm: Matrix3f, vc: VertexConsumer,
        light: Int, overlay: Int,
        radius: Float, inset: Float, dir: Direction,
        sprite: Sprite, color: Int,
        coreMin: Float, coreMax: Float
    ) {
        val min = 0.5f - radius + inset
        val max = 0.5f + radius - inset
        val joinMin = (coreMin - FLUID_JOIN_OVERLAP).coerceAtLeast(0.0f)
        val joinMax = (coreMax + FLUID_JOIN_OVERLAP).coerceAtMost(1.0f)
        val edgeMin = -FLUID_BLOCK_JOIN_OVERLAP
        val edgeMax = 1.0f + FLUID_BLOCK_JOIN_OVERLAP
        when (dir) {
            Direction.NORTH -> drawCuboidFluid(pos, norm, vc, light, overlay, min, min, edgeMin, max, max, joinMin, sprite, color)
            Direction.SOUTH -> drawCuboidFluid(pos, norm, vc, light, overlay, min, min, joinMax, max, max, edgeMax, sprite, color)
            Direction.WEST -> drawCuboidFluid(pos, norm, vc, light, overlay, edgeMin, min, min, joinMin, max, max, sprite, color)
            Direction.EAST -> drawCuboidFluid(pos, norm, vc, light, overlay, joinMax, min, min, edgeMax, max, max, sprite, color)
            Direction.DOWN -> drawCuboidFluid(pos, norm, vc, light, overlay, min, edgeMin, min, max, joinMin, max, sprite, color)
            Direction.UP -> drawCuboidFluid(pos, norm, vc, light, overlay, min, joinMax, min, max, edgeMax, max, sprite, color)
        }
    }

    // ========== 通用几何体绘制 ==========

    private fun drawCuboidColor(
        pos: Matrix4f, norm: Matrix3f, vc: VertexConsumer,
        light: Int, overlay: Int,
        minX: Float, minY: Float, minZ: Float,
        maxX: Float, maxY: Float, maxZ: Float,
        r: Int, g: Int, b: Int, a: Int
    ) {
        // South (+Z)
        quadColor(vc, pos, norm, light, overlay,
            minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ,
            0f, 0f, 1f, r, g, b, a)
        // North (-Z)
        quadColor(vc, pos, norm, light, overlay,
            maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ,
            0f, 0f, -1f, r, g, b, a)
        // West (-X)
        quadColor(vc, pos, norm, light, overlay,
            minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ,
            -1f, 0f, 0f, r, g, b, a)
        // East (+X)
        quadColor(vc, pos, norm, light, overlay,
            maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ,
            1f, 0f, 0f, r, g, b, a)
        // Up (+Y)
        quadColor(vc, pos, norm, light, overlay,
            minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ,
            0f, 1f, 0f, r, g, b, a)
        // Down (-Y)
        quadColor(vc, pos, norm, light, overlay,
            minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ,
            0f, -1f, 0f, r, g, b, a)
    }

    private fun drawCuboidWindowFrame(
        pos: Matrix4f, norm: Matrix3f, vc: VertexConsumer,
        light: Int, overlay: Int,
        minX: Float, minY: Float, minZ: Float,
        maxX: Float, maxY: Float, maxZ: Float,
        r: Int, g: Int, b: Int, a: Int
    ) {
        val widthX = maxX - minX
        val widthY = maxY - minY
        val widthZ = maxZ - minZ
        val fx = WINDOW_FRAME.coerceAtMost(widthX * 0.5f - 0.0001f)
        val fy = WINDOW_FRAME.coerceAtMost(widthY * 0.5f - 0.0001f)
        val fz = WINDOW_FRAME.coerceAtMost(widthZ * 0.5f - 0.0001f)

        if (fx <= 0.0f || fy <= 0.0f || fz <= 0.0f) {
            drawCuboidColor(pos, norm, vc, light, overlay, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, a)
            return
        }

        // South (+Z)
        drawFaceFrameXY(pos, norm, vc, light, overlay, minX, minY, maxX, maxY, maxZ, fx, fy, 1f, r, g, b, a)
        // North (-Z)
        drawFaceFrameXY(pos, norm, vc, light, overlay, minX, minY, maxX, maxY, minZ, fx, fy, -1f, r, g, b, a)
        // West (-X)
        drawFaceFrameYZ(pos, norm, vc, light, overlay, minY, minZ, maxY, maxZ, minX, fy, fz, -1f, r, g, b, a)
        // East (+X)
        drawFaceFrameYZ(pos, norm, vc, light, overlay, minY, minZ, maxY, maxZ, maxX, fy, fz, 1f, r, g, b, a)
        // Up (+Y)
        drawFaceFrameXZ(pos, norm, vc, light, overlay, minX, minZ, maxX, maxZ, maxY, fx, fz, 1f, r, g, b, a)
        // Down (-Y)
        drawFaceFrameXZ(pos, norm, vc, light, overlay, minX, minZ, maxX, maxZ, minY, fx, fz, -1f, r, g, b, a)
    }

    private fun drawCornerRibsOnAxis(
        axis: Direction.Axis,
        from: Float,
        to: Float,
        min: Float,
        max: Float,
        pos: Matrix4f,
        norm: Matrix3f,
        vc: VertexConsumer,
        light: Int,
        overlay: Int,
        r: Int,
        g: Int,
        b: Int
    ) {
        if (to <= from) return
        val edge = WINDOW_FRAME.coerceAtMost((max - min) * 0.5f - 0.0001f)
        if (edge <= 0.0f) return
        val lo = min
        val hi = max
        val lo2 = min + edge
        val hi2 = max - edge
        when (axis) {
            Direction.Axis.X -> {
                drawCuboidColor(pos, norm, vc, light, overlay, from, lo, lo, to, lo2, lo2, r, g, b, 255)
                drawCuboidColor(pos, norm, vc, light, overlay, from, lo, hi2, to, lo2, hi, r, g, b, 255)
                drawCuboidColor(pos, norm, vc, light, overlay, from, hi2, lo, to, hi, lo2, r, g, b, 255)
                drawCuboidColor(pos, norm, vc, light, overlay, from, hi2, hi2, to, hi, hi, r, g, b, 255)
            }
            Direction.Axis.Y -> {
                drawCuboidColor(pos, norm, vc, light, overlay, lo, from, lo, lo2, to, lo2, r, g, b, 255)
                drawCuboidColor(pos, norm, vc, light, overlay, lo, from, hi2, lo2, to, hi, r, g, b, 255)
                drawCuboidColor(pos, norm, vc, light, overlay, hi2, from, lo, hi, to, lo2, r, g, b, 255)
                drawCuboidColor(pos, norm, vc, light, overlay, hi2, from, hi2, hi, to, hi, r, g, b, 255)
            }
            Direction.Axis.Z -> {
                drawCuboidColor(pos, norm, vc, light, overlay, lo, lo, from, lo2, lo2, to, r, g, b, 255)
                drawCuboidColor(pos, norm, vc, light, overlay, lo, hi2, from, lo2, hi, to, r, g, b, 255)
                drawCuboidColor(pos, norm, vc, light, overlay, hi2, lo, from, hi, lo2, to, r, g, b, 255)
                drawCuboidColor(pos, norm, vc, light, overlay, hi2, hi2, from, hi, hi, to, r, g, b, 255)
            }
        }
    }

    private fun drawFaceFrameXY(
        pos: Matrix4f, norm: Matrix3f, vc: VertexConsumer,
        light: Int, overlay: Int,
        minX: Float, minY: Float, maxX: Float, maxY: Float, z: Float,
        frameX: Float, frameY: Float, normalZ: Float,
        r: Int, g: Int, b: Int, a: Int
    ) {
        val left = minX + frameX
        val right = maxX - frameX
        val bottom = minY + frameY
        val top = maxY - frameY
        // bottom strip
        quadColor(vc, pos, norm, light, overlay,
            minX, minY, z, maxX, minY, z, maxX, bottom, z, minX, bottom, z,
            0f, 0f, normalZ, r, g, b, a)
        // top strip
        quadColor(vc, pos, norm, light, overlay,
            minX, top, z, maxX, top, z, maxX, maxY, z, minX, maxY, z,
            0f, 0f, normalZ, r, g, b, a)
        // left strip
        quadColor(vc, pos, norm, light, overlay,
            minX, bottom, z, left, bottom, z, left, top, z, minX, top, z,
            0f, 0f, normalZ, r, g, b, a)
        // right strip
        quadColor(vc, pos, norm, light, overlay,
            right, bottom, z, maxX, bottom, z, maxX, top, z, right, top, z,
            0f, 0f, normalZ, r, g, b, a)
    }

    private fun drawFaceFrameYZ(
        pos: Matrix4f, norm: Matrix3f, vc: VertexConsumer,
        light: Int, overlay: Int,
        minY: Float, minZ: Float, maxY: Float, maxZ: Float, x: Float,
        frameY: Float, frameZ: Float, normalX: Float,
        r: Int, g: Int, b: Int, a: Int
    ) {
        val bottom = minY + frameY
        val top = maxY - frameY
        val near = minZ + frameZ
        val far = maxZ - frameZ
        // bottom strip
        quadColor(vc, pos, norm, light, overlay,
            x, minY, minZ, x, minY, maxZ, x, bottom, maxZ, x, bottom, minZ,
            normalX, 0f, 0f, r, g, b, a)
        // top strip
        quadColor(vc, pos, norm, light, overlay,
            x, top, minZ, x, top, maxZ, x, maxY, maxZ, x, maxY, minZ,
            normalX, 0f, 0f, r, g, b, a)
        // near strip
        quadColor(vc, pos, norm, light, overlay,
            x, bottom, minZ, x, bottom, near, x, top, near, x, top, minZ,
            normalX, 0f, 0f, r, g, b, a)
        // far strip
        quadColor(vc, pos, norm, light, overlay,
            x, bottom, far, x, bottom, maxZ, x, top, maxZ, x, top, far,
            normalX, 0f, 0f, r, g, b, a)
    }

    private fun drawFaceFrameXZ(
        pos: Matrix4f, norm: Matrix3f, vc: VertexConsumer,
        light: Int, overlay: Int,
        minX: Float, minZ: Float, maxX: Float, maxZ: Float, y: Float,
        frameX: Float, frameZ: Float, normalY: Float,
        r: Int, g: Int, b: Int, a: Int
    ) {
        val left = minX + frameX
        val right = maxX - frameX
        val near = minZ + frameZ
        val far = maxZ - frameZ
        // near strip
        quadColor(vc, pos, norm, light, overlay,
            minX, y, minZ, maxX, y, minZ, maxX, y, near, minX, y, near,
            0f, normalY, 0f, r, g, b, a)
        // far strip
        quadColor(vc, pos, norm, light, overlay,
            minX, y, far, maxX, y, far, maxX, y, maxZ, minX, y, maxZ,
            0f, normalY, 0f, r, g, b, a)
        // left strip
        quadColor(vc, pos, norm, light, overlay,
            minX, y, near, left, y, near, left, y, far, minX, y, far,
            0f, normalY, 0f, r, g, b, a)
        // right strip
        quadColor(vc, pos, norm, light, overlay,
            right, y, near, maxX, y, near, maxX, y, far, right, y, far,
            0f, normalY, 0f, r, g, b, a)
    }

    private fun drawCuboidFluid(
        pos: Matrix4f, norm: Matrix3f, vc: VertexConsumer,
        light: Int, overlay: Int,
        minX: Float, minY: Float, minZ: Float,
        maxX: Float, maxY: Float, maxZ: Float,
        sprite: Sprite, color: Int
    ) {
        val u0 = sprite.minU
        val u1 = sprite.maxU
        val v0 = sprite.minV
        val v1 = sprite.maxV
        val r = (color shr 16 and 0xFF)
        val g = (color shr 8 and 0xFF)
        val b = (color and 0xFF)

        // South (+Z)
        quadFluid(vc, pos, norm, light, overlay,
            minX, minY, maxZ, u0, v1,
            maxX, minY, maxZ, u1, v1,
            maxX, maxY, maxZ, u1, v0,
            minX, maxY, maxZ, u0, v0,
            0f, 0f, 1f, r, g, b)
        // North (-Z)
        quadFluid(vc, pos, norm, light, overlay,
            maxX, minY, minZ, u0, v1,
            minX, minY, minZ, u1, v1,
            minX, maxY, minZ, u1, v0,
            maxX, maxY, minZ, u0, v0,
            0f, 0f, -1f, r, g, b)
        // West (-X)
        quadFluid(vc, pos, norm, light, overlay,
            minX, minY, minZ, u0, v1,
            minX, minY, maxZ, u1, v1,
            minX, maxY, maxZ, u1, v0,
            minX, maxY, minZ, u0, v0,
            -1f, 0f, 0f, r, g, b)
        // East (+X)
        quadFluid(vc, pos, norm, light, overlay,
            maxX, minY, maxZ, u0, v1,
            maxX, minY, minZ, u1, v1,
            maxX, maxY, minZ, u1, v0,
            maxX, maxY, maxZ, u0, v0,
            1f, 0f, 0f, r, g, b)
        // Up (+Y)
        quadFluid(vc, pos, norm, light, overlay,
            minX, maxY, maxZ, u0, v1,
            maxX, maxY, maxZ, u1, v1,
            maxX, maxY, minZ, u1, v0,
            minX, maxY, minZ, u0, v0,
            0f, 1f, 0f, r, g, b)
        // Down (-Y)
        quadFluid(vc, pos, norm, light, overlay,
            minX, minY, minZ, u0, v1,
            maxX, minY, minZ, u1, v1,
            maxX, minY, maxZ, u1, v0,
            minX, minY, maxZ, u0, v0,
            0f, -1f, 0f, r, g, b)
    }

    // ========== Quad 辅助 ==========

    private fun quadColor(
        vc: VertexConsumer, pos: Matrix4f, norm: Matrix3f,
        light: Int, overlay: Int,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        x4: Float, y4: Float, z4: Float,
        nx: Float, ny: Float, nz: Float,
        r: Int, g: Int, b: Int, a: Int
    ) {
        vertexColor(vc, pos, norm, x1, y1, z1, 0f, 0f, light, overlay, nx, ny, nz, r, g, b, a)
        vertexColor(vc, pos, norm, x2, y2, z2, 1f, 0f, light, overlay, nx, ny, nz, r, g, b, a)
        vertexColor(vc, pos, norm, x3, y3, z3, 1f, 1f, light, overlay, nx, ny, nz, r, g, b, a)
        vertexColor(vc, pos, norm, x4, y4, z4, 0f, 1f, light, overlay, nx, ny, nz, r, g, b, a)
    }

    private fun quadFluid(
        vc: VertexConsumer, pos: Matrix4f, norm: Matrix3f,
        light: Int, overlay: Int,
        x1: Float, y1: Float, z1: Float, u1: Float, v1: Float,
        x2: Float, y2: Float, z2: Float, u2: Float, v2: Float,
        x3: Float, y3: Float, z3: Float, u3: Float, v3: Float,
        x4: Float, y4: Float, z4: Float, u4: Float, v4: Float,
        nx: Float, ny: Float, nz: Float,
        r: Int, g: Int, b: Int
    ) {
        vertexFluid(vc, pos, norm, x1, y1, z1, u1, v1, light, overlay, nx, ny, nz, r, g, b)
        vertexFluid(vc, pos, norm, x2, y2, z2, u2, v2, light, overlay, nx, ny, nz, r, g, b)
        vertexFluid(vc, pos, norm, x3, y3, z3, u3, v3, light, overlay, nx, ny, nz, r, g, b)
        vertexFluid(vc, pos, norm, x4, y4, z4, u4, v4, light, overlay, nx, ny, nz, r, g, b)
    }

    private fun vertexColor(
        vc: VertexConsumer, pos: Matrix4f, norm: Matrix3f,
        x: Float, y: Float, z: Float,
        u: Float, v: Float,
        light: Int, overlay: Int,
        nx: Float, ny: Float, nz: Float,
        r: Int, g: Int, b: Int, a: Int
    ) {
        vc.vertex(pos, x, y, z)
            .color(r, g, b, a)
            .texture(u, v)
            .overlay(overlay.takeUnless { it == 0 } ?: OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(norm, nx, ny, nz)
            .next()
    }

    private fun vertexFluid(
        vc: VertexConsumer, pos: Matrix4f, norm: Matrix3f,
        x: Float, y: Float, z: Float,
        u: Float, v: Float,
        light: Int, overlay: Int,
        nx: Float, ny: Float, nz: Float,
        r: Int, g: Int, b: Int
    ) {
        vc.vertex(pos, x, y, z)
            .color(r, g, b, 255)
            .texture(u, v)
            .overlay(overlay.takeUnless { it == 0 } ?: OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(norm, nx, ny, nz)
            .next()
    }

    private fun getFluidTintColor(fluid: Fluid): Int {
        val world = MinecraftClient.getInstance().world ?: return 0xFFFFFF
        val handler = FluidRenderHandlerRegistry.INSTANCE.get(fluid) ?: return 0xFFFFFF
        return handler.getFluidColor(world, BlockPos.ORIGIN, fluid.defaultState)
    }

    override fun rendersOutsideBoundingBox(entity: PipeBlockEntity): Boolean = true
}
