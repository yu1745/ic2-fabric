package buildcraft_addon.client.render

import buildcraft_addon.BuildCraftAddon
import buildcraft_addon.content.blockentity.PowerStage
import buildcraft_addon.content.blockentity.RedstoneEngineBlockEntity
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.RenderType
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.Direction
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Quaternionf
import kotlin.math.cos
import kotlin.math.sin
import ic2_120.compat.*

class RedstoneEngineRenderer(ctx: BlockEntityRendererProvider.Context) :
    BlockEntityRenderer<RedstoneEngineBlockEntity> {

    private lateinit var trunkBlue: TextureAtlasSprite
    private lateinit var trunkGreen: TextureAtlasSprite
    private lateinit var trunkYellow: TextureAtlasSprite
    private lateinit var trunkRed: TextureAtlasSprite
    private lateinit var trunkOverheat: TextureAtlasSprite
    private lateinit var chamberSprite: TextureAtlasSprite
    private lateinit var backSprite: TextureAtlasSprite
    private lateinit var sideSprite: TextureAtlasSprite

    private var spritesLoaded = false

    private fun ensureSprites() {
        if (spritesLoaded) return
        val atlas = Minecraft.getInstance().modelManager.getAtlas(InventoryMenu.BLOCK_ATLAS)
        fun sprite(name: String) = atlas.getSprite(ResourceLocation.fromNamespaceAndPath(BuildCraftAddon.MOD_ID, "block/engine/$name"))
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

    private fun getTrunkSprite(stage: PowerStage): TextureAtlasSprite = when (stage) {
        PowerStage.BLUE -> trunkBlue
        PowerStage.GREEN -> trunkGreen
        PowerStage.YELLOW -> trunkYellow
        PowerStage.RED -> trunkRed
        PowerStage.OVERHEAT -> trunkOverheat
        PowerStage.BLACK -> trunkBlue
    }

    private fun applyDirectionRotation(matrices: PoseStack, direction: Direction) {
        if (direction == Direction.UP) return
        matrices.translate(0.5, 0.5, 0.5)
        when (direction) {
            Direction.DOWN ->  matrices.mulPose(Quaternionf().rotateX(Math.toRadians(180.0).toFloat()))
            Direction.NORTH -> matrices.mulPose(Quaternionf().rotateX(Math.toRadians(-90.0).toFloat()))
            Direction.SOUTH -> matrices.mulPose(Quaternionf().rotateX(Math.toRadians(90.0).toFloat()))
            Direction.WEST ->  matrices.mulPose(Quaternionf().rotateZ(Math.toRadians(90.0).toFloat()))
            Direction.EAST ->  matrices.mulPose(Quaternionf().rotateZ(Math.toRadians(-90.0).toFloat()))
            Direction.UP ->    {}
        }
        matrices.translate(-0.5, -0.5, -0.5)
    }

    override fun render(
        entity: RedstoneEngineBlockEntity,
        tickDelta: Float,
        matrices: PoseStack,
        vertexConsumers: MultiBufferSource,
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

        matrices.pushPose()
        val facing = entity.cachedState.getValue(BlockStateProperties.FACING)
        applyDirectionRotation(matrices, facing)

        val world = entity.world
        val pos = entity.pos

        val baseLight = if (world != null) {
            LevelRenderer.getLightColor(world, pos)
        } else {
            light
        }
        val topLight = if (world != null) {
            LevelRenderer.getLightColor(world, pos.above())
        } else {
            light
        }

        val buffer = vertexConsumers.getBuffer(RenderType.cutout())

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

        matrices.popPose()
    }

    private fun renderTrunkBox(
        matrices: PoseStack, buffer: VertexConsumer, light: Int, overlay: Int,
        x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float,
        sprite: TextureAtlasSprite
    ) {
        val ctx = matrices.last()

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

    private fun renderChamberBox(
        matrices: PoseStack, buffer: VertexConsumer, light: Int, overlay: Int, progressSize: Float,
        x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float,
        sprite: TextureAtlasSprite
    ) {
        val ctx = matrices.last()
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

    private fun renderBox(
        matrices: PoseStack, buffer: VertexConsumer, light: Int, overlay: Int,
        x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float,
        topBottomSprite: TextureAtlasSprite, sideSprite: TextureAtlasSprite, allFaces: Boolean
    ) {
        val ctx = matrices.last()

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

    private fun emitQuadBothSides(
        buffer: VertexConsumer,
        entry: PoseStack.Pose, light: Int, overlay: Int,
        x0: Float, y0: Float, z0: Float, x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float, x3: Float, y3: Float, z3: Float,
        u0: Float, v0: Float, u1: Float, v1: Float, u2: Float, v2: Float, u3: Float, v3: Float,
        nx: Float, ny: Float, nz: Float, sprite: TextureAtlasSprite
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

    private fun emitQuadBothSidesPx(
        buffer: VertexConsumer,
        entry: PoseStack.Pose, light: Int, overlay: Int,
        x0: Float, y0: Float, z0: Float, x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float, x3: Float, y3: Float, z3: Float,
        u0: Float, v0: Float, u1: Float, v1: Float, u2: Float, v2: Float, u3: Float, v3: Float,
        nx: Float, ny: Float, nz: Float, sprite: TextureAtlasSprite
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

    private fun emitQuad(
        buffer: VertexConsumer,
        entry: PoseStack.Pose, light: Int, overlay: Int,
        x0: Float, y0: Float, z0: Float, x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float, x3: Float, y3: Float, z3: Float,
        u0: Float, v0: Float, u1: Float, v1: Float, u2: Float, v2: Float, u3: Float, v3: Float,
        nx: Float, ny: Float, nz: Float, sprite: TextureAtlasSprite
    ) {
        val minU = sprite.u0; val maxU = sprite.u1
        val minV = sprite.v0; val maxV = sprite.v1
        fun mapU(u: Float) = minU + u * (maxU - minU)
        fun mapV(v: Float) = minV + v * (maxV - minV)

        buffer.addVertex(entry, x0, y0, z0).setColor(255, 255, 255, 255)
            .setUv(mapU(u0), mapV(v0)).setOverlay(overlay).setLight(light).setNormal(entry, nx, ny, nz)
        buffer.addVertex(entry, x1, y1, z1).setColor(255, 255, 255, 255)
            .setUv(mapU(u1), mapV(v1)).setOverlay(overlay).setLight(light).setNormal(entry, nx, ny, nz)
        buffer.addVertex(entry, x2, y2, z2).setColor(255, 255, 255, 255)
            .setUv(mapU(u2), mapV(v2)).setOverlay(overlay).setLight(light).setNormal(entry, nx, ny, nz)
        buffer.addVertex(entry, x3, y3, z3).setColor(255, 255, 255, 255)
            .setUv(mapU(u3), mapV(v3)).setOverlay(overlay).setLight(light).setNormal(entry, nx, ny, nz)
    }

    override fun shouldRenderOffScreen(entity: RedstoneEngineBlockEntity): Boolean = true
}
