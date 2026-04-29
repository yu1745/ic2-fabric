package ic2_120.content.item

import net.minecraft.server.network.ServerPlayerEntity
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 按住模式键切模式时，客户端仍可能向服务端发送 [MiningLaserItem.use]。
 * 在收到切模式包的本服务端 tick 内，抵消紧随其后的那一次发射（不扣电、不生成弹体）。
 */
object MiningLaserServerSuppress {
    private val toggleTickByPlayer = ConcurrentHashMap<UUID, Int>()

    fun onMiningLaserModeToggled(player: ServerPlayerEntity) {
        toggleTickByPlayer[player.uuid] = player.server.ticks
    }

    /**
     * 若本 tick 内已处理过切模式，返回 true 并清除标记；否则清理过期标记并返回 false。
     */
    fun consumeSuppressNextFire(player: ServerPlayerEntity): Boolean {
        val marked = toggleTickByPlayer[player.uuid] ?: return false
        val cur = player.server.ticks
        if (marked != cur) {
            toggleTickByPlayer.remove(player.uuid)
            return false
        }
        toggleTickByPlayer.remove(player.uuid)
        return true
    }
}
