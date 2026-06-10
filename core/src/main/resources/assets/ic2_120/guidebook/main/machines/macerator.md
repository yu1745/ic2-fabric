---
navigation:
  title: Macerator
  parent: index.md
  position: 29
  icon: ic2_120:macerator
item_ids:
  - ic2_120:macerator
---

# Macerator

<BlockImage id="ic2_120:macerator" p:facing="north" p:active="true" scale="4" />

The Macerator is the first machine in the ore processing chain. It grinds ores, raw metals, stone, plants, and finished materials into crushed ore, dust, or other smaller products.

For ores and raw metals, one input becomes two crushed ore items. Those crushed ores can be sent on to the Ore Washing Plant for purified crushed ore and byproducts, or handled by the rest of your processing line.

## Operation

- **Tier:** 1
- **EU Storage:** 416 EU
- **Base input:** up to 32 EU/t
- **Energy use:** 2 EU/t while working
- **Processing time:** 130 ticks, about 6.5 seconds
- **Total energy:** about 260 EU per operation before upgrades

The machine only starts when the input stack matches a macerating recipe and the output slot can accept the full result. If the input is missing, the recipe is invalid, or the output is blocked, progress resets to 0.

## Recipe

Notable recipe groups include:

- Iron, gold, copper, lead, tin, and uranium ores, including deepslate variants, to 2 crushed ore.
- Raw iron, raw gold, raw copper, raw lead, raw tin, and raw uranium to 2 crushed ore.
- Ingots and plates to their matching dusts.
- Coal, coal ore, coal blocks, clay blocks, netherrack, obsidian, lapis, and diamond to dust products.
- Cobblestone, stone, granite, diorite, and andesite into simpler stone materials.
- Plant materials into Bio Chaff, and poisonous potatoes or spider eyes into Grin Powder.

## Slots

- Upper left slot: recipe input.
- Lower left slot: discharging battery. It accepts one battery item and can draw from any battery tier 1 or higher.
- Middle right slot: output.
- Four slots on the far right: upgrades.

Shift-clicking routes upgrades to the upgrade slots, batteries to the battery slot, valid recipe inputs to the input slot, and machine contents back to the player inventory.

## Upgrades and Automation

The Macerator supports Overclocker, Transformer, Energy Storage, Ejector, and Pulling upgrades.

- Overclockers increase energy use and speed. Progress uses the integer part of the speed multiplier, so the first overclocker increases draw but the second and later overclockers begin shortening the 130-tick process.
- Transformer upgrades raise the accepted input tier by one level each, increasing the input limit from the base 32 EU/t.
- Energy Storage upgrades add 10,000 EU of buffer each.
- Ejector upgrades push items from the output slot into adjacent inventories, with optional item filter and side settings.
- Pulling upgrades pull valid recipe inputs from adjacent inventories, also using the item filter and side settings.

External item automation can insert valid inputs, insert batteries, insert upgrade items, and extract only the output slot.

## Recipe

<Recipe id="ic2_120:macerator" />
