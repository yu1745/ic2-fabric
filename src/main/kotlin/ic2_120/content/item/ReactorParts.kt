package ic2_120.content.item

import ic2_120.content.reactor.AbstractReactorComponent
import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.type
import net.minecraft.item.Item
import net.fabricmc.fabric.api.item.v1.FabricItemSettings

// ========== 反应堆核心部件 ==========

@ModItem(name = "reactor_coolant_cell", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class ReactorCoolantCellItem : Item(FabricItemSettings())

@ModItem(name = "triple_reactor_coolant_cell", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class TripleReactorCoolantCellItem : Item(FabricItemSettings())

@ModItem(name = "sextuple_reactor_coolant_cell", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class SextupleReactorCoolantCellItem : Item(FabricItemSettings())

@ModItem(name = "reactor_plating", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class ReactorPlatingItem : Item(FabricItemSettings())

@ModItem(name = "reactor_heat_plating", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class ReactorHeatPlatingItem : Item(FabricItemSettings())

@ModItem(name = "containment_reactor_plating", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class ContainmentReactorPlatingItem : Item(FabricItemSettings())

@ModItem(name = "neutron_reflector", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class NeutronReflectorItem : AbstractReactorComponent(FabricItemSettings()) {
    override fun acceptUraniumPulse(
        stack: net.minecraft.item.ItemStack,
        reactor: ic2_120.content.reactor.IReactor,
        pulsingStack: net.minecraft.item.ItemStack,
        youX: Int,
        youY: Int,
        pulseX: Int,
        pulseY: Int,
        heatRun: Boolean
    ): Boolean = true
}

@ModItem(name = "thick_neutron_reflector", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class ThickNeutronReflectorItem : AbstractReactorComponent(FabricItemSettings()) {
    override fun acceptUraniumPulse(
        stack: net.minecraft.item.ItemStack,
        reactor: ic2_120.content.reactor.IReactor,
        pulsingStack: net.minecraft.item.ItemStack,
        youX: Int,
        youY: Int,
        pulseX: Int,
        pulseY: Int,
        heatRun: Boolean
    ): Boolean = true
}

@ModItem(name = "iridium_neutron_reflector", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class IridiumNeutronReflectorItem : AbstractReactorComponent(FabricItemSettings()) {
    override fun acceptUraniumPulse(
        stack: net.minecraft.item.ItemStack,
        reactor: ic2_120.content.reactor.IReactor,
        pulsingStack: net.minecraft.item.ItemStack,
        youX: Int,
        youY: Int,
        pulseX: Int,
        pulseY: Int,
        heatRun: Boolean
    ): Boolean = true
}

@ModItem(name = "rsh_condensator", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class RshCondensatorItem : Item(FabricItemSettings())

@ModItem(name = "lzh_condensator", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class LzhCondensatorItem : Item(FabricItemSettings())

@ModItem(name = "lithium_fuel_rod", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class LithiumFuelRodItem : Item(FabricItemSettings())

@ModItem(name = "depleted_isotope_fuel_rod", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class DepletedIsotopeFuelRodItem : Item(FabricItemSettings())

@ModItem(name = "heatpack", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class HeatpackItem : Item(FabricItemSettings())
