package ic2_120.integration.jade

import ic2_120.content.block.pipes.BasePipeBlock
import ic2_120.content.block.pipes.PipeBlockEntity
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.Registries
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import snownee.jade.api.BlockAccessor
import snownee.jade.api.IBlockComponentProvider
import snownee.jade.api.IServerDataProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.WailaPlugin
import snownee.jade.api.config.IPluginConfig
import java.util.LinkedList

/**
 * Jade plugin for IC2 pipe system.
 * Registered via "jade" entrypoint in fabric.mod.json.
 */
@WailaPlugin
class Ic2JadePlugin : snownee.jade.api.IWailaPlugin {

    override fun register(registration: snownee.jade.api.IWailaCommonRegistration) {
        registration.registerBlockDataProvider(PipeJadeProvider, PipeBlockEntity::class.java)
    }

    override fun registerClient(registration: snownee.jade.api.IWailaClientRegistration) {
        registration.registerBlockComponent(PipeJadeProvider, BasePipeBlock::class.java)
    }
}

/**
 * Jade provider for IC2 pipe system.
 */
object PipeJadeProvider : IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    private val PIPE_FLOW = Identifier("ic2_120", "pipe_flow")

    /**
     * Per-block-position sliding window filter to smooth flow rate display.
     * Cache is LRU-limited to 256 entries to prevent unbounded memory growth.
     */
    private val filterCache = object : LinkedHashMap<Long, FilteredLong>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, FilteredLong>?): Boolean =
            size > 256
    }

    /**
     * Sliding-window average filter for Long values.
     * Window size 100 ticks = 5 seconds at 20 tps.
     */
    private class FilteredLong(private val windowSize: Int) {
        private val window = LinkedList<Long>()

        fun update(value: Long): Long {
            window.addLast(value)
            if (window.size > windowSize) {
                window.removeFirst()
            }
            return window.sum() / window.size
        }
    }

    private fun filteredLoad(posLong: Long, raw: Long): Long {
        return filterCache.getOrPut(posLong) { FilteredLong(100) }.update(raw)
    }

    // IServerDataProvider — server side
    override fun appendServerData(data: NbtCompound, accessor: BlockAccessor) {
        val be = accessor.blockEntity as? PipeBlockEntity
        if (be == null) {
            return
        }
        data.putLong("pipeLoad", be.pipeLoad)
        data.putBoolean("isPump", be.isPumpAttachment())

        val block = accessor.blockState.block as? BasePipeBlock
        if (block != null) {
            data.putLong("pipeCapacity", calculateCapacity(block))
        }

        val network = be.network
        if (network != null) {
            data.putBoolean("stalled", network.stalledByMixedProviders)
            val fluidId = network.primaryFluidId
            if (fluidId != null) {
                data.putString("fluidName", fluidId)
            }
        }
    }

    // IBlockComponentProvider — client side
    override fun appendTooltip(tooltip: ITooltip, accessor: BlockAccessor, config: IPluginConfig) {
        val be = accessor.blockEntity as? PipeBlockEntity ?: return
        if (!accessor.serverData.contains("pipeLoad", 4)) return

        val rawLoad = accessor.serverData.getLong("pipeLoad")
        val capacity = accessor.serverData.getLong("pipeCapacity")
        val posLong = be.pos.asLong()
        val load = filteredLoad(posLong, rawLoad)
        val percent = if (capacity > 0) (load * 100 / capacity).toInt() else 0

        val flowText = Text.literal("")
            .append(Text.translatable("ic2_120.jade.flow_label"))
            .append(Text.literal("$load / ").setStyle(Style.EMPTY.withColor(Formatting.YELLOW)))
            .append(Text.literal("${capacity}mb/s = ").setStyle(Style.EMPTY.withColor(Formatting.GRAY)))
            .append(Text.literal("${percent}%").setStyle(Style.EMPTY.withColor(flowColor(percent))))
        tooltip.add(flowText)
        if (accessor.serverData.contains("fluidName")) {
            val fluidId = accessor.serverData.getString("fluidName")
            val fluid = Registries.FLUID.get(Identifier.tryParse(fluidId) ?: return)
            val fluidText = Registries.FLUID.getId(fluid).toTranslationKey().let { Text.translatable("block.$it") }
            tooltip.add(Text.translatable("ic2_120.jade.fluid_name", fluidText))
        }
        if (accessor.serverData.getBoolean("stalled")) {
            tooltip.add(Text.translatable("ic2_120.jade.stalled"))
        }
        if (accessor.serverData.getBoolean("isPump")) {
            tooltip.add(Text.translatable("ic2_120.jade.pump"))
        }
    }

    private fun flowColor(percent: Int): Formatting {
        return when {
            percent >= 90 -> Formatting.RED
            percent >= 70 -> Formatting.GOLD
            percent >= 50 -> Formatting.YELLOW
            else -> Formatting.GREEN
        }
    }

    override fun getUid(): Identifier = PIPE_FLOW

    private fun calculateCapacity(block: BasePipeBlock): Long {
        return kotlin.math.floor(
            block.size.baseBucketsPerSecond * block.material.multiplier * FluidConstants.BUCKET.toDouble() / 20.0
        ).toLong().coerceAtLeast(1L)
    }
}
