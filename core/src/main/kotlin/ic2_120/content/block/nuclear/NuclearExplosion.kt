package ic2_120.content.block.nuclear

import ic2_120.config.Ic2Config
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.sounds.SoundEvents
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.ClipContext
import org.slf4j.LoggerFactory
import kotlin.math.*
import java.util.UUID

/**
 * 自定义核爆炸模拟，严格对齐 IC2 原版 Ic2Explosion。
 *
 * 两阶段多 tick 处理：
 *  1. CALCULATING — 每次 tick 处理一批射线，收集受影响方块
 *  2. DESTROYING  — 每次 tick 摧毁一批方块
 */
class NuclearExplosion private constructor(
    val world: ServerLevel,
    val centerX: Double,
    val centerY: Double,
    val centerZ: Double,
    val power: Float,
    private val damageSource: DamageSource,
    private val ownerUuid: UUID?
) {
    /** 是否已完成全部工作 */
    val finished: Boolean get() = phase == Phase.DONE

    /** 总射线数（初始计算后有效） */
    val totalRays: Int get() = 2 * segments * segments

    /** 最终摧毁的方块总数（摧毁完成后有效） */
    val destroyedBlockCount: Int get() = blocksToDestroy.size

    /** 完成回调（摧毁全部结束后在主线程执行，参数为当前实例） */
    var onComplete: ((NuclearExplosion) -> Unit)? = null

    private enum class Phase { CALCULATING, DESTROYING, DONE }

    private var phase = Phase.CALCULATING

    // ---- 计算阶段（CALCULATING）状态 ----
    private var segments: Int = 0
    private var currentH = 0
    private var currentV = 0
    private val affectedSet = mutableSetOf<BlockPos>()
    private val mutablePos = BlockPos.MutableBlockPos()

    // ---- 摧毁阶段（DESTROYING）状态 ----
    private val blocksToDestroy = mutableListOf<BlockPos>()
    private var destroyIndex = 0
    private val chunkProtectionCache = HashMap<Long, Boolean>()

    init {
        val maxDistance = (power / 0.4f).toDouble()
        // IC2 原版公式：segments = ceil(PI / atan(1.0 / maxDistance))
        segments = ceil(PI / atan(1.0 / maxOf(maxDistance, 1.0))).toInt()
        LOG.info("[核爆炸] 初始化 power={} maxDistance={} segments={} totalRays={}",
            power, String.format("%.1f", maxDistance), segments, totalRays)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger("ic2_120/NuclearExplosion")

        /** 每 tick 最多处理的射线数（计算阶段） */
        private const val RAYS_PER_TICK = 40_000

        /** 子射线每 tick 上限 */
        private const val SUB_RAYS_PER_TICK = 5_000

        fun create(
            world: ServerLevel,
            x: Double, y: Double, z: Double,
            power: Float,
            damageSource: DamageSource,
            ownerUuid: UUID? = null
        ): NuclearExplosion {
            return NuclearExplosion(world, x, y, z, power, damageSource, ownerUuid)
        }
    }

    /** FTB Chunks 保护检查（按 chunk 缓存，受保护的方块截断射线） */
    private fun isProtected(pos: BlockPos): Boolean {
        val chunkKey = net.minecraft.world.level.ChunkPos.pack(pos)
        return chunkProtectionCache.getOrPut(chunkKey) {
            ic2_120.integration.ftbchunks.ClaimProtection.isExplosionProtected(world, pos, ownerUuid)
        }
    }

    // ============================================================
    //  主入口：每 tick 调用一次
    // ============================================================

    fun tick(): Boolean {
        return when (phase) {
            Phase.CALCULATING -> {
                if (tickCalculating()) true
                else false
            }
            Phase.DESTROYING -> {
                if (tickDestroying()) true
                else false
            }
            Phase.DONE -> true
        }
    }

    /**
     * 预估剩余 tick 数。
     * 计算阶段：粗略按剩余射线估算；摧毁阶段：按方块数估算。
     */
    fun estimatedRemainingTicks(): Int {
        if (phase == Phase.CALCULATING) {
            val completedRays = currentH * segments + currentV
            val remainingRays = totalRays - completedRays
            return (remainingRays + RAYS_PER_TICK - 1) / RAYS_PER_TICK
        }
        val remaining = blocksToDestroy.size - destroyIndex
        val blocksPerTick = Ic2Config.current.nuclear.explosionBlocksPerTick.coerceAtLeast(1)
        return (remaining + blocksPerTick - 1) / blocksPerTick
    }

    // ============================================================
    //  阶段 1：计算（分 tick 射线投射）
    // ============================================================

    private var subRayQueue = mutableListOf<SubRay>()

    private data class SubRay(
        val x: Double, val y: Double, val z: Double,
        val dx: Double, val dy: Double, val dz: Double,
        val power: Double
    )

    private fun tickCalculating(): Boolean {
        var raysThisTick = 0
        val subRayBudget = SUB_RAYS_PER_TICK

        // 处理主射线
        while (raysThisTick < RAYS_PER_TICK && currentH < 2 * segments) {
            val azimuth = 2.0 * PI / segments * currentH
            val polar = PI / segments * currentV
            val dx = sin(polar) * cos(azimuth)
            val dy = cos(polar)
            val dz = sin(polar) * sin(azimuth)

            // 主射线及其子射线一起处理
            processRay(centerX, centerY, centerZ, dx, dy, dz, power.toDouble(), subRayBudget)

            raysThisTick++
            currentV++
            if (currentV >= segments) {
                currentV = 0
                currentH++
            }
        }

        // 处理积压的子射线
        if (currentH >= 2 * segments && subRayQueue.isNotEmpty()) {
            var subRaysThisTick = 0
            val iter = subRayQueue.iterator()
            while (iter.hasNext() && subRaysThisTick < subRayBudget) {
                val sr = iter.next()
                iter.remove()
                processRayNoSub(sr.x, sr.y, sr.z, sr.dx, sr.dy, sr.dz, sr.power)
                subRaysThisTick++
            }
            LOG.info("[核爆炸] 本 tick 处理 {} 条积压子射线，剩余 {} 条", subRaysThisTick, subRayQueue.size)
        }

        // 全部主射线完成且无积压子射线 → 切换到摧毁阶段
        if (currentH >= 2 * segments && subRayQueue.isEmpty()) {
            finishCalculating()
        }

        return false
    }

    /**
     * 处理一条射线，包含递归子射线。
     * 子射线超过预算时入队留到后续 tick 处理。
     */
    private fun processRay(
        x: Double, y: Double, z: Double,
        dx: Double, dy: Double, dz: Double,
        initialPower: Double,
        subBudget: Int
    ) {
        var rx = x; var ry = y; var rz = z
        var remaining = initialPower

        while (remaining > 0) {
            rx += dx; ry += dy; rz += dz

            if (ry < world.minY.toDouble() || ry > world.maxY.toDouble()) break

            mutablePos.set(rx.toInt(), ry.toInt(), rz.toInt())
            val state = world.getBlockState(mutablePos)
            val block = state.block
            val isAir = state.isAir() || block === Blocks.VOID_AIR || block === Blocks.CAVE_AIR

            val absorption = if (isAir) 0.5 else {
                val res = block.explosionResistance.toDouble().coerceAtLeast(0.0)
                0.5 + maxOf(0.0, (res + 4.0) * 0.3)
            }

            // FTB Chunks 保护：受保护的方块充当屏障，截断射线
            if (!isAir && isProtected(mutablePos)) break

            if (absorption > remaining) break
            remaining -= absorption

            // 非空气加入摧毁
            if (!isAir) {
                affectedSet.add(mutablePos.immutable())

                // 高抗性 → 产子射线
                if (absorption > 10.0 && subBudget > 0) {
                    var spawned = 0
                    for (i in 0 until 5) {
                        val sx = world.random.nextDouble() * 4.0 - 2.0
                        val sy = world.random.nextDouble() * 4.0 - 2.0
                        val sz = world.random.nextDouble() * 4.0 - 2.0
                        val len = sqrt(sx * sx + sy * sy + sz * sz)
                        if (len < 0.01) continue
                        if (spawned < subBudget) {
                            processRay(rx, ry, rz, sx / len, sy / len, sz / len, absorption * 0.4, subBudget - spawned - 1)
                            spawned++
                        } else {
                            subRayQueue.add(SubRay(rx, ry, rz, sx / len, sy / len, sz / len, absorption * 0.4))
                        }
                    }
                    break
                }
            }
        }
    }

    /**
     * 无子射线的简单射线处理（用于积压子射线）。
     */
    private fun processRayNoSub(
        x: Double, y: Double, z: Double,
        dx: Double, dy: Double, dz: Double,
        initialPower: Double
    ) {
        var rx = x; var ry = y; var rz = z
        var remaining = initialPower
        while (remaining > 0) {
            rx += dx; ry += dy; rz += dz
            if (ry < world.minY.toDouble() || ry > world.maxY.toDouble()) break
            mutablePos.set(rx.toInt(), ry.toInt(), rz.toInt())
            val state = world.getBlockState(mutablePos)
            val block = state.block
            val isAir = state.isAir() || block === Blocks.VOID_AIR || block === Blocks.CAVE_AIR
            val absorption = if (isAir) 0.5 else {
                val res = block.explosionResistance.toDouble().coerceAtLeast(0.0)
                0.5 + maxOf(0.0, (res + 4.0) * 0.3)
            }
            // FTB Chunks 保护：受保护的方块充当屏障，截断射线
            if (!isAir && isProtected(mutablePos)) break
            if (absorption > remaining) break
            remaining -= absorption
            if (!isAir) affectedSet.add(mutablePos.immutable())
        }
    }

    private fun finishCalculating() {
        blocksToDestroy.addAll(affectedSet)
        blocksToDestroy.sortBy { it.distToCenterSqr(centerX, centerY, centerZ) }
        affectedSet.clear()

        LOG.info("[核爆炸] 计算完成 segments={} rays={} blocks={}",
            segments, totalRays, blocksToDestroy.size)

        // 播放爆炸音效 & 粒子
        val soundPos = Vec3(centerX, centerY, centerZ)
        world.playSound(null, soundPos.x, soundPos.y, soundPos.z,
            SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS,
            4.0f, (1.0f + (world.random.nextFloat() - 0.5f) * 0.4f) * 0.7f)
        world.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
            centerX, centerY, centerZ, 1, 0.0, 0.0, 0.0, 0.0)

        // 立即施加实体伤害
        applyEntityDamage()
        phase = Phase.DESTROYING
    }

    // ============================================================
    //  阶段 2：摧毁（分 tick 方块移除）
    // ============================================================

    private fun tickDestroying(): Boolean {
        val blocksPerTick = Ic2Config.current.nuclear.explosionBlocksPerTick.coerceAtLeast(1)
        val batchEnd = minOf(destroyIndex + blocksPerTick, blocksToDestroy.size)

        while (destroyIndex < batchEnd) {
            val pos = blocksToDestroy[destroyIndex]
            val state = world.getBlockState(pos)
            if (!state.isAir()) {
                // TODO(1.21.1): onDestroyedByExplosion 已移除；爆炸破坏不掉落，直接置空气
                world.destroyBlock(pos, false)
            }
            destroyIndex++
        }

        if (destroyIndex >= blocksToDestroy.size) {
            phase = Phase.DONE
            LOG.info("[核爆炸] 全部摧毁完毕 总方块={}", blocksToDestroy.size)
            onComplete?.invoke(this)
            return true
        }
        return false
    }

    // ============================================================
    //  实体伤害
    // ============================================================

    private fun applyEntityDamage() {
        val q = (power * 2.0).coerceAtLeast(1.0)
        val box = AABB(
            centerX - q, centerY - q, centerZ - q,
            centerX + q, centerY + q, centerZ + q
        )

        for (entity in world.getEntitiesOfClass(LivingEntity::class.java, box) { true }) {
            val dist = entity.position().distanceTo(Vec3(centerX, centerY, centerZ))
            val w = dist / q
            if (w > 1.0) continue

            val exposure = computeExposure(entity)

            val ac = (1.0 - w) * exposure
            val damage = (((ac * ac + ac) / 2.0) * 7.0 * q + 1.0).toFloat()

            if (damage > 0f) entity.hurt(damageSource, damage)

            val dir = entity.position().subtract(Vec3(centerX, centerY, centerZ)).normalize()
            entity.push(dir.x * exposure, dir.y * exposure, dir.z * exposure)
        }
    }

    private fun computeExposure(entity: Entity): Double {
        val box = entity.boundingBox
        val steps = 4
        var visible = 0
        val total = (steps + 1) * (steps + 1) * (steps + 1)

        for (xi in 0..steps) for (yi in 0..steps) for (zi in 0..steps) {
            val px = box.minX + (box.maxX - box.minX) * xi / steps
            val py = box.minY + (box.maxY - box.minY) * yi / steps
            val pz = box.minZ + (box.maxZ - box.minZ) * zi / steps

            val result = world.clip(ClipContext(
                Vec3(centerX, centerY, centerZ),
                Vec3(px, py, pz),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                entity
            ))
            if (result.type == net.minecraft.world.phys.HitResult.Type.MISS) visible++
        }

        return visible.toDouble() / total
    }
}
