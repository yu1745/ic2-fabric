package buildcraft_addon.integration.jade

import buildcraft_addon.content.block.CreativeEngineBlock
import buildcraft_addon.content.block.IronEngineBlock
import buildcraft_addon.content.block.PumpBlock
import buildcraft_addon.content.block.RFEngineBlock
import buildcraft_addon.content.block.RedstoneEngineBlock
import buildcraft_addon.content.block.StoneEngineBlock
import buildcraft_addon.content.blockentity.CreativeEngineBlockEntity
import buildcraft_addon.content.blockentity.IronEngineBlockEntity
import buildcraft_addon.content.blockentity.PumpBlockEntity
import buildcraft_addon.content.blockentity.RFEngineBlockEntity
import buildcraft_addon.content.blockentity.RedstoneEngineBlockEntity
import buildcraft_addon.content.blockentity.StoneEngineBlockEntity
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
import snownee.jade.api.IWailaClientRegistration
import snownee.jade.api.IWailaCommonRegistration
import snownee.jade.api.IWailaPlugin
import snownee.jade.api.WailaPlugin
import snownee.jade.api.config.IPluginConfig

@WailaPlugin
class IC2BuildCraftJadePlugin : IWailaPlugin {

    override fun register(registration: IWailaCommonRegistration) {
        registration.registerBlockDataProvider(BcJadeProvider, RedstoneEngineBlockEntity::class.java)
        registration.registerBlockDataProvider(BcJadeProvider, StoneEngineBlockEntity::class.java)
        registration.registerBlockDataProvider(BcJadeProvider, IronEngineBlockEntity::class.java)
        registration.registerBlockDataProvider(BcJadeProvider, RFEngineBlockEntity::class.java)
        registration.registerBlockDataProvider(BcJadeProvider, CreativeEngineBlockEntity::class.java)
        registration.registerBlockDataProvider(BcJadeProvider, PumpBlockEntity::class.java)
    }

    override fun registerClient(registration: IWailaClientRegistration) {
        registration.registerBlockComponent(BcJadeProvider, RedstoneEngineBlock::class.java)
        registration.registerBlockComponent(BcJadeProvider, StoneEngineBlock::class.java)
        registration.registerBlockComponent(BcJadeProvider, IronEngineBlock::class.java)
        registration.registerBlockComponent(BcJadeProvider, RFEngineBlock::class.java)
        registration.registerBlockComponent(BcJadeProvider, CreativeEngineBlock::class.java)
        registration.registerBlockComponent(BcJadeProvider, PumpBlock::class.java)
    }
}

object BcJadeProvider : IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    private val BC_INFO = Identifier.of("buildcraft_addon", "bc_info")

    override fun appendServerData(data: NbtCompound, accessor: BlockAccessor) {
        when (val be = accessor.blockEntity) {
            is RedstoneEngineBlockEntity -> {
                data.putString("kind", "redstone_engine")
                data.putDouble("heat", be.heat)
                data.putString("stage", be.currentStage.name)
                data.putInt("outputKu", be.pendingOutputKu)
            }
            is StoneEngineBlockEntity -> {
                data.putString("kind", "stone_engine")
                data.putDouble("heat", be.heat)
                data.putString("stage", be.currentStage.name)
                data.putInt("burnTime", be.burnTime)
                data.putInt("totalBurnTime", be.totalBurnTime)
                data.putInt("outputKu", be.pendingOutputKu)
            }
            is IronEngineBlockEntity -> {
                data.putString("kind", "iron_engine")
                data.putDouble("heat", be.heat)
                data.putString("stage", be.currentStage.name)
                data.putBoolean("isBurning", be.isBurning)
                data.putInt("fuelAmount", be.fuelAmount)
                data.putInt("coolantAmount", be.coolantAmount)
                data.putDouble("powerPerCycle", be.currentPowerPerCycle)
                data.putInt("outputKu", be.pendingOutputKu)
            }
            is RFEngineBlockEntity -> {
                data.putString("kind", "rf_engine")
                data.putDouble("heat", be.heat)
                data.putString("stage", be.currentStage.name)
                data.putBoolean("isBurning", be.isBurning)
                data.putInt("currentRF", be.currentRF)
                data.putInt("outputKu", be.pendingOutputKu)
            }
            is CreativeEngineBlockEntity -> {
                data.putString("kind", "creative_engine")
                data.putInt("outputIndex", be.outputIndex)
                data.putInt("outputMJ", be.getCurrentOutputMJ())
                data.putInt("outputKu", be.pendingOutputKu)
            }
            is PumpBlockEntity -> {
                data.putString("kind", "pump")
                data.putLong("tankAmount", be.tank.amount)
                data.putLong("tankCapacity", be.tank.capacity)
                data.putBoolean("isActive", be.isActive)
                data.putBoolean("isBlocked", be.isBlocked)
                if (!be.tank.variant.isBlank) {
                    data.putString("tankFluid", net.minecraft.registry.Registries.FLUID.getId(be.tank.variant.fluid).toString())
                }
            }
        }
    }

    override fun appendTooltip(tooltip: ITooltip, accessor: BlockAccessor, config: IPluginConfig) {
        val kind = accessor.serverData.getString("kind")
        when (kind) {
            "redstone_engine", "stone_engine", "iron_engine", "rf_engine" -> {
                val heat = accessor.serverData.getDouble("heat")
                val stage = accessor.serverData.getString("stage")
                val outputKu = accessor.serverData.getInt("outputKu")
                tooltip.add(heatAndStageLine(heat, stage))
                if (outputKu > 0) {
                    tooltip.add(Text.translatable("buildcraft_addon.jade.engine_output_ku", outputKu).setStyle(Style.EMPTY.withColor(Formatting.GREEN)))
                } else {
                    tooltip.add(Text.translatable("buildcraft_addon.jade.engine_idle").setStyle(Style.EMPTY.withColor(Formatting.GRAY)))
                }
                when (kind) {
                    "stone_engine" -> {
                        val burnTime = accessor.serverData.getInt("burnTime")
                        val totalBurnTime = accessor.serverData.getInt("totalBurnTime")
                        if (totalBurnTime > 0) {
                            val secs = String.format("%.1f", burnTime / 20.0)
                            val totalSecs = String.format("%.1f", totalBurnTime / 20.0)
                            tooltip.add(Text.translatable("buildcraft_addon.jade.engine_fuel_time", secs, totalSecs).setStyle(Style.EMPTY.withColor(Formatting.YELLOW)))
                        }
                    }
                    "iron_engine" -> {
                        val fuelAmount = accessor.serverData.getInt("fuelAmount")
                        val coolantAmount = accessor.serverData.getInt("coolantAmount")
                        val powerPerCycle = accessor.serverData.getDouble("powerPerCycle")
                        if (powerPerCycle > 0) {
                            val mj = String.format("%.1f", powerPerCycle)
                            val ku = (powerPerCycle * 128).toInt()
                            tooltip.add(Text.translatable("buildcraft_addon.jade.engine_fuel_power", mj, ku).setStyle(Style.EMPTY.withColor(Formatting.YELLOW)))
                        }
                        tooltip.add(Text.translatable("buildcraft_addon.jade.engine_fuel_tank", fuelAmount, 10000).setStyle(Style.EMPTY.withColor(Formatting.GOLD)))
                        if (coolantAmount > 0) {
                            tooltip.add(Text.translatable("buildcraft_addon.jade.engine_coolant", coolantAmount).setStyle(Style.EMPTY.withColor(Formatting.AQUA)))
                        } else {
                            tooltip.add(Text.translatable("buildcraft_addon.jade.engine_no_coolant").setStyle(Style.EMPTY.withColor(Formatting.RED)))
                        }
                    }
                    "rf_engine" -> {
                        val currentRF = accessor.serverData.getInt("currentRF")
                        tooltip.add(Text.translatable("buildcraft_addon.jade.engine_rf", currentRF, 10000).setStyle(Style.EMPTY.withColor(Formatting.GOLD)))
                    }
                }
            }
            "creative_engine" -> {
                val outputMJ = accessor.serverData.getInt("outputMJ")
                val outputKu = accessor.serverData.getInt("outputKu")
                val level = accessor.serverData.getInt("outputIndex") + 1
                if (outputKu > 0) {
                    tooltip.add(Text.translatable("buildcraft_addon.jade.creative_output", level, 9, outputMJ, outputKu).setStyle(Style.EMPTY.withColor(Formatting.GREEN)))
                } else {
                    tooltip.add(Text.translatable("buildcraft_addon.jade.creative_standby", level, 9).setStyle(Style.EMPTY.withColor(Formatting.GRAY)))
                }
            }
            "pump" -> {
                val tankAmount = accessor.serverData.getLong("tankAmount")
                val tankCapacity = accessor.serverData.getLong("tankCapacity")
                val isActive = accessor.serverData.getBoolean("isActive")
                val isBlocked = accessor.serverData.getBoolean("isBlocked")
                val fluidName = if (accessor.serverData.contains("tankFluid")) {
                    val fid = accessor.serverData.getString("tankFluid")
                    val id = Identifier.tryParse(fid)
                    if (id != null) {
                        val fluid = net.minecraft.registry.Registries.FLUID.get(id)
                        Registries.FLUID.getId(fluid).toTranslationKey().let { Text.translatable("block.$it") }
                    } else null
                } else null

                when {
                    isBlocked -> tooltip.add(Text.translatable("buildcraft_addon.jade.pump_blocked").setStyle(Style.EMPTY.withColor(Formatting.RED)))
                    isActive -> tooltip.add(Text.translatable("buildcraft_addon.jade.pump_active").setStyle(Style.EMPTY.withColor(Formatting.GREEN)))
                    else -> tooltip.add(Text.translatable("buildcraft_addon.jade.pump_idle").setStyle(Style.EMPTY.withColor(Formatting.GRAY)))
                }
                if (fluidName != null) {
                    tooltip.add(Text.translatable("buildcraft_addon.jade.pump_fluid", fluidName, tankAmount / net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET, tankCapacity / net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET).setStyle(Style.EMPTY.withColor(Formatting.AQUA)))
                } else {
                    tooltip.add(Text.translatable("buildcraft_addon.jade.pump_empty").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY)))
                }
            }
        }
    }

    override fun getUid(): Identifier = BC_INFO

    private fun heatAndStageLine(heat: Double, stage: String): Text {
        val color = when (stage) {
            "BLUE" -> Formatting.AQUA
            "GREEN" -> Formatting.GREEN
            "YELLOW" -> Formatting.YELLOW
            "RED" -> Formatting.RED
            "OVERHEAT" -> Formatting.DARK_RED
            "BLACK" -> Formatting.DARK_GRAY
            else -> Formatting.GRAY
        }
        val stageText = when (stage) {
            "BLUE" -> "■"
            "GREEN" -> "■"
            "YELLOW" -> "■"
            "RED" -> "■"
            "OVERHEAT" -> "■"
            "BLACK" -> "■"
            else -> "■"
        }
        return Text.literal("")
            .append(Text.translatable("buildcraft_addon.jade.engine_heat", String.format("%.0f", heat)).setStyle(Style.EMPTY.withColor(Formatting.GRAY)))
            .append(Text.literal(" [").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY)))
            .append(Text.literal(stageText).setStyle(Style.EMPTY.withColor(color)))
            .append(Text.literal(" $stage]").setStyle(Style.EMPTY.withColor(color)))
    }
}
