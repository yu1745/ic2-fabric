package ic2_120.content.network

import ic2_120.config.Ic2Config
import ic2_120.integration.jei.Ic2JeiPlugin
import org.slf4j.LoggerFactory

object ConfigSyncReceiver {
    private val logger = LoggerFactory.getLogger("ic2_120/config_sync")
    private var totalChunks = 0
    private var chunks = arrayOfNulls<ByteArray>(0)
    private var receivedCount = 0

    fun accept(packet: ConfigSyncPacket) {
        if (totalChunks != packet.totalChunks) {
            totalChunks = packet.totalChunks
            chunks = arrayOfNulls(totalChunks)
            receivedCount = 0
        }

        if (chunks[packet.chunkIndex] != null) return
        chunks[packet.chunkIndex] = packet.chunkData
        receivedCount++

        if (receivedCount == totalChunks) {
            val totalSize = chunks.sumOf { it!!.size }
            val fullData = ByteArray(totalSize)
            var offset = 0
            for (i in 0 until totalChunks) {
                val chunk = chunks[i]!!
                chunk.copyInto(fullData, offset)
                offset += chunk.size
            }
            totalChunks = 0
            chunks = arrayOfNulls(0)
            receivedCount = 0
            val json = String(fullData, Charsets.UTF_8)
            Ic2Config.applyServerConfig(json)
            Ic2JeiPlugin.refreshReplicatorRecipes()
        }
    }
}
