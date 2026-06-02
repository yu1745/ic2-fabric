---
navigation:
  title: Wind Kinetic Generator
  parent: index.md
  position: 22
  icon: ic2_120:wind_kinetic_generator
item_ids:
  - ic2_120:wind_kinetic_generator
---

# Wind Kinetic Generator

<BlockImage id="ic2_120:wind_kinetic_generator" p:facing="north" p:active="true" scale="4" />

The Wind Kinetic Generator turns wind into KU. It does not make EU by itself: feed its rear kinetic port into a <ItemLink id="ic2_120:kinetic_generator" /> or into a kinetic transmission line, then let the Kinetic Generator convert that motion into EU.

The rotor is mounted on the front face. The kinetic output port is on the opposite face, so a direct setup is:

- Wind Kinetic Generator front facing open air
- Kinetic Generator directly behind it
- Kinetic Generator front facing the Wind Kinetic Generator

## Rotor Slot

The machine accepts only these wind rotors:

| Rotor | Radius | Multiplier | Clear-weather lifetime |
|-------|--------|------------|------------------------|
| <ItemLink id="ic2_120:wooden_rotor" /> | 2 blocks | 1x | 3 h |
| <ItemLink id="ic2_120:iron_rotor" /> | 3 blocks | 2x | 24 h |
| <ItemLink id="ic2_120:steel_rotor" /> | 4 blocks | 3x | 48 h |
| <ItemLink id="ic2_120:carbon_rotor" /> | 5 blocks | 4x | 168 h |

Right-click with a valid rotor to install one. Right-click with an empty hand to remove the installed rotor. The slot holds one rotor and shift-clicking from the inventory inserts only one.

## Wind Strength

Effective wind is recalculated from height, chunk gusts, and weather:

`effectiveWind = meanWindAtY * chunkGust * weatherMultiplier`

Mean wind is highest around Y=150:

| Y level | Mean wind |
|---------|-----------|
| 150 | about 1.00 |
| 115 or 185 | about 0.62 |
| 64 | about 0.08 |
| 0 | about 0.03 |

The exact curve is `0.03 + 0.97 * exp(-(y - 150)^2 / (2 * 35^2))`.

Each chunk also gets a deterministic gust factor from 0.5 to 1.5. It updates every 200 ticks, with a per-chunk offset, so two wind machines in the same chunk share the same gust value while machines in different chunks can behave differently.

Weather multiplies the result:

| Weather | Multiplier |
|---------|------------|
| Clear | 1.0x |
| Rain | 1.2x |
| Thunder | 1.5x |

## Starting And Output

A rotor must reach its start threshold before it spins. Once active, it keeps running until wind falls below 85% of that threshold.

| Rotor | Start threshold | Stop threshold |
|-------|-----------------|----------------|
| Wooden | 0.10 | 0.085 |
| Iron | 0.20 | 0.170 |
| Steel | 0.30 | 0.255 |
| Carbon | 0.40 | 0.340 |

When the rotor is not blocked and wind is strong enough, generated KU is:

`floor(128 * rotorMultiplier * effectiveWind) KU/t`

At Y=150 with a carbon rotor, clear weather and a 1.0 gust gives 512 KU/t. The strongest normal case, thunder with a 1.5 gust, gives 1152 KU/t.

The guide and screen distinguish two values:

- Generated KU: what the wind and rotor can currently produce
- Output KU: what was actually extracted into an adjacent kinetic receiver or transmission network this tick

If nothing is connected to the rear port, the machine may show generated KU but output 0 KU/t.

## Obstructions

The rotor checks the space in front of the machine. It stops if either condition is true:

- An opaque full block intersects the current swept blade path in the rotor plane
- Another kinetic rotor provider has an installed rotor in the same plane and the two rotor discs overlap

Larger rotors need more clear space. A carbon rotor has the widest sweep and is the easiest to block. When blocked, generated and output KU become 0 until the obstruction is removed.

## Rotor Wear

Rotor durability is consumed only while KU is being generated. Clear weather uses 1 durability point per tick. Rain consumes 1.2x durability and thunder consumes 1.5x durability; fractional wear is saved on the rotor so long runs stay accurate.

The GUI lifetime is shown in clear-weather hours. A rotor that breaks during operation is removed from the slot.

## Kinetic Generator Link

The <ItemLink id="ic2_120:kinetic_generator" /> pulls KU through its front face and converts `4 KU = 1 EU`. Its per-tick KU intake is capped, and it becomes active at 64 KU/t, staying active down to 48 KU/t. The Wind Kinetic Generator's rear output can also feed shafts and bevel gears; their capacity and loss rules determine how much KU reaches the Kinetic Generator.

## Recipe

<Recipe id="ic2_120:wind_kinetic_generator" />

## Related

- <ItemLink id="ic2_120:wind_meter" />
- <ItemLink id="ic2_120:kinetic_generator" />
- <ItemLink id="ic2_120:wooden_rotor" />
- <ItemLink id="ic2_120:iron_rotor" />
- <ItemLink id="ic2_120:steel_rotor" />
- <ItemLink id="ic2_120:carbon_rotor" />
