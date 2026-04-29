package ic2_120.client

import ic2_120.content.block.CannerBlock
import ic2_120.content.block.CompressorBlock
import ic2_120.content.block.ElectricFurnaceBlock
import ic2_120.content.block.ExtractorBlock
import ic2_120.content.block.BaseMinerBlock
import ic2_120.content.block.GeneratorBlock
import ic2_120.content.block.GeoGeneratorBlock
import ic2_120.content.block.InductionFurnaceBlock
import ic2_120.content.block.IronFurnaceBlock
import ic2_120.content.block.MaceratorBlock
import ic2_120.content.block.MinerBlock
import ic2_120.content.block.PumpBlock
import ic2_120.content.block.RecyclerBlock
import ic2_120.content.block.WaterGeneratorBlock
import ic2_120.content.block.WindGeneratorBlock
import ic2_120.content.block.nuclear.NuclearReactorBlock
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.block.BlockState
import net.minecraft.client.sound.MovingSoundInstance
import net.minecraft.client.world.ClientWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.state.property.BooleanProperty
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.random.Random

object MachineLoopSoundController {
    private const val SCAN_RADIUS = 16
    private const val SCAN_INTERVAL_TICKS = 5
    private const val FADE_OUT_STEP = 0.06f

    private data class LoopDef(val eventId: String, val volume: Float, val pitch: Float)

    private val activeSounds = mutableMapOf<Long, BlockLoopSoundInstance>()
    private var scanTicker = 0

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val world = client.world
            val player = client.player
            if (world == null || player == null) {
                activeSounds.values.forEach { it.setDone() }
                activeSounds.clear()
                return@register
            }

            scanTicker++
            if (scanTicker % SCAN_INTERVAL_TICKS != 0) {
                cleanupFinished()
                return@register
            }

            val desired = collectActiveMachines(world, BlockPos.ofFloored(player.x, player.y, player.z))
            syncSounds(client, desired)
            cleanupFinished()
        }
    }

    private fun collectActiveMachines(world: ClientWorld, center: BlockPos): Map<Long, LoopDef> {
        val result = HashMap<Long, LoopDef>()
        for (x in center.x - SCAN_RADIUS..center.x + SCAN_RADIUS) {
            for (y in center.y - SCAN_RADIUS..center.y + SCAN_RADIUS) {
                for (z in center.z - SCAN_RADIUS..center.z + SCAN_RADIUS) {
                    val pos = BlockPos(x, y, z)
                    val state = world.getBlockState(pos)
                    val def = getLoopDef(state) ?: continue
                    result[pos.asLong()] = def
                }
            }
        }
        return result
    }

    private fun getLoopDef(state: BlockState): LoopDef? {
        return when (state.block) {
            is GeneratorBlock -> fromActive(state, GeneratorBlock.ACTIVE, "generator.generator.loop")
            is GeoGeneratorBlock -> fromActive(state, GeoGeneratorBlock.ACTIVE, "generator.geothermal.loop")
            is WaterGeneratorBlock -> fromActive(state, WaterGeneratorBlock.ACTIVE, "generator.water.loop")
            is WindGeneratorBlock -> fromActive(state, WindGeneratorBlock.ACTIVE, "generator.wind.loop")
            is IronFurnaceBlock -> fromActive(state, IronFurnaceBlock.ACTIVE, "machine.furnace.iron.operate")
            is ElectricFurnaceBlock -> fromActive(state, ElectricFurnaceBlock.ACTIVE, "machine.furnace.electric.loop")
            is InductionFurnaceBlock -> fromActive(state, InductionFurnaceBlock.ACTIVE, "machine.furnace.induction.loop")
            is MaceratorBlock -> fromActive(state, MaceratorBlock.ACTIVE, "machine.macerator.operate")
            is CompressorBlock -> fromActive(state, CompressorBlock.ACTIVE, "machine.compressor.operate")
            is ExtractorBlock -> fromActive(state, ExtractorBlock.ACTIVE, "machine.extractor.operate")
            is CannerBlock -> fromActive(state, CannerBlock.ACTIVE, "machine.canner.operate")
            is RecyclerBlock -> fromActive(state, RecyclerBlock.ACTIVE, "machine.recycler.operate")
            is PumpBlock -> fromActive(state, PumpBlock.ACTIVE, "machine.pump.operate")
            is MinerBlock -> fromActive(state, BaseMinerBlock.ACTIVE, "machine.miner.operate")
            is NuclearReactorBlock -> fromActive(state, NuclearReactorBlock.ACTIVE, "generator.nuclear.loop")
            else -> null
        }
    }

    private fun fromActive(state: BlockState, prop: BooleanProperty, eventId: String): LoopDef? {
        if (!state.contains(prop) || !state.get(prop)) return null
        return LoopDef(eventId = eventId, volume = 0.5f, pitch = 1.0f)
    }

    private fun syncSounds(client: net.minecraft.client.MinecraftClient, desired: Map<Long, LoopDef>) {
        for ((key, def) in desired) {
            val existing = activeSounds[key]
            if (existing == null || existing.isDone || existing.eventId != def.eventId) {
                existing?.setDone()
                val pos = BlockPos.fromLong(key)
                val created = BlockLoopSoundInstance(pos, def)
                activeSounds[key] = created
                client.soundManager.play(created)
            } else {
                existing.setFadingOut(false)
            }
        }

        val staleKeys = activeSounds.keys.filter { it !in desired.keys }
        for (key in staleKeys) {
            activeSounds[key]?.setFadingOut(true)
        }
    }

    private fun cleanupFinished() {
        val toRemove = activeSounds.filterValues { it.isDone }.keys
        toRemove.forEach { activeSounds.remove(it) }
    }

    private class BlockLoopSoundInstance(pos: BlockPos, def: LoopDef) :
        MovingSoundInstance(
            SoundEvent.of(Identifier("ic2", def.eventId)),
            SoundCategory.BLOCKS,
            Random.create()
        ) {
        val eventId: String = def.eventId
        private var fadingOut = false
        private val baseVolume = def.volume

        init {
            repeat = true
            repeatDelay = 0
            volume = baseVolume
            pitch = def.pitch
            x = pos.x + 0.5
            y = pos.y + 0.5
            z = pos.z + 0.5
        }

        override fun tick() {
            if (fadingOut) {
                volume = (volume - FADE_OUT_STEP).coerceAtLeast(0f)
                if (volume <= 0.001f) setDone()
            } else {
                volume = baseVolume
            }
        }

        fun setFadingOut(value: Boolean) {
            fadingOut = value
        }
    }
}
