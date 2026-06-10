---
navigation:
  title: Kinetic System (KU)
  parent: index.md
  position: 217
  icon: minecraft:book
---

# Kinetic System (KU)

The current version implements a `KU` network independent from the EU grid.

## Core Parameters

- Unit: `KU/t`
- Fixed conversion: `4 KU = 1 EU`
- Main consumer: Kinetic Generator

## Transmission Components

| Component | Max Throughput | Loss |
|---|---:|---:|
| Wooden Shaft | 128 KU/t | 0 KU/block |
| Iron Shaft | 512 KU/t | 2 KU/block |
| Steel Shaft | 2048 KU/t | 1 KU/block |
| Carbon Fibre Shaft | 8192 KU/t | 0 KU/block |
| Bevel Gear | 2048 KU/t | 3 KU/block |

## Connections and Distribution

- Shafts only connect along their own axis
- Bevel Gears redirect by 90°
- Multi-source / multi-sink networks are solved globally; output is total supply minus path loss

## Wind Kinetic Generator

### Basics

- Rotor tiers: wood / iron / steel / carbon
- Tier multiplier: `1 / 2 / 3 / 4`
- Kinetic Generator max throughput: `2048 KU/t = 512 EU/t`
- Calibration point: a carbon rotor at `y=150`, clear weather, `gustFactor=1.0` outputs `512 KU/t` (i.e. `128 EU/t`)

### Wind Model

- Primarily influenced by absolute `Y` height
- Gaussian parameters: `mu=150`, `sigma=35`, `floor=0.03`
- Gust factor is updated once per `200 ticks` (10 seconds) per chunk
- Gust range: `0.5 ~ 1.5`
- Weather factor: clear `1.0`, rain `1.2`, thunderstorm `1.5`

### Operating Rules

- Blockage in front or overlapping turbine blades will stall the generator and stop output
- KU is output from the back of the machine into the kinetic network
- Rotors only consume durability while they are actually spinning and producing KU
- Output formula: `effectiveWind = mean(y) * gustFactor * weatherFactor`, `kuOut = baseKu * rotorMultiplier * effectiveWind`
- Startup threshold scales with the multiplier (wood/iron/steel/carbon = `1/2/3/4`); higher-tier rotors require stronger wind

### Rotor Durability (Clear-Weather Baseline)

- Wood: `3` hours
- Iron: `24` hours
- Steel: `48` hours
- Carbon: `168` hours

## Components

The kinetic transmission system uses the following components:

### Crank Handles
- [Wooden Crank Handle](../machines/wooden_crank_handle.md)
- [Iron Crank Handle](../machines/iron_crank_handle.md)
- [Steel Crank Handle](../machines/steel_crank_handle.md)
- [Carbon Crank Handle](../machines/carbon_crank_handle.md)

### Rotors
- [Wooden Rotor](../machines/wooden_rotor.md)
- [Iron Rotor](../machines/iron_rotor.md)
- [Steel Rotor](../machines/steel_rotor.md)
- [Carbon Rotor](../machines/carbon_rotor.md)

### Transmission Shafts
- [Wood Transmission Shaft](../machines/wood_transmission_shaft.md)
- [Iron Transmission Shaft](../machines/iron_transmission_shaft.md)
- [Steel Transmission Shaft](../machines/steel_transmission_shaft.md)
- [Carbon Fibre Transmission Shaft](../machines/carbon_transmission_shaft.md)

### Other
- [Helical Gear](../machines/bevel_gear.md) — changes rotation direction
- [Wind Meter](../machines/wind_meter.md) — measures wind speed for wind generators
