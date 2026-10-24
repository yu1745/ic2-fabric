package ic2_120_industrial_upgrade.content.block

import ic2_120_advanced_solar_addon.content.block.GenerationState
import ic2_120_advanced_solar_addon.content.block.SolarPanelBlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level

/**
 * Sun 变体太阳能发电机抽象基类（日光镜 sunlinse 升级）。
 * 行为对齐 1.4.0 的 TileEntitySunPanel：
 * 仅**晴天白天**发电（夜晚/下雨/雷雨不发电），功率 = 同等级普通面板 dayPower × 2。
 * storage/tier 与同等级普通面板一致。
 */
abstract class SunPanelBlockEntity(
    type: BlockEntityType<out SunPanelBlockEntity>,
    pos: BlockPos,
    state: BlockState,
    /** 同等级普通面板 dayPower（未放大，用于对照）。 */
    dayPower: Int,
    /** 同等级普通面板 nightPower（Sun 变体不使用，用于对照）。 */
    nightPower: Int,
    maxStorage: Long,
    tier: Int,
    /** 实际发电功率 = dayPower × 2。 */
    val sunPower: Int,
    activeProperty: BooleanProperty
) : SolarPanelBlockEntity(
    type, pos, state,
    dayPower = sunPower, nightPower = 0,
    maxStorage = maxStorage, tier = tier,
    activeProperty = activeProperty,
    // Sun 变体产能 = dayPower×2，可能超过 tier 标称输出（如 Ultimate: sunPower=1024 > tier3=512），
    // 将每 tick 输出上限抬高到至少等于产能，避免积压；产能速度本身不变。
    maxOutput = maxOf(ic2_120.content.energy.EnergyTier.euPerTickFromTier(tier), sunPower.toLong())
) {

    override fun checkSky() {
        val world = this.world ?: return
        val pos = this.pos

        if (!hasSkyAccess(world, pos)) {
            generationState = GenerationState.NONE
            markDirty()
            return
        }

        // 末地永远白天、无天气变化，直接按白天发电（与原版 Sun 面板行为一致）
        if (world.registryKey == Level.END) {
            generationState = GenerationState.DAY
            markDirty()
            return
        }

        if (world.registryKey != Level.OVERWORLD) {
            generationState = GenerationState.NONE
            markDirty()
            return
        }

        val time = world.timeOfDay % 24000
        val isRaining = world.isRaining || world.isThundering
        val canRain = world.getBiome(pos).value().hasPrecipitation()
        val isDaytime = time in DAY_START_TICK..DAY_END_TICK

        // 晴天白天才发电：白天 && !(可下雨 && 正在下雨/雷雨)
        generationState =
            if (isDaytime && (!canRain || !isRaining))
                GenerationState.DAY
            else
                GenerationState.NONE
        markDirty()
    }
}
