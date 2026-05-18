package ic2_120.content.block.nuclear

import net.minecraft.server.MinecraftServer
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import org.slf4j.LoggerFactory

/**
 * 核爆炸管理器。
 *
 * 注册到 ServerTickEvents.END_SERVER_TICK，每 tick 处理所有活跃核爆炸的分批方块摧毁。
 */
object NuclearExplosionManager {

    private val LOG = LoggerFactory.getLogger("ic2_120/NuclearExplosionManager")
    private val explosions = mutableListOf<NuclearExplosion>()
    private var activeCount = 0

    /**
     * 启动一个新的核爆炸。
     * 会在当前线程中同步完成方块计算，然后注册到管理器进行分 tick 摧毁。
     *
     * @param world 爆炸所在世界
     * @param cx 爆炸中心 X
     * @param cy 爆炸中心 Y
     * @param cz 爆炸中心 Z
     * @param power 爆炸威力
     * @param damageSource 伤害来源
     */
    fun startExplosion(
        world: ServerWorld,
        cx: Double, cy: Double, cz: Double,
        power: Float,
        damageSource: net.minecraft.entity.damage.DamageSource
    ) {
        if (power <= 0f) return

        val explosion = NuclearExplosion.create(world, cx, cy, cz, power, damageSource)
        explosions.add(explosion)
        activeCount++
        LOG.info("[核爆炸管理器] 注册新爆炸 pos=({}, {}, {}) power={} 预计{}tick完成",
            cx.toInt(), cy.toInt(), cz.toInt(), power,
            explosion.estimatedRemainingTicks()
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
