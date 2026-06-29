---
navigation:
  title: Heat System (HU)
  parent: index.md
  position: 216
  icon: minecraft:book
---

# Heat System (HU)

The heat system uses **HU** (Heat Unit) as its energy unit. Unlike the EU grid, heat cannot be transmitted through a network; it must be transferred between directly adjacent machines.

---

## Basic Rules

- **No heat network**: heat cannot be transmitted through cables or pipes
- **Direct adjacency**: two machines must be touching to exchange heat
- **Rear-face transfer only**: every heat machine's heat face is its **back** (the side facing the partner)
- **No heat buffer**: any heat that is not consumed is lost
- **Activation condition**: requires both fuel/energy **and** an active heat consumer on the back face

---

## Heat Generators

Heat generators produce heat and transfer it to an adjacent consumer.

### Fluid Heat Generator

- **Machine ID**: `fluid_heat_generator`
- **Fuel**: Biofuel
- **Heat output**: 32 HU/t (instant)
- **Fuel consumption**: 10 mB/s
- **Heat buffer**: **none** (settled once per second; 640 HU is produced instantly, and any untransferred heat is lost)
- **Voltage tier**: Tier 4
- **Wiring and facing**: input on the front face only; all other sides are output

### Electric Heat Generator

- **Machine ID**: `electric_heat_generator`
- **Internal coil slots**: 10 slots, 1 coil per slot
- **Heat per coil**: 10 HU/t (100 HU/t with all coils filled)
- **Conversion ratio**: 1 EU = 1 HU
- **Energy buffer**: 10,000 EU
- **Heat buffer**: **none** (consumed EU is converted directly to HU)
- **Redstone control**: stops on signal (invertible with a Redstone Inverter Upgrade)
- **Facing**: horizontal; heat face is the back

### Solid Heat Generator

- **Machine ID**: `solid_heat_generator`
- **Heat output**: 20 HU/t (instant)
- **Fuel**: coal / charcoal
- **Heat per fuel item**: 8,000 HU (400 ticks)
- **Heat buffer**: **none** (20 HU produced per tick while burning)

---

## Heat Transfer Rules in Detail

### Heat Face

Every heat machine's heat face is its **back**. For a successful transfer, both machines must:
1. Occupy adjacent block positions
2. Have their heat faces touching (the back of A against the back of B)

### Activation State

A heat machine is active only when both conditions are met:
- **Has usable fuel/energy**: the fluid heat generator has biofuel, the electric heat generator has EU, or the solid heat generator is actively burning
- **Active consumer on the back face**: heat faces are touching

If either condition fails, the machine stays active and will not consume fuel, even when fuel is present.

### Impact of No Heat Buffer

Because heat cannot be buffered, any heat produced while the back face has no consumer — or while the consumer is full — is wasted entirely. When you lay out your heat machines, always make sure the back face is connected to something that can accept heat (primarily a nuclear reactor).

---

## Working with the Nuclear Reactor

The main use of the heat system is feeding heat into a nuclear reactor (fluid mode):

- The Fluid Heat Generator is a good fit for continuous biofuel operation
- The Electric Heat Generator is a good fit for a high-voltage grid (pair it with Transformer Upgrades)
- The Solid Heat Generator is a good fit for burning off excess coal stockpiles

Make sure every heat generator's back face is aimed squarely at the nuclear reactor's fluid port or another heat consumer.
