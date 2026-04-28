package ic2_120.content.block.energy

import ic2_120.content.block.IGenerator
import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.cables.BaseCableBlock
import ic2_120.content.item.energy.ITiered
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.inventory.Inventory
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.particle.ParticleTypes
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import org.apache.commons.logging.LogFactory
import org.slf4j.LoggerFactory
import team.reborn.energy.api.EnergyStorage
import kotlin.math.pow
import java.util.PriorityQueue

/**
 * 电网：一组相互连接的导线。
 *
 * 能量传输全部走"直通"路径：
 * - 外部注入 → 直接找到消费者路径送达，不进池
 * - 外部抽取 → 直接从 providers 沿路径拉取
 * - 内部分发 → providers → 路径 → consumers
 * - 所有路径共享 [cableTransferRemaining] 容量跟踪，确保每根导线每 tick 不超 transferRate
 *
 * [energy] 仅用于 BFS 构建时旧网合并的能量中转，分发完毕即归零。
 */
class EnergyNetwork : SnapshotParticipant<Long>() {

    companion object {
        const val damageIntervalTicks = 100
        val log = LoggerFactory.getLogger("EnergyNetwork")

        var ENABLE_OVERVOLTAGE_LOG = false
        var ENABLE_ENERGY_TRANSFER_LOG = false
        var ENABLE_TOPOLOGY_LOG = false
        var ENABLE_LEAK_LOG = false
        var ENABLE_CABLE_BURN_LOG = false
    }

    private val underTierCableBurnRatio = 1

    val cables = mutableSetOf<Long>()
    var energy: Long = 0
    var capacity: Long = 0
    var outputLevel: Int = 1
    var damageTickOffset: Int = 0

    private val cableLossMilliEu = mutableMapOf<Long, Long>()

    /** 本 tick 每根导线的剩余容量。所有传输路径共享此账本，保证不超载。 */
    private val cableTransferRemaining = mutableMapOf<Long, Long>()

    /** 用于 [cableTransferRemaining] 的 tick 重置检测。 */
    private var lastResetTime: Long = -1

    private var topologyCache: TopologyCache? = null
    private val dijkstraCacheByEntries = mutableMapOf<String, DijkstraResult>()
    private val bufferedCandidatesCacheByEntries = mutableMapOf<String, List<PathCandidate>>()
    var lastTickTime: Long = -1

    override fun createSnapshot(): Long = energy
    override fun readSnapshot(snapshot: Long) {
        energy = snapshot
    }

    fun addCable(pos: BlockPos, transferRate: Long, lossMilliEu: Long) {
        val key = pos.asLong()
        cables.add(key)
        capacity += transferRate
        cableLossMilliEu[key] = lossMilliEu
        invalidatePathCaches()
    }

    fun invalidateConnectionCaches() {
        invalidatePathCaches()
    }

    /** 每 tick 第一次访问时重置容量跟踪。 */
    private fun ensureCapacityTracking(world: World, cableRates: Map<Long, Long>) {
        if (lastResetTime == world.time && cableTransferRemaining.isNotEmpty()) return
        lastResetTime = world.time
        cableTransferRemaining.clear()
        for ((pos, rate) in cableRates) {
            cableTransferRemaining[pos] = rate
        }
    }

    /**
     * 外部通过导线向电网注入能量，直接沿路径送往消费者。
     * 只接收实际送达的量，不进池缓冲。
     * 注入速率受该导线的 [cableTransferRemaining] 限制。
     */
    fun insertAndDeliver(
        cablePos: Long, maxAmount: Long, world: World, transaction: TransactionContext
    ): Long {
        if (maxAmount <= 0 || cables.isEmpty()) return 0
        val topology = topologyCache ?: buildTopology(world).also { topologyCache = it }
        ensureCapacityTracking(world, topology.cableRates)

        val cableRemaining = cableTransferRemaining[cablePos] ?: return 0
        if (cableRemaining <= 0) return 0

        val consumers = findConsumers(world, topology)
        if (consumers.isEmpty()) return 0

        val toInject = minOf(maxAmount, cableRemaining)
        val dijkstra = shortestLossFromSourcesCached(setOf(cablePos), topology.neighbors)

        var total = 0L
        var remaining = toInject

        for ((_, consumer) in consumers) {
            if (remaining <= 0) break
            val demand = simulateInsertion(consumer.storage, Long.MAX_VALUE)
            if (demand <= 0) continue

            for (entry in consumer.entryCables) {
                val lossMilli = dijkstra.dist[entry] ?: continue
                val path = buildPath(entry, dijkstra.prev)
                if (path.isEmpty()) continue

                val pathCapacity = path.minOfOrNull { cableTransferRemaining[it] ?: 0L } ?: 0L
                if (pathCapacity <= 0) continue

                val pathLossEu = (lossMilli + 999) / 1000
                val maxDeliverable = (pathCapacity - pathLossEu).coerceAtLeast(0L)
                if (maxDeliverable <= 0) continue

                val stepDemand = minOf(demand, maxDeliverable, remaining)
                if (stepDemand <= 0) continue

                val inserted = consumer.storage.insert(stepDemand, transaction)
                if (inserted > 0) {
                    val moved = (inserted + pathLossEu).coerceAtMost(pathCapacity)
                    for (cablePosLong in path) {
                        cableTransferRemaining[cablePosLong] =
                            (cableTransferRemaining[cablePosLong] ?: 0L) - moved
                    }
                    total += inserted
                    remaining -= inserted
                }
                break
            }
        }
        return total
    }

    /**
     * 外部通过导线从电网抽取能量，直接从 providers 沿路径拉取。
     * 提取速率受该导线的 [cableTransferRemaining] 限制。
     */
    fun extractFromCable(
        cablePos: Long, maxAmount: Long, world: World, transaction: TransactionContext
    ): Long {
        if (maxAmount <= 0 || cables.isEmpty()) return 0
        val topology = topologyCache ?: buildTopology(world).also { topologyCache = it }
        ensureCapacityTracking(world, topology.cableRates)

        val cableRemaining = cableTransferRemaining[cablePos] ?: return 0
        if (cableRemaining <= 0) return 0

        val providers = findProviders(world, topology)
        if (providers.isEmpty()) return 0

        val toExtract = minOf(maxAmount, cableRemaining)
        var total = 0L
        var remaining = toExtract

        for ((_, provider) in providers) {
            if (remaining <= 0) break

            val demand = simulateExtraction(provider.storage, remaining)
            if (demand <= 0) continue

            val dijkstra = shortestLossFromSourcesCached(provider.entryCables.toSet(), topology.neighbors)
            val lossMilli = dijkstra.dist[cablePos] ?: continue
            val path = buildPath(cablePos, dijkstra.prev)
            if (path.isEmpty()) continue

            val pathCapacity = path.minOfOrNull { cableTransferRemaining[it] ?: 0L } ?: 0L
            if (pathCapacity <= 0) continue

            val pathLossEu = (lossMilli + 999) / 1000
            val maxObtainable = (pathCapacity - pathLossEu).coerceAtLeast(0L)
            if (maxObtainable <= 0) continue

            val stepExtract = minOf(demand, maxObtainable, remaining)
            if (stepExtract <= 0) continue

            val needFromProvider = stepExtract + pathLossEu

            val extracted = provider.storage.extract(needFromProvider, transaction)
            if (extracted > 0) {
                val moved = minOf(extracted, pathCapacity)
                for (cablePosLong in path) {
                    cableTransferRemaining[cablePosLong] =
                        (cableTransferRemaining[cablePosLong] ?: 0L) - moved
                }
                val deliverable = (extracted - pathLossEu).coerceAtLeast(0L)
                total += deliverable
                remaining -= deliverable
            }
        }
        return total
    }

    /** 由任一成员导线的 tick 触发；同一 game tick 仅执行一次。 */
    fun tickIfNeeded(world: World) {
        val time = world.time
        if (lastTickTime == time) return
        lastTickTime = time

        val isDamageTick = (time + damageTickOffset) % damageIntervalTicks == 0L

        pushToConsumers(world)
        tryUninsulatedCableDamage(world)
        tryUnderTierCableBurn(world)
        tryMachineOvervoltageExplosion(world)

        if (ENABLE_ENERGY_TRANSFER_LOG) {
            log.debug("[电网 Tick] 世界时间=$time，导线数=${cables.size}，能量=$energy/$capacity，输出等级=$outputLevel")
        }
    }

    private fun tryUninsulatedCableDamage(world: World) {
        if (world.isClient) return
        val serverWorld = world as? ServerWorld ?: return
        if ((world.time + damageTickOffset) % damageIntervalTicks != 0L) return

        val topology = topologyCache ?: buildTopology(world).also { topologyCache = it }
        if (topology.cablesThatLeak.isEmpty() || outputLevel < 1) return

        if (ENABLE_LEAK_LOG) {
            log.debug("[漏电检测] 电网输出等级=$outputLevel，漏电导线数量=${topology.cablesThatLeak.size}")
        }

        val damageSource = createCableShockDamageSource(serverWorld)
        val rangeInt = outputLevel
        val range = rangeInt.toDouble()
        val damageAmount = outputLevel * 2f
        val leakingCables = topology.cablesThatLeak.toHashSet()

        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var minZ = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var maxY = Int.MIN_VALUE
        var maxZ = Int.MIN_VALUE
        for (cablePosLong in leakingCables) {
            val x = BlockPos.unpackLongX(cablePosLong)
            val y = BlockPos.unpackLongY(cablePosLong)
            val z = BlockPos.unpackLongZ(cablePosLong)
            if (x < minX) minX = x
            if (y < minY) minY = y
            if (z < minZ) minZ = z
            if (x > maxX) maxX = x
            if (y > maxY) maxY = y
            if (z > maxZ) maxZ = z
        }

        val scanBox = Box(
            minX - range, minY - range, minZ - range,
            maxX + range + 1.0, maxY + range + 1.0, maxZ + range + 1.0
        )

        val hurtEntities = mutableListOf<LivingEntity>()
        for (entity in world.getEntitiesByClass(LivingEntity::class.java, scanBox) { e ->
            e.isAlive && !e.isSpectator
        }) {
            if (hasLeakingCableNearby(entity.blockPos, leakingCables, rangeInt)) {
                entity.damage(damageSource, damageAmount)
                hurtEntities.add(entity)
            }
        }

        if (hurtEntities.isNotEmpty() && ENABLE_LEAK_LOG) {
            log.debug("[漏电检测] 造成伤害：${hurtEntities.joinToString { it.name.string }}，伤害值=$damageAmount")
        }
    }

    private fun hasLeakingCableNearby(entityPos: BlockPos, leakingCables: Set<Long>, range: Int): Boolean {
        val ex = entityPos.x
        val ey = entityPos.y
        val ez = entityPos.z
        for (dx in -range..range) {
            for (dy in -range..range) {
                for (dz in -range..range) {
                    if (BlockPos.asLong(ex + dx, ey + dy, ez + dz) in leakingCables) return true
                }
            }
        }
        return false
    }

    private fun createCableShockDamageSource(world: ServerWorld): DamageSource {
        val registry = world.registryManager.get(RegistryKeys.DAMAGE_TYPE)
        val key = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of("ic2_120", "cable_shock"))
        val entry = registry.getEntry(key).orElse(null)
            ?: registry.getEntry(RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of("minecraft", "lightning")))
                .orElseThrow()
        return DamageSource(entry)
    }

    private fun tryUnderTierCableBurn(world: World) {
        if (world.isClient) return
        val serverWorld = world as? ServerWorld ?: return
        if ((world.time + damageTickOffset) % damageIntervalTicks != 0L) return

        val topology = topologyCache ?: buildTopology(world).also { topologyCache = it }
        val toBurn = topology.cablesThatCanBurn
        if (toBurn.isEmpty() || outputLevel < 1) return

        if (ENABLE_CABLE_BURN_LOG) {
            log.debug("[导线烧毁] 电网输出等级=$outputLevel，可烧毁导线数量=${toBurn.size}（电压等级 < $outputLevel）")
        }

        val burnCount = (toBurn.size * underTierCableBurnRatio).toInt().coerceIn(0, toBurn.size)
        if (burnCount <= 0) return

        if (ENABLE_CABLE_BURN_LOG) {
            log.debug("[导线烧毁] 准备烧毁 $burnCount 根导线（比例=${underTierCableBurnRatio}）")
        }

        val shuffled = toBurn.toMutableList()
        for (i in shuffled.indices.reversed()) {
            if (i == 0) break
            val j = serverWorld.random.nextInt(i + 1)
            val t = shuffled[i]
            shuffled[i] = shuffled[j]
            shuffled[j] = t
        }
        val actuallyBurned = mutableListOf<Pair<BlockPos, String>>()
        for (i in 0 until burnCount) {
            val posLong = shuffled[i]
            val pos = BlockPos.fromLong(posLong)
            val state = world.getBlockState(pos)
            if (state.isAir) continue
            val block = state.block as? BaseCableBlock ?: continue
            if (block !is ITiered || block.tier >= outputLevel) continue

            actuallyBurned.add(pos to state.block.toString())

            val x = pos.x + 0.5
            val y = pos.y + 0.5
            val z = pos.z + 0.5
            serverWorld.spawnParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 12, 0.2, 0.2, 0.2, 0.02)
            serverWorld.spawnParticles(ParticleTypes.FLAME, x, y, z, 6, 0.15, 0.15, 0.15, 0.01)
            world.breakBlock(pos, false)
        }

        if (actuallyBurned.isNotEmpty() && ENABLE_CABLE_BURN_LOG) {
            log.debug("[导线烧毁] 实际烧毁：${actuallyBurned.joinToString { "${it.second} @ ${it.first}" }}")
        }
    }

    private fun tryMachineOvervoltageExplosion(world: World) {
        if (world.isClient) return
        if ((world.time + damageTickOffset) % damageIntervalTicks != 0L) return

        val topology = topologyCache ?: buildTopology(world).also { topologyCache = it }
        if (outputLevel < 1) return

        if (ENABLE_OVERVOLTAGE_LOG) {
            log.debug("[超压检测] 开始检查，电网输出等级=$outputLevel，边界数量=${topology.boundaries.size}")
        }

        val checked = mutableSetOf<Long>()
        var explodedCount = 0
        var skippedGeneratorCount = 0
        var skippedTierOkCount = 0
        var skippedNotMachineCount = 0

        for (boundary in topology.boundaries) {
            val neighborLong = boundary.neighborPosLong
            if (neighborLong in checked) continue
            checked.add(neighborLong)
            val neighborPos = BlockPos.fromLong(neighborLong)
            val be = world.getBlockEntity(neighborPos) ?: continue

            if (be is IGenerator) {
                if (ENABLE_OVERVOLTAGE_LOG) {
                    log.debug("[超压检测]   → 跳过：是发电机 (IGenerator)")
                }
                skippedGeneratorCount++
                continue
            }
            if (be !is ITieredMachine) {
                if (ENABLE_OVERVOLTAGE_LOG) {
                    log.debug("[超压检测]   → 跳过：不是 ITieredMachine")
                }
                skippedNotMachineCount++
                continue
            }

            val effectiveTier = be.effectiveVoltageTierForSide(boundary.lookupFromNeighborSide)

            val blockState = world.getBlockState(neighborPos)
            val machineTier = be.tier
            val sidedTier = be.getVoltageTierForSide(boundary.lookupFromNeighborSide)
            val lookupSide = boundary.lookupFromNeighborSide
            val cableSide = lookupSide.opposite

            if (ENABLE_OVERVOLTAGE_LOG) {
                log.info(
                    "[超压检测-详细] 方块=$blockState @ $neighborPos" +
                            "\n  → lookupFromNeighborSide=$lookupSide（机器看导线），cableSide=$cableSide（导线看机器）" +
                            "\n  → 基础tier=$machineTier，该面电压=$sidedTier，有效耐压=$effectiveTier" +
                            "\n  → 电网输出等级=$outputLevel，检查方向=$lookupSide"
                )
            }

            if (ENABLE_OVERVOLTAGE_LOG) {
                log.debug("[超压检测]   → 有效耐压等级=$effectiveTier (方向=${boundary.lookupFromNeighborSide})")
            }

            if (outputLevel <= effectiveTier) {
                if (ENABLE_OVERVOLTAGE_LOG) {
                    log.debug("[超压检测]   → 跳过：耐压等级足够 ($outputLevel <= $effectiveTier)")
                }
                skippedTierOkCount++
                continue
            }

            if (ENABLE_OVERVOLTAGE_LOG) {
                log.debug("[超压检测]   → 触发爆炸！电网等级 $outputLevel > 机器耐压 $effectiveTier")
            }
            if (be is Inventory) be.clear()
            world.breakBlock(neighborPos, false)
            val x = neighborPos.x + 0.5
            val y = neighborPos.y + 0.5
            val z = neighborPos.z + 0.5
            val power = explosionPowerForOutputLevel(outputLevel)
            world.createExplosion(null, x, y, z, power, false, World.ExplosionSourceType.BLOCK)
            explodedCount++
        }

        if (ENABLE_OVERVOLTAGE_LOG) {
            log.debug("[超压检测] 检查完成 - 爆炸=$explodedCount, 跳过发电机=$skippedGeneratorCount, 跳过耐压足够=$skippedTierOkCount, 跳过非机器=$skippedNotMachineCount")
        }
    }

    private fun explosionPowerForOutputLevel(level: Int): Float {
        if (level <= 0) return 0.25f
        return (2f * 2.0.pow(level - 4)).toFloat()
    }

    /** 扫描拓扑边界，返回消费者（supportsInsertion）。 */
    private fun findConsumers(world: World, topology: TopologyCache): Map<Long, Endpoint> {
        val consumers = mutableMapOf<Long, Endpoint>()
        for (boundary in topology.boundaries) {
            val neighborPos = BlockPos.fromLong(boundary.neighborPosLong)
            val storage = EnergyStorage.SIDED.find(world, neighborPos, boundary.lookupFromNeighborSide) ?: continue
            if (storage.supportsInsertion()) {
                val ep = consumers.getOrPut(boundary.neighborPosLong) { Endpoint(storage) }
                ep.entryCables.add(boundary.cablePosLong)
                ep.blockPos = boundary.neighborPosLong
            }
        }
        return consumers
    }

    /** 扫描拓扑边界，返回 providers（supportsExtraction）。 */
    private fun findProviders(world: World, topology: TopologyCache): Map<Long, Endpoint> {
        val providers = mutableMapOf<Long, Endpoint>()
        for (boundary in topology.boundaries) {
            val neighborPos = BlockPos.fromLong(boundary.neighborPosLong)
            val storage = EnergyStorage.SIDED.find(world, neighborPos, boundary.lookupFromNeighborSide) ?: continue
            if (storage.supportsExtraction()) {
                val ep = providers.getOrPut(boundary.neighborPosLong) { Endpoint(storage) }
                ep.entryCables.add(boundary.cablePosLong)
                ep.blockPos = boundary.neighborPosLong
            }
        }
        return providers
    }

    private fun pushToConsumers(world: World) {
        if (cables.isEmpty()) return

        val topology = topologyCache ?: buildTopology(world).also { topologyCache = it }
        ensureCapacityTracking(world, topology.cableRates)

        val consumers = findConsumers(world, topology)
        val providers = findProviders(world, topology)

        // Phase 1: 残余池能分发（BFS 合并等场景遗留的 energy，通常一次 tick 即清空）
        for ((_, consumer) in consumers) {
            if (energy <= 0) break
            pullFromBufferedEnergyByPath(world, consumer, topology.neighbors)
        }

        // Phase 2: providers → consumers 直送
        if (providers.isNotEmpty()) {
            for ((_, consumer) in consumers) {
                pullFromProvidersByPath(world, consumer, providers, topology.neighbors)
            }
        }

        syncCableLoadToLocalStorage(world, topology.cableRates)
    }

    private fun pullFromBufferedEnergyByPath(
        world: World,
        consumer: Endpoint,
        neighbors: Map<Long, List<Long>>
    ) {
        while (energy > 0) {
            val demand = simulateInsertion(consumer.storage, Long.MAX_VALUE)
            if (demand <= 0) break

            val candidates = buildBufferedCandidates(consumer, neighbors)
            if (candidates.isEmpty()) break

            var progressed = false
            for (candidate in candidates) {
                if (energy <= 0) break
                val pathCapacity = candidate.path.minOfOrNull { cableTransferRemaining[it] ?: 0L } ?: 0L
                if (pathCapacity <= 0) continue

                val pathLossEu = (candidate.pathLossMilliEu + 999) / 1000
                val maxDeliverable = (pathCapacity - pathLossEu).coerceAtLeast(0L)
                if (maxDeliverable <= 0) continue

                val stepDemand = simulateInsertion(consumer.storage, maxDeliverable)
                if (stepDemand <= 0) break

                val takeFromPool = minOf(pathCapacity, stepDemand + pathLossEu, energy)
                if (takeFromPool <= pathLossEu) continue

                Transaction.openOuter().use { tx ->
                    val deliverable = (takeFromPool - pathLossEu).coerceAtLeast(0L)
                    val inserted = consumer.storage.insert(deliverable, tx)
                    if (inserted > 0) {
                        val moved = (inserted + pathLossEu).coerceAtMost(takeFromPool)
                        updateSnapshots(tx)
                        energy -= moved
                        for (cablePosLong in candidate.path) {
                            cableTransferRemaining[cablePosLong] =
                                (cableTransferRemaining[cablePosLong] ?: 0L) - moved
                        }

                        if (ENABLE_ENERGY_TRANSFER_LOG) {
                            val destPos = consumer.blockPos?.let { BlockPos.fromLong(it) } ?: BlockPos.ORIGIN
                            val destBlock = world.getBlockState(destPos).block
                            log.info(
                                "[能量传输] 电网缓冲 → $destBlock @ $destPos | " +
                                        "传输=$inserted EU, 路径损耗=$pathLossEu EU, 路径长度=${candidate.path.size}"
                            )
                        }

                        tx.commit()
                        progressed = true
                    }
                }
            }

            if (!progressed) break
        }
    }

    private fun pullFromProvidersByPath(
        world: World,
        consumer: Endpoint,
        providers: Map<Long, Endpoint>,
        neighbors: Map<Long, List<Long>>
    ) {
        while (true) {
            val demand = simulateInsertion(consumer.storage, Long.MAX_VALUE)
            if (demand <= 0) break

            val candidates = buildProviderCandidates(consumer, providers, neighbors)
            if (candidates.isEmpty()) break

            var progressed = false
            for (candidate in candidates) {
                val pathCapacity = candidate.path.minOfOrNull { cableTransferRemaining[it] ?: 0L } ?: 0L
                if (pathCapacity <= 0) continue

                val pathLossEu = (candidate.pathLossMilliEu + 999) / 1000
                val maxDeliverable = (pathCapacity - pathLossEu).coerceAtLeast(0L)
                if (maxDeliverable <= 0) continue

                val stepDemand = simulateInsertion(consumer.storage, maxDeliverable)
                if (stepDemand <= 0) break

                val needFromProvider = minOf(pathCapacity, stepDemand + pathLossEu)
                if (needFromProvider <= pathLossEu) continue

                Transaction.openOuter().use { tx ->
                    val extracted = candidate.provider.extract(needFromProvider, tx)
                    if (extracted <= pathLossEu) return@use

                    val deliverable = (extracted - pathLossEu).coerceAtLeast(0L)
                    if (deliverable <= 0) return@use

                    val inserted = consumer.storage.insert(deliverable, tx)
                    if (inserted > 0) {
                        val moved = (inserted + pathLossEu).coerceAtMost(extracted)
                        for (cablePosLong in candidate.path) {
                            cableTransferRemaining[cablePosLong] =
                                (cableTransferRemaining[cablePosLong] ?: 0L) - moved
                        }

                        if (ENABLE_ENERGY_TRANSFER_LOG) {
                            val sourcePos = candidate.providerPos?.let { BlockPos.fromLong(it) } ?: BlockPos.ORIGIN
                            val destPos = consumer.blockPos?.let { BlockPos.fromLong(it) } ?: BlockPos.ORIGIN
                            val sourceBlock = world.getBlockState(sourcePos).block
                            val destBlock = world.getBlockState(destPos).block
                            log.info(
                                "[能量传输] $sourceBlock @ $sourcePos → $destBlock @ $destPos | " +
                                        "提取=$extracted EU, 传输=$inserted EU, 路径损耗=$pathLossEu EU, 路径长度=${candidate.path.size}"
                            )
                        }

                        tx.commit()
                        progressed = true
                    }
                }
            }

            if (!progressed) break
            break
        }
    }

    private fun syncCableLoadToLocalStorage(
        world: World,
        cableRates: Map<Long, Long>
    ) {
        if (world.isClient) return
        for (cablePosLong in cables) {
            if (cablePosLong !in cableLossMilliEu) continue
            val transferRate = cableRates[cablePosLong] ?: continue
            val remaining = cableTransferRemaining[cablePosLong] ?: transferRate
            val used = transferRate - remaining
            val pos = BlockPos.fromLong(cablePosLong)
            val be = world.getBlockEntity(pos)
            if (be is ic2_120.content.block.cables.CableBlockEntity) {
                be.cableLoad = used
            }
        }
    }

    private fun buildProviderCandidates(
        consumer: Endpoint,
        providers: Map<Long, Endpoint>,
        neighbors: Map<Long, List<Long>>
    ): List<ProviderPath> {
        val candidates = mutableListOf<ProviderPath>()
        for (source in consumer.entryCables) {
            val dijkstra = shortestLossFromSourcesCached(setOf(source), neighbors)
            for ((providerPosLong, provider) in providers) {
                for (entry in provider.entryCables) {
                    val loss = dijkstra.dist[entry] ?: continue
                    val path = buildPath(entry, dijkstra.prev)
                    if (path.isNotEmpty()) {
                        candidates.add(ProviderPath(provider.storage, path, loss, providerPosLong))
                    }
                }
            }
        }
        candidates.sortBy { it.pathLossMilliEu }
        return candidates
    }

    private fun buildBufferedCandidates(
        consumer: Endpoint,
        neighbors: Map<Long, List<Long>>
    ): List<PathCandidate> {
        val cacheKey = entriesKey(consumer.entryCables)
        bufferedCandidatesCacheByEntries[cacheKey]?.let { return it }
        val candidates = mutableListOf<PathCandidate>()
        for (source in consumer.entryCables) {
            val dijkstra = shortestLossFromSourcesCached(setOf(source), neighbors)
            for (cablePosLong in cables) {
                val loss = dijkstra.dist[cablePosLong] ?: continue
                val path = buildPath(cablePosLong, dijkstra.prev)
                if (path.isNotEmpty()) {
                    candidates.add(PathCandidate(path, loss))
                }
            }
        }
        candidates.sortBy { it.pathLossMilliEu }
        bufferedCandidatesCacheByEntries[cacheKey] = candidates
        trimPathCachesIfNeeded()
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
        trimPathCachesIfNeeded()
        return result
    }

    private fun entriesKey(entries: Set<Long>): String =
        entries.sorted().joinToString(",")

    private fun trimPathCachesIfNeeded() {
        if (dijkstraCacheByEntries.size > 512) dijkstraCacheByEntries.clear()
        if (bufferedCandidatesCacheByEntries.size > 512) bufferedCandidatesCacheByEntries.clear()
    }

    private fun invalidatePathCaches() {
        topologyCache = null
        dijkstraCacheByEntries.clear()
        bufferedCandidatesCacheByEntries.clear()
    }

    private fun buildTopology(world: World): TopologyCache {
        val cableRates = mutableMapOf<Long, Long>()
        val neighbors = mutableMapOf<Long, MutableList<Long>>()
        val boundaries = mutableListOf<BoundaryEdge>()
        var maxLevel = 1

        if (ENABLE_TOPOLOGY_LOG) {
            log.debug("[电网拓扑] 开始构建电网拓扑，导线数量=${cables.size}")
        }

        for (cablePosLong in cables) {
            val cablePos = BlockPos.fromLong(cablePosLong)
            val state = world.getBlockState(cablePos)
            val block = state.block as? BaseCableBlock ?: continue
            cableRates[cablePosLong] = block.getTransferRate()
            val adjacent = neighbors.getOrPut(cablePosLong) { mutableListOf() }
            for (dir in Direction.values()) {
                if (!state.get(BaseCableBlock.propertyFor(dir))) continue
                val neighborPos = cablePos.offset(dir)
                val neighborLong = neighborPos.asLong()
                if (neighborLong in cables) {
                    adjacent.add(neighborLong)
                } else {
                    boundaries.add(BoundaryEdge(cablePosLong, neighborLong, dir.opposite))
                    val neighborBe = world.getBlockEntity(neighborPos)
                    val storage = EnergyStorage.SIDED.find(world, neighborPos, dir.opposite)
                    if (neighborBe is ITieredMachine && storage?.supportsExtraction() == true) {
                        val tierForSide = neighborBe.effectiveVoltageTierForSide(dir.opposite)
                        val oldMax = maxLevel
                        maxLevel = maxOf(maxLevel, tierForSide)

                        if (ENABLE_TOPOLOGY_LOG) {
                            val blockName = world.getBlockState(neighborPos).block.toString()
                            val machineTier = neighborBe.tier
                            val sidedTier = neighborBe.getVoltageTierForSide(dir)
                            val effectiveTier = neighborBe.effectiveVoltageTierForSide(dir)

                            log.info(
                                "[电网拓扑-输出检测] 方块=$blockName @ $neighborPos，方向=$dir（导线->机器）" +
                                        "\n  → 基础tier=$machineTier，该面电压=$sidedTier，有效电压=$effectiveTier" +
                                        "\n  → 该面只输出不输入，判定为真正的输出面" +
                                        "\n  → 当前电网输出等级=$oldMax，更新后=$maxLevel"
                            )

                            if (maxLevel != oldMax) {
                                log.debug(
                                    "[电网拓扑] 更新输出等级：$oldMax -> $maxLevel (来源：$blockName @ $neighborPos，方向=$dir，该面电压=$tierForSide)"
                                )
                            }
                        }
                    } else if (ENABLE_TOPOLOGY_LOG && neighborBe is ITieredMachine && storage?.supportsExtraction() == true && storage.supportsInsertion()) {
                        val blockName = world.getBlockState(neighborPos).block.toString()
                        val machineTier = neighborBe.tier
                        val sidedTier = neighborBe.getVoltageTierForSide(dir)

                        log.info(
                            "[电网拓扑-跳过输入输出面] 方块=$blockName @ $neighborPos，方向=$dir（导线->机器）" +
                                    "\n  → 该面同时支持输入和输出，判定为输入面，跳过电网输出等级计算" +
                                    "\n  → 基础tier=$machineTier，该面电压=$sidedTier"
                        )
                    }
                }
            }
        }

        outputLevel = maxLevel
        if (ENABLE_TOPOLOGY_LOG) {
            log.debug("[电网拓扑] 电网拓扑构建完成，最终输出等级=$outputLevel，边界数量=${boundaries.size}")
        }

        val cablesThatLeak = cables.filter { cablePosLong ->
            val block =
                world.getBlockState(BlockPos.fromLong(cablePosLong)).block as? BaseCableBlock ?: return@filter true
            block.insulationLevel < outputLevel
        }

        val cablesThatCanBurn = cables.filter { cablePosLong ->
            val block = world.getBlockState(BlockPos.fromLong(cablePosLong)).block
            val tier = (block as? ITiered)?.tier ?: 1
            tier < outputLevel
        }

        return TopologyCache(
            cableRates = cableRates,
            neighbors = neighbors.mapValues { it.value.toList() },
            boundaries = boundaries,
            cablesThatLeak = cablesThatLeak,
            cablesThatCanBurn = cablesThatCanBurn
        )
    }

    /**
     * 模拟插入能量，不改变实际存储，因为有事务在，会自动回滚storage
     */
    private fun simulateInsertion(storage: EnergyStorage, maxAmount: Long): Long {
        var accepted = 0L
        Transaction.openOuter().use { tx ->
            accepted = storage.insert(maxAmount, tx)
        }
        return accepted
    }

    /** 模拟抽取能量，不改变实际存储。 */
    private fun simulateExtraction(storage: EnergyStorage, maxAmount: Long): Long {
        var accepted = 0L
        Transaction.openOuter().use { tx ->
            accepted = storage.extract(maxAmount, tx)
        }
        return accepted
    }

    private data class Endpoint(
        val storage: EnergyStorage,
        val entryCables: MutableSet<Long> = mutableSetOf(),
        var blockPos: Long? = null
    )

    private data class ProviderPath(
        val provider: EnergyStorage,
        val path: List<Long>,
        val pathLossMilliEu: Long,
        val providerPos: Long? = null
    )

    private data class PathCandidate(
        val path: List<Long>,
        val pathLossMilliEu: Long
    )

    private data class BoundaryEdge(
        val cablePosLong: Long,
        val neighborPosLong: Long,
        val lookupFromNeighborSide: Direction
    )

    private data class TopologyCache(
        val cableRates: Map<Long, Long>,
        val neighbors: Map<Long, List<Long>>,
        val boundaries: List<BoundaryEdge>,
        val cablesThatLeak: List<Long>,
        val cablesThatCanBurn: List<Long>
    )

    private data class DijkstraResult(
        val dist: Map<Long, Long>,
        val prev: Map<Long, Long?>
    )

    private fun shortestLossFromSources(
        sources: Set<Long>,
        neighbors: Map<Long, List<Long>>
    ): DijkstraResult {
        if (sources.isEmpty()) return DijkstraResult(emptyMap(), emptyMap())

        val dist = mutableMapOf<Long, Long>()
        val prev = mutableMapOf<Long, Long?>()
        val pq = PriorityQueue(compareBy<Pair<Long, Long>> { it.second })

        for (source in sources) {
            val startLoss = cableLossMilliEu[source] ?: 0L
            dist[source] = startLoss
            prev[source] = null
            pq.add(source to startLoss)
        }

        while (pq.isNotEmpty()) {
            val (node, currentDist) = pq.poll()
            if (currentDist != dist[node]) continue
            val nextNodes = neighbors[node] ?: emptyList()
            for (next in nextNodes) {
                val weight = cableLossMilliEu[next] ?: 0L
                val nd = currentDist + weight
                val od = dist[next]
                if (od == null || nd < od) {
                    dist[next] = nd
                    prev[next] = node
                    pq.add(next to nd)
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

    fun getEnergySharePerCable(): Long {
        val count = cables.size
        return if (count > 0) energy / count else 0
    }
}
