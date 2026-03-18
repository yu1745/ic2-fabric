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

    fun hasSelfVent(): Boolean = selfVent > 0

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

        val isThermal = reactor.isFluidCooled()
        var totalDissipated = 0

        // 从反应堆吸收热量（reactorVent - 不是散失热量）
        if (reactorVent > 0) {
            // 使用“有效堆温”防止同周期内多个散热片重复预扣同一份热量。
            val reactorDrain = minOf(reactor.getEffectiveHeatForDrain(), reactorVent)
            if (alterHeat(stack, reactor, x, y, reactorDrain) > 0) return
            // reactorVent 改回即时扣减，避免“先顶满再周期末回扣”造成温度振荡。
            reactor.setHeat(reactor.getHeat() - reactorDrain)
        }

        // 自身蒸发热量（selfVent - 这才是真正的散失热量）
        val dissipated = selfVent
        if (dissipated > 0) {
            if (isThermal) {
                // 热模式：记录到 addHeatDissipated（用于冷却液转换）
                // 耐久修复条件：检查冷却液是否充足
                val hasCoolant = reactor.hasCoolant()

                if (hasCoolant) {
                    // 有冷却液时，可以修复耐久
                    alterHeat(stack, reactor, x, y, -dissipated)
                }
                // 记录额定散热能力
                reactor.addHeatDissipated(dissipated)
            } else {
                // 电模式：正常蒸发，修复自身耐久
                alterHeat(stack, reactor, x, y, -dissipated)
                reactor.addHeatDissipated(dissipated)
            }
            totalDissipated += dissipated
        }

        // 报告总散热
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
        val isThermal = reactor.isFluidCooled()
        // 热模式下无冷却液时，不允许元件散热片“免费散热”。
        if (isThermal && !reactor.hasCoolant()) {
            reactor.addSlotHeatInfo(x * 9 + y, 0, 0)
            return
        }
        val scaledSideVent = sideVent
        var totalDissipated = 0
        totalDissipated += cool(reactor, x - 1, y, scaledSideVent)
        totalDissipated += cool(reactor, x + 1, y, scaledSideVent)
        totalDissipated += cool(reactor, x, y - 1, scaledSideVent)
        totalDissipated += cool(reactor, x, y + 1, scaledSideVent)

        // 报告总散热
        reactor.addHeatDissipated(totalDissipated)
        // 报告槽位散热（元件散热片自己没有散热，都是帮邻接散的）
        reactor.addSlotHeatInfo(x * 9 + y, 0, 0)
    }

    private fun cool(reactor: IReactor, targetX: Int, targetY: Int, sideVentAmount: Int): Int {
        val other = reactor.getItemAt(targetX, targetY) ?: return 0
        if (other.item !is IReactorComponent) return 0
        val comp = other.item as IReactorComponent
        if (!comp.canStoreHeat(other, reactor, targetX, targetY)) return 0
        comp.alterHeat(other, reactor, targetX, targetY, -sideVentAmount)
        // 记录散热量到被散热的组件槽位
        reactor.addSlotHeatInfo(targetX * 9 + targetY, 0, sideVentAmount)
        return sideVentAmount
    }
}
