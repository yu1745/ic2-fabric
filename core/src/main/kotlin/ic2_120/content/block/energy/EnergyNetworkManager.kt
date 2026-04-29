package ic2_120.content.block.energy

import ic2_120.content.block.cables.BaseCableBlock
import ic2_120.content.block.cables.CableBlockEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.registry.RegistryKey
import net.minecraft.world.World
import org.slf4j.LoggerFactory

/**
 * 全局电网管理器。
 *
 * - **惰性构建**：导线首次 tick 时通过 [getOrCreateNetwork] 触发 BFS 构建电网。
 * - **自动合并**：BFS 过程中发现已有旧电网的导线，会将旧网能量吸收进新网。
 * - **自动拆分**：导线被破坏时 [invalidateAt] 溶解旧电网，剩余导线下一 tick 惰性重建。
 */
object EnergyNetworkManager {

    private val logger = LoggerFactory.getLogger("ic2_120/EnergyNetworkManager")
    private val worldPosToNetwork = mutableMapOf<RegistryKey<World>, MutableMap<Long, EnergyNetwork>>()

    private fun mapFor(world: World): MutableMap<Long, EnergyNetwork> =
        worldPosToNetwork.getOrPut(world.registryKey) { mutableMapOf() }

    /** 获取 [pos] 所在电网；不存在时 BFS 构建。 */
    fun getOrCreateNetwork(world: World, pos: BlockPos): EnergyNetwork {
        val posToNetwork = mapFor(world)
        posToNetwork[pos.asLong()]?.let { return it }
        return buildNetwork(world, pos, posToNetwork)
    }

    /**
     * 从 [startPos] 出发 BFS，收集所有通过连接属性相连的 [BaseCableBlock]，
     * 构建一个新的 [EnergyNetwork]，并吸收途中遇到的任何旧网。
     */
    private fun buildNetwork(
        world: World,
        startPos: BlockPos,
        posToNetwork: MutableMap<Long, EnergyNetwork>
    ): EnergyNetwork {
        val network = EnergyNetwork()
        network.damageTickOffset =
            world.random.nextBetween(0, EnergyNetwork.damageIntervalTicks - 1) // 0–x 秒随机偏移，错开不同电网的伤害时机
        val visited = mutableSetOf<Long>()
        val queue = ArrayDeque<BlockPos>()
        val absorbedNetworks = mutableSetOf<EnergyNetwork>()

        queue.add(startPos)
        visited.add(startPos.asLong())

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val state = world.getBlockState(current)
            val block = state.block as? BaseCableBlock ?: continue

            posToNetwork[current.asLong()]?.let { oldNet ->
                if (oldNet !in absorbedNetworks) {
                    absorbedNetworks.add(oldNet)
                    network.energy += oldNet.energy
                    if (oldNet.lastTickTime == world.time) {
                        network.lastTickTime = world.time
                    }
                }
            }

            network.addCable(current, block.getTransferRate(), block.getEnergyLoss())
            posToNetwork[current.asLong()] = network

            val be = world.getBlockEntity(current) as? CableBlockEntity
            if (be != null) {
                network.energy += be.localEnergy
                be.localEnergy = 0
                be.network = network
            }

            for (dir in Direction.values()) {
                if (!state.get(BaseCableBlock.propertyFor(dir))) continue
                val neighbor = current.offset(dir)
                val neighborLong = neighbor.asLong()
                if (neighborLong in visited) continue
                visited.add(neighborLong)
                if (world.getBlockState(neighbor).block is BaseCableBlock) {
                    queue.add(neighbor)
                }
            }
        }

        network.energy = network.energy.coerceAtMost(network.capacity)
        logger.debug("构建电网：{} 根导线，容量 {}，能量 {}", network.cables.size, network.capacity, network.energy)
        return network
    }

    /**
     * 溶解 [pos] 所在的电网：将池中能量均分回各导线 BE 的
     * [CableBlockEntity.localEnergy]，清除所有网络引用。
     * 剩余导线会在下次 tick 惰性重建新电网。
     */
    fun invalidateAt(world: World, pos: BlockPos) {
        val posToNetwork = worldPosToNetwork[world.registryKey] ?: return
        val network = posToNetwork[pos.asLong()] ?: return
        distributeEnergyToEntities(world, network)
        for (cablePosLong in network.cables) {
            posToNetwork.remove(cablePosLong)
            (world.getBlockEntity(BlockPos.fromLong(cablePosLong)) as? CableBlockEntity)?.network = null
        }
    }

    /** 连接边界变化（如相邻机器放置/移除）时，让所在电网刷新拓扑缓存。 */
    fun invalidateConnectionCachesAt(world: World, pos: BlockPos) {
        val posToNetwork = worldPosToNetwork[world.registryKey] ?: return
        val network = posToNetwork[pos.asLong()] ?: return
        network.invalidateConnectionCaches()
    }

    private fun distributeEnergyToEntities(world: World, network: EnergyNetwork) {
        val entities = network.cables.mapNotNull { posLong ->
            world.getBlockEntity(BlockPos.fromLong(posLong)) as? CableBlockEntity
        }
        if (entities.isEmpty()) return
        val share = network.energy / entities.size
        var remainder = network.energy % entities.size
        for (be in entities) {
            be.localEnergy = share + if (remainder > 0) {
                remainder--; 1
            } else 0
        }
    }

    /** 世界卸载时仅清理该维度缓存，避免误伤其他维度电网。 */
    fun onWorldUnload(world: World) {
        worldPosToNetwork.remove(world.registryKey)
    }
}
