package ic2_120.content.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import ic2_120.Ic2_120
import ic2_120.content.block.RubberLeavesBlock
import ic2_120.content.block.RubberLogBlock
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.registry.Registries
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.world.chunk.ChunkStatus
import java.util.ArrayDeque
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.stream.IntStream

object RubberTreeCommand {
    private const val PERMISSION_LEVEL = 4
    private const val LEAF_SEARCH_DISTANCE = 6
    private val searchExecutor = Executors.newFixedThreadPool(4)
    private val rubberLogBlock by lazy { Registries.BLOCK.get(Ic2_120.id("rubber_log")) }

    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                CommandManager.literal("ic2rubber")
                    .requires { source -> source.hasPermissionLevel(PERMISSION_LEVEL) }
                    .executes { ctx -> execute(ctx, radius = 64, teleport = false) }
                    .then(
                        CommandManager.argument("radius", IntegerArgumentType.integer(1))
                            .executes { ctx ->
                                val radius = IntegerArgumentType.getInteger(ctx, "radius")
                                execute(ctx, radius, teleport = false)
                            }
                    )
                    .then(
                        CommandManager.literal("tp")
                            .executes { ctx -> execute(ctx, radius = 1000, teleport = true) }
                            .then(
                                CommandManager.argument("radius", IntegerArgumentType.integer(1))
                                    .executes { ctx ->
                                        val radius = IntegerArgumentType.getInteger(ctx, "radius")
                                        execute(ctx, radius, teleport = true)
                                    }
                            )
                    )
            )
        }
    }

    private fun execute(ctx: CommandContext<ServerCommandSource>, radius: Int, teleport: Boolean): Int {
        val source = ctx.source
        val player = source.player ?: run {
            source.sendError(Text.literal("此命令只能由玩家执行"))
            return 0
        }

        val world = source.world as? ServerWorld ?: run {
            source.sendError(Text.literal("此命令只能在服务端执行"))
            return 0
        }

        source.sendFeedback({ Text.literal("正在搜索 ${radius} 格范围内的橡胶树...") }, true)

        val startTime = System.currentTimeMillis()

        CompletableFuture.supplyAsync(
            { searchRubberTrees(world, player.blockPos, radius) },
            searchExecutor
        ).thenAcceptAsync({ result ->
            val elapsedMs = System.currentTimeMillis() - startTime

            if (result.trees.isEmpty()) {
                source.sendFeedback({
                    Text.literal("在 ${radius} 格范围内未找到橡胶树 (耗时 ${elapsedMs}ms)")
                }, false)
                return@thenAcceptAsync
            }

            val nearest = result.trees.first()
            if (teleport) {
                val tpPos = findSafeTeleportPos(world, nearest)
                player.teleport(player.serverWorld, tpPos.x + 0.5, tpPos.y.toDouble(), tpPos.z + 0.5, player.yaw, player.pitch)
                source.sendFeedback({
                    Text.literal("已传送到橡胶树位置 [${nearest.x}, ${nearest.y}, ${nearest.z}] (共 ${result.count} 棵, 耗时 ${elapsedMs}ms)")
                }, true)
            } else {
                source.sendFeedback({
                    Text.literal("找到 ${result.count} 棵橡胶树，最近的: [${nearest.x}, ${nearest.y}, ${nearest.z}] (耗时 ${elapsedMs}ms)")
                }, false)
                highlightTree(world, player, nearest)
            }
        }) { task -> world.server.execute(task) }

        return Command.SINGLE_SUCCESS
    }

    private data class SearchResult(
        val trees: List<BlockPos>,
        val count: Int
    )

    private fun searchRubberTrees(world: ServerWorld, center: BlockPos, radius: Int): SearchResult {
        val treeXZ = mutableSetOf<Pair<Int, Int>>()
        val treePositions = mutableListOf<BlockPos>()

        val minChunkX = (center.x - radius) shr 4
        val maxChunkX = (center.x + radius) shr 4
        val minChunkZ = (center.z - radius) shr 4
        val maxChunkZ = (center.z + radius) shr 4
        val chunkCount = (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1)

        if (chunkCount > 100) {
            IntStream.rangeClosed(minChunkX, maxChunkX).parallel().forEach { chunkX ->
                IntStream.rangeClosed(minChunkZ, maxChunkZ).forEach { chunkZ ->
                    searchChunk(world, chunkX, chunkZ, center, radius, treeXZ, treePositions)
                }
            }
        } else {
            for (chunkX in minChunkX..maxChunkX) {
                for (chunkZ in minChunkZ..maxChunkZ) {
                    searchChunk(world, chunkX, chunkZ, center, radius, treeXZ, treePositions)
                }
            }
        }

        return SearchResult(treePositions.sortedBy { it.getSquaredDistance(center) }, treeXZ.size)
    }

    private fun searchChunk(
        world: ServerWorld,
        chunkX: Int,
        chunkZ: Int,
        center: BlockPos,
        radius: Int,
        treeXZ: MutableSet<Pair<Int, Int>>,
        treePositions: MutableList<BlockPos>
    ) {
        val chunk = if (!world.chunkManager.isChunkLoaded(chunkX, chunkZ)) {
            world.chunkManager.getChunk(chunkX, chunkZ, ChunkStatus.FULL, true)
        } else {
            world.getChunk(chunkX, chunkZ)
        } ?: return

        val minX = maxOf((chunkX shl 4), center.x - radius)
        val maxX = minOf((chunkX shl 4) + 15, center.x + radius)
        val minZ = maxOf((chunkZ shl 4), center.z - radius)
        val maxZ = minOf((chunkZ shl 4) + 15, center.z + radius)
        val minY = maxOf(world.bottomY, center.y - radius)
        val maxY = minOf(world.topY, center.y + radius)

        for (x in minX..maxX) {
            for (z in minZ..maxZ) {
                for (y in minY..maxY) {
                    val pos = BlockPos(x, y, z)
                    if (pos.getSquaredDistance(center) > radius * radius) continue

                    val state = chunk.getBlockState(pos)
                    if (!state.isOf(rubberLogBlock)) continue
                    if (!hasConnectedRubberLeaves(world, pos)) continue

                    val xz = Pair(x, z)
                    synchronized(treeXZ) {
                        if (treeXZ.add(xz)) {
                            treePositions.add(pos)
                        }
                    }
                }
            }
        }
    }

    private fun hasConnectedRubberLeaves(world: ServerWorld, startPos: BlockPos): Boolean {
        val queue = ArrayDeque<Pair<BlockPos, Int>>()
        val visited = HashSet<BlockPos>()

        val start = startPos.toImmutable()
        queue.add(start to 0)
        visited.add(start)

        while (queue.isNotEmpty()) {
            val (current, depth) = queue.removeFirst()
            if (depth >= LEAF_SEARCH_DISTANCE) continue

            for (direction in Direction.values()) {
                val neighborPos = current.offset(direction).toImmutable()
                if (!visited.add(neighborPos)) continue

                val neighborBlock = world.getBlockState(neighborPos).block
                if (neighborBlock is RubberLeavesBlock) {
                    return true
                }
                if (neighborBlock is RubberLogBlock) {
                    queue.add(neighborPos to (depth + 1))
                }
            }
        }

        return false
    }

    private fun findSafeTeleportPos(world: ServerWorld, logPos: BlockPos): BlockPos {
        for (y in logPos.y downTo world.bottomY) {
            val pos = BlockPos(logPos.x, y, logPos.z)
            val state = world.getBlockState(pos)

            if (!state.isAir && state.shouldSuffocate(world, pos)) {
                val feetPos = pos.up()
                val headPos = pos.up(2)

                val feetEmpty = world.getBlockState(feetPos).isAir
                val headEmpty = world.getBlockState(headPos).isAir

                if (feetEmpty && headEmpty) {
                    return feetPos
                }
            }
        }

        val directions = listOf(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)
        for (dir in directions) {
            for (offset in 1..3) {
                val checkPos = logPos.offset(dir, offset)
                val groundPos = findGroundAt(world, checkPos)
                if (groundPos != null) {
                    return groundPos
                }
            }
        }

        return logPos.up()
    }

    private fun findGroundAt(world: ServerWorld, pos: BlockPos): BlockPos? {
        for (y in pos.y downTo maxOf(world.bottomY, pos.y - 10)) {
            val checkPos = BlockPos(pos.x, y, pos.z)
            val state = world.getBlockState(checkPos)

            if (!state.isAir && state.shouldSuffocate(world, checkPos)) {
                val feetPos = checkPos.up()
                val headPos = checkPos.up(2)

                if (world.getBlockState(feetPos).isAir && world.getBlockState(headPos).isAir) {
                    return feetPos
                }
            }
        }
        return null
    }

    private fun highlightTree(world: ServerWorld, player: net.minecraft.server.network.ServerPlayerEntity, pos: BlockPos) {
        val vec = Vec3d.ofCenter(pos)
        world.spawnParticles(
            player,
            net.minecraft.particle.ParticleTypes.HEART,
            true,
            vec.x, vec.y, vec.z,
            10,
            0.5, 1.0, 0.5,
            0.02
        )
    }
}
