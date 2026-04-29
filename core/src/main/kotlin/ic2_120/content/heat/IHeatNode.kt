package ic2_120.content.heat

import net.minecraft.util.math.Direction

/**
 * 热能节点：只有一个传热面（背面），单位 HU。
 */
interface IHeatNode {
    fun getHeatTransferFace(): Direction
}

