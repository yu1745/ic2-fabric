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
import net.minecraft.nbt.CompoundTag
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.resources.Identifier
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

    private val BC_INFO = Identifier.fromNamespaceAndPath("buildcraft_addon", "bc_info")

    override fun appendServerData(data: CompoundTag, accessor: BlockAccessor) {
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
                    data.putString("tankFluid", net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(be.tank.variant.fluid).toString())
                }
            }
        }
    }

    override fun appendTooltip(tooltip: ITooltip, accessor: BlockAccessor, config: IPluginConfig) {
        val kind = accessor.serverData.getStringOr("kind", "")
        when (kind) {
            "redstone_engine", "stone_engine", "iron_engine", "rf_engine" -> {
                val heat = accessor.serverData.getDoubleOr("heat", 0.0)
                val stage = accessor.serverData.getStringOr("stage", "")
                val outputKu = accessor.serverData.getIntOr("outputKu", 0)
                tooltip.add(heatAndStageLine(heat, stage))
                if (outputKu > 0) {
                    tooltip.add(Component.translatable("buildcraft_addon.jade.engine_output_ku", outputKu).setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)))
                } else {
                    tooltip.add(Component.translatable("buildcraft_addon.jade.engine_idle").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)))
                }
                when (kind) {
                    "stone_engine" -> {
                        val burnTime = accessor.serverData.getIntOr("burnTime", 0)
                        val totalBurnTime = accessor.serverData.getIntOr("totalBurnTime", 0)
                        if (totalBurnTime > 0) {
                            val secs = String.format("%.1f", burnTime / 20.0)
                            val totalSecs = String.format("%.1f", totalBurnTime / 20.0)
                            tooltip.add(Component.translatable("buildcraft_addon.jade.engine_fuel_time", secs, totalSecs).setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)))
                        }
                    }
                    "iron_engine" -> {
                        val fuelAmount = accessor.serverData.getIntOr("fuelAmount", 0)
                        val coolantAmount = accessor.serverData.getIntOr("coolantAmount", 0)
                        val powerPerCycle = accessor.serverData.getDoubleOr("powerPerCycle", 0.0)
                        if (powerPerCycle > 0) {
                            val mj = String.format("%.1f", powerPerCycle)
                            val ku = (powerPerCycle * 128).toInt()
                            tooltip.add(Component.translatable("buildcraft_addon.jade.engine_fuel_power", mj, ku).setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)))
                        }
                        tooltip.add(Component.translatable("buildcraft_addon.jade.engine_fuel_tank", fuelAmount, 10000).setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)))
                        if (coolantAmount > 0) {
                            tooltip.add(Component.translatable("buildcraft_addon.jade.engine_coolant", coolantAmount).setStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)))
                        } else {
                            tooltip.add(Component.translatable("buildcraft_addon.jade.engine_no_coolant").setStyle(Style.EMPTY.withColor(ChatFormatting.RED)))
                        }
                    }
                    "rf_engine" -> {
                        val currentRF = accessor.serverData.getIntOr("currentRF", 0)
                        tooltip.add(Component.translatable("buildcraft_addon.jade.engine_rf", currentRF, 10000).setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)))
                    }
                }
            }
            "creative_engine" -> {
                val outputMJ = accessor.serverData.getIntOr("outputMJ", 0)
                val outputKu = accessor.serverData.getIntOr("outputKu", 0)
                val level = accessor.serverData.getIntOr("outputIndex", 0) + 1
                if (outputKu > 0) {
                    tooltip.add(Component.translatable("buildcraft_addon.jade.creative_output", level, 9, outputMJ, outputKu).setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)))
                } else {
                    tooltip.add(Component.translatable("buildcraft_addon.jade.creative_standby", level, 9).setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)))
                }
            }
            "pump" -> {
                val tankAmount = accessor.serverData.getLongOr("tankAmount", 0L)
                val tankCapacity = accessor.serverData.getLongOr("tankCapacity", 0L)
                val isActive = accessor.serverData.getBooleanOr("isActive", false)
                val isBlocked = accessor.serverData.getBooleanOr("isBlocked", false)
                val fluidName = if (accessor.serverData.contains("tankFluid")) {
                    val fid = accessor.serverData.getStringOr("tankFluid", "")
                    val id = Identifier.tryParse(fid)
                    if (id != null) {
                        val fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.getValue(id)
                        BuiltInRegistries.FLUID.getKey(fluid).toLanguageKey().let { Component.translatable("block.$it") }
                    } else null
                } else null

                when {
                    isBlocked -> tooltip.add(Component.translatable("buildcraft_addon.jade.pump_blocked").setStyle(Style.EMPTY.withColor(ChatFormatting.RED)))
                    isActive -> tooltip.add(Component.translatable("buildcraft_addon.jade.pump_active").setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)))
                    else -> tooltip.add(Component.translatable("buildcraft_addon.jade.pump_idle").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)))
                }
                if (fluidName != null) {
                    tooltip.add(Component.translatable("buildcraft_addon.jade.pump_fluid", fluidName, tankAmount / net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET, tankCapacity / net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET).setStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)))
                } else {
                    tooltip.add(Component.translatable("buildcraft_addon.jade.pump_empty").setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)))
                }
            }
        }
    }

    override fun getUid(): Identifier = BC_INFO

    private fun heatAndStageLine(heat: Double, stage: String): Component {
        val color = when (stage) {
            "BLUE" -> ChatFormatting.AQUA
            "GREEN" -> ChatFormatting.GREEN
            "YELLOW" -> ChatFormatting.YELLOW
            "RED" -> ChatFormatting.RED
            "OVERHEAT" -> ChatFormatting.DARK_RED
            "BLACK" -> ChatFormatting.DARK_GRAY
            else -> ChatFormatting.GRAY
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
        return Component.literal("")
            .append(Component.translatable("buildcraft_addon.jade.engine_heat", String.format("%.0f", heat)).setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)))
            .append(Component.literal(" [").setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)))
            .append(Component.literal(stageText).setStyle(Style.EMPTY.withColor(color)))
            .append(Component.literal(" $stage]").setStyle(Style.EMPTY.withColor(color)))
    }
}
