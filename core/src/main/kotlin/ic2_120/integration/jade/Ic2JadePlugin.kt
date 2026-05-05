package ic2_120.integration.jade

import ic2_120.content.block.cables.BaseCableBlock
import ic2_120.content.block.cables.CableBlockEntity
import ic2_120.content.block.CropBlock
import ic2_120.content.block.CropBlockEntity
import ic2_120.content.block.CropStickBlock
import ic2_120.content.block.CropStickBlockEntity
import ic2_120.content.block.AnimalmatronBlock
import ic2_120.content.block.machines.AnimalmatronBlockEntity
import ic2_120.content.block.TeleporterBlock
import ic2_120.content.block.machines.TeleporterBlockEntity
import ic2_120.content.block.KineticGeneratorBlock
import ic2_120.content.block.WindKineticGeneratorBlock
import ic2_120.content.block.WaterKineticGeneratorBlock
import ic2_120.content.block.ManualKineticGeneratorBlock
import ic2_120.content.block.pipes.BasePipeBlock
import ic2_120.content.block.pipes.PipeBlockEntity
import ic2_120.content.block.machines.KineticGeneratorBlockEntity
import ic2_120.content.block.machines.WindKineticGeneratorBlockEntity
import ic2_120.content.block.machines.WaterKineticGeneratorBlockEntity
import ic2_120.content.block.machines.LeashKineticGeneratorBlockEntity
import ic2_120.content.block.machines.ManualKineticGeneratorBlockEntity
import ic2_120.content.block.transmission.BevelGearBlock
import ic2_120.content.block.transmission.CarbonTransmissionShaftBlock
import ic2_120.content.block.transmission.IronTransmissionShaftBlock
import ic2_120.content.block.transmission.SteelTransmissionShaftBlock
import ic2_120.content.block.transmission.TransmissionBlockEntity
import ic2_120.content.block.transmission.TransmissionShaftBlock
import ic2_120.content.block.transmission.WoodTransmissionShaftBlock
import ic2_120.content.entity.AnimalFoodMapping
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.minecraft.entity.passive.PassiveEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.Registries
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import snownee.jade.api.BlockAccessor
import snownee.jade.api.EntityAccessor
import snownee.jade.api.IEntityComponentProvider
import snownee.jade.api.IBlockComponentProvider
import snownee.jade.api.IServerDataProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.WailaPlugin
import snownee.jade.api.config.IPluginConfig
import java.util.LinkedList

/**
 * Sliding-window average filter for Long values.
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

/**
 * Jade plugin for IC2 pipe system.
 * Registered via "jade" entrypoint in fabric.mod.json.
 */
@WailaPlugin
class Ic2JadePlugin : snownee.jade.api.IWailaPlugin {

    override fun register(registration: snownee.jade.api.IWailaCommonRegistration) {
        registration.registerBlockDataProvider(PipeJadeProvider, PipeBlockEntity::class.java)
        registration.registerBlockDataProvider(CableJadeProvider, CableBlockEntity::class.java)
        registration.registerBlockDataProvider(CropJadeProvider, CropBlockEntity::class.java)
        registration.registerBlockDataProvider(CropJadeProvider, CropStickBlockEntity::class.java)
        registration.registerBlockDataProvider(KineticJadeProvider, TransmissionBlockEntity::class.java)
        registration.registerBlockDataProvider(KineticJadeProvider, WindKineticGeneratorBlockEntity::class.java)
        registration.registerBlockDataProvider(KineticJadeProvider, WaterKineticGeneratorBlockEntity::class.java)
        registration.registerBlockDataProvider(KineticJadeProvider, ManualKineticGeneratorBlockEntity::class.java)
        registration.registerBlockDataProvider(KineticJadeProvider, LeashKineticGeneratorBlockEntity::class.java)
        registration.registerBlockDataProvider(KineticJadeProvider, KineticGeneratorBlockEntity::class.java)
        registration.registerBlockDataProvider(TeleporterJadeProvider, TeleporterBlockEntity::class.java)
        registration.registerEntityDataProvider(AnimalJadeProvider, PassiveEntity::class.java)
    }

    override fun registerClient(registration: snownee.jade.api.IWailaClientRegistration) {
        registration.registerBlockComponent(PipeJadeProvider, BasePipeBlock::class.java)
        registration.registerBlockComponent(CableJadeProvider, BaseCableBlock::class.java)
        registration.registerBlockComponent(CropJadeProvider, CropBlock::class.java)
        registration.registerBlockComponent(CropJadeProvider, CropStickBlock::class.java)
        registration.registerBlockComponent(KineticJadeProvider, TransmissionShaftBlock::class.java)
        registration.registerBlockComponent(KineticJadeProvider, BevelGearBlock::class.java)
        registration.registerBlockComponent(KineticJadeProvider, WindKineticGeneratorBlock::class.java)
        registration.registerBlockComponent(KineticJadeProvider, WaterKineticGeneratorBlock::class.java)
        registration.registerBlockComponent(KineticJadeProvider, ManualKineticGeneratorBlock::class.java)
        registration.registerBlockComponent(KineticJadeProvider, KineticGeneratorBlock::class.java)
        registration.registerBlockComponent(TeleporterJadeProvider, TeleporterBlock::class.java)
        registration.registerEntityComponent(AnimalJadeProvider, PassiveEntity::class.java)
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

    private fun filteredLoad(posLong: Long, raw: Long): Long {
        return filterCache.getOrPut(posLong) { FilteredLong(20) }.update(raw)
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

/**
 * Jade provider for IC2 cable system.
 */
object CableJadeProvider : IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    private val CABLE_LOAD = Identifier("ic2_120", "cable_load")

    private val filterCache = object : LinkedHashMap<Long, FilteredLong>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, FilteredLong>?): Boolean =
            size > 256
    }

    private fun filteredLoad(posLong: Long, raw: Long): Long {
        return filterCache.getOrPut(posLong) { FilteredLong(20) }.update(raw)
    }

    // IServerDataProvider — server side
    override fun appendServerData(data: NbtCompound, accessor: BlockAccessor) {
        val be = accessor.blockEntity as? CableBlockEntity ?: return
        data.putLong("cableLoad", be.cableLoad)

        val block = accessor.blockState.block as? BaseCableBlock
        if (block != null) {
            data.putLong("cableCapacity", block.getTransferRate())
        }
    }

    // IBlockComponentProvider — client side
    override fun appendTooltip(tooltip: ITooltip, accessor: BlockAccessor, config: IPluginConfig) {
        if (!accessor.serverData.contains("cableLoad", 4)) return

        val rawLoad = accessor.serverData.getLong("cableLoad")
        val capacity = accessor.serverData.getLong("cableCapacity")
        val posLong = accessor.position.asLong()
        val load = filteredLoad(posLong, rawLoad)
        val percent = if (capacity > 0) (load * 100 / capacity).toInt() else 0

        val loadText = Text.literal("")
            .append(Text.translatable("ic2_120.jade.cable_load_label"))
            .append(Text.literal("${formatEu(load)} / ").setStyle(Style.EMPTY.withColor(Formatting.YELLOW)))
            .append(Text.literal("${formatEu(capacity)}/t = ").setStyle(Style.EMPTY.withColor(Formatting.GRAY)))
            .append(Text.literal("${percent}%").setStyle(Style.EMPTY.withColor(flowColor(percent))))
        tooltip.add(loadText)
    }

    private fun formatEu(value: Long): String {
        return when {
            value >= 1_000_000_000 -> "${String.format("%.1f", value / 1_000_000_000.0)} GEU"
            value >= 1_000_000 -> "${String.format("%.1f", value / 1_000_000.0)} MEU"
            value >= 1_000 -> "${String.format("%.1f", value / 1_000.0)} kEU"
            else -> "${value} EU"
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

    override fun getUid(): Identifier = CABLE_LOAD
}

object CropJadeProvider : IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    private val CROP_GROWTH = Identifier("ic2_120", "crop_growth")

    override fun appendServerData(data: NbtCompound, accessor: BlockAccessor) {
        val world = accessor.level
        val blockEntity = accessor.blockEntity ?: return

        if (blockEntity is CropStickBlockEntity) {
            val (nutrients, water, weedEx) = blockEntity.storageSnapshot()
            data.putBoolean("isStick", true)
            data.putBoolean(
                "crossingBase",
                if (accessor.blockState.contains(CropStickBlock.CROSSING_BASE)) {
                    accessor.blockState.get(CropStickBlock.CROSSING_BASE)
                } else {
                    false
                }
            )
            data.putInt("nutrients", nutrients)
            data.putInt("water", water)
            data.putInt("weedEx", weedEx)
            return
        }

        val be = blockEntity as? CropBlockEntity ?: return
        val state = world.getBlockState(accessor.position)
        if (!state.contains(CropBlock.CROP_TYPE) || !state.contains(CropBlock.AGE)) return
        val estimate = be.estimateGrowth(world, accessor.position, state)
        val (nutrients, water, weedEx) = be.storageSnapshot()
        val requirements = be.getGrowthRequirements(world, accessor.position, state, estimate.cropType)

        data.putInt("age", estimate.age)
        data.putInt("maxAge", estimate.maxAge)
        data.putInt("progress", estimate.progressPercent)
        data.putBoolean("canGrowNow", estimate.canGrowNow)
        data.putBoolean("isMature", estimate.isMature)
        data.putInt("nutrients", nutrients)
        data.putInt("water", water)
        data.putInt("weedEx", weedEx)
        if (estimate.remainingSeconds != null) {
            data.putDouble("remainingSeconds", estimate.remainingSeconds)
        }
        if (requirements.isNotEmpty()) {
            val serialized = requirements.map { req ->
                val argsStr = req.args.joinToString(",")
                "${req.key}|$argsStr"
            }.joinToString(";")
            data.putString("requirements", serialized)
        }
    }

    override fun appendTooltip(tooltip: ITooltip, accessor: BlockAccessor, config: IPluginConfig) {
        if (accessor.serverData.getBoolean("isStick")) {
            val crossingBase = accessor.serverData.getBoolean("crossingBase")
            val nutrients = accessor.serverData.getInt("nutrients")
            val water = accessor.serverData.getInt("water")
            val weedEx = accessor.serverData.getInt("weedEx")

            tooltip.add(
                if (crossingBase) Text.translatable("ic2_120.jade.crop_stick_crossing").formatted(Formatting.YELLOW)
                else Text.translatable("ic2_120.jade.crop_stick_empty").formatted(Formatting.GRAY)
            )
            tooltip.add(
                if (nutrients > 0) {
                    Text.translatable("ic2_120.jade.crop_fertilized_yes", nutrients).formatted(Formatting.GREEN)
                } else {
                    Text.translatable("ic2_120.jade.crop_fertilized_no").formatted(Formatting.GRAY)
                }
            )
            tooltip.add(
                if (water > 0) {
                    Text.translatable("ic2_120.jade.crop_hydrated_yes", water).formatted(Formatting.AQUA)
                } else {
                    Text.translatable("ic2_120.jade.crop_hydrated_no").formatted(Formatting.GRAY)
                }
            )
            tooltip.add(
                if (weedEx > 0) {
                    Text.translatable("ic2_120.jade.crop_weedex_yes", weedEx).formatted(Formatting.GREEN)
                } else {
                    Text.translatable("ic2_120.jade.crop_weedex_no").formatted(Formatting.GRAY)
                }
            )
            return
        }

        if (!accessor.serverData.contains("progress")) return

        val age = accessor.serverData.getInt("age")
        val maxAge = accessor.serverData.getInt("maxAge")
        val progress = accessor.serverData.getInt("progress")
        val canGrowNow = accessor.serverData.getBoolean("canGrowNow")
        val isMature = accessor.serverData.getBoolean("isMature")
        val nutrients = accessor.serverData.getInt("nutrients")
        val water = accessor.serverData.getInt("water")
        val weedEx = accessor.serverData.getInt("weedEx")

        tooltip.add(Text.translatable("ic2_120.jade.crop_progress", age, maxAge, progress))
        tooltip.add(
            if (nutrients > 0) {
                Text.translatable("ic2_120.jade.crop_fertilized_yes", nutrients).formatted(Formatting.GREEN)
            } else {
                Text.translatable("ic2_120.jade.crop_fertilized_no").formatted(Formatting.GRAY)
            }
        )
        tooltip.add(
            if (water > 0) {
                Text.translatable("ic2_120.jade.crop_hydrated_yes", water).formatted(Formatting.AQUA)
            } else {
                Text.translatable("ic2_120.jade.crop_hydrated_no").formatted(Formatting.GRAY)
            }
        )
        tooltip.add(
            if (weedEx > 0) {
                Text.translatable("ic2_120.jade.crop_weedex_yes", weedEx).formatted(Formatting.GREEN)
            } else {
                Text.translatable("ic2_120.jade.crop_weedex_no").formatted(Formatting.GRAY)
            }
        )

        if (isMature) {
            tooltip.add(Text.translatable("ic2_120.jade.crop_mature").formatted(Formatting.GREEN))
            return
        }

        if (!canGrowNow) {
            tooltip.add(Text.translatable("ic2_120.jade.crop_cannot_grow").formatted(Formatting.RED))
            if (accessor.serverData.contains("requirements")) {
                val serialized = accessor.serverData.getString("requirements")
                val entries = serialized.split(";")
                for (entry in entries) {
                    val parts = entry.split("|", limit = 2)
                    val key = parts[0]
                    val args: Array<Any> = if (parts.size > 1 && parts[1].isNotEmpty()) {
                        parts[1].split(",").map { it.toIntOrNull() ?: it.toDoubleOrNull() ?: it as Any }.toTypedArray()
                    } else {
                        emptyArray()
                    }
                    tooltip.add(Text.literal("  ⚠ ").append(Text.translatable("ic2_120.jade.crop_req.$key", *args)).formatted(Formatting.GRAY))
                }
            }
            return
        }

        if (accessor.serverData.contains("remainingSeconds")) {
            val sec = accessor.serverData.getDouble("remainingSeconds").coerceAtLeast(0.0)
            val min = sec / 60.0
            tooltip.add(Text.translatable("ic2_120.jade.crop_remaining_time", String.format("%.1f", sec), String.format("%.2f", min)))
        } else {
            tooltip.add(Text.translatable("ic2_120.jade.crop_remaining_unknown").formatted(Formatting.YELLOW))
        }
    }

    override fun getUid(): Identifier = CROP_GROWTH
}

object KineticJadeProvider : IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    private val KINETIC_INFO = Identifier("ic2_120", "kinetic_info")

    override fun appendServerData(data: NbtCompound, accessor: BlockAccessor) {
        val world = accessor.level
        val pos = accessor.position
        val state = accessor.blockState

        when (val be = accessor.blockEntity) {
            is TransmissionBlockEntity -> {
                val (capacity, loss) = transmissionLimits(state.block)
                data.putString("kind", "transmission")
                data.putInt("ku", be.currentKu.coerceAtLeast(0))
                data.putInt("capacity", capacity)
                data.putInt("loss", loss)
            }

            is WindKineticGeneratorBlockEntity -> {
                data.putString("kind", "wind_kinetic")
                data.putInt("generatedKu", be.getGeneratedKu())
                data.putInt("outputKu", be.getOutputKu())
                data.putBoolean("blocked", be.isStuck)
                data.putDouble("rotorHours", be.getRotorRemainingClearHours())
            }

            is WaterKineticGeneratorBlockEntity -> {
                data.putString("kind", "water_kinetic")
                data.putInt("generatedKu", be.getGeneratedKu())
                data.putInt("outputKu", be.getOutputKu())
                data.putBoolean("blocked", be.isStuck)
                data.putBoolean("submerged", be.isSubmerged)
                data.putBoolean("flowBonus", be.waterFlowBonus)
                data.putDouble("rotorHours", be.getRotorRemainingClearHours())
            }

            is ManualKineticGeneratorBlockEntity -> {
                data.putString("kind", "manual_kinetic")
                data.putInt("storedKu", be.sync.storedKu.coerceAtLeast(0))
                data.putInt("outputKu", be.sync.outputKu.coerceAtLeast(0))
            }

            is LeashKineticGeneratorBlockEntity -> {
                data.putString("kind", "leash_kinetic")
                data.putInt("generatedKu", be.sync.generatedKu.coerceAtLeast(0))
                data.putInt("angularVelocity", be.sync.angularVelocityDegPerSec.coerceAtLeast(0))
                data.putString("animalName", be.getMobName())
            }

            is KineticGeneratorBlockEntity -> {
                val inputKu = be.sync.currentKu.coerceAtLeast(0)
                val outputEu = be.sync.outputEu.coerceAtLeast(0)
                data.putString("kind", "kinetic_generator")
                data.putInt("inputKu", inputKu)
                data.putInt("outputEu", outputEu)
                data.putLong("eu", be.sync.amount.coerceAtLeast(0L))
                data.putLong("euCap", ic2_120.content.sync.KineticGeneratorSync.ENERGY_CAPACITY)
            }
        }
    }

    override fun appendTooltip(tooltip: ITooltip, accessor: BlockAccessor, config: IPluginConfig) {
        val kind = accessor.serverData.getString("kind")
        when (kind) {
            "transmission" -> {
                val ku = accessor.serverData.getInt("ku")
                val cap = accessor.serverData.getInt("capacity")
                val loss = accessor.serverData.getInt("loss")
                tooltip.add(Text.translatable("ic2_120.jade.ku_line", ku, cap))
                tooltip.add(Text.translatable("ic2_120.jade.ku_loss_line", loss))
            }

            "wind_kinetic" -> {
                val generatedKu = accessor.serverData.getInt("generatedKu")
                val outputKu = accessor.serverData.getInt("outputKu")
                val blocked = accessor.serverData.getBoolean("blocked")
                val rotorHours = accessor.serverData.getDouble("rotorHours")
                tooltip.add(Text.translatable("ic2_120.jade.wind_ku_generated", generatedKu))
                tooltip.add(Text.translatable("ic2_120.jade.wind_ku_output", outputKu))
                val windInsufficient = generatedKu == 0 && !blocked
                tooltip.add(Text.translatable(
                    when {
                        blocked -> "ic2_120.jade.wind_blocked"
                        windInsufficient -> "ic2_120.jade.wind_insufficient"
                        else -> "ic2_120.jade.wind_clear"
                    }
                ))
                tooltip.add(Text.translatable("ic2_120.jade.wind_rotor_lifetime", String.format("%.1f", rotorHours)))
            }

            "water_kinetic" -> {
                val generatedKu = accessor.serverData.getInt("generatedKu")
                val outputKu = accessor.serverData.getInt("outputKu")
                val blocked = accessor.serverData.getBoolean("blocked")
                val submerged = accessor.serverData.getBoolean("submerged")
                val flowBonus = accessor.serverData.getBoolean("flowBonus")
                val rotorHours = accessor.serverData.getDouble("rotorHours")
                tooltip.add(Text.translatable("ic2_120.jade.water_ku_generated", generatedKu))
                tooltip.add(Text.translatable("ic2_120.jade.water_ku_output", outputKu))
                tooltip.add(Text.translatable(if (blocked) "ic2_120.jade.water_blocked" else if (submerged) "ic2_120.jade.water_submerged" else "ic2_120.jade.water_not_submerged"))
                if (flowBonus) {
                    tooltip.add(Text.translatable("ic2_120.jade.water_flow_bonus"))
                }
                tooltip.add(Text.translatable("ic2_120.jade.water_rotor_lifetime", String.format("%.1f", rotorHours)))
            }

            "manual_kinetic" -> {
                val storedKu = accessor.serverData.getInt("storedKu")
                val outputKu = accessor.serverData.getInt("outputKu")
                tooltip.add(Text.translatable("ic2_120.jade.manual_stored_ku", storedKu))
                tooltip.add(Text.translatable("ic2_120.jade.manual_extracted_ku", outputKu))
            }

            "leash_kinetic" -> {
                val generatedKu = accessor.serverData.getInt("generatedKu")
                val angularVelocity = accessor.serverData.getInt("angularVelocity")
                val animalName = accessor.serverData.getString("animalName")
                tooltip.add(Text.translatable("ic2_120.jade.leash_kinetic.generated", generatedKu))
                tooltip.add(Text.translatable("ic2_120.jade.leash_kinetic.velocity", angularVelocity))
                if (animalName.isNotEmpty()) {
                    tooltip.add(Text.translatable("ic2_120.jade.leash_kinetic.animal", animalName))
                }
            }

            "kinetic_generator" -> {
                val inputKu = accessor.serverData.getInt("inputKu")
                val outputEu = accessor.serverData.getInt("outputEu")
                val eu = accessor.serverData.getLong("eu")
                val euCap = accessor.serverData.getLong("euCap")
                tooltip.add(Text.translatable("ic2_120.jade.kinetic_input", inputKu))
                tooltip.add(Text.translatable("ic2_120.jade.kinetic_output", outputEu))
                tooltip.add(Text.translatable("ic2_120.jade.kinetic_buffer", eu, euCap))
            }
        }
    }

    override fun getUid(): Identifier = KINETIC_INFO

    private fun transmissionLimits(block: net.minecraft.block.Block): Pair<Int, Int> = when (block) {
        is WoodTransmissionShaftBlock -> 128 to 0
        is IronTransmissionShaftBlock -> 512 to 2
        is SteelTransmissionShaftBlock -> 2048 to 1
        is CarbonTransmissionShaftBlock -> 8192 to 0
        is BevelGearBlock -> 2048 to 3
        else -> 0 to 0
    }
}

/**
 * Jade provider for animals managed by Animalmatron.
 */
object AnimalJadeProvider : IEntityComponentProvider, IServerDataProvider<EntityAccessor> {

    private val ANIMAL_MONITOR = Identifier("ic2_120", "animal_monitor")

    override fun appendServerData(data: NbtCompound, accessor: EntityAccessor) {
        val entity = accessor.entity
        if (entity !is PassiveEntity) return

        val world = accessor.level
        val pos = entity.blockPos

        // 只显示被管理的动物
        if (!AnimalFoodMapping.isManagedAnimal(entity)) {
            data.putBoolean("isManaged", false)
            return
        }

        // 检查附近4格内是否有牲畜监管机
        val box = net.minecraft.util.math.Box(pos).expand(4.0)
        var foundMachine: AnimalmatronBlockEntity? = null
        var hasWater = false
        var hasWeedEx = false
        var totalFed = 0
        var todayFed = 0

        val minX = box.minX.toInt()
        val minY = box.minY.toInt()
        val minZ = box.minZ.toInt()
        val maxX = box.maxX.toInt()
        val maxY = box.maxY.toInt()
        val maxZ = box.maxZ.toInt()

        for (x in minX..maxX) {
            for (y in minY..maxY) {
                for (z in minZ..maxZ) {
                    val checkPos = net.minecraft.util.math.BlockPos(x, y, z)
                    val be = world.getBlockEntity(checkPos)
                    if (be is AnimalmatronBlockEntity) {
                        foundMachine = be
                        hasWater = be.sync.waterAmountMb > 0
                        hasWeedEx = be.sync.weedExAmountMb > 0
                        // 获取喂食进度
                        val progress = be.getAnimalFeedProgress(entity.uuid)
                        if (progress != null) {
                            totalFed = progress.first
                            todayFed = progress.second
                        }
                        // 获取繁殖状态
                        data.putBoolean("canBreed", be.isAnimalCanBreed(entity.uuid))
                        break
                    }
                }
                if (foundMachine != null) break
            }
            if (foundMachine != null) break
        }

        data.putBoolean("isManaged", foundMachine != null)
        data.putBoolean("hasWater", hasWater)
        data.putBoolean("hasWeedEx", hasWeedEx)
        data.putInt("totalFed", totalFed)
        data.putInt("todayFed", todayFed)
        data.putInt("requiredToFed", AnimalmatronBlockEntity.FOOD_TO_GROW) // 10
    }

    override fun appendTooltip(tooltip: ITooltip, accessor: EntityAccessor, config: IPluginConfig) {
        if (!accessor.serverData.contains("isManaged")) return
        if (!accessor.serverData.getBoolean("isManaged")) return

        // 显示被监管标识
        tooltip.add(Text.translatable("ic2_120.jade.animal_monitored").formatted(Formatting.GREEN))

        val hasWater = accessor.serverData.getBoolean("hasWater")
        val hasWeedEx = accessor.serverData.getBoolean("hasWeedEx")

        // 显示水状态
        tooltip.add(
            if (hasWater) {
                Text.translatable("ic2_120.jade.animal_water_ok").formatted(Formatting.AQUA)
            } else {
                Text.translatable("ic2_120.jade.animal_water_low").formatted(Formatting.RED)
            }
        )

        // 显示除草剂状态
        tooltip.add(
            if (hasWeedEx) {
                Text.translatable("ic2_120.jade.animal_weedex_ok").formatted(Formatting.GREEN)
            } else {
                Text.translatable("ic2_120.jade.animal_weedex_low").formatted(Formatting.RED)
            }
        )

        // 显示喂食进度
        if (accessor.serverData.contains("canBreed")) {
            val canBreed = accessor.serverData.getBoolean("canBreed")
            if (canBreed) {
                // 可以繁殖
                tooltip.add(Text.translatable("ic2_120.jade.animal_ready_to_breed").formatted(Formatting.GREEN))
            } else {
                // 还没长成，显示进度
                val totalFed = accessor.serverData.getInt("totalFed")
                val requiredToFed = accessor.serverData.getInt("requiredToFed")
                tooltip.add(Text.literal("喂食: $totalFed/$requiredToFed").formatted(Formatting.YELLOW))
                val remaining = requiredToFed - totalFed
                tooltip.add(Text.translatable("ic2_120.jade.animal_remaining_feed", remaining).formatted(Formatting.GRAY))
            }
        }
    }

    override fun getUid(): Identifier = ANIMAL_MONITOR
}
