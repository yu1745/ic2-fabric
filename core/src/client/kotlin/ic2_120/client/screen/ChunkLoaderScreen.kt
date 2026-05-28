package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.t

import ic2_120.content.block.ChunkLoaderBlock
import ic2_120.content.screen.ChunkLoaderScreenHandler
import ic2_120.content.screen.GuiSize
import ic2_120.content.sync.ChunkLoaderSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.Heightmap

@ModScreen(block = ChunkLoaderBlock::class)
class ChunkLoaderScreen(
    handler: ChunkLoaderScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<ChunkLoaderScreenHandler>(handler, playerInventory, title) {

    companion object {
        private val TEXTURE = Identifier("ic2", "textures/gui/guichunkloader.png")
        private const val TEXTURE_SIZE = 256
        private const val GUI_WIDTH = 176
        private const val GUI_HEIGHT = 250
        private const val PLAYER_INV_Y = 168
        private const val HOTBAR_Y = 226

        private const val MAP_LEFT = 15
        private const val MAP_TOP = 16
        private const val CELL_SIZE = 16
        private const val GRID = ChunkLoaderSync.GRID_SIZE
        private const val MAP_SIZE = GRID * CELL_SIZE
        private const val CENTER_INDEX = GRID / 2 * GRID + GRID / 2

        /** 缓存：BlockPos → Identifier (注册纹理) */
        private val mapTextureCache = mutableMapOf<BlockPos, Identifier>()
        /** NativeImage 引用，用于关闭释放 */
        private val mapImageCache = mutableMapOf<Identifier, NativeImage>()
    }

    init {
        backgroundWidth = GUI_WIDTH
        backgroundHeight = GUI_HEIGHT
        titleY = 6
        setSlotPositions()
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, backgroundWidth, backgroundHeight, TEXTURE_SIZE, TEXTURE_SIZE)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        val left = x
        val top = y

        drawTitle(context, left, top)
        drawMap(context, left, top, mouseX, mouseY)
        drawSideText(context, left, top)

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun setSlotPositions() {
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                val idx = ChunkLoaderScreenHandler.PLAYER_INV_START + row * 9 + col
                handler.slots[idx].x = 8 + col * GuiSize.SLOT_SIZE
                handler.slots[idx].y = PLAYER_INV_Y + row * GuiSize.SLOT_SIZE
            }
        }
        for (col in 0 until 9) {
            val idx = ChunkLoaderScreenHandler.PLAYER_INV_START + 27 + col
            handler.slots[idx].x = 8 + col * GuiSize.SLOT_SIZE
            handler.slots[idx].y = HOTBAR_Y
        }
    }

    private fun drawTitle(context: DrawContext, left: Int, top: Int) {
        val tx = left + (backgroundWidth - textRenderer.getWidth(title)) / 2
        context.drawText(textRenderer, title, tx, top + 6, 0x404040, false)
    }

    private fun drawMap(context: DrawContext, left: Int, top: Int, mouseX: Int, mouseY: Int) {
        val textureId = getMapTexture() ?: return
        val mapLeft = left + MAP_LEFT
        val mapTop = top + MAP_TOP

        // 144×144 纹理一次性绘制
        context.drawTexture(textureId, mapLeft, mapTop, 0f, 0f, MAP_SIZE, MAP_SIZE, MAP_SIZE, MAP_SIZE)

        // 逐格叠加加载蒙层 + 悬停高亮
        for (row in 0 until GRID) {
            for (col in 0 until GRID) {
                val index = row * GRID + col
                val cx = mapLeft + col * CELL_SIZE
                val cy = mapTop + row * CELL_SIZE
                val isCenter = index == CENTER_INDEX
                val isEnabled = handler.sync.isChunkEnabled(index)
                val isHovered = mouseX in cx until cx + CELL_SIZE && mouseY in cy until cy + CELL_SIZE

                // 已加载区块 → 叠加 60% 透明绿色
                if (isEnabled) {
                    context.fill(cx, cy, cx + CELL_SIZE, cy + CELL_SIZE, 0xCC00FF00.toInt())
                }

                // 悬停 / 中心高亮
                if (isCenter || isHovered) {
                    val alpha = when {
                        isCenter && isHovered -> 0x4D
                        isCenter -> 0x30
                        else -> 0x30
                    }
                    context.fill(cx, cy, cx + CELL_SIZE, cy + CELL_SIZE,
                        (alpha shl 24) or 0xFFFFFF)
                }
            }
        }

        // 网格细线（半透明，仅区块边界）
        val gridColor = 0x33FFFFFF.toInt()
        for (i in 1 until GRID) {
            val lineX = mapLeft + i * CELL_SIZE
            context.fill(lineX, mapTop, lineX + 1, mapTop + MAP_SIZE, gridColor)
            val lineY = mapTop + i * CELL_SIZE
            context.fill(mapLeft, lineY, mapLeft + MAP_SIZE, lineY + 1, gridColor)
        }
        // 地图外边框
        context.drawBorder(mapLeft, mapTop, MAP_SIZE, MAP_SIZE, 0xFF888888.toInt())

    }

    private fun drawSideText(context: DrawContext, left: Int, top: Int) {
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = ChunkLoaderSync.ENERGY_CAPACITY
        val chunkCount = handler.sync.getChunkCount()
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val consumeRate = handler.sync.getSyncedConsumedAmount()

        val energyText = "${"%,d".format(energy)} / ${"%,d".format(cap)} EU"
        val inputText = t("gui.ic2_120.input_eu", EnergyFormatUtils.formatEu(inputRate))
        val consumeText = t("gui.ic2_120.chunk_loader.consume_chunks", EnergyFormatUtils.formatEu(consumeRate), chunkCount)
        val sideTextWidth = maxOf(
            textRenderer.getWidth(energyText),
            textRenderer.getWidth(inputText),
            textRenderer.getWidth(consumeText)
        )
        val sideTextX = left - sideTextWidth - 4

        context.drawText(textRenderer, energyText, sideTextX, top + 8, 0xFFFFFF, false)
        context.drawText(textRenderer, inputText, sideTextX, top + 20, 0xAAAAAA, false)
        context.drawText(textRenderer, consumeText, sideTextX, top + 32, 0xAAAAAA, false)
    }

    private fun getMapTexture(): Identifier? {
        val bePos = handler.machinePos
        if (bePos == BlockPos.ORIGIN) return null

        mapTextureCache[bePos]?.let { return it }

        val image = renderMapImage(bePos) ?: return null
        val id = Identifier("ic2_120", "chunk_map_${bePos.asLong()}")
        MinecraftClient.getInstance().textureManager.registerTexture(id, NativeImageBackedTexture(image))
        mapTextureCache[bePos] = id
        mapImageCache[id] = image
        return id
    }

    private fun renderMapImage(machinePos: BlockPos): NativeImage? {
        val world = MinecraftClient.getInstance().world ?: return null
        val centerX = machinePos.x shr 4
        val centerZ = machinePos.z shr 4
        val half = GRID / 2
        val image = NativeImage(MAP_SIZE, MAP_SIZE, false)

        for (chunkRow in 0 until GRID) {
            for (chunkCol in 0 until GRID) {
                val cx = centerX + chunkCol - half
                val cz = centerZ + chunkRow - half
                val originX = chunkCol * CELL_SIZE
                val originY = chunkRow * CELL_SIZE
                for (bz in 0 until CELL_SIZE) {
                    for (bx in 0 until CELL_SIZE) {
                        val wx = cx * 16 + bx
                        val wz = cz * 16 + bz
                        val topY = world.getTopY(Heightmap.Type.WORLD_SURFACE, wx, wz) - 1
                        val rgb = if (topY >= world.bottomY) {
                            val state = world.getBlockState(BlockPos(wx, topY, wz))
                            0xFF000000.toInt() or state.getMapColor(world, BlockPos(wx, topY, wz)).color
                        } else {
                            0xFF404040.toInt()
                        }
                        image.setColor(originX + bx, originY + bz, rgbToAbgr(rgb))
                    }
                }
            }
        }
        return image
    }

    /** RGB → ABGR（NativeImage 像素格式） */
    private fun rgbToAbgr(rgb: Int): Int {
        val r = (rgb shr 16) and 0xFF
        val g = (rgb shr 8) and 0xFF
        val b = rgb and 0xFF
        return (0xFF shl 24) or (b shl 16) or (g shl 8) or r
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) {
            val mx = mouseX.toInt()
            val my = mouseY.toInt()
            val mapLeft = x + MAP_LEFT
            val mapTop = y + MAP_TOP
            val mapRight = mapLeft + MAP_SIZE
            val mapBottom = mapTop + MAP_SIZE
            if (mx in mapLeft until mapRight && my in mapTop until mapBottom) {
                val col = (mx - mapLeft) / CELL_SIZE
                val row = (my - mapTop) / CELL_SIZE
                if (col in 0 until GRID && row in 0 until GRID) {
                    val index = row * GRID + col
                    if (index != CENTER_INDEX) {
                        client?.player?.networkHandler?.sendPacket(
                            ButtonClickC2SPacket(handler.syncId, index)
                        )
                    }
                    return true
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun removed() {
        // 缓存不清，GUI 关闭后保留
    }
}
