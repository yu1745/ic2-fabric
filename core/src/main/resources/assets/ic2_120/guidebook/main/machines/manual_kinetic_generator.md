---
navigation:
  title: Manual Kinetic Generator
  parent: index.md
  position: 21
  icon: ic2_120:manual_kinetic_generator
item_ids:
  - ic2_120:manual_kinetic_generator
---

# Manual Kinetic Generator

<BlockImage id="ic2_120:manual_kinetic_generator" p:facing="north" p:active="true" scale="4" />

The Manual Kinetic Generator produces kinetic energy (KU) when a player right-clicks it while holding a crank handle. A crank must be inserted into the machine first by right-clicking with the crank. Once installed, right-clicking with an empty hand starts cranking, generating KU per tick.

Different crank materials provide different KU output. The generated KU is output to adjacent kinetic machines, transmission shafts, or gears.

## Output

- **KU Output**: Variable (depends on crank material)
- **Tier**: 1
- **Output Sides**: horizontal faces

### Crank Materials

| Wooden | Iron | Steel | Carbon |
|:-----:|:----:|:-----:|:------:|
| <ItemImage id="ic2_120:wooden_crank_handle" scale="2" /> | <ItemImage id="ic2_120:iron_crank_handle" scale="2" /> | <ItemImage id="ic2_120:steel_crank_handle" scale="2" /> | <ItemImage id="ic2_120:carbon_crank_handle" scale="2" /> |
| 64 KU/t | 128 KU/t | 256 KU/t | 512 KU/t |

## Cranking Mechanics

While a crank is installed, right-clicking the generator with an empty hand starts continuous KU production at the rated output. The player must remain nearby and the generator produces KU each tick for as long as the cranking action is sustained.

Higher-tier cranks produce significantly more power but require proportionally more investment in materials to craft.

## Slots

- Crank slot: holds the installed crank handle

Players can swap cranks by right-clicking with a new crank while one is installed.

## Recipe

<Recipe id="ic2_120:manual_kinetic_generator" />

## Related

- <ItemLink id="ic2_120:wooden_crank_handle" />
- <ItemLink id="ic2_120:iron_crank_handle" />
- <ItemLink id="ic2_120:steel_crank_handle" />
- <ItemLink id="ic2_120:carbon_crank_handle" />
- <ItemLink id="ic2_120:wood_transmission_shaft" />
- <ItemLink id="ic2_120:iron_transmission_shaft" />
- <ItemLink id="ic2_120:steel_transmission_shaft" />
- <ItemLink id="ic2_120:carbon_transmission_shaft" />
- <ItemLink id="ic2_120:bevel_gear" />
