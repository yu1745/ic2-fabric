package buildcraft_addon.client.render

import buildcraft_addon.BuildCraftAddon
import buildcraft_addon.content.blockentity.PumpBlockEntity
import buildcraft_addon.client.render.EngineRenderHelper.emitQuadBothSides
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory
import net.minecraft.client.texture.Sprite
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.screen.PlayerScreenHandler
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

class PumpRenderer(ctx: BlockEntityRendererFactory.Context) :
    BlockEntityRenderer<PumpBlockEntity> {

    private var spritesLoaded = false
    private lateinit var tubeSprite: Sprite
    private lateinit var ledGreen: Sprite
    private lateinit var ledRed: Sprite

    private fun ensureSprites() {
        if (spritesLoaded) return
        val atlas = MinecraftClient.getInstance().bakedModelManager.getAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE)
        fun sprite(path: String) = atlas.getSprite(Identifier.of(BuildCraftAddon.MOD_ID, path))
        tubeSprite = sprite("block/pump/tube")
        ledGreen = sprite("block/pump/led_green")
        ledRed = sprite("block/pump/led_red")
        spritesLoaded = true
    }

    override fun render(
        entity: PumpBlockEntity,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        ensureSprites()
        val world = entity.world ?: return
        val pos = entity.pos
        val buffer = vertexConsumers.getBuffer(RenderLayer.getCutout())

        // ========== 管道渲染 ==========
        // MatrixStack 已平移到方块位置，使用局部坐标
        val depth = entity.getRenderTubeDepth(tickDelta).toFloat()
        if (depth > 0.01f) {
            matrices.push()
            val halfTube = 1.0f / 16.0f
            val cx = 0.5f
            val cz = 0.5f
            // 管道从泵底部 (y=0) 向下延伸到 y=-depth
            renderTubeColumn(matrices, buffer, world, pos, overlay,
                cx - halfTube, -depth, cz - halfTube,
                cx + halfTube, 0f, cz + halfTube)
            matrices.pop()
        }

        // ========== LED 指示灯 ==========
        val pumpLight = WorldRenderer.getLightmapCoordinates(world, pos)
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
        matrices: MatrixStack, buffer: VertexConsumer,
        world: net.minecraft.world.World, pos: BlockPos, overlay: Int,
        x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float
    ) {
        val ctx = matrices.peek()

        val height = y2 - y1
        val fullBlocks = kotlin.math.floor(height.toDouble()).toInt()
        val frac = height - fullBlocks

        // 底部不满一格
        if (frac > 0.001f) {
            val segBot = y1
            val segTop = y1 + frac
            // 取该格的世界坐标算光照
            val worldY = pos.y + kotlin.math.floor(segBot.toDouble()).toInt()
            val segLight = WorldRenderer.getLightmapCoordinates(world, BlockPos(pos.x, worldY, pos.z))
            renderTubeSegment(ctx, buffer, segLight, overlay, x1, segBot, z1, x2, segTop, z2, frac)
        }

        // 完整格
        for (i in 0 until fullBlocks) {
            val segBot = y1 + frac + i
            val segTop = segBot + 1f
            val worldY = pos.y + kotlin.math.floor(segBot.toDouble()).toInt()
            val segLight = WorldRenderer.getLightmapCoordinates(world, BlockPos(pos.x, worldY, pos.z))
            renderTubeSegment(ctx, buffer, segLight, overlay, x1, segBot, z1, x2, segTop, z2, 1f)
        }
    }

    private fun renderTubeSegment(
        entry: MatrixStack.Entry, buffer: VertexConsumer, light: Int, overlay: Int,
        x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float,
        vScale: Float
    ) {
        val sp = tubeSprite
        val vt = 1f
        val vb = 1f - vScale

        // NORTH (z=z1)
        emitQuadBothSides(buffer, entry, light, overlay,
            x1, y1, z1, x2, y1, z1, x2, y2, z1, x1, y2, z1,
            0f, vt, 1f, vt, 1f, vb, 0f, vb, 0f, 0f, -1f, sp)
        // SOUTH (z=z2)
        emitQuadBothSides(buffer, entry, light, overlay,
            x2, y1, z2, x1, y1, z2, x1, y2, z2, x2, y2, z2,
            0f, vt, 1f, vt, 1f, vb, 0f, vb, 0f, 0f, 1f, sp)
        // WEST (x=x1)
        emitQuadBothSides(buffer, entry, light, overlay,
            x1, y1, z2, x1, y1, z1, x1, y2, z1, x1, y2, z2,
            0f, vt, 1f, vt, 1f, vb, 0f, vb, -1f, 0f, 0f, sp)
        // EAST (x=x2)
        emitQuadBothSides(buffer, entry, light, overlay,
            x2, y1, z1, x2, y1, z2, x2, y2, z2, x2, y2, z1,
            0f, vt, 1f, vt, 1f, vb, 0f, vb, 1f, 0f, 0f, sp)
    }

    private fun renderLedSide(
        matrices: MatrixStack, buffer: VertexConsumer, light: Int, overlay: Int,
        x: Float, y: Float, z: Float,
        side: Direction, sprite: Sprite, brightness: Float
    ) {
        val ctx = matrices.peek()
        val size = 0.8f / 16f

        fun quad(nx: Float, ny: Float, nz: Float, offset: Float) {
            emitQuadBothSides(buffer, ctx, light, overlay,
                x + size, y - size, z + offset, x - size, y - size, z + offset,
                x - size, y + size, z + offset, x + size, y + size, z + offset,
                0f, 0f, 1f, 0f,
                1f, 1f, 0f, 1f,
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

    override fun rendersOutsideBoundingBox(entity: PumpBlockEntity): Boolean = true
}
