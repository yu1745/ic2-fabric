package buildcraft_addon.client.render

import buildcraft_addon.BuildCraftAddon
import buildcraft_addon.content.blockentity.PumpBlockEntity
import buildcraft_addon.client.render.EngineRenderHelper.emitQuadBothSides
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderType
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import org.joml.Matrix3f
import org.joml.Matrix4f
import ic2_120.compat.*

class PumpRenderer(ctx: BlockEntityRendererProvider.Context) :
    BlockEntityRenderer<PumpBlockEntity> {

    private var spritesLoaded = false
    private lateinit var tubeSprite: TextureAtlasSprite
    private lateinit var ledGreen: TextureAtlasSprite
    private lateinit var ledRed: TextureAtlasSprite

    private fun ensureSprites() {
        if (spritesLoaded) return
        val atlas = Minecraft.getInstance().modelManager.getAtlas(InventoryMenu.BLOCK_ATLAS)
        fun sprite(path: String) = atlas.getSprite(ResourceLocation.fromNamespaceAndPath(BuildCraftAddon.MOD_ID, path))
        tubeSprite = sprite("block/pump/tube")
        ledGreen = sprite("block/pump/led_green")
        ledRed = sprite("block/pump/led_red")
        spritesLoaded = true
    }

    override fun render(
        entity: PumpBlockEntity,
        tickDelta: Float,
        matrices: PoseStack,
        vertexConsumers: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        ensureSprites()
        val world = entity.world ?: return
        val pos = entity.pos
        val buffer = vertexConsumers.getBuffer(RenderType.cutout())

        // ========== 管道渲染 ==========
        // PoseStack 已平移到方块位置，使用局部坐标
        val depth = entity.getRenderTubeDepth(tickDelta).toFloat()
        if (depth > 0.01f) {
            matrices.pushPose()
            val halfTube = 1.0f / 16.0f
            val cx = 0.5f
            val cz = 0.5f
            // 管道从泵底部 (y=0) 向下延伸到 y=-depth
            renderTubeColumn(matrices, buffer, world, pos, overlay,
                cx - halfTube, -depth, cz - halfTube,
                cx + halfTube, 0f, cz + halfTube)
            matrices.popPose()
        }

        // ========== LED 指示灯 ==========
        val pumpLight = LevelRenderer.getLightColor(world, pos)
        val active = entity.isActive
        val tankFill = if (entity.tank.capacity > 0)
            entity.tank.amount.toFloat() / entity.tank.capacity.toFloat() else 0f

        for (dir in listOf(Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH)) {
            val lx = when (dir) {
                Direction.WEST -> 1.5f / 16f
                Direction.EAST -> 14.5f / 16f
                else -> 0.5f
            }
            val lz = when (dir) {
                Direction.NORTH -> 1.5f / 16f
                Direction.SOUTH -> 14.5f / 16f
                else -> 0.5f
            }

            // 状态 LED
            val statusBright = if (active) 0.85f else 0.05f
            val statusLed = if (active) ledGreen else ledRed
            renderLedSide(matrices, buffer, pumpLight, overlay,
                lx, 13.5f / 16f, lz, dir, statusLed, statusBright)

            // 储罐填充 LED
            renderLedSide(matrices, buffer, pumpLight, overlay,
                lx, 12f / 16f, lz, dir, ledGreen, tankFill.coerceIn(0f, 1f))
        }
    }

    private fun renderTubeColumn(
        matrices: PoseStack, buffer: VertexConsumer,
        world: net.minecraft.world.level.Level, pos: BlockPos, overlay: Int,
        x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float
    ) {
        val ctx = matrices.last()

        val height = y2 - y1
        val fullBlocks = kotlin.math.floor(height.toDouble()).toInt()
        val frac = height - fullBlocks

        // 底部不满一格
        if (frac > 0.001f) {
            val segBot = y1
            val segTop = y1 + frac
            // 取该格的世界坐标算光照
            val worldY = pos.y + kotlin.math.floor(segBot.toDouble()).toInt()
            val segLight = LevelRenderer.getLightColor(world, BlockPos(pos.x, worldY, pos.z))
            renderTubeSegment(ctx, buffer, segLight, overlay, x1, segBot, z1, x2, segTop, z2, frac)
        }

        // 完整格
        for (i in 0 until fullBlocks) {
            val segBot = y1 + frac + i
            val segTop = segBot + 1f
            val worldY = pos.y + kotlin.math.floor(segBot.toDouble()).toInt()
            val segLight = LevelRenderer.getLightColor(world, BlockPos(pos.x, worldY, pos.z))
            renderTubeSegment(ctx, buffer, segLight, overlay, x1, segBot, z1, x2, segTop, z2, 1f)
        }
    }

    private fun renderTubeSegment(
        entry: PoseStack.Pose, buffer: VertexConsumer, light: Int, overlay: Int,
        x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float,
        vScale: Float
    ) {
        val sp = tubeSprite
        val vt = 1f
        val vb = 1f - vScale

        // NORTH (z=z1)
        emitQuadBothSides(buffer, entry, light, overlay,
            x1, y1, z1, x2, y1, z1, x2, y2, z1, x1, y2, z1,
            sp.u0, vt, sp.u1, vt, sp.u1, vb, sp.u0, vb, 0f, 0f, -1f, sp)
        // SOUTH (z=z2)
        emitQuadBothSides(buffer, entry, light, overlay,
            x2, y1, z2, x1, y1, z2, x1, y2, z2, x2, y2, z2,
            sp.u0, vt, sp.u1, vt, sp.u1, vb, sp.u0, vb, 0f, 0f, 1f, sp)
        // WEST (x=x1)
        emitQuadBothSides(buffer, entry, light, overlay,
            x1, y1, z2, x1, y1, z1, x1, y2, z1, x1, y2, z2,
            sp.u0, vt, sp.u1, vt, sp.u1, vb, sp.u0, vb, -1f, 0f, 0f, sp)
        // EAST (x=x2)
        emitQuadBothSides(buffer, entry, light, overlay,
            x2, y1, z1, x2, y1, z2, x2, y2, z2, x2, y2, z1,
            sp.u0, vt, sp.u1, vt, sp.u1, vb, sp.u0, vb, 1f, 0f, 0f, sp)
    }

    private fun renderLedSide(
        matrices: PoseStack, buffer: VertexConsumer, light: Int, overlay: Int,
        x: Float, y: Float, z: Float,
        side: Direction, sprite: TextureAtlasSprite, brightness: Float
    ) {
        val ctx = matrices.last()
        val size = 0.8f / 16f

        fun quad(nx: Float, ny: Float, nz: Float, offset: Float) {
            emitQuadBothSides(buffer, ctx, light, overlay,
                x + size, y - size, z + offset, x - size, y - size, z + offset,
                x - size, y + size, z + offset, x + size, y + size, z + offset,
                sprite.u0, sprite.v0, sprite.u1, sprite.v0,
                sprite.u1, sprite.v1, sprite.u0, sprite.v1,
                nx, ny, nz, sprite)
        }

        when (side) {
            Direction.NORTH -> quad(0f, 0f, -1f, -0.001f)
            Direction.SOUTH -> quad(0f, 0f, 1f, 0.001f)
            Direction.WEST  -> quad(-1f, 0f, 0f, 0f)
            Direction.EAST  -> quad(1f, 0f, 0f, 0f)
            else -> {}
        }
    }

    override fun shouldRenderOffScreen(entity: PumpBlockEntity): Boolean = true
}
