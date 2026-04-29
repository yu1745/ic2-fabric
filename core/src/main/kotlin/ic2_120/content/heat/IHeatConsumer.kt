package ic2_120.content.heat

import net.minecraft.util.math.Direction

/**
 * 耗热机接口：由加热机向其注入 HU。
 */
interface IHeatConsumer : IHeatNode {
    fun receiveHeat(hu: Long, fromSide: Direction): Long
}

