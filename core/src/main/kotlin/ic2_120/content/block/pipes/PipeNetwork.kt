package ic2_120.content.block.pipes

import ic2_120.compat.time

import ic2_120.compat.cachedState

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.minecraft.world.level.material.Fluid
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level

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

    /** Reused per receiver; normally remains empty unless an endpoint rejects a transfer. */
    private val reusableFailedProviderEntries = HashSet<Long>()

    /** Last synced fluid ID, used to detect fluid type changes that require full resync. */
    private var lastSyncedFluidId: String? = null

    fun addPipe(pos: BlockPos, block: BasePipeBlock) {
        val key = pos.asLong()
        pipes.add(key)
        val amountPerTick = kotlin.math.floor(
            block.flowRateBucketsPerSecond * FluidConstants.BUCKET.toDouble() / 20.0
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

    fun tickIfNeeded(world: Level) {
        if (lastTickTime == world.time) return
        lastTickTime = world.time
        pushFluids(world)
    }

    private fun pushFluids(world: Level) {
        if (pipes.isEmpty()) return

        val topology = topologyCache ?: buildTopology(world).also { topologyCache = it }

        currentTouchedPipes.clear()

        // Find providers via FluidStorage API (dry-run extract), no IFluidPipeUpgradeSupport needed
        val providers = findProviders(world, topology)

        val primaryFluid = providers.firstOrNull()?.variant?.fluid
        stalledByMixedProviders = primaryFluid != null && providers.any { it.variant.fluid != primaryFluid }
        primaryFluidId = primaryFluid?.let { BuiltInRegistries.FLUID.getKey(it).toString() }

        if (stalledByMixedProviders) {
            conflictingFluidIds = providers.asSequence()
                .map { BuiltInRegistries.FLUID.getKey(it.variant.fluid).toString() }
                .distinct()
                .toList()
            syncPipeLoad(world, topology.pipeRates, emptyMap())
            return
        } else {
            conflictingFluidIds = emptyList()
        }

        if (primaryFluid == null) {
            // No transfer can happen, so avoid copying every pipe capacity.
            // An empty remaining map means zero usage to syncPipeLoad.
            syncPipeLoad(world, topology.pipeRates, emptyMap())
            return
        }

        // Find receivers via FluidStorage API + simulateInsertion, no IFluidPipeUpgradeSupport needed
        val receivers = findReceivers(world, topology, primaryFluid)

        if (receivers.isEmpty()) {
            syncPipeLoad(world, topology.pipeRates, emptyMap())
            return
        }

        reusableRemaining.clear()
        for ((k, v) in topology.pipeRates) reusableRemaining[k] = v
        val remaining = reusableRemaining

        val primaryVariant = FluidVariant.of(primaryFluid)

        for (receiver in receivers) {
            reusableFailedProviderEntries.clear()

            while (true) {
                val best = findBestPath(
                    receiver,
                    providers,
                    reusableFailedProviderEntries,
                    topology.neighbors,
                    remaining
                )
                if (best == null) break

                val (provider, path, pathCap) = best
                val moved = transferOnce(provider.storage, receiver.storage, primaryVariant, pathCap)
                if (moved <= 0L) {
                    reusableFailedProviderEntries.add(provider.entryPipe)
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
            // findReceivers already simulated insertion of the exact
            // FluidVariant used by transferOnce. Also exclude storage self-loops
            // here to avoid allocating a filtered provider list per receiver.
            if (provider.storage === receiver.storage) continue

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
        // Probe both ends in one transaction. Closing it without commit rolls both
        // storages back, while avoiding a second outer transaction for every
        // provider/receiver attempt.
        val insertable = Transaction.openOuter().use { tx ->
            val extractable = provider.extract(variant, cap, tx)
            if (extractable <= 0L) 0L else receiver.insert(variant, extractable, tx)
        }
        if (insertable <= 0L) return 0L

        var moved = 0L
        Transaction.openOuter().use { tx ->
            val ext = provider.extract(variant, insertable, tx)
            val ins = if (ext > 0L) receiver.insert(variant, ext, tx) else 0L
            // Only commit a balanced transfer. A compatibility storage is allowed
            // to change its answer between simulation and execution; committing a
            // partial insertion here would otherwise destroy the difference.
            if (ext > 0L && ins == ext) {
                moved = ext
                tx.commit()
            }
        }
        return moved
    }

    private fun resolveProviderVariant(storage: Storage<FluidVariant>, filterFluid: net.minecraft.world.level.material.Fluid?): FluidVariant? {
        // 探测量必须 ≥ 1 mB（FluidConstants.BUCKET / 1000 = 81 droplets）。
        // 否则经 ForgeCompatUtil.toForgeBucket 桥接到 Forge IFluidHandler 时会被截断成 0 mB，
        // handler.drain(0 mB) 恒返回 0，导致所有 Forge 流体容器探测失败、泵附件抽不到它们的流体。
        // Fabric 原生 storage 按 droplet 精确计量的，不受此影响。
        val probe = FluidConstants.BUCKET / 1000L
        if (filterFluid != null) {
            val filtered = FluidVariant.of(filterFluid)
            val available = Transaction.openOuter().use { tx ->
                storage.extract(filtered, probe, tx) > 0L
            }
            return if (available) filtered else null
        }
        // dry-run extract 确认 view 真的支持提取，避免选中只进不出的储罐
        for (view in storage) {
            if (view.isResourceBlank || view.amount <= 0L) continue
            val resource = view.resource
            Transaction.openOuter().use { tx ->
                if (view.extract(resource, probe, tx) > 0L) return resource
            }
        }
        return null
    }

    private fun findProviders(world: Level, topology: TopologyCache): List<ProviderEndpoint> {
        val providers = mutableListOf<ProviderEndpoint>()
        for (edge in topology.boundaries) {
            val sourcePos = BlockPos.of(edge.cablePosLong)
            val sourceState = world.getBlockState(sourcePos)
            if (sourceState.block !is PumpAttachmentBlock) continue
            // 泵附件只能从正面抽取
            if (sourceState.getValue(BlockStateProperties.FACING) != edge.lookupFromNeighborSide.opposite) continue
            val neighborPos = BlockPos.of(edge.neighborPosLong)
            val storage = FluidStorage.SIDED.find(world, neighborPos, edge.lookupFromNeighborSide) ?: continue
            if (!storage.supportsExtraction()) continue
            val variant = resolveProviderVariant(storage, pumpFilterFor(world, edge))
            if (variant != null) {
                providers.add(ProviderEndpoint(storage, edge.cablePosLong, variant))
            }
        }
        return providers
    }

    private fun findReceivers(world: Level, topology: TopologyCache, primaryFluid: Fluid): List<ReceiverEndpoint> {
        val primaryVariant = FluidVariant.of(primaryFluid)
        val receivers = mutableListOf<ReceiverEndpoint>()
        for (edge in topology.boundaries) {
            if (isPumpFrontFace(world, edge)) continue
            val neighborPos = BlockPos.of(edge.neighborPosLong)
            val storage = FluidStorage.SIDED.find(world, neighborPos, edge.lookupFromNeighborSide) ?: continue
            if (!storage.supportsInsertion()) continue
            if (simulateInsertion(storage, primaryVariant, FluidConstants.BUCKET) > 0L) {
                receivers.add(ReceiverEndpoint(storage, edge.cablePosLong))
            }
        }
        return receivers
    }

    private fun pumpFilterFor(world: Level, edge: BoundaryEdge): Fluid? {
        val sourceState = world.getBlockState(BlockPos.of(edge.cablePosLong))
        if (sourceState.block !is PumpAttachmentBlock) return null
        val sourceBe = world.getBlockEntity(BlockPos.of(edge.cablePosLong)) as? PipeBlockEntity ?: return null
        return sourceBe.pumpFilterFluid()
    }

    private fun isPumpFrontFace(world: Level, edge: BoundaryEdge): Boolean {
        val sourcePos = BlockPos.of(edge.cablePosLong)
        val sourceState = world.getBlockState(sourcePos)
        val block = sourceState.block
        if (block !is PumpAttachmentBlock) return false
        return sourceState.getValue(BlockStateProperties.FACING) == edge.lookupFromNeighborSide.opposite
    }

    private fun simulateInsertion(storage: Storage<FluidVariant>, variant: FluidVariant, amount: Long): Long =
        Transaction.openOuter().use { tx -> storage.insert(variant, amount, tx) }

    private fun buildTopology(world: Level): TopologyCache {
        val neighbors = mutableMapOf<Long, MutableList<Long>>()
        val boundaries = mutableListOf<BoundaryEdge>()

        for (pipePosLong in pipes) {
            val pos = BlockPos.of(pipePosLong)
            val state = world.getBlockState(pos)
            if (state.block !is BasePipeBlock) continue
            val adjacent = neighbors.getOrPut(pipePosLong) { mutableListOf() }
            for (dir in Direction.entries) {
                if (!state.getValue(BasePipeBlock.propertyFor(dir))) continue
                val np = pos.relative(dir)
                val npLong = np.asLong()
                if (npLong in pipes) {
                    val ns = world.getBlockState(np)
                    if (ns.block is BasePipeBlock && ns.getValue(BasePipeBlock.propertyFor(dir.opposite))) {
                        adjacent.add(npLong)
                    }
                } else {
                    boundaries.add(BoundaryEdge(pipePosLong, npLong, dir.opposite))
                }
            }
        }

        return TopologyCache(capacityByPipe.toMap(), neighbors.mapValues { it.value.toList() }, boundaries)
    }

    private fun syncPipeLoad(world: Level, rates: Map<Long, Long>, remaining: Map<Long, Long>) {
        val fluidTypeChanged = primaryFluidId != lastSyncedFluidId

        if (fluidTypeChanged || currentTouchedPipes.isNotEmpty()) {
            val pipesToUpdate = if (fluidTypeChanged) pipes else currentTouchedPipes
            for (pipePos in pipesToUpdate) {
                val rate = rates[pipePos] ?: continue
                val used = rate - (remaining[pipePos] ?: rate)
                val pos = BlockPos.of(pipePos)
                val be = world.getBlockEntity(pos) as? PipeBlockEntity ?: continue
                val beFluidChanged = be.currentFluidId != primaryFluidId
                if (be.pipeLoad == used && !beFluidChanged) continue
                be.pipeLoad = used
                be.currentFluidId = primaryFluidId
                if (beFluidChanged) {
                    world.sendBlockUpdated(pos, be.cachedState, be.cachedState, net.minecraft.world.level.block.Block.UPDATE_CLIENTS)
                }
            }
        }

        // Clear load on pipes that had flow last tick but not this tick
        if (!fluidTypeChanged) {
            for (pipePos in lastTouchedPipes) {
                if (pipePos in currentTouchedPipes) continue
                val pos = BlockPos.of(pipePos)
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
