package ic2_120.content.block.energy

import net.minecraft.core.registries.Registries

import ic2_120.compat.time

import ic2_120.compat.isClient

import ic2_120.content.block.IGenerator
import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.machines.TransformerBlockEntity
import ic2_120.content.block.cables.BaseCableBlock
import ic2_120.content.block.cables.CableBlockEntity
import ic2_120.content.item.energy.ITiered
import ic2_120.integration.ftbchunks.ClaimProtection
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.Container
import net.minecraft.resources.ResourceKey
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerLevel
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
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
class EnergyNetwork : SnapshotParticipant<EnergyNetwork.NetworkSnapshot>() {

    companion object {
        const val damageIntervalTicks = 100

        /** 端点缓存强制全量重查间隔（tick），兜底无 BE 邻居被替换/能力变化的场景。 */
        private const val ENDPOINT_VERIFY_INTERVAL = 20

        val log = LoggerFactory.getLogger("EnergyNetwork")

        var ENABLE_OVERVOLTAGE_LOG = false
        var ENABLE_ENERGY_TRANSFER_LOG = false
        var ENABLE_TOPOLOGY_LOG = false
        var ENABLE_LEAK_LOG = false
        var ENABLE_CABLE_BURN_LOG = false
    }

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

    /** 边界端点缓存（SIDED.find 结果 + 邻居 BE 引用），避免每 tick 对不变拓扑全量重查世界。 */
    private var boundaryEndpointCache: List<CachedEndpointLookup>? = null

    /** 端点强制重查相位（0..INTERVAL-1），把不同电网的全量重查分散到不同 tick。 */
    private val endpointVerifyPhase: Int = java.util.concurrent.ThreadLocalRandom.current().nextInt(ENDPOINT_VERIFY_INTERVAL)

    /** provider 候选路径缓存（key = consumer entries + providers 签名），避免每 tick 重建+排序。 */
    private val providerCandidatesCacheByConsumer = mutableMapOf<String, List<ProviderPath>>()

    var lastTickTime: Long = -1

    /**
     * 消费者公平分发的轮转起点。每 tick 向后移动，避免同一个 consumer 永远先拿到首轮配额。
     */
    private var consumerRoundRobinOffset = 0

    override fun createSnapshot(): NetworkSnapshot = NetworkSnapshot(
        energy = energy,
        cableTransferRemaining = cableTransferRemaining.toMutableMap()
    )

    override fun readSnapshot(snapshot: NetworkSnapshot) {
        energy = snapshot.energy
        cableTransferRemaining.clear()
        cableTransferRemaining.putAll(snapshot.cableTransferRemaining)
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
    private fun ensureCapacityTracking(world: Level, cableRates: Map<Long, Long>) {
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
        cablePos: Long, maxAmount: Long, world: Level, transaction: TransactionContext
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
            var demandRemaining = simulateInsertion(consumer.storage, Long.MAX_VALUE, transaction)
            if (demandRemaining <= 0) continue

            val sortedEntries = consumer.entryCables.sortedBy { dijkstra.dist[it] ?: Long.MAX_VALUE }
            for (entry in sortedEntries) {
                if (remaining <= 0 || demandRemaining <= 0) break
                val lossMilli = dijkstra.dist[entry] ?: continue
                val path = buildPath(entry, dijkstra.prev)
                if (path.isEmpty()) continue

                val pathCapacity = path.minOfOrNull { cableTransferRemaining[it] ?: 0L } ?: 0L
                if (pathCapacity <= 0) continue

                val pathLossEu = (lossMilli + 999) / 1000
                val maxDeliverable = (pathCapacity - pathLossEu).coerceAtLeast(0L)
                if (maxDeliverable <= 0) continue

                val stepDemand = minOf(demandRemaining, maxDeliverable, remaining)
                if (stepDemand <= 0) continue

                val inserted = consumer.storage.insert(stepDemand, transaction)
                if (inserted > 0) {
                    val moved = (inserted + pathLossEu).coerceAtMost(pathCapacity)
                    // 容量账本纳入事务快照：外部事务回滚时一并恢复，避免同 tick 可用容量被多扣。
                    updateSnapshots(transaction)
                    for (cablePosLong in path) {
                        cableTransferRemaining[cablePosLong] =
                            (cableTransferRemaining[cablePosLong] ?: 0L) - moved
                    }
                    total += inserted
                    remaining -= inserted
                    demandRemaining -= inserted
                }
            }
        }
        return total
    }

    /**
     * 外部通过导线从电网抽取能量，直接从 providers 沿路径拉取。
     * 提取速率受该导线的 [cableTransferRemaining] 限制。
     */
    fun extractFromCable(
        cablePos: Long, maxAmount: Long, world: Level, transaction: TransactionContext
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

            val available = simulateExtraction(provider.storage, remaining, transaction)
            if (available <= 0) continue

            val dijkstra = shortestLossFromSourcesCached(provider.entryCables.toSet(), topology.neighbors)
            val lossMilli = dijkstra.dist[cablePos] ?: continue
            val path = buildPath(cablePos, dijkstra.prev)
            if (path.isEmpty()) continue

            val pathCapacity = path.minOfOrNull { cableTransferRemaining[it] ?: 0L } ?: 0L
            if (pathCapacity <= 0) continue

            val pathLossEu = (lossMilli + 999) / 1000
            val maxObtainable = (pathCapacity - pathLossEu).coerceAtLeast(0L)
            if (maxObtainable <= 0) continue

            val stepExtract = minOf(available, maxObtainable, remaining)
            if (stepExtract <= 0) continue

            val needFromProvider = stepExtract + pathLossEu

            val extracted = provider.storage.extract(needFromProvider, transaction)
            if (extracted > 0) {
                val moved = minOf(extracted, pathCapacity)
                // 容量账本纳入事务快照：外部事务回滚时一并恢复。
                updateSnapshots(transaction)
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
    fun tickIfNeeded(world: Level) {
        val time = world.time
        if (lastTickTime == time) return
        lastTickTime = time

        pushToConsumers(world)
        tryUninsulatedCableDamage(world)
        tryUnderTierCableBurn(world)
        tryMachineOvervoltageExplosion(world)

        if (ENABLE_ENERGY_TRANSFER_LOG) {
            log.debug("[电网 Tick] 世界时间=$time，导线数=${cables.size}，能量=$energy/$capacity，输出等级=$outputLevel")
        }
    }

    private fun tryUninsulatedCableDamage(world: Level) {
        if (world.isClient) return
        val serverWorld = world as? ServerLevel ?: return
        if ((world.time + damageTickOffset) % damageIntervalTicks != 0L) return

        val topology = topologyCache ?: buildTopology(world).also { topologyCache = it }
        if (topology.cablesThatLeak.isEmpty() || outputLevel < 1) return
        if (!ic2_120.config.Ic2Config.current.general.shockWhenNoEnergyFlow && energy <= 0) return

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
            val x = BlockPos.getX(cablePosLong)
            val y = BlockPos.getY(cablePosLong)
            val z = BlockPos.getZ(cablePosLong)
            if (x < minX) minX = x
            if (y < minY) minY = y
            if (z < minZ) minZ = z
            if (x > maxX) maxX = x
            if (y > maxY) maxY = y
            if (z > maxZ) maxZ = z
        }

        val scanBox = AABB(
            minX - range, minY - range, minZ - range,
            maxX + range + 1.0, maxY + range + 1.0, maxZ + range + 1.0
        )

        val hurtEntities = mutableListOf<LivingEntity>()
        for (entity in world.getEntitiesOfClass(LivingEntity::class.java, scanBox) { e ->
            e.isAlive && !e.isSpectator
        }) {
            if (hasLeakingCableNearby(entity.blockPosition(), leakingCables, rangeInt)) {
                entity.hurt(damageSource, damageAmount)
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

    private fun createCableShockDamageSource(world: ServerLevel): DamageSource {
        val registry = world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
        val key = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("ic2_120", "cable_shock"))
        val entry = registry.getHolder(key).orElse(null)
            ?: registry.getHolder(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("minecraft", "lightning")))
                .orElseThrow()
        return DamageSource(entry)
    }

    private fun tryUnderTierCableBurn(world: Level) {
        if (world.isClient) return
        val serverWorld = world as? ServerLevel ?: return
        if ((world.time + damageTickOffset) % damageIntervalTicks != 0L) return

        val topology = topologyCache ?: buildTopology(world).also { topologyCache = it }
        val toBurn = topology.cablesThatCanBurn
        if (toBurn.isEmpty() || outputLevel < 1) return

        if (ENABLE_CABLE_BURN_LOG) {
            log.debug("[导线烧毁] 电网输出等级=$outputLevel，可烧毁导线数量=${toBurn.size}（电压等级 < $outputLevel）")
        }

        val generalConfig = ic2_120.config.Ic2Config.current.general
        val minBurnPercent = generalConfig.cableBurnMinPercent.coerceIn(1, 100)
        val maxBurnPercent = generalConfig.cableBurnMaxPercent.coerceIn(1, 100)
        val lowerPercent = minOf(minBurnPercent, maxBurnPercent)
        val upperPercent = maxOf(minBurnPercent, maxBurnPercent)
        val burnPercent = serverWorld.random.nextIntBetweenInclusive(lowerPercent, upperPercent)
        // 向上取整，避免少量导线因百分比计算得到 0 根烧毁。
        val burnCount = ((toBurn.size.toLong() * burnPercent + 99L) / 100L)
            .toInt()
            .coerceIn(1, toBurn.size)
        if (burnCount <= 0) return

        if (ENABLE_CABLE_BURN_LOG) {
            log.debug("[导线烧毁] 准备烧毁 $burnCount 根导线（随机比例=${burnPercent}%）")
        }

        /**
         * 从随机种子开始沿拓扑邻接关系扩张，形成连续的烧毁区域。
         * 只有当前区域已经没有可烧邻居，或电网本身不连通时，才选择新的种子。
         */
        val remaining = toBurn.toMutableSet()
        val selected = mutableListOf<Long>()
        while (selected.size < burnCount && remaining.isNotEmpty()) {
            val seed = remaining.elementAt(serverWorld.random.nextInt(remaining.size))
            remaining.remove(seed)

            val frontier = ArrayList<Long>()
            frontier.add(seed)
            while (frontier.isNotEmpty() && selected.size < burnCount) {
                val frontierIndex = serverWorld.random.nextInt(frontier.size)
                val current = frontier.removeAt(frontierIndex)
                selected.add(current)

                for (neighbor in topology.neighbors[current].orEmpty()) {
                    if (remaining.remove(neighbor)) {
                        frontier.add(neighbor)
                    }
                }
            }
        }
        val actuallyBurned = mutableListOf<Pair<BlockPos, String>>()
        val burnPositions = mutableListOf<BlockPos>()
        for (posLong in selected) {
            val pos = BlockPos.of(posLong)
            val state = world.getBlockState(pos)
            if (state.isAir) continue
            val block = state.block as? BaseCableBlock ?: continue
            if (block !is ITiered || block.tier >= outputLevel) continue

            burnPositions.add(pos.immutable())
        }
        if (!ClaimProtection.allAllowedUuid(world, burnPositions, null, ClaimProtection.EDIT_BLOCK)) return

        for (posLong in selected) {
            val pos = BlockPos.of(posLong)
            val state = world.getBlockState(pos)
            if (state.isAir) continue
            val block = state.block as? BaseCableBlock ?: continue
            if (block !is ITiered || block.tier >= outputLevel) continue

            actuallyBurned.add(pos to state.block.toString())

            val x = pos.x + 0.5
            val y = pos.y + 0.5
            val z = pos.z + 0.5
            serverWorld.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 12, 0.2, 0.2, 0.2, 0.02)
            serverWorld.sendParticles(ParticleTypes.FLAME, x, y, z, 6, 0.15, 0.15, 0.15, 0.01)
            world.destroyBlock(pos, false)
        }

        if (actuallyBurned.isNotEmpty() && ENABLE_CABLE_BURN_LOG) {
            log.debug("[导线烧毁] 实际烧毁：${actuallyBurned.joinToString { "${it.second} @ ${it.first}" }}")
        }
    }

    private fun tryMachineOvervoltageExplosion(world: Level) {
        if (world.isClient) return
        if ((world.time + damageTickOffset) % damageIntervalTicks != 0L) return
        if (!ic2_120.config.Ic2Config.current.general.enableOvervoltageExplosion) return
        if (!ic2_120.config.Ic2Config.current.general.explodeWhenNoEnergyFlow && energy <= 0) return

        val topology = topologyCache ?: buildTopology(world).also { topologyCache = it }
        if (outputLevel < 1) return

        if (ENABLE_OVERVOLTAGE_LOG) {
            log.debug("[超压检测] 开始检查，电网输出等级=$outputLevel，边界数量=${topology.boundaries.size}")
        }

        val checked = mutableSetOf<Long>()
        var explodedCount = 0
        var skippedGeneratorCount = 0
        var skippedTransformerCount = 0
        var skippedTierOkCount = 0
        var skippedNotMachineCount = 0

        val explosionCandidates = mutableListOf<BlockPos>()
        val candidateSeen = mutableSetOf<Long>()
        for (boundary in topology.boundaries) {
            if (!candidateSeen.add(boundary.neighborPosLong)) continue
            val candidatePos = BlockPos.of(boundary.neighborPosLong)
            val candidate = world.getBlockEntity(candidatePos)
            if (candidate is IGenerator || candidate is TransformerBlockEntity || candidate !is ITieredMachine) continue
            if (outputLevel > candidate.effectiveVoltageTierForSide(boundary.lookupFromNeighborSide)) {
                explosionCandidates.add(candidatePos)
            }
        }
        val candidatePower = explosionPowerForOutputLevel(outputLevel)
        if (!ClaimProtection.allAllowedUuid(world, explosionCandidates, null, ClaimProtection.EDIT_BLOCK) ||
            !explosionCandidates.all { ClaimProtection.explosionCubeAllowed(world, it, candidatePower, null) }) return

        for (boundary in topology.boundaries) {
            val neighborLong = boundary.neighborPosLong
            if (neighborLong in checked) continue
            checked.add(neighborLong)
            val neighborPos = BlockPos.of(neighborLong)
            val be = world.getBlockEntity(neighborPos) ?: continue

            if (be is IGenerator) {
                if (ENABLE_OVERVOLTAGE_LOG) {
                    log.debug("[超压检测]   → 跳过：是发电机 (IGenerator)")
                }
                skippedGeneratorCount++
                continue
            }
            if (be is TransformerBlockEntity) {
                if (ENABLE_OVERVOLTAGE_LOG) {
                    log.debug("[超压检测]   → 跳过：是变压器 (TransformerBlockEntity)")
                }
                skippedTransformerCount++
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
            if (be is Container) be.clear()
            world.destroyBlock(neighborPos, false)
            val x = neighborPos.x + 0.5
            val y = neighborPos.y + 0.5
            val z = neighborPos.z + 0.5
            world.explode(null, x, y, z, candidatePower, false, Level.ExplosionInteraction.BLOCK)
            explodedCount++
        }

        if (ENABLE_OVERVOLTAGE_LOG) {
            log.debug("[超压检测] 检查完成 - 爆炸=$explodedCount, 跳过发电机=$skippedGeneratorCount, 跳过变压器=$skippedTransformerCount, 跳过耐压足够=$skippedTierOkCount, 跳过非机器=$skippedNotMachineCount")
        }
    }

    private fun explosionPowerForOutputLevel(level: Int): Float {
        if (level <= 0) return 0.25f
        return (2f * 2.0.pow(level - 4)).toFloat()
    }

    /**
     * 获取（并按需重建）边界端点缓存。缓存构建遍历拓扑边界逐个 [EnergyStorage.SIDED.find]，
     * 结果在拓扑/端点不变时跨 tick 复用，避免每 tick 全量重查世界。
     */
    private fun getBoundaryEndpoints(world: Level, topology: TopologyCache): List<CachedEndpointLookup> {
        val cached = boundaryEndpointCache
        if (cached != null) {
            val dueVerify = Math.floorMod(world.time + endpointVerifyPhase, ENDPOINT_VERIFY_INTERVAL.toLong()) == 0L
            if (!dueVerify) {
                var stale = false
                for (entry in cached) {
                    val be = entry.blockEntity
                    if (be != null) {
                        if (be.isRemoved) {
                            stale = true
                            break
                        }
                    } else if (world.getBlockEntity(BlockPos.of(entry.neighborPosLong)) != null) {
                        // null 条目（无 BE 邻居，如空气/石头）可能是 chunk 重载或邻居替换刚带来的
                        // 新 BE（新 BE 创建不发 block update，isRemoved 无法兜底）。实时轻查一次：
                        // 只对 null 条目查世界，机器边界条目（非 null）不增加查询，收益保留。
                        stale = true
                        break
                    }
                }
                if (!stale) return cached
            }
        }
        return rebuildBoundaryEndpoints(world, topology).also { boundaryEndpointCache = it }
    }

    private fun rebuildBoundaryEndpoints(world: Level, topology: TopologyCache): List<CachedEndpointLookup> {
        // 端点缓存重建 = storage 引用整体刷新；provider 候选缓存依赖 storage 引用，一并失效。
        providerCandidatesCacheByConsumer.clear()
        return topology.boundaries.map { boundary ->
            val neighborPos = BlockPos.of(boundary.neighborPosLong)
            CachedEndpointLookup(
                cablePosLong = boundary.cablePosLong,
                neighborPosLong = boundary.neighborPosLong,
                blockEntity = world.getBlockEntity(neighborPos),
                storage = EnergyStorage.SIDED.find(world, neighborPos, boundary.lookupFromNeighborSide)
            )
        }
    }

    /** 扫描拓扑边界，返回消费者（supportsInsertion）。 */
    private fun findConsumers(world: Level, topology: TopologyCache): Map<Long, Endpoint> {
        val consumers = mutableMapOf<Long, Endpoint>()
        for (entry in getBoundaryEndpoints(world, topology)) {
            val storage = entry.storage ?: continue
            if (storage.supportsInsertion()) {
                val ep = consumers.getOrPut(entry.neighborPosLong) { Endpoint(storage) }
                ep.entryCables.add(entry.cablePosLong)
                ep.blockPos = entry.neighborPosLong
            }
        }
        return consumers
    }

    /** 扫描拓扑边界，返回 providers（supportsExtraction）。 */
    private fun findProviders(world: Level, topology: TopologyCache): Map<Long, Endpoint> {
        val providers = mutableMapOf<Long, Endpoint>()
        for (entry in getBoundaryEndpoints(world, topology)) {
            val storage = entry.storage ?: continue
            if (storage.supportsExtraction()) {
                val ep = providers.getOrPut(entry.neighborPosLong) { Endpoint(storage) }
                ep.entryCables.add(entry.cablePosLong)
                ep.blockPos = entry.neighborPosLong
            }
        }
        return providers
    }

    private fun pushToConsumers(world: Level) {
        if (cables.isEmpty()) return

        val topology = topologyCache ?: buildTopology(world).also { topologyCache = it }
        val consumers = findConsumers(world, topology)

        // 无消费者 = 空载：本 tick 不会有任何传输，cableLoad 无需每 tick 同步
        // （避免孤立导线/空载电网每 tick 对全部导线 O(N) getBlockEntity）；
        // 但每 20 tick 强制同步一次，让 Jade 的 cableLoad 窗口能衰减归零，不永久残留旧值。
        if (consumers.isEmpty()) {
            if (isCableLoadSyncDue(world)) {
                ensureCapacityTracking(world, topology.cableRates)
                syncCableLoadToLocalStorage(world, topology.cableRates)
            }
            return
        }

        // Only active networks need a per-cable capacity ledger and provider
        // discovery. Empty networks skip both on 19/20 ticks.
        ensureCapacityTracking(world, topology.cableRates)
        val providers = findProviders(world, topology)

        var anyTransfer = false

        // 每 tick 轮转首个 consumer，避免固定的拓扑发现顺序长期造成饥饿。
        val consumerList = consumers.values.toList()
        val offset = consumerRoundRobinOffset % consumerList.size
        val orderedConsumers = consumerList.drop(offset) + consumerList.take(offset)
        consumerRoundRobinOffset = (offset + 1) % consumerList.size

        // Phase 1: 残余池能分发（BFS 合并等场景遗留的 energy，通常一次 tick 即清空）
        for (consumer in orderedConsumers) {
            if (energy <= 0) break
            if (pullFromBufferedEnergyByPath(world, consumer, topology.neighbors)) anyTransfer = true
        }

        // Phase 2: providers → consumers 直送
        if (providers.isNotEmpty()) {
            // 首轮给每个 consumer 一个有限额度，保证后面的 consumer 至少有一次取电机会。
            // 这里的额度按 provider 的提取量计算，已包含路径损耗的消耗。
            val totalProviderEnergy = providers.values.sumOf {
                simulateExtraction(it.storage, Long.MAX_VALUE)
            }
            val firstRoundBudget = maxOf(1L, totalProviderEnergy / orderedConsumers.size)

            for (consumer in orderedConsumers) {
                if (pullFromProvidersByPath(
                        world,
                        consumer,
                        providers,
                        topology.neighbors,
                        maxAmount = firstRoundBudget
                    )
                ) anyTransfer = true
            }

            // 首轮完成后，剩余 provider 能量继续按轮转顺序分发。
            for (consumer in orderedConsumers) {
                if (pullFromProvidersByPath(world, consumer, providers, topology.neighbors)) anyTransfer = true
            }
        }

        // cableLoad 仅用于 Jade 显示：确有传输时同步；无传输时每 20 tick 强制刷一次，
        // 让停止传输后窗口能归零（不永久残留旧值），其余 tick 不遍历导线。
        if (anyTransfer || isCableLoadSyncDue(world)) syncCableLoadToLocalStorage(world, topology.cableRates)
    }

    /** cableLoad 定期同步判定（相位与端点 verify 一致，各网络错开）。 */
    private fun isCableLoadSyncDue(world: Level): Boolean =
        Math.floorMod(world.time + endpointVerifyPhase, ENDPOINT_VERIFY_INTERVAL.toLong()) == 0L

    private fun pullFromBufferedEnergyByPath(
        world: Level,
        consumer: Endpoint,
        neighbors: Map<Long, List<Long>>
    ): Boolean {
        var anyTransferred = false
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
                            val destPos = consumer.blockPos?.let { BlockPos.of(it) } ?: BlockPos.ZERO
                            val destBlock = world.getBlockState(destPos).block
                            log.info(
                                "[能量传输] 电网缓冲 → $destBlock @ $destPos | " +
                                        "传输=$inserted EU, 路径损耗=$pathLossEu EU, 路径长度=${candidate.path.size}"
                            )
                        }

                        tx.commit()
                        progressed = true
                        anyTransferred = true
                    }
                }
            }

            if (!progressed) break
        }
        return anyTransferred
    }

    private fun pullFromProvidersByPath(
        world: Level,
        consumer: Endpoint,
        providers: Map<Long, Endpoint>,
        neighbors: Map<Long, List<Long>>,
        maxAmount: Long = Long.MAX_VALUE
    ): Boolean {
        var anyTransferred = false
        var remainingBudget = maxAmount
        while (remainingBudget > 0) {
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

                val needFromProvider = minOf(
                    pathCapacity,
                    stepDemand + pathLossEu,
                    remainingBudget
                )
                if (needFromProvider <= pathLossEu) continue

                Transaction.openOuter().use { tx ->
                    val extracted = candidate.provider.extract(needFromProvider, tx)
                    if (extracted <= pathLossEu) return@use

                    val deliverable = (extracted - pathLossEu).coerceAtLeast(0L)
                    if (deliverable <= 0) return@use

                    val inserted = consumer.storage.insert(deliverable, tx)
                    if (inserted > 0) {
                        val moved = (inserted + pathLossEu).coerceAtMost(extracted)
                        // 容量账本纳入事务快照：事务回滚时一并恢复，避免同 tick 可用容量被多扣。
                        updateSnapshots(tx)
                        for (cablePosLong in candidate.path) {
                            cableTransferRemaining[cablePosLong] =
                                (cableTransferRemaining[cablePosLong] ?: 0L) - moved
                        }
                        remainingBudget -= extracted

                        if (ENABLE_ENERGY_TRANSFER_LOG) {
                            val sourcePos = candidate.providerPos?.let { BlockPos.of(it) } ?: BlockPos.ZERO
                            val destPos = consumer.blockPos?.let { BlockPos.of(it) } ?: BlockPos.ZERO
                            val sourceBlock = world.getBlockState(sourcePos).block
                            val destBlock = world.getBlockState(destPos).block
                            log.info(
                                "[能量传输] $sourceBlock @ $sourcePos → $destBlock @ $destPos | " +
                                        "提取=$extracted EU, 传输=$inserted EU, 路径损耗=$pathLossEu EU, 路径长度=${candidate.path.size}"
                            )
                        }

                        tx.commit()
                        progressed = true
                        anyTransferred = true
                    }
                }
            }

            if (!progressed) break
            // 有意保持每 tick 单轮扫描：对当前线性能量容器，一轮完整候选遍历已足够。
            break
        }
        return anyTransferred
    }

    private fun syncCableLoadToLocalStorage(
        world: Level,
        cableRates: Map<Long, Long>
    ) {
        if (world.isClient) return
        for (cablePosLong in cables) {
            if (cablePosLong !in cableLossMilliEu) continue
            val transferRate = cableRates[cablePosLong] ?: continue
            val remaining = cableTransferRemaining[cablePosLong] ?: transferRate
            val used = transferRate - remaining
            val pos = BlockPos.of(cablePosLong)
            // 只为 Jade 显示同步，绝不为它同步加载区块（曾致主线程卡 60s 被 Watchdog 砍）
            if (!world.isChunkLoaded(pos)) continue
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
        // 候选路径/损耗只依赖拓扑，跨 tick 缓存，避免每 tick 重建 + 排序。
        val cacheKey = providerCandidatesKey(consumer, providers)
        providerCandidatesCacheByConsumer[cacheKey]?.let { return it }

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
        providerCandidatesCacheByConsumer[cacheKey] = candidates
        trimPathCachesIfNeeded()
        return candidates
    }

    /**
     * 候选缓存 key：consumer 入口导线 + providers（位置 + 入口导线）。
     *
     * 不含 storage 引用本身——端点缓存重建（[rebuildBoundaryEndpoints]）会清空本缓存，
     * 保证缓存的 storage 引用始终与当前端点一致。
     */
    private fun providerCandidatesKey(
        consumer: Endpoint,
        providers: Map<Long, Endpoint>
    ): String {
        val consumerKey = consumer.entryCables.sorted().joinToString(",")
        val providerKey = providers.entries.sortedBy { it.key }.joinToString(";") { (pos, ep) ->
            "$pos:${ep.entryCables.sorted().joinToString(",")}"
        }
        return "$consumerKey|$providerKey"
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
        if (providerCandidatesCacheByConsumer.size > 512) providerCandidatesCacheByConsumer.clear()
    }

    private fun invalidatePathCaches() {
        topologyCache = null
        boundaryEndpointCache = null
        providerCandidatesCacheByConsumer.clear()
        dijkstraCacheByEntries.clear()
        bufferedCandidatesCacheByEntries.clear()
    }

    private fun buildTopology(world: Level): TopologyCache {
        val cableRates = mutableMapOf<Long, Long>()
        val neighbors = mutableMapOf<Long, MutableList<Long>>()
        val boundaries = mutableListOf<BoundaryEdge>()
        var maxLevel = 1

        if (ENABLE_TOPOLOGY_LOG) {
            log.debug("[电网拓扑] 开始构建电网拓扑，导线数量=${cables.size}")
        }

        for (cablePosLong in cables) {
            val cablePos = BlockPos.of(cablePosLong)
            val state = world.getBlockState(cablePos)
            val block = state.block as? BaseCableBlock ?: continue
            val be = world.getBlockEntity(cablePos) as? CableBlockEntity
            cableRates[cablePosLong] = be?.effectiveTransferRate ?: block.getTransferRate()
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
                world.getBlockState(BlockPos.of(cablePosLong)).block as? BaseCableBlock ?: return@filter true
            block.insulationLevel < outputLevel
        }

        val cablesThatCanBurn = cables.filter { cablePosLong ->
            val block = world.getBlockState(BlockPos.of(cablePosLong)).block
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
     * 模拟插入能量，不改变实际存储。
     * 有父事务时用 [Transaction.openNested] 避免嵌套 [Transaction.openOuter] 异常。
     */
    private fun simulateInsertion(storage: EnergyStorage, maxAmount: Long, parentTx: TransactionContext? = null): Long {
        var accepted = 0L
        if (parentTx != null) {
            Transaction.openNested(parentTx).use { tx ->
                accepted = storage.insert(maxAmount, tx)
            }
        } else {
            Transaction.openOuter().use { tx ->
                accepted = storage.insert(maxAmount, tx)
            }
        }
        return accepted
    }

    /** 模拟抽取能量，不改变实际存储。 */
    private fun simulateExtraction(storage: EnergyStorage, maxAmount: Long, parentTx: TransactionContext? = null): Long {
        var accepted = 0L
        if (parentTx != null) {
            Transaction.openNested(parentTx).use { tx ->
                accepted = storage.extract(maxAmount, tx)
            }
        } else {
            Transaction.openOuter().use { tx ->
                accepted = storage.extract(maxAmount, tx)
            }
        }
        return accepted
    }

    /**
     * 边界端点查找缓存条目：SIDED.find 结果 + 邻居 BE 引用（isRemoved 兜底）。
     *
     * 缓存失效时机：
     * 1. 拓扑失效（[invalidatePathCaches]，连接/导线变化）→ 整体清空；
     * 2. 条目 BE isRemoved（机器方块被替换/区块卸载）→ 实时兜底，触发整体重查；
     * 3. 每 [ENDPOINT_VERIFY_INTERVAL] tick 强制全量重查（相位随机分散）→ 兜底
     *    "无 BE 邻居被替换"与"能力变化但连接属性不变"（Energy API 协议要求能力变化发
     *    block update，但导线侧仅连接属性变化才失效）的场景。
     */
    private class CachedEndpointLookup(
        val cablePosLong: Long,
        val neighborPosLong: Long,
        var blockEntity: BlockEntity?,
        var storage: EnergyStorage?
    )

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

    data class NetworkSnapshot(
        val energy: Long,
        val cableTransferRemaining: MutableMap<Long, Long>
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
