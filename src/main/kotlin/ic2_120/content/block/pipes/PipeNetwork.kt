package ic2_120.content.block.pipes

import ic2_120.content.block.MachineBlock
import ic2_120.content.upgrade.IFluidPipeUpgradeSupport
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.state.property.Properties
import net.minecraft.registry.Registries
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

class PipeNetwork {
    val pipes = mutableSetOf<Long>()
    private val capacityByPipe = mutableMapOf<Long, Long>()
    private var topologyCache: TopologyCache? = null
    var lastTickTime: Long = -1

    var stalledByMixedProviders: Boolean = false
        private set

    var primaryFluidId: String? = null
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

        val providers = mutableListOf<ProviderEndpoint>()
        val receivers = mutableListOf<ReceiverEndpoint>()

        for (edge in topology.boundaries) {
            val neighborPos = BlockPos.fromLong(edge.neighborPosLong)
            val block = world.getBlockState(neighborPos).block
            val be = world.getBlockEntity(neighborPos) as? IFluidPipeUpgradeSupport
            val storage = FluidStorage.SIDED.find(world, neighborPos, edge.lookupFromNeighborSide) ?: continue
            val machineSide = edge.lookupFromNeighborSide
            val sourcePipePos = BlockPos.fromLong(edge.cablePosLong)
            val sourceState = world.getBlockState(sourcePipePos)
            val sourceBlock = sourceState.block
            val sourceBe = world.getBlockEntity(sourcePipePos) as? PipeBlockEntity

            val isPumpBlock = sourceBlock is PumpAttachmentBlock
            val pumpFacingCorrect = isPumpBlock && sourceState.get(Properties.FACING) == machineSide.opposite
            val forcedByPumpAttachment = isPumpBlock && pumpFacingCorrect && storage.supportsExtraction()
            val forcedFilter = if (forcedByPumpAttachment) sourceBe?.pumpFilterFluid() else null

            val providerCond = be != null && be.fluidPipeProviderEnabled && allowsProviderOnSide(be, machineSide) && storage.supportsExtraction()
            val isProvider = providerCond || forcedByPumpAttachment
            if (isProvider) {
                val filter = if (forcedByPumpAttachment) forcedFilter else be?.fluidPipeProviderFilter
                val variant = resolveProviderVariant(storage, filter)
                if (variant != null) {
                    providers.add(ProviderEndpoint(storage, edge.cablePosLong, variant))
                }
            }
            if (storage.supportsInsertion() && !forcedByPumpAttachment) {
                if (block is MachineBlock) {
                    if (be?.fluidPipeReceiverEnabled == true && allowsReceiverOnSide(be, machineSide)) {
                        receivers.add(ReceiverEndpoint(storage, edge.cablePosLong, be.fluidPipeReceiverFilter))
                    }
                } else {
                    receivers.add(ReceiverEndpoint(storage, edge.cablePosLong, null))
                }
            }
        }

        val fluidKinds = providers.map { it.variant.fluid }.toSet()
        stalledByMixedProviders = fluidKinds.size > 1
        primaryFluidId = providers.firstOrNull()?.variant?.fluid?.let { Registries.FLUID.getId(it).toString() }

        if (providers.isEmpty() || receivers.isEmpty()) {
            syncPipeLoad(world, topology.pipeRates, remaining)
            return
        }

        for (receiver in receivers) {
            val receiverTarget = receiver.filter
            val failedProviderEntries = mutableSetOf<Long>()

            while (true) {
                val best = findBestPath(receiver, providers, failedProviderEntries, receiverTarget, topology.neighbors, remaining)
                if (best == null) break

                val (provider, path, pathCap) = best
                val moved = transferOnce(provider.storage, receiver.storage, provider.variant, pathCap)
                if (moved <= 0L) {
                    failedProviderEntries.add(provider.entryPipe)
                    if (failedProviderEntries.size >= providers.size) break
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
        receiverTarget: net.minecraft.fluid.Fluid?,
        neighbors: Map<Long, List<Long>>,
        remaining: Map<Long, Long>
    ): Triple<ProviderEndpoint, List<Long>, Long>? {
        val uncachedProviders = mutableListOf<ProviderEndpoint>()
        var best: Triple<ProviderEndpoint, List<Long>, Long>? = null

        for (provider in providers) {
            if (provider.entryPipe in failedProviders) continue
            if (receiverTarget != null && provider.variant.fluid != receiverTarget) continue
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
            if (ext <= 0L) return@use
            val ins = receiver.insert(variant, ext, tx)
            if (ins <= 0L) return@use
            moved = minOf(ext, ins)
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
        return storage.iterator().asSequence().firstOrNull { !it.resource.isBlank && it.amount > 0L }?.resource
    }

    private fun canReceiverAccept(receiver: Storage<FluidVariant>, variant: FluidVariant): Boolean =
        Transaction.openOuter().use { tx -> receiver.insert(variant, 1L, tx) > 0L }

    // 规则：一方指定方向、另一方任意时，任意方自动排除已指定方向，避免同一面既入又出。
    private fun allowsProviderOnSide(be: IFluidPipeUpgradeSupport, side: Direction): Boolean {
        val providerSide = be.fluidPipeProviderSide
        val receiverSide = be.fluidPipeReceiverSide
        return when {
            providerSide != null -> providerSide == side
            receiverSide != null -> receiverSide != side
            else -> true
        }
    }

    // 规则：一方指定方向、另一方任意时，任意方自动排除已指定方向，避免同一面既入又出。
    private fun allowsReceiverOnSide(be: IFluidPipeUpgradeSupport, side: Direction): Boolean {
        val providerSide = be.fluidPipeProviderSide
        val receiverSide = be.fluidPipeReceiverSide
        return when {
            receiverSide != null -> receiverSide == side
            providerSide != null -> providerSide != side
            else -> true
        }
    }

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
        val entryPipe: Long,
        val filter: net.minecraft.fluid.Fluid?
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
