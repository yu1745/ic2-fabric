package ic2_120_industrial_upgrade.content.block

import ic2_120_advanced_solar_addon.content.block.GenerationState
import ic2_120_advanced_solar_addon.content.block.SolarPanelBlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level

/**
 * Rain 变体太阳能发电机抽象基类（雨镜 rainlinse 升级）。
 * 行为对齐 1.4.0 的 TileEntityRainPanel：
 * 仅**下雨/雷雨**时发电（晴天、夜晚不发电），功率 = 同等级普通面板 nightPower。
 * storage/tier 与同等级普通面板一致。
 */
abstract class RainPanelBlockEntity(
    type: BlockEntityType<out RainPanelBlockEntity>,
    pos: BlockPos,
    state: BlockState,
    /** 同等级普通面板 dayPower（Rain 变体不使用，用于对照）。 */
    dayPower: Int,
    /** 同等级普通面板 nightPower（用于对照）。 */
    nightPower: Int,
    maxStorage: Long,
    tier: Int,
    /** 实际发电功率 = 普通面板 nightPower（传给父类 rainPower）。 */
    rainPower: Int,
    activeProperty: BooleanProperty
) : SolarPanelBlockEntity(
    type, pos, state,
    dayPower = dayPower, nightPower = rainPower,
    maxStorage = maxStorage, tier = tier,
    activeProperty = activeProperty,
    rainPower = rainPower
) {

    override fun checkSky() {
        val world = this.world ?: return
        val pos = this.pos

        if (!hasSkyAccess(world, pos)) {
            generationState = GenerationState.NONE
            markDirty()
            return
        }

        // Rain 变体仅在主世界生效（末地下雨面板不发电）
        if (world.registryKey != Level.OVERWORLD) {
            generationState = GenerationState.NONE
            markDirty()
            return
        }

        val isRaining = world.isRaining || world.isThundering
        val canRain = world.getBiome(pos).value().hasPrecipitation()

        generationState = if (canRain && isRaining) GenerationState.RAIN else GenerationState.NONE
        markDirty()
    }
}
