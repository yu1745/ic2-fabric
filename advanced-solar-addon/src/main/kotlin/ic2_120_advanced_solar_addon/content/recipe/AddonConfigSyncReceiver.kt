package ic2_120_advanced_solar_addon.content.recipe

import ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon
import ic2_120_advanced_solar_addon.config.Ic2AdvancedSolarAddonConfig
import ic2_120_advanced_solar_addon.integration.jei.Ic2AdvancedSolarAddonJeiPlugin

object AddonConfigSyncReceiver {
    private val logger = IC2AdvancedSolarAddon.LOGGER
    private var totalChunks = 0
    private var chunks = arrayOfNulls<ByteArray>(0)
    private var receivedCount = 0

    fun accept(packet: AddonConfigSyncPacket) {
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
            Ic2AdvancedSolarAddonConfig.applyServerConfig(json)
            MTRecipes.loadFromConfig()
            Ic2AdvancedSolarAddonJeiPlugin.refreshMTRecipes()
        }
    }
}
