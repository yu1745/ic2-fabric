package ic2_120.content.item

import ic2_120.content.reactor.AbstractDamageableReactorComponent
import ic2_120.content.reactor.AbstractReactorComponent
import ic2_120.content.reactor.IReactor
import ic2_120.content.reactor.IReactorComponent
import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.type
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.item.ItemStack

/**
 * 散热片基类：热容量 + 自身蒸发 + 吸堆温。
 * selfVent: 每周期自身蒸发热量；reactorVent: 每周期从堆吸收热量。
 */
abstract class ReactorHeatVentBase(
    settings: FabricItemSettings,
    heatStorage: Int,
    private val selfVent: Int,
    private val reactorVent: Int
) : AbstractDamageableReactorComponent(settings, heatStorage) {

    override fun canStoreHeat(stack: ItemStack, reactor: IReactor, x: Int, y: Int): Boolean = true

    override fun getMaxHeat(stack: ItemStack, reactor: IReactor, x: Int, y: Int): Int = maxUse

    override fun getCurrentHeat(stack: ItemStack, reactor: IReactor, x: Int, y: Int): Int = getUse(stack)

    override fun alterHeat(stack: ItemStack, reactor: IReactor, x: Int, y: Int, heat: Int): Int {
        var myHeat = getCurrentHeat(stack, reactor, x, y)
        myHeat += heat
        val max = getMaxHeat(stack, reactor, x, y)
        return if (myHeat > max) {
            reactor.setItemAt(x, y, null)
            max - myHeat + heat  // 返回未能吸收的热量（溢出）
        } else {
            if (myHeat < 0) {
                val overflow = myHeat
                myHeat = 0
                setUse(stack, 0)
                overflow
            } else {
                setUse(stack, myHeat)
                0
            }
        }
    }

    override fun processChamber(stack: ItemStack, reactor: IReactor, x: Int, y: Int, heatRun: Boolean) {
        if (!heatRun) return

        var totalDissipated = 0

        if (reactorVent > 0) {
            var rheat = reactor.getHeat()
            val reactorDrain = minOf(rheat, reactorVent)
            rheat -= reactorDrain
            if (alterHeat(stack, reactor, x, y, reactorDrain) > 0) return
            reactor.setHeat(rheat)
        }

        val dissipated = selfVent
        alterHeat(stack, reactor, x, y, -dissipated)
        totalDissipated += dissipated

        // 报告总散热
        reactor.addHeatDissipated(totalDissipated)
        // 报告槽位散热
        reactor.addSlotHeatInfo(x * 9 + y, 0, totalDissipated)
    }
}

@ModItem(name = "heat_vent", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class HeatVentItem : ReactorHeatVentBase(FabricItemSettings(), 1000, 6, 0)

@ModItem(name = "reactor_heat_vent", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class ReactorHeatVentItem : ReactorHeatVentBase(FabricItemSettings(), 1000, 5, 5)

@ModItem(name = "advanced_heat_vent", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class AdvancedHeatVentItem : ReactorHeatVentBase(FabricItemSettings(), 1000, 12, 0)

@ModItem(name = "overclocked_heat_vent", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class OverclockedHeatVentItem : ReactorHeatVentBase(FabricItemSettings(), 1000, 20, 36)

/** 元件散热片：无热容量，向四方向邻接可储热组件蒸发 4 点热量 */
@ModItem(name = "component_heat_vent", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class ComponentHeatVentItem(settings: FabricItemSettings = FabricItemSettings()) : AbstractReactorComponent(settings) {

    private val sideVent = 4

    override fun processChamber(stack: ItemStack, reactor: IReactor, x: Int, y: Int, heatRun: Boolean) {
        if (!heatRun) return
        var totalDissipated = 0
        totalDissipated += cool(reactor, x - 1, y, x, y)
        totalDissipated += cool(reactor, x + 1, y, x, y)
        totalDissipated += cool(reactor, x, y - 1, x, y)
        totalDissipated += cool(reactor, x, y + 1, x, y)

        // 报告总散热
        reactor.addHeatDissipated(totalDissipated)
        // 报告槽位散热（元件散热片自己没有散热，都是帮邻接散的）
        reactor.addSlotHeatInfo(x * 9 + y, 0, 0)
    }

    private fun cool(reactor: IReactor, targetX: Int, targetY: Int, sourceX: Int, sourceY: Int): Int {
        val other = reactor.getItemAt(targetX, targetY) ?: return 0
        if (other.item !is IReactorComponent) return 0
        val comp = other.item as IReactorComponent
        if (!comp.canStoreHeat(other, reactor, targetX, targetY)) return 0
        comp.alterHeat(other, reactor, targetX, targetY, -sideVent)
        // 记录散热量到被散热的组件槽位
        reactor.addSlotHeatInfo(targetX * 9 + targetY, 0, sideVent)
        return sideVent
    }
}
