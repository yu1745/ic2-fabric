package ic2_120.client.renderers

import ic2_120.content.block.KineticGeneratorBlock
import ic2_120.content.block.WindKineticGeneratorBlock
import ic2_120.content.block.WaterKineticGeneratorBlock
import ic2_120.content.block.ManualKineticGeneratorBlock
import ic2_120.content.block.transmission.BevelGearBlock
import ic2_120.content.block.transmission.IKineticMachinePort
import ic2_120.content.block.transmission.KineticConnectionRules
import ic2_120.content.block.transmission.ShaftMaterial
import ic2_120.content.block.transmission.TransmissionBlockEntity
import ic2_120.content.block.transmission.TransmissionShaftBlock
import net.minecraft.client.render.LightmapTextureManager
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.state.property.Properties
import net.minecraft.util.Identifier
import net.minecraft.util.math.Direction
import net.minecraft.util.math.RotationAxis
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class TransmissionBlockEntityRenderer(
    context: BlockEntityRendererFactory.Context
) : BlockEntityRenderer<TransmissionBlockEntity> {
    companion object {
        private val WHITE_TEXTURE = Identifier.ofVanilla("textures/misc/white.png")
        private const val SHAFT_HALF = 1.0f / 6.0f
        private const val SHAFT_LENGTH_HALF = 0.5f
        private const val BEVEL_GEAR_TOOTH_COUNT = 8
        private const val PRESSURE_ANGLE_DEGREES = 20.0f
        // 啮合时要让一侧齿尖对另一侧齿槽，因此相位差取半个齿距。
        private const val MESH_PHASE_OFFSET_DEGREES = 180.0f / BEVEL_GEAR_TOOTH_COUNT
        // 齿顶高系数 ha*，按齿轮术语定义为相对模数 m 的系数，而不是相对分度半径。
        private const val ADDENDUM_COEFFICIENT = 0.3f
        // 齿根高系数 hf*。这里保持略深齿根，视觉上不至于过于发胖。
        private const val DEDENDUM_COEFFICIENT = 1.0f
    }

    init {
        @Suppress("UNUSED_VARIABLE")
        val ignored = context
    }

    override fun render(
        entity: TransmissionBlockEntity,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        val world = entity.world
        val state = entity.cachedState
        val isItem = world == null
        val angle = if (isItem) 0.0f
            else ((world!!.time + tickDelta) * degreesPerTickFromKu(entity.currentKu)) % 360.0f
        val fullLight = LightmapTextureManager.MAX_LIGHT_COORDINATE

        when (val block = state.block) {
            is TransmissionShaftBlock -> {
                val axis = if (isItem) Direction.Axis.Y else state.get(Properties.AXIS)
                val vc = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(WHITE_TEXTURE))
                val materialColor = baseColorForMaterial(block.material)
                matrices.push()
                matrices.translate(0.5, 0.5, 0.5)
                rotateByAxis(matrices, axis, angle)
                drawShaftAlongAxis(matrices, vc, fullLight, overlay, axis, materialColor)
                matrices.pop()
            }

            is BevelGearBlock -> {
                val gearThickness = block.gearThickness
                val gearFaceWidthHalf = (gearThickness * 0.5f).coerceAtLeast(0.02f)
                val pitchRadius = (0.5f - gearFaceWidthHalf).coerceAtLeast(0.05f)
                val gearOffset = pitchRadius
                val vc = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(WHITE_TEXTURE))
                val materialColor = intArrayOf(122, 126, 132)

                matrices.push()
                matrices.translate(0.5, 0.5, 0.5)

                if (isItem) {
                    // 物品渲染：以默认 XZ 平面展示两个方向的齿轮
                    val firstAxis = Direction.Axis.X
                    val secondAxis = Direction.Axis.Z

                    matrices.push()
                    translateAlongAxis(matrices, firstAxis, gearOffset)
                    rotateByAxis(matrices, firstAxis, angle)
                    drawGear8Teeth(matrices, vc, fullLight, overlay, firstAxis, pitchRadius, gearFaceWidthHalf, materialColor)
                    matrices.pop()

                    matrices.push()
                    translateAlongAxis(matrices, secondAxis, -gearOffset)
                    rotateByAxis(matrices, secondAxis, angle + MESH_PHASE_OFFSET_DEGREES)
                    drawGear8Teeth(matrices, vc, fullLight, overlay, secondAxis, pitchRadius, gearFaceWidthHalf, materialColor)
                    matrices.pop()
                } else {
                    val renderDirs = bevelRenderDirections(world!!, entity.pos)

                    if (renderDirs.isEmpty()) {
                        val plane = state.get(BevelGearBlock.PLANE)
                        val (firstAxis, secondAxis) = plane.axes()
                        val firstSideSign = gearSideSign(world, entity.pos, firstAxis, preferPositive = true)
                        val secondSideSign = gearSideSign(world, entity.pos, secondAxis, preferPositive = false)

                        matrices.push()
                        translateAlongAxis(matrices, firstAxis, gearOffset * firstSideSign)
                        rotateByAxis(matrices, firstAxis, angle)
                        drawGear8Teeth(matrices, vc, fullLight, overlay, firstAxis, pitchRadius, gearFaceWidthHalf, materialColor)
                        matrices.pop()

                        matrices.push()
                        translateAlongAxis(matrices, secondAxis, gearOffset * secondSideSign)
                        rotateByAxis(matrices, secondAxis, angle + MESH_PHASE_OFFSET_DEGREES)
                        drawGear8Teeth(matrices, vc, fullLight, overlay, secondAxis, pitchRadius, gearFaceWidthHalf, materialColor)
                        matrices.pop()
                    } else {
                        for (i in renderDirs.indices) {
                            val direction = renderDirs[i]
                            val axis = direction.axis
                            val sideSign = directionSign(direction)
                            val phase = angle + MESH_PHASE_OFFSET_DEGREES * i

                            matrices.push()
                            translateAlongAxis(matrices, axis, gearOffset * sideSign)
                            rotateByAxis(matrices, axis, phase)
                            drawGear8Teeth(matrices, vc, fullLight, overlay, axis, pitchRadius, gearFaceWidthHalf, materialColor)
                            matrices.pop()
                        }
                    }
                }

                matrices.pop()
            }
        }
    }

    /**
     * 渲染端与服务端一致的伞齿轮连通约束：
     * - 允许：2 面垂直（L）
     * - 允许：3 面 T（两面对穿 + 一面垂直）
     * - 禁止：2 面对穿
     * - 禁止：3 面正交
     */
    private fun bevelRenderDirections(world: net.minecraft.world.World, pos: net.minecraft.util.math.BlockPos): List<Direction> {
        return KineticConnectionRules.nodeDirections(world, pos, world.getBlockState(pos))
    }

    private fun isRenderableTransmissionConnection(
        world: net.minecraft.world.World,
        centerPos: net.minecraft.util.math.BlockPos,
        direction: Direction
    ): Boolean {
        val neighborPos = centerPos.offset(direction)
        val neighborState = world.getBlockState(neighborPos)
        return when {
            KineticConnectionRules.isTransmissionNode(neighborState) ->
                KineticConnectionRules.canConnectFromDirection(neighborState, direction.opposite)

            else -> KineticConnectionRules.isMachinePortFacing(world, neighborPos, direction)
        }
    }

    private fun directionSign(direction: Direction): Float = when (direction) {
        Direction.EAST, Direction.UP, Direction.SOUTH -> 1.0f
        Direction.WEST, Direction.DOWN, Direction.NORTH -> -1.0f
    }

    private fun directionPriority(direction: Direction): Int = when (direction) {
        Direction.EAST -> 0
        Direction.SOUTH -> 1
        Direction.UP -> 2
        Direction.WEST -> 3
        Direction.NORTH -> 4
        Direction.DOWN -> 5
    }

    private fun degreesPerTickFromKu(ku: Int): Float {
        if (ku <= 0) return 0.0f
        val capped = ku.coerceAtMost(8192)
        val rpm = when {
            capped <= 512 -> 6.0f + 18.0f * (capped / 512.0f)
            capped <= 2048 -> 24.0f + 24.0f * ((capped - 512) / 1536.0f)
            else -> 48.0f + 24.0f * ((capped - 2048) / 6144.0f)
        }
        return rpm * 6.0f / 20.0f
    }

    private fun baseColorForMaterial(material: ShaftMaterial): IntArray = when (material) {
        ShaftMaterial.WOOD -> intArrayOf(118, 96, 74)
        ShaftMaterial.IRON -> intArrayOf(136, 139, 142)
        ShaftMaterial.STEEL -> intArrayOf(120, 126, 132)
        ShaftMaterial.CARBON -> intArrayOf(72, 76, 82)
    }

    private fun translateAlongAxis(matrices: MatrixStack, axis: Direction.Axis, offset: Float) {
        when (axis) {
            Direction.Axis.X -> matrices.translate(offset.toDouble(), 0.0, 0.0)
            Direction.Axis.Y -> matrices.translate(0.0, offset.toDouble(), 0.0)
            Direction.Axis.Z -> matrices.translate(0.0, 0.0, offset.toDouble())
        }
    }

    private fun gearSideSign(world: net.minecraft.world.World, pos: net.minecraft.util.math.BlockPos, axis: Direction.Axis, preferPositive: Boolean): Float {
        val positiveDirection = positiveDirectionForAxis(axis)
        val negativeDirection = positiveDirection.opposite
        val hasPositiveShaft = hasMatchingConnection(world, pos, positiveDirection, axis)
        val hasNegativeShaft = hasMatchingConnection(world, pos, negativeDirection, axis)

        return when {
            hasPositiveShaft && !hasNegativeShaft -> 1.0f
            hasNegativeShaft && !hasPositiveShaft -> -1.0f
            hasPositiveShaft -> 1.0f
            hasNegativeShaft -> -1.0f
            preferPositive -> 1.0f
            else -> -1.0f
        }
    }

    private fun positiveDirectionForAxis(axis: Direction.Axis): Direction = when (axis) {
        Direction.Axis.X -> Direction.EAST
        Direction.Axis.Y -> Direction.UP
        Direction.Axis.Z -> Direction.SOUTH
    }

    private fun hasMatchingConnection(
        world: net.minecraft.world.World,
        centerPos: net.minecraft.util.math.BlockPos,
        direction: Direction,
        axis: Direction.Axis
    ): Boolean {
        val pos = centerPos.offset(direction)
        val state = world.getBlockState(pos)
        val block = state.block

        if (block is TransmissionShaftBlock) {
            return state.get(Properties.AXIS) == axis
        }

        if (!state.contains(Properties.HORIZONTAL_FACING)) return false
        val facing = state.get(Properties.HORIZONTAL_FACING)
        return when (block) {
            // 风力动能发生机：吸附渲染放宽为相邻即识别，输出方向由服务端逻辑决定。
            is WindKineticGeneratorBlock -> true
            is WaterKineticGeneratorBlock -> true
            is ManualKineticGeneratorBlock -> true
            // 动能发电机：正面为动能输入口。
            is KineticGeneratorBlock -> facing == direction.opposite
            else -> false
        }
    }

    private fun rotateByAxis(matrices: MatrixStack, axis: Direction.Axis, angle: Float) {
        when (axis) {
            Direction.Axis.X -> matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(angle))
            Direction.Axis.Y -> matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle))
            Direction.Axis.Z -> matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle))
        }
    }

    private fun drawShaftAlongAxis(
        matrices: MatrixStack,
        vc: VertexConsumer,
        light: Int,
        overlay: Int,
        axis: Direction.Axis,
        baseColor: IntArray
    ) {
        drawOctagonalShaft(matrices, vc, light, overlay, axis, SHAFT_LENGTH_HALF, SHAFT_HALF, baseColor)
    }

    private fun drawOctagonalShaft(
        matrices: MatrixStack,
        vc: VertexConsumer,
        light: Int,
        overlay: Int,
        axis: Direction.Axis,
        halfLength: Float,
        radius: Float,
        baseColor: IntArray
    ) {
        val entry = matrices.peek()

        val basis = basisForAxis(axis)
        val ax = basis[0]
        val ay = basis[1]
        val az = basis[2]
        val ux = basis[3]
        val uy = basis[4]
        val uz = basis[5]
        val vx = basis[6]
        val vy = basis[7]
        val vz = basis[8]
        val segments = 8
        val angleStep = (2.0 * PI / segments).toFloat()

        for (i in 0 until segments) {
            val a0 = i * angleStep
            val a1 = (i + 1) * angleStep

            val r0x = (cos(a0.toDouble()).toFloat() * ux + sin(a0.toDouble()).toFloat() * vx) * radius
            val r0y = (cos(a0.toDouble()).toFloat() * uy + sin(a0.toDouble()).toFloat() * vy) * radius
            val r0z = (cos(a0.toDouble()).toFloat() * uz + sin(a0.toDouble()).toFloat() * vz) * radius

            val r1x = (cos(a1.toDouble()).toFloat() * ux + sin(a1.toDouble()).toFloat() * vx) * radius
            val r1y = (cos(a1.toDouble()).toFloat() * uy + sin(a1.toDouble()).toFloat() * vy) * radius
            val r1z = (cos(a1.toDouble()).toFloat() * uz + sin(a1.toDouble()).toFloat() * vz) * radius

            val sx = -ax * halfLength
            val sy = -ay * halfLength
            val sz = -az * halfLength
            val ex = ax * halfLength
            val ey = ay * halfLength
            val ez = az * halfLength

            val p1x = sx + r0x
            val p1y = sy + r0y
            val p1z = sz + r0z
            val p2x = sx + r1x
            val p2y = sy + r1y
            val p2z = sz + r1z
            val p3x = ex + r1x
            val p3y = ey + r1y
            val p3z = ez + r1z
            val p4x = ex + r0x
            val p4y = ey + r0y
            val p4z = ez + r0z

            val mid = (a0 + a1) * 0.5f
            val nx = cos(mid.toDouble()).toFloat() * ux + sin(mid.toDouble()).toFloat() * vx
            val ny = cos(mid.toDouble()).toFloat() * uy + sin(mid.toDouble()).toFloat() * vy
            val nz = cos(mid.toDouble()).toFloat() * uz + sin(mid.toDouble()).toFloat() * vz

            val u0 = i / segments.toFloat()
            val u1 = (i + 1) / segments.toFloat()
            quadUv(
                vc, entry, light, overlay,
                p1x, p1y, p1z, u0, 0f,
                p2x, p2y, p2z, u1, 0f,
                p3x, p3y, p3z, u1, 1f,
                p4x, p4y, p4z, u0, 1f,
                nx, ny, nz,
                baseColor
            )
        }
    }

    // 为不同轴定义纹理/几何基向量，修正旧实现的 90 度偏转。
    private fun basisForAxis(axis: Direction.Axis): FloatArray = when (axis) {
        Direction.Axis.X -> floatArrayOf(
            1f, 0f, 0f,   // a (轴向)
            0f, 0f, 1f,   // u
            0f, 1f, 0f    // v
        )
        Direction.Axis.Y -> floatArrayOf(
            0f, 1f, 0f,
            1f, 0f, 0f,
            0f, 0f, 1f
        )
        Direction.Axis.Z -> floatArrayOf(
            0f, 0f, 1f,
            1f, 0f, 0f,
            0f, 1f, 0f
        )
    }

    private fun drawGear8Teeth(
        matrices: MatrixStack,
        vc: VertexConsumer,
        light: Int,
        overlay: Int,
        axis: Direction.Axis,
        pitchRadius: Float,
        gearFaceWidthHalf: Float,
        baseColor: IntArray
    ) {
        val outline = buildInvoluteGearOutline(
            pitchRadius = pitchRadius,
            toothCount = BEVEL_GEAR_TOOTH_COUNT,
            involuteSegments = 3,
            tipArcSegments = 2,
            rootArcSegments = 2
        )
        drawExtrudedGearFromOutline(matrices, vc, light, overlay, axis, outline, gearFaceWidthHalf, baseColor)
    }

    private fun buildInvoluteGearOutline(
        pitchRadius: Float,
        toothCount: Int,
        involuteSegments: Int,
        tipArcSegments: Int,
        rootArcSegments: Int
    ): List<Pair<Float, Float>> {
        val module = (2.0f * pitchRadius / toothCount.toFloat()).coerceAtLeast(0.01f)
        val pressureAngle = Math.toRadians(PRESSURE_ANGLE_DEGREES.toDouble()).toFloat()
        val outerRadiusActual = gearOuterRadiusFromPitchRadius(pitchRadius)
        val baseRadius = (pitchRadius * cos(pressureAngle.toDouble()).toFloat()).coerceAtMost(outerRadiusActual * 0.98f)
        val rootRadius = (pitchRadius - DEDENDUM_COEFFICIENT * module).coerceAtLeast(0.05f)
        val toothAngle = (2.0 * PI / toothCount).toFloat()
        // 标准 1:1 齿轮在分度圆上的半齿厚角。
        val halfThicknessAtPitch = toothAngle * 0.25f
        val tPitch = sqrt(((pitchRadius * pitchRadius) / (baseRadius * baseRadius) - 1.0f).coerceAtLeast(0.0f))
        val involuteAtPitch = tPitch - atan2(tPitch, 1.0f)
        val tOuter = sqrt(((outerRadiusActual * outerRadiusActual) / (baseRadius * baseRadius) - 1.0f).coerceAtLeast(0.0f))
        val points = ArrayList<Pair<Float, Float>>(toothCount * (involuteSegments * 2 + tipArcSegments + rootArcSegments + 6))

        fun involutePolar(t: Float): Pair<Float, Float> {
            val x = baseRadius * (cos(t.toDouble()).toFloat() + t * sin(t.toDouble()).toFloat())
            val y = baseRadius * (sin(t.toDouble()).toFloat() - t * cos(t.toDouble()).toFloat())
            val r = sqrt(x * x + y * y)
            val a = atan2(y, x)
            return r to a
        }

        for (tooth in 0 until toothCount) {
            val center = tooth * toothAngle

            // 渐开线在分度圆处有 inv(alpha) 的极角偏置，需要先扣掉再展开，
            // 否则齿厚会被错误压缩，看起来会“尖且胖”。
            val leftBaseAngle = center - halfThicknessAtPitch - involuteAtPitch
            val rightBaseAngle = center + halfThicknessAtPitch + involuteAtPitch
            val leftRootX = rootRadius * cos(leftBaseAngle.toDouble()).toFloat()
            val leftRootY = rootRadius * sin(leftBaseAngle.toDouble()).toFloat()
            points.add(leftRootX to leftRootY)

            val leftInvolute = ArrayList<Pair<Float, Float>>(involuteSegments + 1)
            for (i in 0..involuteSegments) {
                val t = tOuter * (i / involuteSegments.toFloat())
                val (r, a) = involutePolar(t)
                val ang = leftBaseAngle + a
                leftInvolute.add(r * cos(ang.toDouble()).toFloat() to r * sin(ang.toDouble()).toFloat())
            }
            points.addAll(leftInvolute)

            val leftOuterAng = atan2(leftInvolute.last().second, leftInvolute.last().first)
            val rightInvolute = ArrayList<Pair<Float, Float>>(involuteSegments + 1)
            for (i in 0..involuteSegments) {
                val t = tOuter * (1.0f - i / involuteSegments.toFloat())
                val (r, a) = involutePolar(t)
                val ang = rightBaseAngle - a
                rightInvolute.add(r * cos(ang.toDouble()).toFloat() to r * sin(ang.toDouble()).toFloat())
            }
            val rightOuterAng = atan2(rightInvolute.first().second, rightInvolute.first().first)
            val tipStart = leftOuterAng
            var tipEnd = rightOuterAng
            if (tipEnd < tipStart) tipEnd += (2.0f * PI).toFloat()
            for (i in 1 until tipArcSegments) {
                val t = i / tipArcSegments.toFloat()
                val ang = tipStart + (tipEnd - tipStart) * t
                points.add(outerRadiusActual * cos(ang.toDouble()).toFloat() to outerRadiusActual * sin(ang.toDouble()).toFloat())
            }
            points.addAll(rightInvolute)

            val nextCenter = ((tooth + 1) % toothCount) * toothAngle
            // 齿根圆弧必须连到“下一齿真实的左基圆角”，否则轮廓会在齿根处折返自交。
            val nextLeftBaseAngle = nextCenter - halfThicknessAtPitch - involuteAtPitch
            var rootStart = rightBaseAngle
            var rootEnd = nextLeftBaseAngle
            if (rootEnd <= rootStart) rootEnd += (2.0f * PI).toFloat()
            for (i in 1..rootArcSegments) {
                val t = i / (rootArcSegments + 1).toFloat()
                val ang = rootStart + (rootEnd - rootStart) * t
                points.add(rootRadius * cos(ang.toDouble()).toFloat() to rootRadius * sin(ang.toDouble()).toFloat())
            }
        }
        return points
    }

    private fun drawExtrudedGearFromOutline(
        matrices: MatrixStack,
        vc: VertexConsumer,
        light: Int,
        overlay: Int,
        axis: Direction.Axis,
        outline: List<Pair<Float, Float>>,
        halfThickness: Float,
        baseColor: IntArray
    ) {
        if (outline.size < 3) return
        val entry = matrices.peek()

        fun to3D(u: Float, v: Float, depth: Float): FloatArray = when (axis) {
            Direction.Axis.X -> floatArrayOf(depth, u, v)
            Direction.Axis.Y -> floatArrayOf(u, depth, v)
            Direction.Axis.Z -> floatArrayOf(u, v, depth)
        }

        val n = outline.size
        for (i in 0 until n) {
            val (u0, v0) = outline[i]
            val (u1, v1) = outline[(i + 1) % n]
            val f0 = to3D(u0, v0, halfThickness)
            val f1 = to3D(u1, v1, halfThickness)
            val b0 = to3D(u0, v0, -halfThickness)
            val b1 = to3D(u1, v1, -halfThickness)

            val ex = u1 - u0
            val ey = v1 - v0
            val len = sqrt(ex * ex + ey * ey).coerceAtLeast(1.0e-6f)
            val nx2 = ey / len
            val ny2 = -ex / len
            val nz2 = 0f
            val wn = when (axis) {
                Direction.Axis.X -> floatArrayOf(0f, nx2, ny2)
                Direction.Axis.Y -> floatArrayOf(nx2, 0f, ny2)
                Direction.Axis.Z -> floatArrayOf(nx2, ny2, 0f)
            }

            quadUv(
                vc, entry, light, overlay,
                f0[0], f0[1], f0[2], 0f, 0f,
                f1[0], f1[1], f1[2], 1f, 0f,
                b1[0], b1[1], b1[2], 1f, 1f,
                b0[0], b0[1], b0[2], 0f, 1f,
                wn[0], wn[1], wn[2],
                baseColor
            )
        }

        val frontNormal = when (axis) {
            Direction.Axis.X -> floatArrayOf(1f, 0f, 0f)
            Direction.Axis.Y -> floatArrayOf(0f, 1f, 0f)
            Direction.Axis.Z -> floatArrayOf(0f, 0f, 1f)
        }
        val backNormal = floatArrayOf(-frontNormal[0], -frontNormal[1], -frontNormal[2])
        val cFront = to3D(0f, 0f, halfThickness)
        val cBack = to3D(0f, 0f, -halfThickness)
        for (i in 0 until n) {
            val (u0, v0) = outline[i]
            val (u1, v1) = outline[(i + 1) % n]
            val f0 = to3D(u0, v0, halfThickness)
            val f1 = to3D(u1, v1, halfThickness)
            val b0 = to3D(u0, v0, -halfThickness)
            val b1 = to3D(u1, v1, -halfThickness)

            triangleUv(
                vc, entry, light, overlay,
                cFront[0], cFront[1], cFront[2], 0.5f, 0.5f,
                f0[0], f0[1], f0[2], 0f, 0f,
                f1[0], f1[1], f1[2], 1f, 0f,
                frontNormal[0], frontNormal[1], frontNormal[2],
                baseColor
            )

            triangleUv(
                vc, entry, light, overlay,
                cBack[0], cBack[1], cBack[2], 0.5f, 0.5f,
                b1[0], b1[1], b1[2], 1f, 0f,
                b0[0], b0[1], b0[2], 0f, 0f,
                backNormal[0], backNormal[1], backNormal[2],
                baseColor
            )
        }
    }

    /**
     * 当前渲染近似将轴向厚度中线视为分度圆面，因此分度圆由 gearThickness 直接决定。
     */
    private fun gearOuterRadiusFromPitchRadius(pitchRadius: Float): Float {
        val module = (2.0f * pitchRadius / BEVEL_GEAR_TOOTH_COUNT.toFloat()).coerceAtLeast(0.01f)
        val addendum = ADDENDUM_COEFFICIENT * module
        return (pitchRadius + addendum).coerceAtMost(0.46f)
    }

    private fun drawCuboid(
        matrices: MatrixStack,
        vc: VertexConsumer,
        light: Int,
        overlay: Int,
        minX: Float,
        maxX: Float,
        minY: Float,
        maxY: Float,
        minZ: Float,
        maxZ: Float
    ) {
        val entry = matrices.peek()
        val fallbackColor = intArrayOf(128, 128, 128)

        quad(vc, entry, light, overlay, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, 0f, 0f, 1f, fallbackColor)
        quad(vc, entry, light, overlay, maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, 0f, 0f, -1f, fallbackColor)
        quad(vc, entry, light, overlay, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, -1f, 0f, 0f, fallbackColor)
        quad(vc, entry, light, overlay, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, 1f, 0f, 0f, fallbackColor)
        quad(vc, entry, light, overlay, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ, 0f, 1f, 0f, fallbackColor)
        quad(vc, entry, light, overlay, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, 0f, -1f, 0f, fallbackColor)
    }

    private fun quad(
        vc: VertexConsumer,
        entry: MatrixStack.Entry,
        light: Int,
        overlay: Int,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        x4: Float, y4: Float, z4: Float,
        nx: Float, ny: Float, nz: Float,
        baseColor: IntArray
    ) {
        quadUv(
            vc, entry, light, overlay,
            x1, y1, z1, 0f, 0f,
            x2, y2, z2, 1f, 0f,
            x3, y3, z3, 1f, 1f,
            x4, y4, z4, 0f, 1f,
            nx, ny, nz,
            baseColor
        )
    }

    private fun quadUv(
        vc: VertexConsumer,
        entry: MatrixStack.Entry,
        light: Int,
        overlay: Int,
        x1: Float, y1: Float, z1: Float, u1: Float, v1: Float,
        x2: Float, y2: Float, z2: Float, u2: Float, v2: Float,
        x3: Float, y3: Float, z3: Float, u3: Float, v3: Float,
        x4: Float, y4: Float, z4: Float, u4: Float, v4: Float,
        nx: Float, ny: Float, nz: Float,
        baseColor: IntArray
    ) {
        vertex(vc, entry, x1, y1, z1, u1, v1, light, overlay, nx, ny, nz, baseColor)
        vertex(vc, entry, x2, y2, z2, u2, v2, light, overlay, nx, ny, nz, baseColor)
        vertex(vc, entry, x3, y3, z3, u3, v3, light, overlay, nx, ny, nz, baseColor)
        vertex(vc, entry, x4, y4, z4, u4, v4, light, overlay, nx, ny, nz, baseColor)
    }

    private fun triangleUv(
        vc: VertexConsumer,
        entry: MatrixStack.Entry,
        light: Int,
        overlay: Int,
        x1: Float, y1: Float, z1: Float, u1: Float, v1: Float,
        x2: Float, y2: Float, z2: Float, u2: Float, v2: Float,
        x3: Float, y3: Float, z3: Float, u3: Float, v3: Float,
        nx: Float, ny: Float, nz: Float,
        baseColor: IntArray
    ) {
        // EntityCutout/NoCull 这类渲染层按 QUADS 消费顶点。
        // 这里用退化四边形表达三角形，避免 3 顶点写入把后续几何串坏。
        vertex(vc, entry, x1, y1, z1, u1, v1, light, overlay, nx, ny, nz, baseColor)
        vertex(vc, entry, x2, y2, z2, u2, v2, light, overlay, nx, ny, nz, baseColor)
        vertex(vc, entry, x3, y3, z3, u3, v3, light, overlay, nx, ny, nz, baseColor)
        vertex(vc, entry, x3, y3, z3, u3, v3, light, overlay, nx, ny, nz, baseColor)
    }

    private fun vertex(
        vc: VertexConsumer,
        entry: MatrixStack.Entry,
        x: Float,
        y: Float,
        z: Float,
        u: Float,
        v: Float,
        light: Int,
        overlay: Int,
        nx: Float,
        ny: Float,
        nz: Float,
        baseColor: IntArray
    ) {
        vc.vertex(entry.positionMatrix, x, y, z)
            .color(baseColor[0], baseColor[1], baseColor[2], 255)
            .texture(u, v)
            .overlay(overlay.takeUnless { it == 0 } ?: OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry, nx, ny, nz)
    }
}
