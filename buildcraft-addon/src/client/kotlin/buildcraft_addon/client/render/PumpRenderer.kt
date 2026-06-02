package buildcraft_addon.client.render

import buildcraft_addon.BuildCraftAddon
import buildcraft_addon.content.blockentity.PumpBlockEntity
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
import net.minecraft.util.math.MathHelper
import net.minecraft.world.LightType

class PumpRenderer(ctx: BlockEntityRendererFactory.Context) :
    BlockEntityRenderer<PumpBlockEntity> {

    private var spritesLoaded = false
    private lateinit var tubeSprite: Sprite
    private lateinit var ledGreen: Sprite
    private lateinit var ledRed: Sprite

    // LED 位置（4 个侧面，BC 原版 1:1）
    private data class LedPos(val x: Double, val z: Double)
    private val ledPositions = listOf(
        LedPos(0.4 / 16.0, 0.5),          // WEST
        LedPos(15.6 / 16.0, 0.5),          // EAST
        LedPos(0.5, 0.4 / 16.0),          // NORTH
        LedPos(0.5, 15.6 / 16.0),          // SOUTH
    )

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

        // ========== 管道渲染 ==========
        var tubeBottomY = pos.y.toDouble()
        for (y in pos.y - 1 downTo world.bottomY) {
            val checkPos = BlockPos(pos.x, y, pos.z)
            val state = world.getBlockState(checkPos)
            if (state.isOf(net.minecraft.block.Blocks.AIR)) {
                tubeBottomY = y.toDouble()
            } else {
                val fs = world.getFluidState(checkPos)
                if (!fs.isEmpty) { tubeBottomY = y.toDouble(); break }
                break
            }
        }

        val buffer = vertexConsumers.getBuffer(RenderLayer.getCutout())

        if (tubeBottomY < pos.y.toDouble()) {
            matrices.push()
            val halfTube = 2.0 / 16.0
            renderTubeBox(
                matrices, buffer,
                pos.x.toDouble() + 0.5 - halfTube, tubeBottomY,
                pos.z.toDouble() + 0.5 - halfTube,
                pos.x.toDouble() + 0.5 + halfTube, pos.y.toDouble(),
                pos.z.toDouble() + 0.5 + halfTube,
                tubeSprite
            )
            matrices.pop()
        }

        // ========== LED 指示灯（BC 原版 1:1） ==========
        matrices.push()
        val pumpLight = WorldRenderer.getLightmapCoordinates(world, pos)
        val active = entity.isActive
        val tankFill = if (entity.tank.capacity > 0)
            entity.tank.amount.toFloat() / entity.tank.capacity.toFloat() else 0f

        for ((lx, lz) in ledPositions) {
            // 状态 LED：工作中绿色，空闲暗色
            val statusSprite = if (active) ledGreen else ledRed
            val mixBright = if (active) 0.3f else 0.05f
            val statusBright = if (active) 0xF000F0 else pumpLight

            val size = 0.8 / 16.0
            val yPos = 13.5 / 16.0

            renderLed(matrices, buffer, statusBright, overlay,
                pos.x.toDouble() + lx - size, yPos - size, pos.z.toDouble() + lz - size,
                pos.x.toDouble() + lx + size, yPos + size, pos.z.toDouble() + lz + size,
                statusSprite, mixBright)

            // 能耗 LED：无能量系统，全亮蓝色表示"待接入"
            val powerFill = tankFill.coerceIn(0f, 1f)
            val powerBright = MathHelper.lerp(powerFill, 0.2f, 1f)
            val powerLedY = 12.0 / 16.0
            val powerSize = 0.6 / 16.0
            renderLed(matrices, buffer, pumpLight, overlay,
                pos.x.toDouble() + lx - powerSize, powerLedY - powerSize, pos.z.toDouble() + lz - powerSize,
                pos.x.toDouble() + lx + powerSize, powerLedY + powerSize, pos.z.toDouble() + lz + powerSize,
                ledGreen, powerBright)
        }
        matrices.pop()
    }

    private fun renderLed(
        matrices: MatrixStack, buffer: VertexConsumer, light: Int, overlay: Int,
        x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double,
        sprite: Sprite, brightness: Float
    ) {
        val ctx = matrices.peek()
        val u1 = sprite.minU; val u2 = sprite.maxU
        val v1 = sprite.minV; val v2 = sprite.maxV
        val r = (255 * brightness).toInt().coerceIn(0, 255)
        val g = r; val b = r

        // TOP face only (LED is on the pump top surface)
        emitLedQuad(buffer, ctx, light, overlay,
            x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2,
            u1, v2, u2, v2, u2, v1, u1, v1,
            0f, 1f, 0f, r, g, b)
    }

    private fun renderTubeBox(
        matrices: MatrixStack, buffer: VertexConsumer,
        x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double,
        sprite: Sprite
    ) {
        val ctx = matrices.peek()
        val u1 = sprite.minU; val u2 = sprite.maxU
        val v1 = sprite.minV; val v2 = sprite.maxV

        val r = 255; val g = 255; val b = 255

        // 4 sides
        emitLedQuad(buffer, ctx, 0xF000F0, 0,
            x1, y1, z1, x2, y1, z1, x2, y2, z1, x1, y2, z1,
            u1, v1, u2, v1, u2, v2, u1, v2, 0f, 0f, -1f, r, g, b)
        emitLedQuad(buffer, ctx, 0xF000F0, 0,
            x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2,
            u1, v1, u2, v1, u2, v2, u1, v2, 0f, 0f, 1f, r, g, b)
        emitLedQuad(buffer, ctx, 0xF000F0, 0,
            x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1,
            u1, v1, u2, v1, u2, v2, u1, v2, -1f, 0f, 0f, r, g, b)
        emitLedQuad(buffer, ctx, 0xF000F0, 0,
            x2, y1, z2, x2, y1, z1, x2, y2, z1, x2, y2, z2,
            u1, v1, u2, v1, u2, v2, u1, v2, 1f, 0f, 0f, r, g, b)
    }

    private fun emitLedQuad(
        buffer: VertexConsumer, entry: MatrixStack.Entry, light: Int, overlay: Int,
        x0: Double, y0: Double, z0: Double, x1: Double, y1: Double, z1: Double,
        x2: Double, y2: Double, z2: Double, x3: Double, y3: Double, z3: Double,
        u0: Float, v0: Float, u1: Float, v1: Float, u2: Float, v2: Float, u3: Float, v3: Float,
        nx: Float, ny: Float, nz: Float, r: Int, g: Int, b: Int
    ) {
        buffer.vertex(entry.positionMatrix, x0.toFloat(), y0.toFloat(), z0.toFloat()).color(r, g, b, 255)
            .texture(u0, v0).overlay(overlay).light(light).normal(entry, nx, ny, nz)
        buffer.vertex(entry.positionMatrix, x1.toFloat(), y1.toFloat(), z1.toFloat()).color(r, g, b, 255)
            .texture(u1, v1).overlay(overlay).light(light).normal(entry, nx, ny, nz)
        buffer.vertex(entry.positionMatrix, x2.toFloat(), y2.toFloat(), z2.toFloat()).color(r, g, b, 255)
            .texture(u2, v2).overlay(overlay).light(light).normal(entry, nx, ny, nz)
        buffer.vertex(entry.positionMatrix, x3.toFloat(), y3.toFloat(), z3.toFloat()).color(r, g, b, 255)
            .texture(u3, v3).overlay(overlay).light(light).normal(entry, nx, ny, nz)
    }

    override fun rendersOutsideBoundingBox(entity: PumpBlockEntity): Boolean = true
}
