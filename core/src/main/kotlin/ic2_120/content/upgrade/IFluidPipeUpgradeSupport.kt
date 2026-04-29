package ic2_120.content.upgrade

import net.minecraft.fluid.Fluid
import net.minecraft.util.math.Direction

/**
 * 机器参与管道流体网络分配的能力接口。
 * 仅在安装对应流体升级后才会被管道网识别。
 */
interface IFluidPipeUpgradeSupport {
    var fluidPipeProviderEnabled: Boolean
    var fluidPipeReceiverEnabled: Boolean
    var fluidPipeProviderFilter: Fluid?
    var fluidPipeReceiverFilter: Fluid?
    var fluidPipeProviderSide: Direction?
    var fluidPipeReceiverSide: Direction?
}
