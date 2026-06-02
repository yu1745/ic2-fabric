---
navigation:
  title: Radioisotope Thermoelectric Generator
  parent: index.md
  position: 16
  icon: ic2_120:rt_generator
item_ids:
  - ic2_120:rt_generator
---

# Radioisotope Thermoelectric Generator

<BlockImage id="ic2_120:rt_generator" p:facing="north" p:active="true" scale="4" />

The Radioisotope Thermoelectric Generator turns <ItemLink id="ic2_120:rtg_pellet" /> heat into EU directly. It is the electric RTG machine: it does not make HU, does not need a Stirling Generator, and does not connect by heat faces.

The machine has six pellet slots. Each slot accepts one RTG Pellet, and the pellets are not consumed. Once loaded, they keep generating until removed by a player or automation.

## EU Output

Output depends only on how many pellet slots are filled:

| Pellets | Generation |
|------|------|
| 1 | 1 EU/t |
| 2 | 2 EU/t |
| 3 | 4 EU/t |
| 4 | 8 EU/t |
| 5 | 16 EU/t |
| 6 | 32 EU/t |

The RT Generator stores up to 20,000 EU internally. If the buffer is full, new EU is not added until some energy is extracted. Its energy tier is 1, and the whole machine can output up to 32 EU/t total per tick.

The machine accepts no EU input. EU can be extracted from every side except the block's front face.

## Slots

- Pellet grid: 6 slots, one RTG Pellet per slot
- No output slot: pellets are not consumed and do not produce waste

## Automation

Item automation can insert <ItemLink id="ic2_120:rtg_pellet" /> into empty pellet slots. Each slot is limited to one pellet. Automation can also extract the pellets again, which is how you lower or shut off the generator.

Energy automation should treat the RT Generator as an output-only EU source. Use any side except the front face for extraction.

## RTG Heat Generator

The <ItemLink id="ic2_120:rt_heat_generator" /> uses the same pellets but outputs HU instead of EU. Its output is twice the electric RT Generator's number for the same pellet count, from 2 HU/t with one pellet to 64 HU/t with six pellets, and it has no heat buffer. Use the heat version when a machine specifically consumes HU; use this RT Generator when you want direct EU storage and output.

## Recipe

<Recipe id="ic2_120:rt_generator" />
