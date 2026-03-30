package ic2_120.content.worldgen

import ic2_120.Ic2_120
import net.fabricmc.fabric.api.biome.v1.BiomeModifications
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors
// import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.tag.BiomeTags
import net.minecraft.util.Identifier
import net.minecraft.world.biome.BiomeKeys
// import net.minecraft.util.math.BlockPos
// import net.minecraft.util.math.Direction
// import net.minecraft.world.World
import net.minecraft.world.gen.GenerationStep
// import org.slf4j.LoggerFactory
// import java.util.ArrayDeque

/**
 * 橡胶树世界生成。
 * ConfiguredFeature 和 PlacedFeature 通过 data/ic2_120/worldgen/ 下的 JSON 定义。
 */
object RubberTreeGeneration {

    // private val logger = LoggerFactory.getLogger("ic2_120.rubber_tree")

    val RUBBER_TREE_PLACED_KEY: RegistryKey<net.minecraft.world.gen.feature.PlacedFeature> =
        RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier(Ic2_120.MOD_ID, "rubber_tree"))

    fun register() {
        // 确保自定义 FoliagePlacerType 已注册（供 JSON 中 rubber_tree 配置引用）
        ModWorldgen.RUBBER_TREE_FOLIAGE_PLACER_TYPE
        BiomeModifications.addFeature(
            BiomeSelectors.foundInOverworld().and { context ->
                !context.hasTag(BiomeTags.IS_OCEAN) &&
                    !context.hasTag(BiomeTags.IS_RIVER) &&
                    context.biomeKey != BiomeKeys.DESERT
            },
            GenerationStep.Feature.VEGETAL_DECORATION,
            RUBBER_TREE_PLACED_KEY
        )

        // 出生点周围扫描橡胶树数量（16x16 区块起步，找不到则扩大范围）
        // ServerWorldEvents.LOAD.register { _, world ->
        //     if (world.registryKey != World.OVERWORLD) return@register
        //     world.server?.execute {
        //         logRubberTreeCount(world)
        //     }
        // }
    }

    // private fun logRubberTreeCount(world: World) {
    //     val rubberLog = Registries.BLOCK.get(Identifier(Ic2_120.MOD_ID, "rubber_log"))
    //     val spawn = world.getSpawnPos()
    //     val spawnChunkX = spawn.x shr 4
    //     val spawnChunkZ = spawn.z shr 4
    //     val bottomY = world.bottomY
    //     val topY = world.topY
    //
    //     var radius = 8
    //     var treeCount = 0
    //     var logCount = 0
    //     var scannedChunks = 0
    //
    //     while (true) {
    //         val logPositions = mutableSetOf<BlockPos>()
    //         val minChunkX = spawnChunkX - radius
    //         val maxChunkX = spawnChunkX + radius
    //         val minChunkZ = spawnChunkZ - radius
    //         val maxChunkZ = spawnChunkZ + radius
    //
    //         for (cx in minChunkX..maxChunkX) {
    //             for (cz in minChunkZ..maxChunkZ) {
    //                 val chunk = world.getChunk(cx, cz)
    //                 for (x in (cx shl 4) until ((cx + 1) shl 4)) {
    //                     for (z in (cz shl 4) until ((cz + 1) shl 4)) {
    //                         for (y in bottomY..topY) {
    //                             val pos = BlockPos(x, y, z)
    //                             if (chunk.getBlockState(pos).block == rubberLog) {
    //                                 logPositions.add(pos)
    //                             }
    //                         }
    //                     }
    //                 }
    //             }
    //         }
    //
    //         scannedChunks = (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1)
    //         logCount = logPositions.size
    //
    //         // 连通分量计数：一棵树 = 一组相连的橡胶原木
    //         treeCount = countConnectedComponents(logPositions) { pos ->
    //             Direction.values().map { pos.offset(it) }.filter { it in logPositions }
    //         }
    //
    //         val chunkSize = (radius * 2 + 1)
    //         logger.info(
    //             "[橡胶树扫描] 出生点周围 {}x{} 区块（共 {} 区块）：橡胶原木 {} 个，橡胶树 {} 棵",
    //             chunkSize, chunkSize, scannedChunks, logCount, treeCount
    //         )
    //
    //         if (treeCount > 0) break
    //         radius += 8
    //         if (radius > 64) {
    //             logger.warn("[橡胶树扫描] 已扩大到 128x128 区块仍未找到，停止扫描")
    //             break
    //         }
    //     }
    // }
    //
    // private fun <T> countConnectedComponents(nodes: Set<T>, neighbors: (T) -> List<T>): Int {
    //     val visited = mutableSetOf<T>()
    //     var count = 0
    //     for (start in nodes) {
    //         if (start in visited) continue
    //         count++
    //         val queue = ArrayDeque<T>()
    //         queue.add(start)
    //         visited.add(start)
    //         while (queue.isNotEmpty()) {
    //             val cur = queue.removeFirst()
    //             for (n in neighbors(cur)) {
    //                 if (n !in visited) {
    //                     visited.add(n)
    //                     queue.add(n)
    //                 }
    //             }
    //         }
    //     }
    //     return count
    // }
}
