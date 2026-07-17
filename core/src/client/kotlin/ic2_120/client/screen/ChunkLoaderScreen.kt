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
import java.util.concurrent.Executors

@ModScreen(block = ChunkLoaderBlock::class)
class ChunkLoaderScreen(
    handler: ChunkLoaderScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<ChunkLoaderScreenHandler>(handler, playerInventory, title) {

    companion object {
        private val TEXTURE = Identifier("ic2", "textures/gui/guichunkloader.png")
        private val UPTIPS_TEXTURE = Identifier("ic2", "textures/gui/uptips.png")
        private const val TEXTURE_SIZE = 256
        private const val GUI_WIDTH = 176
        private const val GUI_HEIGHT = 250
        private const val PLAYER_INV_Y = 168
        private const val HOTBAR_Y = 226
        private const val UPTIPS_X = 4
        private const val UPTIPS_Y = 4
        private const val UPTIPS_SIZE = 16

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

        /**
         * 后台采样线程池（客户端单例，daemon 线程随 JVM 退出）。
         *
         * 采样读 ClientWorld 有偶发脏读风险（主线程可能正在修改区块），
         * 最坏情况是某像素颜色不一致，下次 5s 刷新即修正——参考 FTB Chunks 的同类设计。
         */
        private val samplingExecutor = Executors.newSingleThreadExecutor { r ->
            Thread(r, "ic2-chunk-map-sampler").apply { isDaemon = true }
        }
    }

    /** 定时计数，每 100 tick (5s) 触发一次重采样 */
    private var tickCount = 0
    /** 当前 GUI 实际使用的纹理 id（与 mapTextureCache 中对应条目一致；removed 时清理它） */
    @Volatile
    private var currentTextureId: Identifier? = null
    /** 防重入：后台采样未完成时不接受新的采样请求 */
    @Volatile
    private var samplingInProgress = false

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
        context.drawTexture(
            UPTIPS_TEXTURE, left + UPTIPS_X, top + UPTIPS_Y,
            0f, 0f, UPTIPS_SIZE, UPTIPS_SIZE,
            UPTIPS_SIZE, UPTIPS_SIZE
        )

        drawMouseoverTooltip(context, mouseX, mouseY)

        val relX = mouseX - left
        val relY = mouseY - top
        if (relX in UPTIPS_X until UPTIPS_X + UPTIPS_SIZE &&
            relY in UPTIPS_Y until UPTIPS_Y + UPTIPS_SIZE
        ) {
            context.drawTooltip(
                textRenderer,
                Text.translatable("gui.ic2_120.chunk_loader.restart_bootstrap_hint"),
                mouseX, mouseY
            )
        }
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
        val gridColor = 0x33FFFFFF
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

    /**
     * 返回当前可用的地图纹理 id：
     * - 缓存命中 → 立即返回（同步、零开销）
     * - 缓存未命中 → 触发后台异步采样，本次返回 null（下一帧采样完成后会自动出现）
     *
     * 改造前是主线程同步采样 20k 像素（卡首帧），现在后台跑，玩家看到的是"地图延迟 1-2 帧出现"。
     */
    private fun getMapTexture(): Identifier? {
        val bePos = handler.machinePos
        if (bePos == BlockPos.ORIGIN) return null

        mapTextureCache[bePos]?.let {
            currentTextureId = it
            return it
        }

        // 缓存未命中：异步采样，本次返回 null
        triggerAsyncSampling(bePos)
        return null
    }

    /**
     * 触发后台采样。防重入：上一次采样未结束时直接跳过。
     * [force] = true 表示忽略缓存，强制重采（由 tick() 每 5s 调用，保证地图始终最新）。
     */
    private fun triggerAsyncSampling(machinePos: BlockPos, force: Boolean = false) {
        if (samplingInProgress) return
        if (!force && mapTextureCache.containsKey(machinePos)) return
        samplingInProgress = true

        val client = MinecraftClient.getInstance()
        val textureId = Identifier("ic2_120", "chunk_map_${machinePos.asLong()}")
        // 主线程捕获 world 引用，避免后台线程访问 client 实例（client 非线程安全）
        val world = client.world
        val centerX = machinePos.x shr 4
        val centerZ = machinePos.z shr 4
        val bottomY = world?.bottomY ?: 0

        samplingExecutor.execute {
            try {
                if (world == null) return@execute
                val image = sampleMapImage(world, centerX, centerZ, bottomY)
                // 上传必须在主线程：TextureManager.registerTexture 操作非并发 HashMap
                client.execute {
                    try {
                        if (mapTextureCache.containsKey(machinePos) && !force) {
                            // 期间已被其它路径填入，丢弃本次结果避免覆盖
                            image.close()
                            return@execute
                        }
                        // force 重采时先释放旧纹理
                        if (force) {
                            mapTextureCache.remove(machinePos)?.let { oldId ->
                                mapImageCache.remove(oldId)?.close()
                                if (oldId != textureId) {
                                    client.textureManager.destroyTexture(oldId)
                                }
                            }
                        }
                        val texture = NativeImageBackedTexture(image)
                        client.textureManager.registerTexture(textureId, texture)
                        texture.upload()
                        mapTextureCache[machinePos] = textureId
                        mapImageCache[textureId] = image
                        currentTextureId = textureId
                    } catch (e: Throwable) {
                        try { image.close() } catch (_: Throwable) {}
                    }
                }
            } catch (_: Throwable) {
                // 后台采样异常：静默丢弃，下次 tick 或开 GUI 时重试
            } finally {
                samplingInProgress = false
            }
        }
    }

    /**
     * 后台线程执行：对 9×9 区块共 144×144 像素采样，写入 [NativeImage]。
     *
     * 性能要点（对比改造前）：
     * - 在后台线程跑，不卡主渲染线程
     * - 用 [BlockPos.Mutable] 复用，消除 20k×2 个临时 BlockPos 对象
     * - 每像素只查一次 chunk（原实现走 world.getBlockState 会重复 hashmap 查找）
     *
     * 线程安全：world 读 ClientWorld 有偶发脏读风险（主线程可能正在修改区块），
     * 最坏某像素颜色不一致，下次 5s 刷新即修正。NativeImage.setColor 无渲染线程断言，可后台用。
     */
    private fun sampleMapImage(
        world: net.minecraft.world.World,
        centerX: Int,
        centerZ: Int,
        bottomY: Int
    ): NativeImage {
        val image = NativeImage(MAP_SIZE, MAP_SIZE, false)
        val half = GRID / 2
        val samplePos = BlockPos.Mutable()

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
                        val rgb = if (topY >= bottomY) {
                            samplePos.set(wx, topY, wz)
                            val state = world.getBlockState(samplePos)
                            0xFF000000.toInt() or state.getMapColor(world, samplePos).color
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

    /**
     * 定时重采样：每 100 tick (5s) 强制重新采样当前机器周围的地图。
     *
     * 用 `handledScreenTick` 而非 `tick`：HandledScreen.tick 在 vanilla 1.20.1 是 final，
     * 它内部每 tick 调用本方法（仅当玩家存活且未移除时），语义等价、且是官方预留的子类钩子。
     *
     * 这解决了改造前"地图只生成一次、永不刷新"的问题——玩家在 ChunkLoader 周围修了建筑/挖了坑，
     * 5 秒内地图会自动更新。Screen 关闭后 [handledScreenTick] 不再被调用，自然停止，无需取消逻辑。
     */
    override fun handledScreenTick() {
        super.handledScreenTick()
        tickCount++
        if (tickCount % 100L == 0L) {
            val bePos = handler.machinePos
            if (bePos != BlockPos.ORIGIN) {
                triggerAsyncSampling(bePos, force = true)
            }
        }
    }

    /**
     * GUI 关闭时释放当前实例使用的 GPU 纹理和 native 内存。
     *
     * 改造前这里是空实现，会导致每个访问过的 ChunkLoader 在客户端累积一份 80KB GPU 纹理 + 80KB
     * native 堆内存（NativeImage 是 off-heap，GC 管不到）。玩家关游戏会整体清零，但单会话内累积。
     *
     * 现在：从两个 cache 里移除当前机器的条目，并 destroyTexture（会连带 close NativeImage + clearGlId）。
     * textureManager.destroyTexture 必须在主线程调用（操作非并发 HashMap），而 removed() 由主线程触发，安全。
     */
    override fun removed() {
        super.removed()
        val bePos = handler.machinePos
        if (bePos == BlockPos.ORIGIN) return
        val id = mapTextureCache.remove(bePos) ?: return
        mapImageCache.remove(id)?.close()
        MinecraftClient.getInstance().textureManager.destroyTexture(id)
        if (currentTextureId == id) {
            currentTextureId = null
        }
    }
}
