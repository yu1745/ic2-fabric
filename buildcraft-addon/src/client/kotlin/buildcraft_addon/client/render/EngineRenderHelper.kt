package buildcraft_addon.client.render

import buildcraft_addon.content.blockentity.PowerStage
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.texture.Sprite
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import org.joml.Quaternionf
import net.minecraft.util.math.BlockPos

object EngineRenderHelper {

    fun applyDirectionRotation(matrices: MatrixStack, direction: Direction) {
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

    fun renderEngine(
        matrices: MatrixStack,
        buffer: VertexConsumer,
        light: Int,
        overlay: Int,
        progress: Float,
        stage: PowerStage,
        facing: Direction,
        world: World?,
        pos: BlockPos,
        backSprite: Sprite,
        sideSprite: Sprite,
        trunkSprite: Sprite,
        chamberSprite: Sprite
    ) {
        val progressSize = if (progress > 0.5f) {
            (1f - progress) * 15.98f
        } else {
            progress * 15.98f
        }
        val ps = progressSize / 16f

        matrices.push()
        applyDirectionRotation(matrices, facing)

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

        // Part 1: base (0,0,0) to (1, 0.25, 1)
        renderBox(matrices, buffer, baseLight, overlay,
            0f, 0f, 0f, 1f, 0.25f, 1f, backSprite, sideSprite, allFaces = true)

        // Part 2: base_moving - piston head slides with progress
        renderBox(matrices, buffer, topLight, overlay,
            0f, 0.25f + ps, 0f, 1f, 0.5f + ps, 1f, backSprite, sideSprite, allFaces = true)

        // Part 3: trunk (0.25, 0.25, 0.25) to (0.75, 1, 0.75)
        renderTrunkBox(matrices, buffer, topLight, overlay,
            0.25f, 0.25f, 0.25f, 0.75f, 1f, 0.75f, trunkSprite)

        // Part 4: chamber frame - only side faces
        if (ps > 0.001f) {
            renderChamberBox(matrices, buffer, topLight, overlay, progressSize,
                3f / 16f, 0.25f, 3f / 16f, 13f / 16f, 0.25f + ps, 13f / 16f,
                chamberSprite)
        }

        matrices.pop()
    }

    fun renderTrunkBox(
        matrices: MatrixStack, buffer: VertexConsumer, light: Int, overlay: Int,
        x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float,
        sprite: Sprite
    ) {
        val ctx = matrices.peek()
        emitQuadBothSidesPx(buffer, ctx, light, overlay,
            x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2,
            0f, 0f, 8f, 0f, 8f, 8f, 0f, 8f, 0f, -1f, 0f, sprite)
        emitQuadBothSidesPx(buffer, ctx, light, overlay,
            x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2,
            0f, 0f, 8f, 0f, 8f, 8f, 0f, 8f, 0f, 1f, 0f, sprite)
        emitQuadBothSidesPx(buffer, ctx, light, overlay,
            x2, y1, z1, x1, y1, z1, x1, y2, z1, x2, y2, z1,
            8f, 12f, 16f, 12f, 16f, 0f, 8f, 0f, 0f, 0f, -1f, sprite)
        emitQuadBothSidesPx(buffer, ctx, light, overlay,
            x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2,
            8f, 12f, 16f, 12f, 16f, 0f, 8f, 0f, 0f, 0f, 1f, sprite)
        emitQuadBothSidesPx(buffer, ctx, light, overlay,
            x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1,
            8f, 12f, 16f, 12f, 16f, 0f, 8f, 0f, -1f, 0f, 0f, sprite)
        emitQuadBothSidesPx(buffer, ctx, light, overlay,
            x2, y1, z2, x2, y1, z1, x2, y2, z1, x2, y2, z2,
            8f, 12f, 16f, 12f, 16f, 0f, 8f, 0f, 1f, 0f, 0f, sprite)
    }

    fun renderChamberBox(
        matrices: MatrixStack, buffer: VertexConsumer, light: Int, overlay: Int, progressSize: Float,
        x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float,
        sprite: Sprite
    ) {
        val ctx = matrices.peek()
        val vBottom = progressSize.coerceIn(0f, 16f)
        val vTop = 0f

        emitQuadBothSidesPx(buffer, ctx, light, overlay,
            x2, y1, z1, x1, y1, z1, x1, y2, z1, x2, y2, z1,
            3f, vBottom, 13f, vBottom, 13f, vTop, 3f, vTop, 0f, 0f, -1f, sprite)
        emitQuadBothSidesPx(buffer, ctx, light, overlay,
            x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2,
            3f, vBottom, 13f, vBottom, 13f, vTop, 3f, vTop, 0f, 0f, 1f, sprite)
        emitQuadBothSidesPx(buffer, ctx, light, overlay,
            x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1,
            3f, vBottom, 13f, vBottom, 13f, vTop, 3f, vTop, -1f, 0f, 0f, sprite)
        emitQuadBothSidesPx(buffer, ctx, light, overlay,
            x2, y1, z2, x2, y1, z1, x2, y2, z1, x2, y2, z2,
            3f, vBottom, 13f, vBottom, 13f, vTop, 3f, vTop, 1f, 0f, 0f, sprite)
    }

    fun renderBox(
        matrices: MatrixStack, buffer: VertexConsumer, light: Int, overlay: Int,
        x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float,
        topBottomSprite: Sprite, sideSprite: Sprite, allFaces: Boolean
    ) {
        val ctx = matrices.peek()
        if (allFaces) {
            emitQuadBothSides(buffer, ctx, light, overlay,
                x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2,
                0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f, 0f, -1f, 0f, topBottomSprite)
            emitQuadBothSides(buffer, ctx, light, overlay,
                x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2,
                0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f, 0f, 1f, 0f, topBottomSprite)
        }
        emitQuadBothSides(buffer, ctx, light, overlay,
            x2, y1, z1, x1, y1, z1, x1, y2, z1, x2, y2, z1,
            0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f, 0f, 0f, -1f, sideSprite)
        emitQuadBothSides(buffer, ctx, light, overlay,
            x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2,
            0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, sideSprite)
        emitQuadBothSides(buffer, ctx, light, overlay,
            x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1,
            0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f, -1f, 0f, 0f, sideSprite)
        emitQuadBothSides(buffer, ctx, light, overlay,
            x2, y1, z2, x2, y1, z1, x2, y2, z1, x2, y2, z2,
            0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f, 1f, 0f, 0f, sideSprite)
    }

    fun emitQuadBothSides(
        buffer: VertexConsumer,
        entry: MatrixStack.Entry, light: Int, overlay: Int,
        x0: Float, y0: Float, z0: Float, x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float, x3: Float, y3: Float, z3: Float,
        u0: Float, v0: Float, u1: Float, v1: Float, u2: Float, v2: Float, u3: Float, v3: Float,
        nx: Float, ny: Float, nz: Float, sprite: Sprite
    ) {
        emitQuad(buffer, entry, light, overlay,
            x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3,
            u0, v0, u1, v1, u2, v2, u3, v3,
            nx, ny, nz, sprite)
        emitQuad(buffer, entry, light, overlay,
            x3, y3, z3, x2, y2, z2, x1, y1, z1, x0, y0, z0,
            u3, v3, u2, v2, u1, v1, u0, v0,
            -nx, -ny, -nz, sprite)
    }

    fun emitQuadBothSidesPx(
        buffer: VertexConsumer,
        entry: MatrixStack.Entry, light: Int, overlay: Int,
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
            buffer, entry, light, overlay,
            x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3,
            su0, sv0, su1, sv1, su2, sv2, su3, sv3, nx, ny, nz, sprite
        )
    }

    fun emitQuad(
        buffer: VertexConsumer,
        entry: MatrixStack.Entry, light: Int, overlay: Int,
        x0: Float, y0: Float, z0: Float, x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float, x3: Float, y3: Float, z3: Float,
        u0: Float, v0: Float, u1: Float, v1: Float, u2: Float, v2: Float, u3: Float, v3: Float,
        nx: Float, ny: Float, nz: Float, sprite: Sprite
    ) {
        val minU = sprite.minU; val maxU = sprite.maxU
        val minV = sprite.minV; val maxV = sprite.maxV
        fun mapU(u: Float) = minU + u * (maxU - minU)
        fun mapV(v: Float) = minV + v * (maxV - minV)

        buffer.vertex(entry.positionMatrix, x0, y0, z0).color(255, 255, 255, 255)
            .texture(mapU(u0), mapV(v0)).overlay(overlay).light(light).normal(entry, nx, ny, nz)
        buffer.vertex(entry.positionMatrix, x1, y1, z1).color(255, 255, 255, 255)
            .texture(mapU(u1), mapV(v1)).overlay(overlay).light(light).normal(entry, nx, ny, nz)
        buffer.vertex(entry.positionMatrix, x2, y2, z2).color(255, 255, 255, 255)
            .texture(mapU(u2), mapV(v2)).overlay(overlay).light(light).normal(entry, nx, ny, nz)
        buffer.vertex(entry.positionMatrix, x3, y3, z3).color(255, 255, 255, 255)
            .texture(mapU(u3), mapV(v3)).overlay(overlay).light(light).normal(entry, nx, ny, nz)
    }
}
