package ic2_120.integration.jade

import ic2_120.content.block.machines.TeleporterBlockEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import snownee.jade.api.BlockAccessor
import snownee.jade.api.IBlockComponentProvider
import snownee.jade.api.IServerDataProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.config.IPluginConfig

object TeleporterJadeProvider : IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    private val TELEPORTER_DIAG: Identifier = Identifier.of("ic2_120", "teleporter_diag")!!

    override fun appendServerData(data: NbtCompound, accessor: BlockAccessor) {
        val be = accessor.blockEntity as? TeleporterBlockEntity ?: return
        val world = accessor.level
        val pos = accessor.position
        val target = be.targetPos

        data.putLong("mfeEnergy", be.simulateAdjacentMfeEnergy(world, pos).coerceAtLeast(0L))
        data.putInt("cooldown", be.getTeleportCooldown().coerceAtLeast(0))
        data.putBoolean("targetSet", target != null && be.targetDimension.isNotBlank())
        if (target != null) {
            data.putInt("targetX", target.x)
            data.putInt("targetY", target.y)
            data.putInt("targetZ", target.z)
            data.putString("targetDim", be.targetDimension)
        }
        data.putBoolean("hasMfe", be.hasAdjacentMfeOrMfsu(world, pos))
        val thisDim = world.registryKey.value.toString()
        data.putBoolean("sameDim", target != null && thisDim == be.targetDimension)
        data.putBoolean("chunkLoaded", target != null && world.isChunkLoaded(target))
        val targetBe = target?.let { world.getBlockEntity(it) as? TeleporterBlockEntity }
        data.putBoolean("targetBeValid", targetBe != null)
        data.putBoolean("targetBeCooldown", targetBe?.getTeleportCooldown()?.let { it > 0 } ?: false)
        data.putBoolean("charging", be.isCharging())
        data.putBoolean("redstonePowered", world.isReceivingRedstonePower(pos))
        val box = be.getActivationBox(pos)
        data.putBoolean("entityInRange", !world.getOtherEntities(null, box) { it.isAlive && !it.hasVehicle() && !it.hasPassengers() }.isEmpty())
    }

    override fun appendTooltip(tooltip: ITooltip, accessor: BlockAccessor, config: IPluginConfig) {
        if (!accessor.serverData.contains("mfeEnergy")) return
        val data = accessor.serverData

        tooltip.add(Text.translatable("ic2_120.jade.teleporter.header").formatted(Formatting.WHITE))
        val mfeEnergy = data.getLong("mfeEnergy")
        val cooldown = data.getInt("cooldown")
        tooltip.add(Text.translatable("ic2_120.jade.teleporter.mfe_energy", formatEnergy(mfeEnergy)))
        if (cooldown > 0) tooltip.add(Text.translatable("ic2_120.jade.teleporter.cooldown", cooldown).formatted(Formatting.RED))

        if (data.getBoolean("targetSet")) {
            val tx = data.getInt("targetX"); val ty = data.getInt("targetY")
            val tz = data.getInt("targetZ"); val tdim = data.getString("targetDim").substringAfterLast(':')
            tooltip.add(Text.translatable("ic2_120.jade.teleporter.target", tx, ty, tz, tdim).setStyle(Style.EMPTY.withColor(Formatting.GRAY)))
        } else {
            tooltip.add(Text.translatable("ic2_120.jade.teleporter.target_none").formatted(Formatting.RED))
        }

        fun check(key: String, trans: String) {
            val ok = data.getBoolean(key)
            tooltip.add(Text.literal(if (ok) "  \u2713 " else "  \u2717 ")
                .append(Text.translatable(trans))
                .setStyle(Style.EMPTY.withColor(if (ok) Formatting.GREEN else Formatting.RED)))
        }
        check("hasMfe", "ic2_120.jade.teleporter.check.mfe")
        check("targetSet", "ic2_120.jade.teleporter.check.target")
        check("sameDim", "ic2_120.jade.teleporter.check.dimension")
        check("chunkLoaded", "ic2_120.jade.teleporter.check.chunk")
        check("targetBeValid", "ic2_120.jade.teleporter.check.target_be")
        if (data.getBoolean("targetBeCooldown"))
            tooltip.add(Text.literal("  \u2717 ").append(Text.translatable("ic2_120.jade.teleporter.check.target_be_cooldown")).formatted(Formatting.RED))
        check("redstonePowered", "ic2_120.jade.teleporter.check.redstone")
        check("entityInRange", "ic2_120.jade.teleporter.check.entity")
        check("charging", "ic2_120.jade.teleporter.check.charging")

        if (mfeEnergy < 10_000)
            tooltip.add(Text.literal("  \u2717 ").append(Text.translatable("ic2_120.jade.teleporter.check.energy_insufficient", formatEnergy(mfeEnergy))).formatted(Formatting.RED))
        else
            tooltip.add(Text.literal("  \u2713 ").append(Text.translatable("ic2_120.jade.teleporter.check.energy_sufficient", formatEnergy(mfeEnergy))).formatted(Formatting.GREEN))
    }

    private fun formatEnergy(value: Long): String = when {
        value >= 1_000_000_000 -> "${String.format("%.1f", value / 1_000_000_000.0)}G"
        value >= 1_000_000 -> "${String.format("%.1f", value / 1_000_000.0)}M"
        value >= 1_000 -> "${String.format("%.1f", value / 1_000.0)}k"
        else -> "$value"
    }

    override fun getUid(): Identifier = TELEPORTER_DIAG
}
