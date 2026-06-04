---
navigation:
  title: Solar Generator
  parent: index.md
  position: 14
  icon: ic2_120:solar_generator
item_ids:
  - ic2_120:solar_generator
---

# Solar Generator

<BlockImage id="ic2_120:solar_generator" p:facing="north" p:active="true" scale="4" />

The Solar Generator is the simplest passive EU source: place it under open sky, wait for daylight, and it fills its internal buffer at 1 EU/t. It has no fuel slot and no upgrade slots, so its real output is controlled entirely by placement, time, weather, and whether the energy has somewhere to go.

Generation only runs in the Overworld from 6:20 AM to 5:45 PM. Rain or thunder stops it immediately. The column above the machine must not contain any full opaque blocks; transparent blocks do not block the sky check.

## Energy

- **Generation**: 1 EU/t while all sunlight conditions are met
- **Internal storage**: 400 EU
- **Output tier**: 1
- **External output**: bursts of up to 10 EU/t, delivered every ~10 ticks on average, from every side except the front face
- **Input**: none

The Solar Generator stores energy first. Cable output starts only after the buffer has at least 10 EU, then the generator releases 10 EU in a single burst and resumes accumulating. The effective long-term rate is still 1 EU/t, but delivery happens in larger, less frequent packets that pass LV line-loss checks more reliably. This makes it useful as a trickle charger for BatBoxes, nearby machines, or a small LV cable line, but it cannot accept EU from another source.

## Sunlight Conditions

- Must be in the Overworld.
- Must be between 333 and 11750 world-time ticks, roughly 6:20 AM to 5:45 PM.
- The world must not be raining.
- The space above the machine must have sky access with no full opaque block in the vertical column.

When any condition fails, the machine stops generating and its active state turns off, but stored EU remains in the buffer and can still be used until it runs out.

## Slot And Automation

The single slot accepts one chargeable battery or electric tool. Items in this slot are charged from the internal buffer at the machine's tier-1 rate.

Automation may insert valid chargeable items into the slot and extract from the same slot. This lets the machine feed both adjacent energy networks and item-based batteries, as long as the sunlight conditions keep replenishing the buffer.

## Recipe

<Recipe id="ic2_120:solar_generator" />
