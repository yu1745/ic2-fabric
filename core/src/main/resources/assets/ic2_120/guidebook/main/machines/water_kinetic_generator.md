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

The Water Kinetic Generator converts water flow into kinetic energy (KU). It requires a rotor installed in its front face that spins when submerged in water. The rotor must be fully surrounded by water blocks on its front face plane.

Flowing water provides 1.5x more power than still water. Different rotor materials provide different power multipliers and radii (smaller than wind rotors). Rotors suffer wear over time and will eventually break.

KU is output from the back of the machine, which can be connected to kinetic transmission shafts or the Kinetic Generator.

## Water Flow Mechanics

**KU = floor(64 × rotorMultiplier × flowBonus)**

The flow bonus depends on whether the water surrounding the rotor is flowing or still:

- **Still water**: ×1.0
- **Flowing water**: ×1.5

Submersion is evaluated on the plane directly in front of the rotor. All blocks in the rotor's sweep area must be water for the generator to operate.

### Performance Examples (Carbon Rotor)

| Water Type | Flow Bonus | Rotor Mult | KU/t |
|-----------|-----------|-----------|------|
| Still | 1.0 | 4× | 256 |
| Flowing | 1.5 | 4× | 384 |

## Output

- **KU Output**: Variable (depends on rotor, water flow)
- **Rotor Slot**: 1 (wooden, iron, steel, or carbon rotor)
- **Tier**: 1
- **Water Flow Bonus**: 1.5× for flowing water

### Rotor Specifications

| Rotor | Radius | Multiplier |
|-------|--------|-----------|
| Wooden | 1.0 | 1× |
| Iron | 1.5 | 2× |
| Steel | 2.0 | 3× |
| Carbon | 2.5 | 4× |

Larger radius means the rotor sweeps a wider area and is more likely to be jammed by nearby blocks or other generators.

## Rotor Wear

Rotors wear down only while actively generating KU. The wear rate is multiplied by the flow multiplier (1.5× for flowing water). When fully worn, the rotor breaks and must be replaced. Check remaining lifetime in the GUI.

## Blocking Detection

The rotor will jam if a solid block is within its rotation radius on the front face plane. When jammed, the rotor stops spinning and no KU is generated. Clear the obstructing blocks to resume operation.

## Slots

- Rotor slot: holds the water rotor

Right-click with a rotor to install it, or right-click with an empty hand to remove it.

## Recipe

<Recipe id="ic2_120:water_kinetic_generator" />

## Related

- <ItemLink id="ic2_120:wooden_rotor" />
- <ItemLink id="ic2_120:iron_rotor" />
- <ItemLink id="ic2_120:steel_rotor" />
- <ItemLink id="ic2_120:carbon_rotor" />
- <ItemLink id="ic2_120:wind_meter" />
