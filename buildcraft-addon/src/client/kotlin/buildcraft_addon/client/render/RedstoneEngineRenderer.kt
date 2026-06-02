package buildcraft_addon.client.render

import buildcraft_addon.BuildCraftAddon
import buildcraft_addon.content.blockentity.PowerStage
import buildcraft_addon.content.blockentity.RedstoneEngineBlockEntity
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.LightmapTextureManager
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory
import net.minecraft.client.texture.Sprite
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.screen.PlayerScreenHandler
import net.minecraft.state.property.Properties
import net.minecraft.util.Identifier
import net.minecraft.util.math.Direction
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Quaternionf
import kotlin.math.cos
import kotlin.math.sin

class RedstoneEngineRenderer(ctx: BlockEntityRendererFactory.Context) :
    BlockEntityRenderer<RedstoneEngineBlockEntity> {

    private lateinit var trunkBlue: Sprite
    private lateinit var trunkGreen: Sprite
    private lateinit var trunkYellow: Sprite
    private lateinit var trunkRed: Sprite
    private lateinit var trunkOverheat: Sprite
    private lateinit var chamberSprite: Sprite
    private lateinit var backSprite: Sprite
    private lateinit var sideSprite: Sprite

    private var spritesLoaded = false

    private fun ensureSprites() {
        if (spritesLoaded) return
        val atlas = MinecraftClient.getInstance().bakedModelManager.getAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE)
        fun sprite(name: String) = atlas.getSprite(Identifier(BuildCraftAddon.MOD_ID, "block/engine/$name"))
        trunkBlue = sprite("trunk_blue")
        trunkGreen = sprite("trunk_green")
        trunkYellow = sprite("trunk_yellow")
        trunkRed = sprite("trunk_red")
        trunkOverheat = sprite("trunk_overheat")
        chamberSprite = sprite("chamber_base")
        backSprite = sprite("wood_back")
        sideSprite = sprite("wood_side")
        spritesLoaded = true
    }

    private fun getTrunkSprite(stage: PowerStage): Sprite = when (stage) {
        PowerStage.BLUE -> trunkBlue
        PowerStage.GREEN -> trunkGreen
        PowerStage.YELLOW -> trunkYellow
        PowerStage.RED -> trunkRed
        PowerStage.OVERHEAT -> trunkOverheat
        PowerStage.BLACK -> trunkBlue
    }

    private fun applyDirectionRotation(matrices: MatrixStack, direction: Direction) {
        if (direction == Direction.UP) return
        matrices.translate(0.5, 0.5, 0.5)
        when (direction) {
            Direction.DOWN ->  matrices.multiply(Quaternionf().rotateX(Math.toRadians(180.0).toFloat()))
            Direction.NORTH -> matrices.multiply(Quaternionf().rotateX(Math.toRadians(-90.0).toFloat()))
            Direction.SOUTH -> matrices.multiply(Quaternionf().rotateX(Math.toRadians(90.0).toFloat()))
            Direction.WEST ->  matrices.multiply(Quaternionf().rotateZ(Math.toRadians(90.0).toFloat()))
            Direction.EAST ->  matrices.multiply(Quaternionf().rotateZ(Math.toRadians(-90.0).toFloat()))
            Direction.UP ->    {}
        }
        matrices.translate(-0.5, -0.5, -0.5)
    }

    override fun render(
        entity: RedstoneEngineBlockEntity,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        ensureSprites()

        val progress = entity.progress
        val stage = entity.currentStage

        val progressSize = if (progress > 0.5f) {
            (1f - progress) * 15.98f
        } else {
            progress * 15.98f
        }
        val ps = progressSize / 16f

        matrices.push()
        val facing = entity.cachedState.get(Properties.FACING)
        applyDirectionRotation(matrices, facing)

        val world = entity.world
        val pos = entity.pos

        val baseLight = if (world != null) {
            WorldRenderer.getLightmapCoordinates(world, pos)
        } else {
            light
        }
        val topLight = if (world != null) {
            WorldRenderer.getLightmapCoordinates(world, pos.up())
        } else {
            light
        }

        val buffer = vertexConsumers.getBuffer(RenderLayer.getCutout())

        // Part 1: base (0,0,0) to (1, 0.25, 1)
        renderBox(matrices, buffer, baseLight, overlay,
            0f, 0f, 0f, 1f, 0.25f, 1f, backSprite, sideSprite, allFaces = true)

        // Part 2: base_moving - piston head slides with progress
        renderBox(matrices, buffer, topLight, overlay,
            0f, 0.25f + ps, 0f, 1f, 0.5f + ps, 1f, backSprite, sideSprite, allFaces = true)

        // Part 3: trunk (0.25, 0.25, 0.25) to (0.75, 1, 0.75)
        val trunkSpr = getTrunkSprite(stage)
        renderTrunkBox(matrices, buffer, topLight, overlay,
            0.25f, 0.25f, 0.25f, 0.75f, 1f, 0.75f, trunkSpr)

        // Part 4: chamber frame - only side faces
        if (ps > 0.001f) {
            renderChamberBox(matrices, buffer, topLight, overlay, progressSize,
                3f / 16f, 0.25f, 3f / 16f, 13f / 16f, 0.25f + ps, 13f / 16f,
                chamberSprite)
        }

        matrices.pop()
    }

    private fun renderTrunkBox(
        matrices: MatrixStack, buffer: VertexConsumer, light: Int, overlay: Int,
        x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float,
        sprite: Sprite
    ) {
        val ctx = matrices.peek()
        val posMat = ctx.positionMatrix
        val normMat = ctx.normalMatrix

        emitQuadBothSidesPx(buffer, posMat, normMat, light, overlay,
            x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2,
            0f, 0f, 8f, 0f, 8f, 8f, 0f, 8f, 0f, -1f, 0f, sprite)
        emitQuadBothSidesPx(buffer, posMat, normMat, light, overlay,
            x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2,
            0f, 0f, 8f, 0f, 8f, 8f, 0f, 8f, 0f, 1f, 0f, sprite)
        emitQuadBothSidesPx(buffer, posMat, normMat, light, overlay,
            x2, y1, z1, x1, y1, z1, x1, y2, z1, x2, y2, z1,
            8f, 12f, 16f, 12f, 16f, 0f, 8f, 0f, 0f, 0f, -1f, sprite)
        emitQuadBothSidesPx(buffer, posMat, normMat, light, overlay,
            x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2,
            8f, 12f, 16f, 12f, 16f, 0f, 8f, 0f, 0f, 0f, 1f, sprite)
        emitQuadBothSidesPx(buffer, posMat, normMat, light, overlay,
            x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1,
            8f, 12f, 16f, 12f, 16f, 0f, 8f, 0f, -1f, 0f, 0f, sprite)
        emitQuadBothSidesPx(buffer, posMat, normMat, light, overlay,
            x2, y1, z2, x2, y1, z1, x2, y2, z1, x2, y2, z2,
            8f, 12f, 16f, 12f, 16f, 0f, 8f, 0f, 1f, 0f, 0f, sprite)
    }

    private fun renderChamberBox(
        matrices: MatrixStack, buffer: VertexConsumer, light: Int, overlay: Int, progressSize: Float,
        x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float,
        sprite: Sprite
    ) {
        val ctx = matrices.peek()
        val posMat = ctx.positionMatrix
        val normMat = ctx.normalMatrix
        val vBottom = progressSize.coerceIn(0f, 16f)
        val vTop = 0f

        emitQuadBothSidesPx(buffer, posMat, normMat, light, overlay,
            x2, y1, z1, x1, y1, z1, x1, y2, z1, x2, y2, z1,
            3f, vBottom, 13f, vBottom, 13f, vTop, 3f, vTop, 0f, 0f, -1f, sprite)
        emitQuadBothSidesPx(buffer, posMat, normMat, light, overlay,
            x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2,
            3f, vBottom, 13f, vBottom, 13f, vTop, 3f, vTop, 0f, 0f, 1f, sprite)
        emitQuadBothSidesPx(buffer, posMat, normMat, light, overlay,
            x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1,
            3f, vBottom, 13f, vBottom, 13f, vTop, 3f, vTop, -1f, 0f, 0f, sprite)
        emitQuadBothSidesPx(buffer, posMat, normMat, light, overlay,
            x2, y1, z2, x2, y1, z1, x2, y2, z1, x2, y2, z2,
            3f, vBottom, 13f, vBottom, 13f, vTop, 3f, vTop, 1f, 0f, 0f, sprite)
    }

    private fun renderBox(
        matrices: MatrixStack, buffer: VertexConsumer, light: Int, overlay: Int,
        x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float,
        topBottomSprite: Sprite, sideSprite: Sprite, allFaces: Boolean
    ) {
        val ctx = matrices.peek()
        val posMat = ctx.positionMatrix
        val normMat = ctx.normalMatrix

        if (allFaces) {
            emitQuadBothSides(buffer, posMat, normMat, light, overlay,
                x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2,
                0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f, 0f, -1f, 0f, topBottomSprite)
            emitQuadBothSides(buffer, posMat, normMat, light, overlay,
                x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2,
                0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f, 0f, 1f, 0f, topBottomSprite)
        }
        emitQuadBothSides(buffer, posMat, normMat, light, overlay,
            x2, y1, z1, x1, y1, z1, x1, y2, z1, x2, y2, z1,
            0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f, 0f, 0f, -1f, sideSprite)
        emitQuadBothSides(buffer, posMat, normMat, light, overlay,
            x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2,
            0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, sideSprite)
        emitQuadBothSides(buffer, posMat, normMat, light, overlay,
            x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1,
            0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f, -1f, 0f, 0f, sideSprite)
        emitQuadBothSides(buffer, posMat, normMat, light, overlay,
            x2, y1, z2, x2, y1, z1, x2, y2, z1, x2, y2, z2,
            0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f, 1f, 0f, 0f, sideSprite)
    }

    private fun emitQuadBothSides(
        buffer: VertexConsumer,
        posMat: Matrix4f, normMat: Matrix3f, light: Int, overlay: Int,
        x0: Float, y0: Float, z0: Float, x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float, x3: Float, y3: Float, z3: Float,
        u0: Float, v0: Float, u1: Float, v1: Float, u2: Float, v2: Float, u3: Float, v3: Float,
        nx: Float, ny: Float, nz: Float, sprite: Sprite
    ) {
        emitQuad(buffer, posMat, normMat, light, overlay,
            x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3,
            u0, v0, u1, v1, u2, v2, u3, v3,
            nx, ny, nz, sprite)
        emitQuad(buffer, posMat, normMat, light, overlay,
            x3, y3, z3, x2, y2, z2, x1, y1, z1, x0, y0, z0,
            u3, v3, u2, v2, u1, v1, u0, v0,
            -nx, -ny, -nz, sprite)
    }

    private fun emitQuadBothSidesPx(
        buffer: VertexConsumer,
        posMat: Matrix4f, normMat: Matrix3f, light: Int, overlay: Int,
        x0: Float, y0: Float, z0: Float, x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float, x3: Float, y3: Float, z3: Float,
        u0: Float, v0: Float, u1: Float, v1: Float, u2: Float, v2: Float, u3: Float, v3: Float,
        nx: Float, ny: Float, nz: Float, sprite: Sprite
    ) {
        val su0 = u0 / 16f; val sv0 = v0 / 16f
        val su1 = u1 / 16f; val sv1 = v1 / 16f
        val su2 = u2 / 16f; val sv2 = v2 / 16f
        val su3 = u3 / 16f; val sv3 = v3 / 16f
        emitQuadBothSides(
            buffer, posMat, normMat, light, overlay,
            x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3,
            su0, sv0, su1, sv1, su2, sv2, su3, sv3, nx, ny, nz, sprite
        )
    }

    private fun emitQuad(
        buffer: VertexConsumer,
        posMat: Matrix4f, normMat: Matrix3f, light: Int, overlay: Int,
        x0: Float, y0: Float, z0: Float, x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float, x3: Float, y3: Float, z3: Float,
        u0: Float, v0: Float, u1: Float, v1: Float, u2: Float, v2: Float, u3: Float, v3: Float,
        nx: Float, ny: Float, nz: Float, sprite: Sprite
    ) {
        val minU = sprite.minU; val maxU = sprite.maxU
        val minV = sprite.minV; val maxV = sprite.maxV
        fun mapU(u: Float) = minU + u * (maxU - minU)
        fun mapV(v: Float) = minV + v * (maxV - minV)

        buffer.vertex(posMat, x0, y0, z0).color(255, 255, 255, 255)
            .texture(mapU(u0), mapV(v0)).overlay(overlay).light(light).normal(normMat, nx, ny, nz).next()
        buffer.vertex(posMat, x1, y1, z1).color(255, 255, 255, 255)
            .texture(mapU(u1), mapV(v1)).overlay(overlay).light(light).normal(normMat, nx, ny, nz).next()
        buffer.vertex(posMat, x2, y2, z2).color(255, 255, 255, 255)
            .texture(mapU(u2), mapV(v2)).overlay(overlay).light(light).normal(normMat, nx, ny, nz).next()
        buffer.vertex(posMat, x3, y3, z3).color(255, 255, 255, 255)
            .texture(mapU(u3), mapV(v3)).overlay(overlay).light(light).normal(normMat, nx, ny, nz).next()
    }

    override fun rendersOutsideBoundingBox(entity: RedstoneEngineBlockEntity): Boolean = true
}
