---
navigation:
  title: Electric Furnace
  parent: index.md
  position: 27
  icon: ic2_120:electric_furnace
item_ids:
  - ic2_120:electric_furnace
---

# Electric Furnace

<BlockImage id="ic2_120:electric_furnace" p:facing="north" p:active="true" scale="4" />

The Electric Furnace is the EU-powered step after the Iron Furnace. It uses the same vanilla smelting recipes as a regular furnace, stores the recipe experience internally, and needs no solid fuel.

It is a basic processing machine: simple, compact, and easy to automate. For parallel inputs and heated high-speed smelting, upgrade later to the Induction Furnace.

## Energy

- **Tier:** 1
- **Internal storage:** 416 EU
- **Maximum input:** 32 EU/t before transformer upgrades
- **Smelting cost:** 3 EU/t while progress is advancing
- **Base processing time:** 130 ticks, or 6.5 seconds
- **Base total cost:** 390 EU per smelt

Energy may come from the IC2 energy network or from a battery in the discharging slot. The machine does not output EU.

## Smelting

The input must have a vanilla `smelting` recipe. When the output slot is blocked, full, or contains a different item, progress resets and the furnace stops. Completed smelts add their normal furnace experience to the machine; collect it from the GUI, or manually take output to release the stored experience.

## Slots

- **Input:** items with a valid smelting recipe
- **Output:** smelted result; automation can extract from this slot
- **Battery:** one chargeable IC2 battery item for discharging into the furnace
- **Upgrades:** four upgrade slots

## Upgrades and Automation

The Electric Furnace accepts overclocker, energy storage, transformer, ejector, and pulling upgrades. Overclockers raise EU/t cost and apply a speed multiplier; progress advances in whole-number steps. Energy storage upgrades add 10,000 EU capacity each. Transformer upgrades raise the accepted input tier. Ejector upgrades push output to nearby inventories, while pulling upgrades pull valid smelting inputs from nearby inventories.

External item transfer can insert valid upgrades, one battery, or valid smelting inputs from any side, and can extract only the output slot.

## Progression

Compared with the Iron Furnace, the Electric Furnace trades fuel handling for EU power and uses 130 ticks instead of the Iron Furnace's 160-tick cook time. Compared with the Induction Furnace, it has one input and no heat system, but it is cheaper and starts working immediately whenever it has energy, a valid recipe, and space for output.

## Recipe

<Recipe id="ic2_120:electric_furnace" />
