package ic2_120.content.block.nuclear

import ic2_120.config.Ic2Config
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import net.minecraft.world.World
import org.slf4j.LoggerFactory
import kotlin.math.*

/**
 * 自定义核爆炸模拟，对齐 IC2 原版 Ic2Explosion。
 *
 * 使用射线投射确定受影响方块，分批多 tick 移除以优化性能。
 */
class NuclearExplosion private constructor(
    val world: ServerWorld,
    val centerX: Double,
    val centerY: Double,
    val centerZ: Double,
    val power: Float,
    private val damageSource: DamageSource
) {
    /** 待摧毁方块列表（已去重） */
    private val blocksToDestroy = mutableListOf<BlockPos>()

    /** 当前已处理到的索引 */
    private var currentIndex = 0

    /** 是否已完成初始计算（用于单次初始化） */
    private var calculated = false

    /** 是否已施加过实体伤害 */
    private var entityDamageApplied = false

    /** 是否已完成全部摧毁 */
    var finished: Boolean = false
        private set

    companion object {
        private val LOG = LoggerFactory.getLogger("ic2_120/NuclearExplosion")

        /** 射线段数下限/上限 */
        private const val MIN_SEGMENTS = 8
        private const val MAX_SEGMENTS = 32

        /**
         * 创建并计算核爆炸。
         * @return 包含计算结果的 NuclearExplosion 实例（尚未开始分 tick 摧毁）
         */
        fun create(
            world: ServerWorld,
            x: Double, y: Double, z: Double,
            power: Float,
            damageSource: DamageSource
        ): NuclearExplosion {
            val explosion = NuclearExplosion(world, x, y, z, power, damageSource)
            explosion.calculate()
            return explosion
        }
    }

    // ========== 1. 计算阶段（同步） ==========

    private fun calculate() {
        if (calculated) return
        calculated = true

        val startTime = System.nanoTime()
        val affectedSet = mutableSetOf<BlockPos>()
        val mutablePos = BlockPos.Mutable()

        val maxDistance = (power / 0.4f).toDouble()
        // IC2 原版公式：segments = ceil(PI / atan(1.0 / maxDistance))
        val rawSegments = ceil(PI / atan(1.0 / maxOf(maxDistance, 1.0))).toInt()
        val segments = rawSegments.coerceIn(MIN_SEGMENTS, MAX_SEGMENTS)

        // IC2 原版：2 * segments 个方位角 × segments 个极角，覆盖整个球面
        for (h in 0 until 2 * segments) {
            for (v in 0 until segments) {
                val azimuth = 2.0 * PI / segments * h
                val polar = PI / segments * v
                val dx = sin(polar) * cos(azimuth)
                val dy = cos(polar)
                val dz = sin(polar) * sin(azimuth)
                traceRay(mutablePos, affectedSet, centerX, centerY, centerZ, dx, dy, dz, power.toDouble())
            }
        }

        blocksToDestroy.addAll(affectedSet)
        // 按距离排序，先摧毁近处再摧毁远处
        blocksToDestroy.sortBy { it.getSquaredDistance(centerX, centerY, centerZ) }

        val elapsed = (System.nanoTime() - startTime) / 1_000_000
        LOG.info(
            "[核爆炸计算] segments={} rays={} blocks={} power={} time={}ms",
            segments, 2 * segments * segments, blocksToDestroy.size, power, elapsed
        )
    }

    /**
     * 沿一条射线方向步进，收集被摧毁的方块。
     *
     * 使用 IC2 原版吸收公式：absorption = 0.5 + max(0, (blastResistance + 4) * 0.3)
     * 空气不消耗能量；高抗性方块产生子射线（模拟爆炸绕射）。
     */
    private fun traceRay(
        mutablePos: BlockPos.Mutable,
        affected: MutableSet<BlockPos>,
        startX: Double, startY: Double, startZ: Double,
        dx: Double, dy: Double, dz: Double,
        initialPower: Double
    ) {
        var x = startX
        var y = startY
        var z = startZ
        var remaining = initialPower

        while (remaining > 0) {
            x += dx
            y += dy
            z += dz

            // 超出世界边界
            if (y < world.bottomY.toDouble() || y > world.topY.toDouble()) break

            mutablePos.set(x.toInt(), y.toInt(), z.toInt())
            val state = world.getBlockState(mutablePos)
            val block = state.block

            // 空气/虚空/无碰撞方块不吸收能量
            if (state.isAir() || block === Blocks.VOID_AIR || block === Blocks.CAVE_AIR) continue
            if (state.isReplaceable() && !state.isFullCube(world, mutablePos)) continue

            // IC2 原版吸收公式
            val blastResistance = block.getBlastResistance().toDouble().coerceAtLeast(0.0)
            val absorption = 0.5 + maxOf(0.0, (blastResistance + 4.0) * 0.3)

            if (absorption > remaining) break

            remaining -= absorption
            affected.add(mutablePos.toImmutable())

            // 高抗性方块产生子射线（IC2 原版特性：模拟爆炸绕射）
            if (absorption > 10.0) {
                for (i in 0 until 5) {
                    // 随机偏移方向
                    val sx = world.random.nextDouble() * 4.0 - 2.0
                    val sy = world.random.nextDouble() * 4.0 - 2.0
                    val sz = world.random.nextDouble() * 4.0 - 2.0
                    val len = sqrt(sx * sx + sy * sy + sz * sz)
                    if (len < 0.01) continue
                    // 子射线初始能量 = 父射线经过此方块的吸收量 * 0.4
                    traceRay(mutablePos, affected, x, y, z, sx / len, sy / len, sz / len, absorption * 0.4)
                }
                // 主射线停止（能量被绕射消耗）
                break
            }
        }
    }

    // ========== 2. 分 tick 摧毁阶段 ==========

    /**
     * 每服务器 tick 调用一次，摧毁一批方块并施加实体伤害。
     * @return true 表示爆炸处理完毕
     */
    fun tick(): Boolean {
        if (finished) return true
        if (!calculated) {
            finished = true
            return true
        }

        // 首次 tick：施加实体伤害
        if (!entityDamageApplied) {
            applyEntityDamage()
            entityDamageApplied = true
        }

        // 分批摧毁方块（从配置读取每 tick 数量）
        val blocksPerTick = Ic2Config.current.nuclear.explosionBlocksPerTick.coerceAtLeast(1)
        val batchEnd = minOf(currentIndex + blocksPerTick, blocksToDestroy.size)
        var destroyed = 0
        while (currentIndex < batchEnd) {
            val pos = blocksToDestroy[currentIndex]
            val state = world.getBlockState(pos)
            if (!state.isAir()) {
                world.setBlockState(pos, Blocks.AIR.defaultState, Block.NOTIFY_ALL or Block.FORCE_STATE)
                state.block.onDestroyedByExplosion(world, pos, null)
                destroyed++
            }
            currentIndex++
        }

        if (currentIndex >= blocksToDestroy.size) {
            finished = true
            LOG.info("[核爆炸分tick] 全部摧毁完毕 总方块={}", blocksToDestroy.size)
        }

        return finished
    }

    /** 预估剩余的 tick 数 */
    fun estimatedRemainingTicks(): Int {
        val remaining = blocksToDestroy.size - currentIndex
        val blocksPerTick = Ic2Config.current.nuclear.explosionBlocksPerTick.coerceAtLeast(1)
        return (remaining + blocksPerTick - 1) / blocksPerTick
    }

    // ========== 3. 实体伤害（一次施放） ==========

    private fun applyEntityDamage() {
        val q = (power * 2.0).coerceAtLeast(1.0)
        val box = Box(
            centerX - q, centerY - q, centerZ - q,
            centerX + q, centerY + q, centerZ + q
        )

        for (entity in world.getEntitiesByClass(LivingEntity::class.java, box) { true }) {
            val dist = entity.pos.distanceTo(Vec3d(centerX, centerY, centerZ))
            val w = dist / q
            if (w > 1.0) continue

            // 简化曝光计算：中心到实体的几个采样点进行射线检测
            val exposure = computeExposure(entity)

            // 原版爆炸伤害公式
            val ac = (1.0 - w) * exposure
            val damage = (((ac * ac + ac) / 2.0) * 7.0 * q + 1.0).toFloat()

            if (damage > 0f) {
                entity.damage(damageSource, damage)
            }

            // 击退
            val direction = Vec3d(centerX, centerY, centerZ).relativize(entity.pos).normalize()
            val knockbackScale = exposure.coerceAtLeast(0.0)
            entity.addVelocity(
                direction.x * knockbackScale,
                direction.y * knockbackScale,
                direction.z * knockbackScale
            )
            entity.velocityModified = true
        }
    }

    /**
     * 计算实体对爆炸的暴露程度（0-1）。
     * 在实体包围盒上采样 4×4×4 点，检测与爆炸中心之间是否有阻挡。
     */
    private fun computeExposure(entity: Entity): Double {
        val box = entity.boundingBox
        val steps = 4
        var visible = 0
        val total = (steps + 1) * (steps + 1) * (steps + 1)

        for (xi in 0..steps) {
            for (yi in 0..steps) {
                for (zi in 0..steps) {
                    val px = box.minX + (box.maxX - box.minX) * xi / steps
                    val py = box.minY + (box.maxY - box.minY) * yi / steps
                    val pz = box.minZ + (box.maxZ - box.minZ) * zi / steps

                    val result = world.raycast(RaycastContext(
                        Vec3d(centerX, centerY, centerZ),
                        Vec3d(px, py, pz),
                        RaycastContext.ShapeType.COLLIDER,
                        RaycastContext.FluidHandling.NONE,
                        entity
                    ))
                    if (result.type == net.minecraft.util.hit.HitResult.Type.MISS) {
                        visible++
                    }
                }
            }
        }

        return visible.toDouble() / total
    }
}
