package ic2_120.content.upgrade

import net.minecraft.item.Item
import net.minecraft.util.math.Direction

/**
 * 机器支持弹出/抽入升级的接口。
 */
interface IEjectorUpgradeSupport {
    var itemEjectorEnabled: Boolean
    var itemEjectorFilter: Item?
    var itemEjectorSide: Direction?
}
