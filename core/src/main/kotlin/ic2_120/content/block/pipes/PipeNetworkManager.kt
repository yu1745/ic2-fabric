package ic2_120.content.block.pipes

import net.minecraft.registry.RegistryKey
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

object PipeNetworkManager {
    private val worldPosToNetwork = mutableMapOf<RegistryKey<World>, MutableMap<Long, PipeNetwork>>()

    private fun mapFor(world: World): MutableMap<Long, PipeNetwork> =
        worldPosToNetwork.getOrPut(world.registryKey) { mutableMapOf() }

    fun getOrCreateNetwork(world: World, pos: BlockPos): PipeNetwork {
        val map = mapFor(world)
        map[pos.asLong()]?.let { return it }
        return buildNetwork(world, pos, map)
    }

    private fun buildNetwork(world: World, startPos: BlockPos, map: MutableMap<Long, PipeNetwork>): PipeNetwork {
        val network = PipeNetwork()
        val visited = mutableSetOf<Long>()
        val queue = ArrayDeque<BlockPos>()
        queue.add(startPos)
        visited.add(startPos.asLong())

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val state = world.getBlockState(current)
            val block = state.block as? BasePipeBlock ?: continue

            map[current.asLong()]?.let { old ->
                if (old !== network) {
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

        return network
    }

    fun invalidateAt(world: World, pos: BlockPos) {
        val map = worldPosToNetwork[world.registryKey] ?: return
        val network = map[pos.asLong()] ?: return
        for (pipePos in network.pipes) {
            map.remove(pipePos)
            (world.getBlockEntity(BlockPos.fromLong(pipePos)) as? PipeBlockEntity)?.network = null
        }
    }

    fun invalidateConnectionCachesAt(world: World, pos: BlockPos) {
        val map = worldPosToNetwork[world.registryKey] ?: return
        val network = map[pos.asLong()] ?: return
        network.invalidateConnectionCaches()
    }

    fun onWorldUnload(world: World) {
        worldPosToNetwork.remove(world.registryKey)
    }
}
