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
 * 电网：一组相互连接的导线共享的能量池。
 *
 * 发电机通过任意成员导线的 [EnergyStorage] 向池中注入能量；
 * 每 game tick 电网自动按“消费者拉取”模型为边界处用电者供电。
 *
 * 新传输模型：
 * - 对每个消费者，统计到所有供电者（supportsExtraction）的路径；
 * - 按路径损耗（milliEU 总和）从小到大尝试拉取；
 * - 单条路径每 tick 最大可输送量 = 路径上剩余容量最小导线的剩余容量；
 * - 导线一旦被路径使用，会扣减该导线本 tick 临时容量，避免多路径叠加超载。
 */
class EnergyNetwork : SnapshotParticipant<Long>() {

    companion object {
        /** 漏电/低耐压烧毁的检查周期（tick）。x 秒 */
        const val damageIntervalTicks = 100
        val log = LoggerFactory.getLogger("EnergyNetwork")

        // ========== 日志开关配置 ==========

        /** 超压爆炸日志开关：记录电网过压检测、机器爆炸等 */
        var ENABLE_OVERVOLTAGE_LOG = false

        /** 能量传输日志开关：记录能量流动、输入输出等 */
        var ENABLE_ENERGY_TRANSFER_LOG = false

        /** 拓扑构建日志开关：记录电网拓扑构建、输出等级计算等 */
        var ENABLE_TOPOLOGY_LOG = false

        /** 漏电检测日志开关：记录导线漏电、触电伤害等 */
        var ENABLE_LEAK_LOG = false

        /** 导线烧毁日志开关：记录低耐压导线烧毁等 */
        var ENABLE_CABLE_BURN_LOG = false
    }

    /** 低耐压导线烧毁比例（0.0–1.0）。每次检查时随机选择该比例的“耐压低于电网等级”的导线烧毁。 */
    private val underTierCableBurnRatio = 1

    /** 所有成员导线位置（packed [BlockPos.asLong]）。 */
    val cables = mutableSetOf<Long>()
    var energy: Long = 0
    var capacity: Long = 0

    /** 电网输出等级（1–5），由电网内输出等级最高的机器决定，决定所有导线的电压等级。 */
    var outputLevel: Int = 1

    /** 触电伤害触发 tick 偏移（0–199），用于错开不同电网的伤害时机。 */
    var damageTickOffset: Int = 0

    /** 每根导线的损耗 (milliEU)。 */
    private val cableLossMilliEu = mutableMapOf<Long, Long>()

    /** 拓扑缓存：导线邻接、边界连接、每根导线速率。 */
    private var topologyCache: TopologyCache? = null

    /** 按消费者入口导线集合缓存最短路结果（仅拓扑不变时可复用）。 */
    private val dijkstraCacheByEntries = mutableMapOf<String, DijkstraResult>()

    /** 按消费者入口导线集合缓存到全体导线的候选路径（按损耗升序）。 */
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

    /** 导线邻接边界发生变化（如相邻机器放置/移除）时，刷新拓扑与路径缓存。 */
    fun invalidateConnectionCaches() {
        invalidatePathCaches()
    }

    fun insert(maxAmount: Long, transaction: TransactionContext): Long {
        val space = (capacity - energy).coerceAtLeast(0)
        val toInsert = minOf(maxAmount, space)
        if (toInsert > 0) {
            updateSnapshots(transaction)
            energy += toInsert
        }
        return toInsert
    }

    fun extract(maxAmount: Long, transaction: TransactionContext): Long {
        val toExtract = minOf(maxAmount, energy).coerceAtLeast(0)
        if (toExtract > 0) {
            updateSnapshots(transaction)
            energy -= toExtract
        }
        return toExtract
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

    /** 漏电导线触电伤害：绝缘等级 < 电网输出等级的导线会漏电。每 10 秒触发，范围与伤害量由电网输出等级决定。 */
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
        val damageAmount = outputLevel * 2f // n 心 = n * 2 伤害
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
                // log.debug("触电伤害：${entity.name}，电压等级：$outputLevel")
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
        val key = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier("ic2_120", "cable_shock"))
        val entry = registry.getEntry(key).orElse(null)
            ?: registry.getEntry(RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier("minecraft", "lightning")))
                .orElseThrow()
        return DamageSource(entry)
    }

    /**
     * 低耐压导线烧毁：电网输出等级高于导线电压等级时，按 [underTierCableBurnRatio] 比例随机烧毁部分导线。
     * 仅烧毁电压等级 &lt; outputLevel 的导线（如锡线接入含 MFSU 的玻璃纤维电网会被烧，玻璃纤维不烧）。
     * 烧毁处生成烟雾/火焰粒子，不掉落物品。
     */
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

    /**
     * 机器耐压检测：与电网相连的机器若有效耐压等级 &lt; 电网 outputLevel，直接爆炸。
     * 有效耐压 = 机器 [ITieredMachine.tier] + 高压升级带来的 [ITransformerUpgradeSupport.voltageTierBonus]。
     * 不按比例随机，只要电压等级不对就炸。
     * [IGenerator] 发电机不参与过压爆炸。
     * 爆炸威力按电网电压等级：等级 4 对应 10 颗心伤害，每提高 1 级伤害翻倍。
     */
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

            // 使用分面电压等级，检查从电网方向看向机器时的耐压等级
            val effectiveTier = be.effectiveVoltageTierForSide(boundary.lookupFromNeighborSide)

            // 详细日志：记录过压检测
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

            // 触发爆炸
            if (ENABLE_OVERVOLTAGE_LOG) {
                log.debug("[超压检测]   → 🔥 触发爆炸！电网等级 $outputLevel > 机器耐压 $effectiveTier")
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

    /** 电网电压等级对应的爆炸威力：等级 4 = 10 颗心伤害（power=2），每提高 1 级伤害翻倍。 */
    private fun explosionPowerForOutputLevel(level: Int): Float {
        if (level <= 0) return 0.25f
        return (2f * 2.0.pow(level - 4)).toFloat()
    }

    private fun pushToConsumers(world: World) {
        if (cables.isEmpty()) return

        val topology = topologyCache ?: buildTopology(world).also { topologyCache = it }

        // 初始化所有导线的剩余容量为本 tick 最大载流量
        val remainingCableCapacity = topology.cableRates.toMutableMap()

        val consumers = mutableMapOf<Long, Endpoint>()
        val providers = mutableMapOf<Long, Endpoint>()

        for (boundary in topology.boundaries) {
            val neighborPos = BlockPos.fromLong(boundary.neighborPosLong)
            val storage = EnergyStorage.SIDED.find(world, neighborPos, boundary.lookupFromNeighborSide) ?: continue
            if (storage.supportsInsertion()) {
                val endpoint = consumers.getOrPut(boundary.neighborPosLong) { Endpoint(storage) }
                endpoint.entryCables.add(boundary.cablePosLong)
                endpoint.blockPos = boundary.neighborPosLong
            }
            if (storage.supportsExtraction()) {
                val endpoint = providers.getOrPut(boundary.neighborPosLong) { Endpoint(storage) }
                endpoint.entryCables.add(boundary.cablePosLong)
                endpoint.blockPos = boundary.neighborPosLong
            }
        }

        // 先尝试让消费者从电网缓冲池取能（按路径损耗与路径容量计算）。
        for ((_, consumer) in consumers) {
            if (energy <= 0) break
            pullFromBufferedEnergyByPath(world, consumer, topology.neighbors, remainingCableCapacity)
        }

        if (providers.isNotEmpty()) {
            // 再按路径损耗从小到大，从所有供电者拉取。
            for ((_, consumer) in consumers) {
                pullFromProvidersByPath(world, consumer, providers, topology.neighbors, remainingCableCapacity)
            }
        }

        // 同步导线负载到 cableLoad（供 Jade 显示）
        // 无论是否有能量传输都会调用，确保负载显示正确（包括 0）
        syncCableLoadToLocalStorage(world, topology.cableRates, remainingCableCapacity)
    }

    private fun pullFromBufferedEnergyByPath(
        world: World,
        consumer: Endpoint,
        neighbors: Map<Long, List<Long>>,
        remainingCableCapacity: MutableMap<Long, Long>
    ) {
        while (energy > 0) {
            val demand = simulateInsertion(consumer.storage, Long.MAX_VALUE)
            if (demand <= 0) break

            val candidates = buildBufferedCandidates(consumer, neighbors)
            if (candidates.isEmpty()) break

            var progressed = false
            for (candidate in candidates) {
                if (energy <= 0) break
                val pathCapacity = candidate.path.minOfOrNull { remainingCableCapacity[it] ?: 0L } ?: 0L
                if (pathCapacity <= 0) continue

                // 线损向上取整：(milliEU + 999) / 1000，确保"可以多扣不能少扣"
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
                            remainingCableCapacity[cablePosLong] =
                                (remainingCableCapacity[cablePosLong] ?: 0L) - moved
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
        neighbors: Map<Long, List<Long>>,
        remainingCableCapacity: MutableMap<Long, Long>
    ) {
        while (true) {
            val demand = simulateInsertion(consumer.storage, Long.MAX_VALUE)
            if (demand <= 0) break

            // log.info("demand: $demand")

            val candidates = buildProviderCandidates(consumer, providers, neighbors)
            if (candidates.isEmpty()) break

            var progressed = false
            for (candidate in candidates) {
                val pathCapacity = candidate.path.minOfOrNull { remainingCableCapacity[it] ?: 0L } ?: 0L
                if (pathCapacity <= 0) continue

                // 线损向上取整：(milliEU + 999) / 1000，确保"可以多扣不能少扣"
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
                            remainingCableCapacity[cablePosLong] =
                                (remainingCableCapacity[cablePosLong] ?: 0L) - moved
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
            //todo 这里好像和ticklimited冲突了
            break
        }
    }

    /**
     * 同步导线负载到 cableLoad（供 Jade 显示）。
     *
     * 负载 = 已使用量 = transferRate - remainingCableCapacity
     *
     * 注意：这仅用于显示目的，不影响电网的实际能量传输逻辑。
     * localEnergy 仍然会在电网重建时被并入网络（保持兼容性）。
     */
    private fun syncCableLoadToLocalStorage(
        world: World,
        cableRates: Map<Long, Long>,
        remainingCableCapacity: Map<Long, Long>
    ) {
        if (world.isClient) return
        // println("====================")

        for (cablePosLong in cables) {
            // 只处理已知导线（避免无效导线）
            if (cablePosLong !in cableLossMilliEu) continue

            val transferRate = cableRates[cablePosLong] ?: continue
            val remaining = remainingCableCapacity[cablePosLong] ?: transferRate
            val used = transferRate - remaining

            // 获取导线方块实体并更新 cableLoad
            val pos = BlockPos.fromLong(cablePosLong)
            val be = world.getBlockEntity(pos)
            if (be is ic2_120.content.block.cables.CableBlockEntity) {
                be.cableLoad = used
            }
        }
        // println("====================")
    }

    private fun buildProviderCandidates(
        consumer: Endpoint,
        providers: Map<Long, Endpoint>,
        neighbors: Map<Long, List<Long>>
    ): List<ProviderPath> {
        val candidates = mutableListOf<ProviderPath>()

        // 按每根消费者入口导线分别做 Dijkstra，
        // 确保每个供电者都有经过不同入口的候选路径。
        // 当某条路径容量耗尽时，可以 fallback 到通过其他入口的路径。
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
        // 按每根消费者入口导线分别做 Dijkstra，生成多路候选
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
        // 防止极端场景缓存无限增长（例如频繁移动机器导致入口集合持续变化）
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
                    //电网输出等级计算：只考虑向电网输出能量的面（输出面） 例子：mfsu被接在低压电网充电不应该拉高电压等级，mfsu的输出面并没有接入电网
                    //supportsExtraction() = true 表示机器可以从该方向输出能量
                    //使用分面电压等级，支持变压器等不同面电压不同的设备
                    if (neighborBe is ITieredMachine && storage?.supportsExtraction() == true) {
                        //对于被查询电压等级的机器，反向才是面向导线的方向
                        val tierForSide = neighborBe.effectiveVoltageTierForSide(dir.opposite)
                        val oldMax = maxLevel
                        maxLevel = maxOf(maxLevel, tierForSide)

                        // 详细日志：记录电压等级检测
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
                        // 详细日志：记录跳过的输入输出面
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
        // println("tx open")
        Transaction.openOuter().use { tx ->
            accepted = storage.insert(maxAmount, tx)
        }
        // println("tx close")

        return accepted
    }

    private data class Endpoint(
        val storage: EnergyStorage,
        val entryCables: MutableSet<Long> = mutableSetOf(),
        var blockPos: Long? = null  // 记录方块位置用于日志
    )

    private data class ProviderPath(
        val provider: EnergyStorage,
        val path: List<Long>,
        val pathLossMilliEu: Long,
        val providerPos: Long? = null  // 记录供电者位置用于日志
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

    /** 获取每根导线应分摊的能量（用于 NBT 持久化）。 */
    fun getEnergySharePerCable(): Long {
        val count = cables.size
        return if (count > 0) energy / count else 0
    }
}

