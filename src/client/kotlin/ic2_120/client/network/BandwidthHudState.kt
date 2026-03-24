package ic2_120.client.network

import ic2_120.content.network.BandwidthPlayerStat

object BandwidthHudState {
    @Volatile
    var enabled: Boolean = false

    @Volatile
    var serverBytesPerSecond: Long = 0L

    @Volatile
    var players: List<BandwidthPlayerStat> = emptyList()

    fun update(serverBps: Long, playerStats: List<BandwidthPlayerStat>) {
        serverBytesPerSecond = serverBps
        players = playerStats
    }
}
