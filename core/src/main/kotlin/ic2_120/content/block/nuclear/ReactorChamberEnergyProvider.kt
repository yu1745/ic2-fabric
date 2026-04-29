package ic2_120.content.block.nuclear

import net.minecraft.util.math.Direction
import team.reborn.energy.api.EnergyStorage

/**
 * 核反应仓能量存储提供者。
 * 与核反应堆相邻时，共享反应堆的能量实例。
 */
object ReactorChamberEnergyProvider {

    fun getEnergyStorage(be: ReactorChamberBlockEntity, side: Direction?): EnergyStorage? {
        return be.getEnergyStorage(side)
    }
}
