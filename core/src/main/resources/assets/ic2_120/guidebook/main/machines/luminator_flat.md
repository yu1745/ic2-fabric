---
navigation:
  title: Luminator
  parent: index.md
  position: 44
  icon: ic2_120:luminator_flat
item_ids:
  - ic2_120:luminator_flat
---

# Luminator

<BlockImage id="ic2_120:luminator_flat" scale="4" />

The Luminator is a cable-powered flat lamp. It emits light level 15 while it has EU, and goes dark shortly after the supply stops.

## Operation

- **Storage:** 100 EU
- **Input:** up to 8192 EU/t
- **Consumption:** 1 EU every 4 ticks, or 5 EU/s
- **Light:** level 15 while active

The Luminator has no GUI, no item slots, and no internal battery behavior beyond its tiny 100 EU buffer. Connect it to an EU line and it will keep itself lit; disconnect the line and it will normally shut off within four ticks.

Because it accepts very high voltage but consumes almost nothing, it can be placed anywhere on a powered cable run without needing a transformer chain just for lighting.

## Recipe

<Recipe id="ic2_120:luminator_flat" />
