package ic2_120.content.block.pipes

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.minecraft.fluid.Fluid
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.state.property.Properties
import net.minecraft.registry.Registries
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import org.slf4j.LoggerFactory

class PipeNetwork {
    val pipes = mutableSetOf<Long>()
    private val capacityByPipe = mutableMapOf<Long, Long>()
    private var topologyCache: TopologyCache? = null
    var lastTickTime: Long = -1

    var stalledByMixedProviders: Boolean = false
        private set

    var primaryFluidId: String? = null
        private set

    /** Fluid IDs of conflicting providers when stalled. Empty when not stalled. */
    var conflictingFluidIds: List<String> = emptyList()
        private set

    /** Cross-tick path cache: (start, end) -> path. Invalidated on topology change. */
    private val pathCache = mutableMapOf<PathCacheKey, List<Long>>()

    /** Reusable buffer for remaining capacities, avoids per-tick allocation. */
    private val reusableRemaining = mutableMapOf<Long, Long>()

    /** Pipes that had fluid flow this tick. */
    private val currentTouchedPipes = HashSet<Long>()

    /** Pipes that had fluid flow last tick (for clearing stale load display). */
    private val lastTouchedPipes = HashSet<Long>()

    /** Last synced fluid ID, used to detect fluid type changes that require full resync. */
    private var lastSyncedFluidId: String? = null

    fun addPipe(pos: BlockPos, block: BasePipeBlock) {
        val key = pos.asLong()
        pipes.add(key)
        val amountPerTick = kotlin.math.floor(
            block.size.baseBucketsPerSecond * block.material.multiplier * FluidConstants.BUCKET.toDouble() / 20.0
        ).toLong().coerceAtLeast(1L)
        capacityByPipe[key] = amountPerTick
        topologyCache = null
        pathCache.clear()
    }

    fun invalidateConnectionCaches() {
        topologyCache = null
        pathCache.clear()
        lastSyncedFluidId = null
        lastTouchedPipes.clear()
    }

    fun tickIfNeeded(world: World) {
        if (lastTickTime == world.time) return
        lastTickTime = world.time
        pushFluids(world)
    }

    private fun pushFluids(world: World) {
        if (pipes.isEmpty()) return

        val topology = topologyCache ?: buildTopology(world).also { topologyCache = it }

        reusableRemaining.clear()
        for ((k, v) in topology.pipeRates) reusableRemaining[k] = v
        val remaining = reusableRemaining

        currentTouchedPipes.clear()

        // Find providers via FluidStorage API (dry-run extract), no IFluidPipeUpgradeSupport needed
        val providers = findProviders(world, topology)

        val fluidKinds = providers.map { it.variant.fluid }.toSet()
        stalledByMixedProviders = fluidKinds.size > 1
        primaryFluidId = providers.firstOrNull()?.variant?.fluid?.let { Registries.FLUID.getId(it).toString() }

        if (stalledByMixedProviders) {
            conflictingFluidIds = fluidKinds.map { Registries.FLUID.getId(it).toString() }
            syncPipeLoad(world, topology.pipeRates, remaining)
            return
        } else {
            conflictingFluidIds = emptyList()
        }

        if (providers.isEmpty()) {
            syncPipeLoad(world, topology.pipeRates, remaining)
            return
        }

        // Find receivers via FluidStorage API + simulateInsertion, no IFluidPipeUpgradeSupport needed
        val primaryFluid = providers.first().variant.fluid
        val receivers = findReceivers(world, topology, primaryFluid)

        if (receivers.isEmpty()) {
            syncPipeLoad(world, topology.pipeRates, remaining)
            return
        }

        val primaryVariant = FluidVariant.of(primaryFluid)

        for (receiver in receivers) {
            val failedProviderEntries = mutableSetOf<Long>()
            // 排除 provider 和 receiver 是同一个 storage 的情况（自环）
            val filteredProviders = providers.filter { it.storage !== receiver.storage }

            while (true) {
                val best = findBestPath(receiver, filteredProviders, failedProviderEntries, topology.neighbors, remaining)
                if (best == null) break

                val (provider, path, pathCap) = best
                val moved = transferOnce(provider.storage, receiver.storage, primaryVariant, pathCap)
                if (moved <= 0L) {
                    failedProviderEntries.add(provider.entryPipe)
                    if (failedProviderEntries.size >= filteredProviders.size) break
                    continue
                }
                for (pipe in path) {
                    remaining[pipe] = (remaining[pipe] ?: 0L) - moved
                    currentTouchedPipes.add(pipe)
                }
            }
        }

        syncPipeLoad(world, topology.pipeRates, remaining)
    }

    /**
     * Find the best (shortest path) provider for a receiver.
     * Uses cached paths when available; for cache misses, runs a single multi-source BFS
     * from all uncached providers to the receiver — O(V+E) instead of O(P*(V+E)).
     */
    private fun findBestPath(
        receiver: ReceiverEndpoint,
        providers: List<ProviderEndpoint>,
        failedProviders: Set<Long>,
        neighbors: Map<Long, List<Long>>,
        remaining: Map<Long, Long>
    ): Triple<ProviderEndpoint, List<Long>, Long>? {
        val uncachedProviders = mutableListOf<ProviderEndpoint>()
        var best: Triple<ProviderEndpoint, List<Long>, Long>? = null

        for (provider in providers) {
            if (provider.entryPipe in failedProviders) continue
            if (!canReceiverAccept(receiver.storage, provider.variant)) continue

            val cachedPath = pathCache[PathCacheKey(provider.entryPipe, receiver.entryPipe)]
            if (cachedPath != null && cachedPath.all { (remaining[it] ?: 0L) > 0L }) {
                val cap = cachedPath.minOf { remaining[it] ?: 0L }
                if (cap > 0L && (best == null || cachedPath.size < best.second.size)) {
                    best = Triple(provider, cachedPath, cap)
                }
            } else {
                uncachedProviders.add(provider)
            }
        }

        if (uncachedProviders.isEmpty()) return best

        // Single multi-source BFS from all uncached providers to receiver — O(V+E)
        val prev = multiSourceBfs(uncachedProviders.map { it.entryPipe }, receiver.entryPipe, neighbors, remaining)
        if (prev == null) return best

        for (provider in uncachedProviders) {
            val path = tracePath(provider.entryPipe, receiver.entryPipe, prev) ?: continue
            pathCache[PathCacheKey(provider.entryPipe, receiver.entryPipe)] = path
            pathCache[PathCacheKey(receiver.entryPipe, provider.entryPipe)] = path.reversed()

            val cap = path.minOf { remaining[it] ?: 0L }
            if (cap > 0L && (best == null || path.size < best.second.size)) {
                best = Triple(provider, path, cap)
            }
        }

        return best
    }

    /**
     * Multi-source BFS from all [starts] targeting [target].
     * Returns prev map where prev[node] = predecessor (sentinel: prev[start] = start), or null if unreachable.
     * Runs in O(V+E) regardless of how many start nodes there are.
     */
    private fun multiSourceBfs(
        starts: List<Long>,
        target: Long,
        neighbors: Map<Long, List<Long>>,
        remaining: Map<Long, Long>
    ): Map<Long, Long>? {
        if ((remaining[target] ?: 0L) > 0L && starts.any { it == target }) {
            return mapOf(target to target)
        }

        val prev = HashMap<Long, Long>(pipes.size.coerceAtLeast(16))
        val queue = ArrayDeque<Long>()

        for (s in starts) {
            if ((remaining[s] ?: 0L) <= 0L) continue
            prev[s] = s
            queue.add(s)
        }

        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            for (next in neighbors[cur].orEmpty()) {
                if ((remaining[next] ?: 0L) <= 0L) continue
                if (next in prev) continue
                prev[next] = cur
                if (next == target) return prev
                queue.add(next)
            }
        }

        return if (target in prev) prev else null
    }

    /** Reconstruct path from [start] to [end] using prev map from multi-source BFS. */
    private fun tracePath(start: Long, end: Long, prev: Map<Long, Long>): List<Long>? {
        if (start !in prev || end !in prev) return null
        val path = mutableListOf<Long>()
        var p = end
        while (p != prev[p]) {
            path.add(p)
            val pp = prev[p] ?: return null
            p = pp
        }
        path.add(start)
        path.reverse()
        return path
    }

    private fun transferOnce(provider: Storage<FluidVariant>, receiver: Storage<FluidVariant>, variant: FluidVariant, cap: Long): Long {
        val extractable = Transaction.openOuter().use { tx -> provider.extract(variant, cap, tx) }
        if (extractable <= 0L) return 0L
        val insertable = Transaction.openOuter().use { tx -> receiver.insert(variant, extractable, tx) }
        if (insertable <= 0L) return 0L

        var moved = 0L
        Transaction.openOuter().use { tx ->
            val ext = provider.extract(variant, insertable, tx)
            val ins = if (ext > 0L) receiver.insert(variant, ext, tx) else 0L
            if (ins > 0L) moved = minOf(ext, ins)
            tx.commit()
        }
        return moved
    }

    private fun resolveProviderVariant(storage: Storage<FluidVariant>, filterFluid: net.minecraft.fluid.Fluid?): FluidVariant? {
        if (filterFluid != null) {
            val filtered = FluidVariant.of(filterFluid)
            val available = Transaction.openOuter().use { tx ->
                storage.extract(filtered, 1L, tx) > 0L
            }
            return if (available) filtered else null
        }
        // dry-run extract 确认 view 真的支持提取，避免选中只进不出的储罐
        for (view in storage) {
            if (view.isResourceBlank || view.amount <= 0L) continue
            val resource = view.resource
            Transaction.openOuter().use { tx ->
                if (view.extract(resource, 1L, tx) > 0L) return resource
            }
        }
        return null
    }

    private fun canReceiverAccept(receiver: Storage<FluidVariant>, variant: FluidVariant): Boolean =
        Transaction.openOuter().use { tx -> receiver.insert(variant, 1L, tx) > 0L }

    private fun findProviders(world: World, topology: TopologyCache): List<ProviderEndpoint> {
        val providers = mutableListOf<ProviderEndpoint>()
        for (edge in topology.boundaries) {
            val sourcePos = BlockPos.fromLong(edge.cablePosLong)
            val sourceState = world.getBlockState(sourcePos)
            if (sourceState.block !is PumpAttachmentBlock) continue
            // 泵附件只能从正面抽取
            if (sourceState.get(Properties.FACING) != edge.lookupFromNeighborSide.opposite) continue
            val neighborPos = BlockPos.fromLong(edge.neighborPosLong)
            val storage = FluidStorage.SIDED.find(world, neighborPos, edge.lookupFromNeighborSide) ?: continue
            if (!storage.supportsExtraction()) continue
            val variant = resolveProviderVariant(storage, pumpFilterFor(world, edge))
            if (variant != null) {
                providers.add(ProviderEndpoint(storage, edge.cablePosLong, variant))
            }
        }
        return providers
    }

    private fun findReceivers(world: World, topology: TopologyCache, primaryFluid: Fluid): List<ReceiverEndpoint> {
        val primaryVariant = FluidVariant.of(primaryFluid)
        val receivers = mutableListOf<ReceiverEndpoint>()
        for (edge in topology.boundaries) {
            if (isPumpFrontFace(world, edge)) continue
            val neighborPos = BlockPos.fromLong(edge.neighborPosLong)
            val storage = FluidStorage.SIDED.find(world, neighborPos, edge.lookupFromNeighborSide) ?: continue
            if (!storage.supportsInsertion()) continue
            if (simulateInsertion(storage, primaryVariant, 1L) > 0L) {
                receivers.add(ReceiverEndpoint(storage, edge.cablePosLong))
            }
        }
        return receivers
    }

    private fun pumpFilterFor(world: World, edge: BoundaryEdge): Fluid? {
        val sourceState = world.getBlockState(BlockPos.fromLong(edge.cablePosLong))
        if (sourceState.block !is PumpAttachmentBlock) return null
        val sourceBe = world.getBlockEntity(BlockPos.fromLong(edge.cablePosLong)) as? PipeBlockEntity ?: return null
        return sourceBe.pumpFilterFluid()
    }

    private fun isPumpFrontFace(world: World, edge: BoundaryEdge): Boolean {
        val sourcePos = BlockPos.fromLong(edge.cablePosLong)
        val sourceState = world.getBlockState(sourcePos)
        val block = sourceState.block
        if (block !is PumpAttachmentBlock) return false
        return sourceState.get(Properties.FACING) == edge.lookupFromNeighborSide.opposite
    }

    private fun simulateInsertion(storage: Storage<FluidVariant>, variant: FluidVariant, amount: Long): Long =
        Transaction.openOuter().use { tx -> storage.insert(variant, amount, tx) }

    private fun buildTopology(world: World): TopologyCache {
        val neighbors = mutableMapOf<Long, MutableList<Long>>()
        val boundaries = mutableListOf<BoundaryEdge>()

        for (pipePosLong in pipes) {
            val pos = BlockPos.fromLong(pipePosLong)
            val state = world.getBlockState(pos)
            if (state.block !is BasePipeBlock) continue
            val adjacent = neighbors.getOrPut(pipePosLong) { mutableListOf() }
            for (dir in Direction.entries) {
                if (!state.get(BasePipeBlock.propertyFor(dir))) continue
                val np = pos.offset(dir)
                val npLong = np.asLong()
                if (npLong in pipes) {
                    val ns = world.getBlockState(np)
                    if (ns.block is BasePipeBlock && ns.get(BasePipeBlock.propertyFor(dir.opposite))) {
                        adjacent.add(npLong)
                    }
                } else {
                    boundaries.add(BoundaryEdge(pipePosLong, npLong, dir.opposite))
                }
            }
        }

        return TopologyCache(capacityByPipe.toMap(), neighbors.mapValues { it.value.toList() }, boundaries)
    }

    private fun syncPipeLoad(world: World, rates: Map<Long, Long>, remaining: Map<Long, Long>) {
        val fluidTypeChanged = primaryFluidId != lastSyncedFluidId

        if (fluidTypeChanged || currentTouchedPipes.isNotEmpty()) {
            val pipesToUpdate = if (fluidTypeChanged) pipes else currentTouchedPipes
            for (pipePos in pipesToUpdate) {
                val rate = rates[pipePos] ?: continue
                val used = rate - (remaining[pipePos] ?: rate)
                val pos = BlockPos.fromLong(pipePos)
                val be = world.getBlockEntity(pos) as? PipeBlockEntity ?: continue
                val beFluidChanged = be.currentFluidId != primaryFluidId
                if (be.pipeLoad == used && !beFluidChanged) continue
                be.pipeLoad = used
                be.currentFluidId = primaryFluidId
                if (beFluidChanged) {
                    world.updateListeners(pos, be.cachedState, be.cachedState, net.minecraft.block.Block.NOTIFY_LISTENERS)
                }
            }
        }

        // Clear load on pipes that had flow last tick but not this tick
        if (!fluidTypeChanged) {
            for (pipePos in lastTouchedPipes) {
                if (pipePos in currentTouchedPipes) continue
                val pos = BlockPos.fromLong(pipePos)
                val be = world.getBlockEntity(pos) as? PipeBlockEntity ?: continue
                if (be.pipeLoad == 0L) continue
                be.pipeLoad = 0L
            }
        }

        // Rotate touched pipe buffers for next tick
        lastTouchedPipes.clear()
        lastTouchedPipes.addAll(currentTouchedPipes)
        lastSyncedFluidId = primaryFluidId
    }

    private data class ProviderEndpoint(
        val storage: Storage<FluidVariant>,
        val entryPipe: Long,
        val variant: FluidVariant
    )

    private data class ReceiverEndpoint(
        val storage: Storage<FluidVariant>,
        val entryPipe: Long
    )

    private data class BoundaryEdge(
        val cablePosLong: Long,
        val neighborPosLong: Long,
        val lookupFromNeighborSide: Direction
    )

    private data class TopologyCache(
        val pipeRates: Map<Long, Long>,
        val neighbors: Map<Long, List<Long>>,
        val boundaries: List<BoundaryEdge>
    )

    private data class PathCacheKey(
        val start: Long,
        val end: Long
    )
}
