package ic2_120.content.block.pipes

import ic2_120.content.upgrade.IFluidPipeUpgradeSupport
import ic2_120.content.block.MachineBlock
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.state.property.Properties
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

    fun addPipe(pos: BlockPos, block: BasePipeBlock) {
        val key = pos.asLong()
        pipes.add(key)
        val amountPerTick = kotlin.math.floor(
            block.size.baseBucketsPerSecond * block.material.multiplier * FluidConstants.BUCKET.toDouble() / 20.0
        ).toLong().coerceAtLeast(1L)
        capacityByPipe[key] = amountPerTick
        topologyCache = null
    }

    fun invalidateConnectionCaches() {
        topologyCache = null
    }

    fun tickIfNeeded(world: World) {
        if (lastTickTime == world.time) return
        lastTickTime = world.time
        pushFluids(world)
    }

    private fun pushFluids(world: World) {
        if (pipes.isEmpty()) return

        val topology = topologyCache ?: buildTopology(world).also { topologyCache = it }
        val remaining = topology.pipeRates.toMutableMap()
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

            val forcedByPumpAttachment = sourceBlock is PumpAttachmentBlock &&
                sourceState.get(Properties.FACING) == machineSide.opposite &&
                storage.supportsExtraction()
            val forcedFilter = if (forcedByPumpAttachment) sourceBe?.pumpFilterFluid() else null

            if (
                (be != null && be.fluidPipeProviderEnabled && allowsProviderOnSide(be, machineSide) && storage.supportsExtraction()) ||
                forcedByPumpAttachment
            ) {
                val filter = if (forcedByPumpAttachment) forcedFilter else be?.fluidPipeProviderFilter
                val variant = resolveProviderVariant(storage, filter) ?: continue
                providers.add(ProviderEndpoint(storage, edge.cablePosLong, variant))
            }
            if (storage.supportsInsertion()) {
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
        if (fluidKinds.size > 1) {
            stalledByMixedProviders = true
            syncPipeLoad(world, topology.pipeRates, remaining)
            return
        }
        stalledByMixedProviders = false

        val workingFluid = providers.firstOrNull()?.variant ?: run {
            syncPipeLoad(world, topology.pipeRates, remaining)
            return
        }

        for (receiver in receivers) {
            if (receiver.filter != null && receiver.filter != workingFluid.fluid) continue
            var progress = true
            while (progress) {
                progress = false
                val best = providers
                    .mapNotNull { provider ->
                        if (provider.variant.fluid != workingFluid.fluid) return@mapNotNull null
                        val path = shortestPath(provider.entryPipe, receiver.entryPipe, topology.neighbors, remaining) ?: return@mapNotNull null
                        Triple(provider, path, path.minOf { remaining[it] ?: 0L })
                    }
                    .filter { it.third > 0L }
                    .minByOrNull { it.second.size } ?: break

                val provider = best.first
                val path = best.second
                val pathCap = best.third

                val moved = transferOnce(provider.storage, receiver.storage, workingFluid, pathCap)
                if (moved <= 0L) continue

                for (pipe in path) {
                    remaining[pipe] = (remaining[pipe] ?: 0L) - moved
                }
                progress = true
            }
        }

        syncPipeLoad(world, topology.pipeRates, remaining)
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
            return FluidVariant.of(filterFluid)
        }
        return storage.iterator().asSequence().firstOrNull { !it.resource.isBlank && it.amount > 0L }?.resource
    }

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

    private fun shortestPath(start: Long, end: Long, neighbors: Map<Long, List<Long>>, remaining: Map<Long, Long>): List<Long>? {
        if (start == end && (remaining[start] ?: 0L) > 0L) return listOf(start)
        val queue = ArrayDeque<Long>()
        val prev = mutableMapOf<Long, Long?>()
        queue.add(start)
        prev[start] = null
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            for (next in neighbors[cur].orEmpty()) {
                if ((remaining[next] ?: 0L) <= 0L) continue
                if (next in prev) continue
                prev[next] = cur
                if (next == end) {
                    val path = mutableListOf<Long>()
                    var p: Long? = end
                    while (p != null) {
                        path.add(p)
                        p = prev[p]
                    }
                    path.reverse()
                    return path
                }
                queue.add(next)
            }
        }
        return null
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
        for (pipePos in pipes) {
            val rate = rates[pipePos] ?: continue
            val used = rate - (remaining[pipePos] ?: rate)
            val be = world.getBlockEntity(BlockPos.fromLong(pipePos)) as? PipeBlockEntity ?: continue
            be.pipeLoad = used
        }
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
}
