---
navigation:
  title: Nuclear Blocks and Components
  parent: index.md
  position: 216
  icon: ic2_120:nuclear_reactor
item_ids:
  - ic2_120:uranium_fuel_rod
  - ic2_120:dual_uranium_fuel_rod
  - ic2_120:quad_uranium_fuel_rod
  - ic2_120:mox_fuel_rod
  - ic2_120:dual_mox_fuel_rod
  - ic2_120:quad_mox_fuel_rod
  - ic2_120:depleted_uranium_fuel_rod
  - ic2_120:depleted_dual_uranium_fuel_rod
  - ic2_120:depleted_quad_uranium_fuel_rod
  - ic2_120:depleted_mox_fuel_rod
  - ic2_120:depleted_dual_mox_fuel_rod
  - ic2_120:depleted_quad_mox_fuel_rod
  - ic2_120:lithium_fuel_rod
  - ic2_120:depleted_isotope_fuel_rod
  - ic2_120:reactor_coolant_cell
  - ic2_120:triple_reactor_coolant_cell
  - ic2_120:sextuple_reactor_coolant_cell
  - ic2_120:heat_vent
  - ic2_120:reactor_heat_vent
  - ic2_120:overclocked_heat_vent
  - ic2_120:component_heat_vent
  - ic2_120:advanced_heat_vent
  - ic2_120:heat_exchanger
  - ic2_120:reactor_heat_exchanger
  - ic2_120:component_heat_exchanger
  - ic2_120:advanced_heat_exchanger
  - ic2_120:reactor_plating
  - ic2_120:reactor_heat_plating
  - ic2_120:containment_reactor_plating
  - ic2_120:neutron_reflector
  - ic2_120:thick_neutron_reflector
  - ic2_120:iridium_neutron_reflector
  - ic2_120:rsh_condensator
  - ic2_120:lzh_condensator
  - ic2_120:fuel_rod
  - ic2_120:uranium
  - ic2_120:uranium_235
  - ic2_120:uranium_238
  - ic2_120:uranium_pellet
  - ic2_120:mox
  - ic2_120:mox_pellet
  - ic2_120:plutonium
  - ic2_120:rtg_pellet
  - ic2_120:containment_box
---

# Nuclear Blocks and Components

Nuclear power is built from the reactor block, external ports, and internal components. Read the [Nuclear Reactor System](../systems/nuclear_reactor.md) overview first, then use this page as a component reference.

## Reactor Blocks

| Block | Use |
|------|------|
| Nuclear Reactor | The main block; holds fuel rods and cooling components |
| Reactor Chamber | Extends the reactor's interior space |
| Reactor Access Hatch | Automated input / output of reactor items |
| Reactor Fluid Port | In heat mode, inputs coolant and outputs hot coolant |
| Reactor Redstone Port | Use redstone to control or read reactor state |
| Reactor Pressure Vessel | The structural shell used in heat-mode reactors |

## Fuel Rods

- Uranium Fuel Rod, Dual Uranium Fuel Rod, and Quad Uranium Fuel Rod produce both EU and heat.
- MOX Fuel Rods scale up their output at high reactor temperatures, but carry higher design risk.
- Depleted Fuel Rods are by-products of the fuel cycle and are used for further processing.
- Lithium Fuel Rods and near-decay isotope rods are part of the advanced nuclear fuel chain.

## Cooling and Heat Exchange Components

| Component | Role |
|------|------|
| 10k / 30k / 60k Coolant Cells | Buffer heat; do not actively dissipate it |
| Heat Vent | Self-vent heat |
| Reactor Heat Vent | Pull heat from the reactor and self-vent |
| Overclocked Heat Vent | Aggressively pulls heat from the reactor; high thermal stress |
| Component Heat Vent | Cools adjacent storable components |
| Advanced Heat Vent | Higher self-vent capacity |
| Heat Exchanger family | Redistributes heat between the reactor, components, and adjacent components |

## Plating, Reflectors, and Condensators

- Reactor Plating adds heat capacity and reduces the explosion's impact.
- Containment Reactor Plating is biased toward heat capacity.
- Containment Reactor Plating (high-tolerance) is biased toward reducing incident severity.
- Neutron Reflectors reflect pulses from adjacent fuel rods; the Iridium Neutron Reflector never wears out.
- Redstone Condensator and Lapis Condensator absorb heat, but require their respective materials to repair.

## Safety Checklist

- When bringing up a new design, idle-check the cooling layout first, then add fuel gradually.
- A heat-mode reactor must keep its coolant supply and hot coolant output flowing freely.
- Once reactor temperature crosses the safety line, shut it down first; do not rebuild the internals while it is running.
