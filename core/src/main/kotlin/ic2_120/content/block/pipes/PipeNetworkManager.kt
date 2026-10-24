package ic2_120.content.block.pipes

import net.minecraft.resources.ResourceKey
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level

object PipeNetworkManager {
    private val worldPosToNetwork = mutableMapOf<ResourceKey<Level>, MutableMap<Long, PipeNetwork>>()
    private val worldNetworks = mutableMapOf<ResourceKey<Level>, MutableSet<PipeNetwork>>()

    private fun mapFor(world: Level): MutableMap<Long, PipeNetwork> =
        worldPosToNetwork.getOrPut(world.registryKey) { mutableMapOf() }

    fun getOrCreateNetwork(world: Level, pos: BlockPos): PipeNetwork {
        val map = mapFor(world)
        map[pos.asLong()]?.let { return it }
        return buildNetwork(world, pos, map)
    }

    private fun buildNetwork(world: Level, startPos: BlockPos, map: MutableMap<Long, PipeNetwork>): PipeNetwork {
        val network = PipeNetwork()
        val visited = mutableSetOf<Long>()
        val queue = ArrayDeque<BlockPos>()
        val absorbedNetworks = mutableSetOf<PipeNetwork>()
        queue.add(startPos)
        visited.add(startPos.asLong())

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val state = world.getBlockState(current)
            val block = state.block as? BasePipeBlock ?: continue

            map[current.asLong()]?.let { old ->
                if (old !== network) {
                    absorbedNetworks.add(old)
                    network.lastTickTime = maxOf(network.lastTickTime, old.lastTickTime)
                }
            }

            network.addPipe(current, block)
            map[current.asLong()] = network
            (world.getBlockEntity(current) as? PipeBlockEntity)?.network = network

            for (dir in Direction.entries) {
                if (!state.get(BasePipeBlock.propertyFor(dir))) continue
                val next = current.offset(dir)
                val nextLong = next.asLong()
                if (nextLong in visited) continue
                visited.add(nextLong)
                val nextState = world.getBlockState(next)
                val nextBlock = nextState.block
                if (nextBlock is BasePipeBlock && nextState.get(BasePipeBlock.propertyFor(dir.opposite))) {
                    queue.add(next)
                }
            }
        }

        val networks = worldNetworks.getOrPut(world.registryKey) { mutableSetOf() }
        networks.removeAll(absorbedNetworks)
        networks.add(network)
        return network
    }

    fun invalidateAt(world: Level, pos: BlockPos) {
        val map = worldPosToNetwork[world.registryKey] ?: return
        val network = map[pos.asLong()] ?: return
        worldNetworks[world.registryKey]?.remove(network)
        for (pipePos in network.pipes) {
            map.remove(pipePos)
            (world.getBlockEntity(BlockPos.fromLong(pipePos)) as? PipeBlockEntity)?.network = null
        }
    }

    fun invalidateConnectionCachesAt(world: Level, pos: BlockPos) {
        val map = worldPosToNetwork[world.registryKey] ?: return
        val network = map[pos.asLong()] ?: return
        network.invalidateConnectionCaches()
    }

    fun tickAllWorlds(server: net.minecraft.server.MinecraftServer) {
        for (world in server.worlds) {
            // Snapshot by network count so invalidation during transfer cannot
            // mutate the set being iterated. This replaces the old per-pipe
            // map scan and temporary deduplication set.
            val networks = worldNetworks[world.registryKey]?.toList() ?: continue
            for (network in networks) network.tickIfNeeded(world)
        }
    }

    fun onWorldUnload(world: Level) {
        worldPosToNetwork.remove(world.registryKey)
        worldNetworks.remove(world.registryKey)
    }
}
