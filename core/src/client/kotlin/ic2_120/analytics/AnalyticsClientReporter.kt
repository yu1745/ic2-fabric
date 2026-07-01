package ic2_120.analytics

import java.util.concurrent.atomic.AtomicBoolean
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents

/**
 * 客户端侧匿名统计上报接线：每次客户端加入世界时上报一次（每会话仅一次）。
 * 实际上报逻辑在 [AnalyticsReporter]（common 源码集），这里只负责注册事件 + 单次门控。
 */
object AnalyticsClientReporter {
    private val hasReported = AtomicBoolean(false)

    fun register() {
        ClientPlayConnectionEvents.JOIN.register(ClientPlayConnectionEvents.Join { _, _, _ ->
            if (hasReported.compareAndSet(false, true)) {
                AnalyticsReporter.report("client")
            }
        })
    }
}
