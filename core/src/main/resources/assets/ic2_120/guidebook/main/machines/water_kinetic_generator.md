---
navigation:
  title: Water Kinetic Generator
  parent: index.md
  position: 23
  icon: ic2_120:water_kinetic_generator
item_ids:
  - ic2_120:water_kinetic_generator
---

# Water Kinetic Generator

<BlockImage id="ic2_120:water_kinetic_generator" p:facing="north" p:active="true" scale="4" />

The Water Kinetic Generator is a rotor-driven KU source. Install a <ItemLink id="ic2_120:wooden_rotor" />, <ItemLink id="ic2_120:iron_rotor" />, <ItemLink id="ic2_120:steel_rotor" />, or <ItemLink id="ic2_120:carbon_rotor" /> in its single rotor slot, then build the water around the rotor plane one block in front of the machine.

The rotor is on the front face. KU is available only from the back face, so place shafts or a <ItemLink id="ic2_120:kinetic_generator" /> behind the machine. The Kinetic Generator must face the incoming kinetic line and converts delivered KU at 4 KU = 1 EU.

## Water Setup

The generator runs only when every sampled point in the rotor's front plane is water. Missing water in that swept area stops generation. If at least one of those water blocks is flowing water, the generator receives the flowing-water bonus.

- **Still water only**: normal output and normal water-generator wear
- **Any flowing water in the sampled rotor area**: 1.5x output and 1.5x wear
- **Missing water in the sampled rotor area**: no KU

The required water area grows with rotor radius. Larger rotors make more KU, but they also need a larger clear water plane in front of the block.

## KU Output

**KU/t = floor(64 x rotorMultiplier x waterBonus)**

| Rotor | Radius | Multiplier | Still Water | Flowing Bonus |
|-------|--------|------------|-------------|---------------|
| Wooden | 1.0 | 1x | 64 KU/t | 96 KU/t |
| Iron | 1.5 | 2x | 128 KU/t | 192 KU/t |
| Steel | 2.0 | 3x | 192 KU/t | 288 KU/t |
| Carbon | 2.5 | 4x | 256 KU/t | 384 KU/t |

The screen shows both generated KU and output KU. Generated KU is what the water and rotor can produce this tick. Output KU is what was actually extracted by an adjacent Kinetic Generator or by the kinetic transmission network. If generated KU is positive but output KU is 0, the machine is spinning but nothing is pulling from its back face.

## Placement and Transmission

The Water Kinetic Generator does not store a long-term KU buffer. Each tick it offers the current generated KU from its back side, and any unused KU is gone on the next tick.

For a direct Kinetic Generator setup, put the Kinetic Generator directly behind the water machine and point the Kinetic Generator at it. With shafts, remember that path limits and losses apply before the KU reaches the consumer:

- <ItemLink id="ic2_120:wood_transmission_shaft" />: 128 KU/t, no path loss
- <ItemLink id="ic2_120:iron_transmission_shaft" />: 512 KU/t, 2 KU path loss per shaft
- <ItemLink id="ic2_120:steel_transmission_shaft" />: 2,048 KU/t, 1 KU path loss per shaft
- <ItemLink id="ic2_120:carbon_transmission_shaft" />: 8,192 KU/t, no path loss
- <ItemLink id="ic2_120:bevel_gear" />: 2,048 KU/t, 3 KU path loss

Use at least iron shafts for iron rotors and above if you want the full output to survive the line capacity. Steel or carbon shafts leave more room for several sources or longer routes.

## Rotor Wear

Rotors lose durability only while the machine is actively generating KU. Water generation applies a base wear rate of 2 durability per tick; flowing water raises that by another 1.5x. When the rotor reaches its limit, it breaks and the slot becomes empty.

The GUI lifetime readout is shown in hours under still-water conditions. Flowing water consumes the same rotor faster, so the real lifetime is about two-thirds of the displayed still-water estimate while the flow bonus is active.

## Blocking Detection

The rotor jams if an opaque full block intersects its blade path, or if another kinetic rotor overlaps the same rotor plane. A jammed rotor stops producing KU but keeps its current rotor installed. Clear the obstruction or move the overlapping generator to resume operation.

## Slots

- Rotor slot: accepts one wooden, iron, steel, or carbon rotor

Right-click the block with a valid rotor to install one quickly. Right-click with an empty hand to remove the installed rotor. Opening the screen also gives access to the same single slot, and shift-clicking valid rotors inserts one item.

## Recipe

<Recipe id="ic2_120:water_kinetic_generator" />

## Related

- <ItemLink id="ic2_120:wooden_rotor" />
- <ItemLink id="ic2_120:iron_rotor" />
- <ItemLink id="ic2_120:steel_rotor" />
- <ItemLink id="ic2_120:carbon_rotor" />
- <ItemLink id="ic2_120:kinetic_generator" />
- <ItemLink id="ic2_120:bevel_gear" />
