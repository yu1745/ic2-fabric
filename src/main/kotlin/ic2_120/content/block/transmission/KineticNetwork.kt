package ic2_120.content.block.transmission

import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import java.util.PriorityQueue

/**
 * 动能网络。
 *
 * 除伞齿轮的连通规则外，调度方式严格对齐 EnergyNetwork：
 * - 先构建网络拓扑与边界端点；
 * - 每 tick 由消费者按路径损耗/路径容量从所有供给端拉取；
 * - 路径一旦使用，会扣减路径上每个传动节点的本 tick 剩余吞吐。
 */
class KineticNetwork {
    val nodes: MutableSet<Long> = mutableSetOf()

    private var topologyCache: TopologyCache? = null
    private var lastTickTime: Long = Long.MIN_VALUE
    private var lastRenderedKu: Int = 0

    private val dijkstraCacheByEntries = mutableMapOf<String, DijkstraResult>()

    fun addNode(pos: BlockPos) {
        nodes.add(pos.asLong())
    }

    fun invalidateConnectionCaches() {
        topologyCache = null
        dijkstraCacheByEntries.clear()
    }

    fun wasTickedAt(time: Long): Boolean = lastTickTime == time

    fun markTickedAt(time: Long) {
        lastTickTime = time
    }

    fun tickIfNeeded(world: World) {
        if (world.isClient) return
        if (lastTickTime == world.time) return
        lastTickTime = world.time

        if (nodes.isEmpty()) {
            syncRenderedKuToEntitiesIfDirty(world, 0)
            return
        }

        val topology = topologyCache ?: buildTopology(world).also { topologyCache = it }
        val remainingNodeCapacity = topology.nodeRates.toMutableMap()

        val consumers = mutableMapOf<MachineEndpointKey, Endpoint>()
        val providers = mutableMapOf<MachineEndpointKey, Endpoint>()

        for (boundary in topology.boundaries) {
            val machinePos = BlockPos.fromLong(boundary.neighborPosLong)
            val port = world.getBlockEntity(machinePos) as? IKineticMachinePort ?: continue

            if (port.canInputKuFrom(boundary.sideFromMachine)) {
                val key = MachineEndpointKey(boundary.neighborPosLong, boundary.sideFromMachine)
                val endpoint = consumers.getOrPut(key) {
                    Endpoint(port, boundary.sideFromMachine, blockPos = boundary.neighborPosLong)
                }
                endpoint.entryNodes.add(boundary.nodePosLong)
            }

            if (port.canOutputKuTo(boundary.sideFromMachine)) {
                val key = MachineEndpointKey(boundary.neighborPosLong, boundary.sideFromMachine)
                val endpoint = providers.getOrPut(key) {
                    Endpoint(port, boundary.sideFromMachine, blockPos = boundary.neighborPosLong)
                }
                endpoint.entryNodes.add(boundary.nodePosLong)
            }
        }

        var totalTransferredKu = 0
        if (providers.isNotEmpty()) {
            for ((_, consumer) in consumers) {
                totalTransferredKu += pullFromProvidersByPath(consumer, providers, topology.neighbors, remainingNodeCapacity)
            }
        }

        syncRenderedKuToEntitiesIfDirty(world, totalTransferredKu.coerceAtLeast(0))
    }

    fun getNodeKu(world: World, pos: BlockPos): Int {
        tickIfNeeded(world)
        return lastRenderedKu
    }

    private fun pullFromProvidersByPath(
        consumer: Endpoint,
        providers: Map<MachineEndpointKey, Endpoint>,
        neighbors: Map<Long, List<Long>>,
        remainingNodeCapacity: MutableMap<Long, Int>
    ): Int {
        var movedTotal = 0
        while (true) {
            val demand = simulateInsertion(consumer.port, consumer.sideFromMachine, Int.MAX_VALUE)
            if (demand <= 0) break

            val candidates = buildProviderCandidates(consumer, providers, neighbors)
            if (candidates.isEmpty()) break

            var progressed = false
            for (candidate in candidates) {
                val pathCapacity = candidate.path.minOfOrNull { remainingNodeCapacity[it] ?: 0 } ?: 0
                if (pathCapacity <= 0) continue

                val pathLoss = candidate.pathLossKu
                    .coerceAtMost(Int.MAX_VALUE.toLong())
                    .toInt()
                val maxDeliverable = (pathCapacity - pathLoss).coerceAtLeast(0)
                if (maxDeliverable <= 0) continue

                val stepDemand = simulateInsertion(consumer.port, consumer.sideFromMachine, maxDeliverable)
                if (stepDemand <= 0) break

                val needFromProvider = minOf(pathCapacity, stepDemand + pathLoss)
                if (needFromProvider <= pathLoss) continue

                val simulatedExtract = simulateExtraction(
                    candidate.provider.port,
                    candidate.provider.sideFromMachine,
                    needFromProvider
                )
                if (simulatedExtract <= pathLoss) continue

                val deliverable = (simulatedExtract - pathLoss).coerceAtLeast(0)
                if (deliverable <= 0) continue

                val acceptedPreview = simulateInsertion(consumer.port, consumer.sideFromMachine, deliverable)
                if (acceptedPreview <= 0) continue

                val requestExtract = minOf(simulatedExtract, acceptedPreview + pathLoss)
                val extracted = candidate.provider.port.extractKu(
                    candidate.provider.sideFromMachine,
                    requestExtract,
                    false
                ).coerceAtLeast(0)
                if (extracted <= pathLoss) continue

                val requestInsert = minOf(acceptedPreview, extracted - pathLoss).coerceAtLeast(0)
                if (requestInsert <= 0) continue

                val inserted = consumer.port.insertKu(consumer.sideFromMachine, requestInsert, false)
                    .coerceIn(0, requestInsert)
                if (inserted <= 0) continue

                val moved = minOf(extracted, inserted + pathLoss)
                for (nodePosLong in candidate.path) {
                    remainingNodeCapacity[nodePosLong] = (remainingNodeCapacity[nodePosLong] ?: 0) - moved
                }
                movedTotal += moved
                progressed = true
            }

            if (!progressed) break
            // 与 EnergyNetwork 保持同样的单 tick 调度节奏。
            break
        }
        return movedTotal
    }

    private fun buildProviderCandidates(
        consumer: Endpoint,
        providers: Map<MachineEndpointKey, Endpoint>,
        neighbors: Map<Long, List<Long>>
    ): List<ProviderPath> {
        val dijkstra = shortestLossFromSourcesCached(consumer.entryNodes, neighbors)
        val candidates = mutableListOf<ProviderPath>()

        for ((_, provider) in providers) {
            var bestEnd: Long? = null
            var bestLoss = Long.MAX_VALUE
            for (entry in provider.entryNodes) {
                val loss = dijkstra.dist[entry] ?: continue
                if (loss < bestLoss) {
                    bestLoss = loss
                    bestEnd = entry
                }
            }

            val end = bestEnd ?: continue
            val path = buildPath(end, dijkstra.prev)
            if (path.isNotEmpty()) {
                candidates.add(ProviderPath(provider, path, bestLoss))
            }
        }

        candidates.sortBy { it.pathLossKu }
        return candidates
    }

    private fun shortestLossFromSourcesCached(
        sources: Set<Long>,
        neighbors: Map<Long, List<Long>>
    ): DijkstraResult {
        val cacheKey = entriesKey(sources)
        dijkstraCacheByEntries[cacheKey]?.let { return it }
        val result = shortestLossFromSources(sources, neighbors)
        dijkstraCacheByEntries[cacheKey] = result
        if (dijkstraCacheByEntries.size > 512) {
            dijkstraCacheByEntries.clear()
        }
        return result
    }

    private fun entriesKey(entries: Set<Long>): String =
        entries.sorted().joinToString(",")

    private fun buildTopology(world: World): TopologyCache {
        val nodeRates = mutableMapOf<Long, Int>()
        val nodeLossKu = mutableMapOf<Long, Int>()
        val neighbors = mutableMapOf<Long, MutableList<Long>>()
        val boundaries = mutableListOf<BoundaryEdge>()

        for (nodePosLong in nodes) {
            val pos = BlockPos.fromLong(nodePosLong)
            val state = world.getBlockState(pos)
            if (!KineticConnectionRules.isTransmissionNode(state)) continue

            nodeRates[nodePosLong] = nodeCapacity(state)
            nodeLossKu[nodePosLong] = nodeLoss(state)

            val adjacent = neighbors.getOrPut(nodePosLong) { mutableListOf() }
            for (dir in KineticConnectionRules.nodeDirections(world, pos, state)) {
                val neighborPos = pos.offset(dir)
                val neighborState = world.getBlockState(neighborPos)
                if (KineticConnectionRules.isTransmissionNode(neighborState)) {
                    if (KineticConnectionRules.canConnectFromDirection(neighborState, dir.opposite)) {
                        adjacent.add(neighborPos.asLong())
                    }
                    continue
                }

                if (KineticConnectionRules.isMachinePortFacing(world, neighborPos, dir)) {
                    boundaries.add(BoundaryEdge(nodePosLong, neighborPos.asLong(), dir.opposite))
                }
            }
        }

        return TopologyCache(
            nodeRates = nodeRates,
            nodeLossKu = nodeLossKu,
            neighbors = neighbors.mapValues { it.value.toList() },
            boundaries = boundaries
        )
    }

    /**
     * 渲染口径改为“整张连通动能网统一转速”。
     * 因此 currentKu 仍然同步到每个传动节点，但只在整网渲染 KU 发生变化时批量更新一次。
     * 稳态不再逐 tick 先清零再写回，网络包量会从持续 O(N) 降到“仅变化时 O(N)”。
     */
    private fun syncRenderedKuToEntitiesIfDirty(world: World, renderedKu: Int) {
        val clamped = renderedKu.coerceAtLeast(0)
        if (lastRenderedKu == clamped) return
        lastRenderedKu = clamped
        for (nodePosLong in nodes) {
            val be = world.getBlockEntity(BlockPos.fromLong(nodePosLong)) as? TransmissionBlockEntity ?: continue
            be.setCurrentKu(clamped)
        }
    }

    private fun simulateInsertion(port: IKineticMachinePort, side: Direction, maxAmount: Int): Int =
        port.insertKu(side, maxAmount, true).coerceAtLeast(0)

    private fun simulateExtraction(port: IKineticMachinePort, side: Direction, maxAmount: Int): Int =
        port.extractKu(side, maxAmount, true).coerceAtLeast(0)

    private fun shortestLossFromSources(
        sources: Set<Long>,
        neighbors: Map<Long, List<Long>>
    ): DijkstraResult {
        if (sources.isEmpty()) return DijkstraResult(emptyMap(), emptyMap())

        val dist = mutableMapOf<Long, Long>()
        val prev = mutableMapOf<Long, Long?>()
        val pq = PriorityQueue(compareBy<Pair<Long, Long>> { it.second })

        for (source in sources) {
            val startLoss = lastKnownNodeLoss(source).toLong()
            dist[source] = startLoss
            prev[source] = null
            pq.add(source to startLoss)
        }

        while (pq.isNotEmpty()) {
            val (node, currentDist) = pq.poll()
            if (currentDist != dist[node]) continue

            for (next in neighbors[node].orEmpty()) {
                val weight = lastKnownNodeLoss(next).toLong()
                val nextDist = currentDist + weight
                val oldDist = dist[next]
                if (oldDist == null || nextDist < oldDist) {
                    dist[next] = nextDist
                    prev[next] = node
                    pq.add(next to nextDist)
                }
            }
        }

        return DijkstraResult(dist, prev)
    }

    private fun buildPath(end: Long, prev: Map<Long, Long?>): List<Long> {
        if (end !in prev) return emptyList()
        val reversed = mutableListOf<Long>()
        var current: Long? = end
        while (current != null) {
            reversed.add(current)
            current = prev[current]
        }
        reversed.reverse()
        return reversed
    }

    private fun lastKnownNodeLoss(nodePosLong: Long): Int =
        topologyCache?.nodeLossKu?.get(nodePosLong) ?: 0

    private fun nodeCapacity(state: net.minecraft.block.BlockState): Int = when (state.block) {
        is WoodTransmissionShaftBlock -> 128
        is IronTransmissionShaftBlock -> 512
        is SteelTransmissionShaftBlock -> 2048
        is CarbonTransmissionShaftBlock -> 8192
        is BevelGearBlock -> 2048
        is TransmissionShaftBlock -> 512
        else -> 0
    }

    private fun nodeLoss(state: net.minecraft.block.BlockState): Int = when (state.block) {
        is WoodTransmissionShaftBlock -> 0
        is IronTransmissionShaftBlock -> 2
        is SteelTransmissionShaftBlock -> 1
        is CarbonTransmissionShaftBlock -> 0
        is BevelGearBlock -> 3
        is TransmissionShaftBlock -> 1
        else -> 0
    }

    private data class MachineEndpointKey(
        val blockPosLong: Long,
        val sideFromMachine: Direction
    )

    private data class Endpoint(
        val port: IKineticMachinePort,
        val sideFromMachine: Direction,
        val entryNodes: MutableSet<Long> = mutableSetOf(),
        val blockPos: Long
    )

    private data class ProviderPath(
        val provider: Endpoint,
        val path: List<Long>,
        val pathLossKu: Long
    )

    private data class BoundaryEdge(
        val nodePosLong: Long,
        val neighborPosLong: Long,
        val sideFromMachine: Direction
    )

    private data class TopologyCache(
        val nodeRates: Map<Long, Int>,
        val nodeLossKu: Map<Long, Int>,
        val neighbors: Map<Long, List<Long>>,
        val boundaries: List<BoundaryEdge>
    )

    private data class DijkstraResult(
        val dist: Map<Long, Long>,
        val prev: Map<Long, Long?>
    )
}
