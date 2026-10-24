package ic2_120.content.network

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import io.netty.buffer.Unpooled

import net.minecraft.network.PacketByteBuf
import net.minecraft.network.packet.Packet
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.LongAdder

object BandwidthStatsService {
    private data class PlayerCounter(
        val name: String,
        val totalBytes: LongAdder = LongAdder(),
        val secondBytes: LongAdder = LongAdder(),
        @Volatile var lastBytesPerSecond: Long = 0L
    )

    private val counters = ConcurrentHashMap<UUID, PlayerCounter>()
    private var lastSecondTick = -1L

    @JvmStatic
    fun recordPacket(player: ServerPlayerEntity, packet: Packet<*>) {
        val bytes = estimatePacketBytes(packet)
        if (bytes <= 0) return
        val counter = counters.computeIfAbsent(player.uuid) { PlayerCounter(player.name.string) }
        counter.totalBytes.add(bytes.toLong())
        counter.secondBytes.add(bytes.toLong())
    }

    fun onServerTick(server: MinecraftServer) {
        val tick = server.ticks.toLong()
        if (tick % 20L != 0L) return
        if (lastSecondTick == tick) return
        lastSecondTick = tick

        val statsByPlayer = HashMap<UUID, PlayerCounter>()
        for (player in server.playerManager.playerList) {
            val counter = counters.computeIfAbsent(player.uuid) { PlayerCounter(player.name.string) }
            counter.lastBytesPerSecond = counter.secondBytes.sumThenReset()
            statsByPlayer[player.uuid] = counter
        }

        val snapshot = server.playerManager.playerList.map { player ->
            val stat = statsByPlayer[player.uuid] ?: PlayerCounter(player.name.string)
            BandwidthPlayerStat(
                name = player.name.string,
                bytesPerSecond = stat.lastBytesPerSecond,
                totalBytes = stat.totalBytes.sum()
            )
        }.sortedByDescending { it.bytesPerSecond }

        val serverBps = snapshot.sumOf { it.bytesPerSecond }
        val packet = BandwidthHudPacket(serverBytesPerSecond = serverBps, players = snapshot)
        for (player in server.playerManager.playerList) {
            ServerPlayNetworking.send(player, packet)
        }
    }

    private fun estimatePacketBytes(packet: Packet<*>): Int {
        return 0
    }
}

