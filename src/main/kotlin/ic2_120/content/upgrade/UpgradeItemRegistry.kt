package ic2_120.content.upgrade

import ic2_120.content.item.*
import net.minecraft.item.Item
import kotlin.reflect.KClass

/**
 * 升级物品与机器接口的映射表。
 *
 * 机器未实现对应接口时，该升级不允许放入升级槽。
 * 未在映射表中的升级物品不允许放入任何机器。
 */
object UpgradeItemRegistry {

    private val itemToInterface = mutableMapOf<KClass<out Item>, KClass<*>>()

    init {
        register(OverclockerUpgrade::class, IOverclockerUpgradeSupport::class)
        register(TransformerUpgrade::class, ITransformerUpgradeSupport::class)
        register(EnergyStorageUpgrade::class, IEnergyStorageUpgradeSupport::class)
        register(RedstoneInverterUpgrade::class, IRedstoneInverterUpgradeSupport::class)
        register(EjectorUpgrade::class, IEjectorUpgradeSupport::class)
        register(AdvancedEjectorUpgrade::class, IEjectorUpgradeSupport::class)
        register(PullingUpgrade::class, IEjectorUpgradeSupport::class)
        register(AdvancedPullingUpgrade::class, IEjectorUpgradeSupport::class)
        // 流体弹出/抽取升级走管道逻辑（IFluidPipeUpgradeSupport），与物品弹出升级（IEjectorUpgradeSupport）分开
        register(FluidEjectorUpgrade::class, IFluidPipeUpgradeSupport::class)
        register(FluidPullingUpgrade::class, IFluidPipeUpgradeSupport::class)
    }

    private fun register(itemClass: KClass<out Item>, interfaceClass: KClass<*>) {
        itemToInterface[itemClass] = interfaceClass
    }

    /**
     * 获取该升级物品对应的机器接口。
     * 若未注册则返回 null，该升级不允许放入任何机器。
     */
    fun getRequiredInterface(item: Item): KClass<*>? =
        itemToInterface[item::class]

    /**
     * 检查 [machine] 是否支持放入 [item] 类型的升级。
     */
    fun canAccept(machine: Any?, item: Item): Boolean {
        if (machine == null) return false
        val required = getRequiredInterface(item) ?: return false
        return required.java.isInstance(machine)
    }
}
