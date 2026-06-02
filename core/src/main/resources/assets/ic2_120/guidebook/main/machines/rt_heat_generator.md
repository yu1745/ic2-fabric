---
navigation:
  title: RTG Heat Generator
  parent: index.md
  position: 56
  icon: ic2_120:rt_heat_generator
item_ids:
  - ic2_120:rt_heat_generator
---

# RTG Heat Generator

<BlockImage id="ic2_120:rt_heat_generator" scale="4" />

The Radioisotope Thermoelectric (RTG) Heat Generator is a passive HU source powered by <ItemLink id="ic2_120:rtg_pellet" />. It does not accept EU, fluid, burnable fuel, or uranium fuel rods.

## Heat Output

The generator has six pellet slots. Each slot accepts one RTG Pellet, and the output doubles with each filled slot:

| Pellets | Output |
|------|------|
| 1 | 2 HU/t |
| 2 | 4 HU/t |
| 3 | 8 HU/t |
| 4 | 16 HU/t |
| 5 | 32 HU/t |
| 6 | 64 HU/t |

RTG Pellets are not consumed and never turn into depleted items. Once inserted, they keep their slot filled until removed by a player or automation.

The machine has no heat buffer. HU is offered directly to the adjacent consumer each tick, and any HU the consumer does not accept during that tick is lost. If no correctly aligned heat consumer is connected, no HU is generated or transferred, and the pellets remain untouched.

## Heat Face

HU leaves only through the generator's single heat-transfer face, not through every side. Place the receiving machine directly against that face, and make sure the receiving machine's own heat-transfer face points back at the RTG Heat Generator.

The block shows as active whenever it contains at least one RTG Pellet, but heat transfer still requires the face-to-face connection.

## Slots

- Pellet grid: 6 slots, one RTG Pellet per slot
- No output slot: pellets are not consumed and do not produce waste

## Automation

Item automation can insert <ItemLink id="ic2_120:rtg_pellet" /> into empty pellet slots. Each slot is limited to one pellet. Automation can also extract the pellets again, which is useful when changing the heat output of an installation.

Common heat consumers include:

- <ItemLink id="ic2_120:steam_generator" />
- <ItemLink id="ic2_120:stirling_generator" />
- <ItemLink id="ic2_120:blast_furnace" />
- <ItemLink id="ic2_120:fermenter" />

## Recipe

<Recipe id="ic2_120:rt_heat_generator" />
