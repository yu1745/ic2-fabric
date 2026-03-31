package ic2_120.content.block.transmission

import net.minecraft.block.BlockState
import net.minecraft.registry.RegistryKey
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

object KineticNetworkManager {
    private val worldPosToNetwork = mutableMapOf<RegistryKey<World>, MutableMap<Long, KineticNetwork>>()

    private fun mapFor(world: World): MutableMap<Long, KineticNetwork> =
        worldPosToNetwork.getOrPut(world.registryKey) { mutableMapOf() }

    fun tickAt(world: World, pos: BlockPos) {
        if (world.isClient) return
        val state = world.getBlockState(pos)
        if (!KineticConnectionRules.isTransmissionNode(state)) return
        val network = getOrCreateNetwork(world, pos)
        network.tickIfNeeded(world)
    }

    fun getNodeKu(world: World, pos: BlockPos): Int {
        if (world.isClient) return 0
        val state = world.getBlockState(pos)
        if (!KineticConnectionRules.isTransmissionNode(state)) return 0
        return getOrCreateNetwork(world, pos).getNodeKu(world, pos)
    }

    fun invalidateAt(world: World, pos: BlockPos) {
        val posToNetwork = worldPosToNetwork[world.registryKey] ?: return
        val network = posToNetwork[pos.asLong()] ?: return
        for (node in network.nodes) {
            posToNetwork.remove(node)
            (world.getBlockEntity(BlockPos.fromLong(node)) as? TransmissionBlockEntity)?.setCurrentKu(0)
        }
    }

    fun invalidateConnectionCachesAt(world: World, pos: BlockPos) {
        val posToNetwork = worldPosToNetwork[world.registryKey] ?: return
        val network = posToNetwork[pos.asLong()] ?: return
        network.invalidateConnectionCaches()
    }

    fun onWorldUnload(world: World) {
        worldPosToNetwork.remove(world.registryKey)
    }

    private fun getOrCreateNetwork(world: World, start: BlockPos): KineticNetwork {
        val posToNetwork = mapFor(world)
        posToNetwork[start.asLong()]?.let { return it }
        return buildNetwork(world, start, posToNetwork)
    }

    private fun buildNetwork(world: World, start: BlockPos, posToNetwork: MutableMap<Long, KineticNetwork>): KineticNetwork {
        val network = KineticNetwork()
        val visited = mutableSetOf<Long>()
        val queue = ArrayDeque<BlockPos>()
        val absorbedNetworks = mutableSetOf<KineticNetwork>()
        queue.add(start)
        visited.add(start.asLong())

        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            val state = world.getBlockState(cur)
            if (!KineticConnectionRules.isTransmissionNode(state)) continue
            val key = cur.asLong()

            posToNetwork[key]?.let { oldNet ->
                if (oldNet !in absorbedNetworks) {
                    absorbedNetworks.add(oldNet)
                    if (oldNet.wasTickedAt(world.time)) {
                        network.markTickedAt(world.time)
                    }
                }
            }

            network.addNode(cur)
            posToNetwork[key] = network

            val dirs = KineticConnectionRules.nodeDirections(world, cur, state)
            for (dir in dirs) {
                val next = cur.offset(dir)
                val nextLong = next.asLong()
                if (nextLong in visited) continue
                val nextState = world.getBlockState(next)
                if (!KineticConnectionRules.isTransmissionNode(nextState)) continue
                if (!KineticConnectionRules.canConnectFromDirection(nextState, dir.opposite)) continue
                visited.add(nextLong)
                queue.add(next)
            }
        }
        return network
    }

    fun isTransmissionNode(state: BlockState): Boolean {
        return KineticConnectionRules.isTransmissionNode(state)
    }

    fun canConnectFromDirection(state: BlockState, fromDirection: net.minecraft.util.math.Direction): Boolean =
        KineticConnectionRules.canConnectFromDirection(state, fromDirection)
}
