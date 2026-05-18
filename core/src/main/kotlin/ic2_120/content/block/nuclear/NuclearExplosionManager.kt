package ic2_120.content.block.nuclear

import net.minecraft.server.MinecraftServer
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import org.slf4j.LoggerFactory

/**
 * 核爆炸管理器。
 *
 * 注册到 ServerTickEvents.END_SERVER_TICK，每 tick 处理所有活跃核爆炸。
 * 爆炸分两阶段全分 tick 执行：
 *  1. CALCULATING — 逐步射线投射收集方块
 *  2. DESTROYING  — 逐步摧毁方块
 */
object NuclearExplosionManager {

    private val LOG = LoggerFactory.getLogger("ic2_120/NuclearExplosionManager")
    private val explosions = mutableListOf<NuclearExplosion>()
    private var activeCount = 0

    /**
     * 启动一个新的核爆炸。
     * 计算与摧毁均为分 tick 异步完成，不阻塞调用线程。
     *
     * @param onComplete 爆炸全部完成后在主线程执行的回调，参数为该爆炸实例
     */
    fun startExplosion(
        world: ServerWorld,
        cx: Double, cy: Double, cz: Double,
        power: Float,
        damageSource: net.minecraft.entity.damage.DamageSource,
        onComplete: ((NuclearExplosion) -> Unit)? = null
    ) {
        if (power <= 0f) return

        val explosion = NuclearExplosion.create(world, cx, cy, cz, power, damageSource)
        explosion.onComplete = onComplete
        explosions.add(explosion)
        activeCount++
        LOG.info("[核爆炸管理器] 注册新爆炸 pos=({}, {}, {}) power={} rays={}",
            cx.toInt(), cy.toInt(), cz.toInt(), power,
            explosion.totalRays
        )
    }

    /**
     * 每 tick 调用，处理所有活跃爆炸。
     * @param server Minecraft 服务端
     */
    fun tick(server: MinecraftServer) {
        val iter = explosions.iterator()
        while (iter.hasNext()) {
            val explosion = iter.next()
            if (explosion.tick()) {
                iter.remove()
                activeCount--
            }
        }
    }

    /** 当前活跃的爆炸数 */
    fun getActiveCount(): Int = activeCount
}
