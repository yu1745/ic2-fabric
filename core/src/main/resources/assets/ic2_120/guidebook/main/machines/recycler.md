---
navigation:
  title: Recycler
  parent: index.md
  position: 42
  icon: ic2_120:recycler
item_ids:
  - ic2_120:recycler
---

# Recycler

<BlockImage id="ic2_120:recycler" p:facing="north" p:active="true" scale="4" />

The Recycler destroys almost any item and has a 1 in 8 chance to produce **Scrap** from each operation. Scrap is especially useful as fuel for the [Matter Generator](matter_generator.md), where it lowers UU-Matter production from 1,000,000 EU per mB to 166,667 EU per mB while consuming 34 Scrap per mB.

## Operation

- **Tier:** 1
- **EU Storage:** 416 EU
- **Base input:** up to 32 EU/t
- **Energy use:** 1 EU/t while working
- **Processing time:** 50 ticks, about 2.5 seconds
- **Total energy:** about 50 EU per operation before upgrades
- **Scrap chance:** 12.5% per consumed item

The machine only runs when the input slot contains a recyclable item and the output slot is empty or can accept more Scrap. If the input is missing, blacklisted, or the output is blocked, progress resets to 0.

## Recycling Rules

The Recycler uses a blacklist rather than normal recipes. By default, most non-empty items can be recycled, except battery items and items in the configured `recycler.blacklist`. The default blacklist contains `minecraft:stick`; if the configured blacklist is empty, the built-in fallback also blocks sticks.

## Slots

- Upper left slot: recyclable input.
- Lower left slot: discharging battery. It accepts one battery item and can draw from any battery tier 1 or higher.
- Middle right slot: Scrap output.
- Four slots on the far right: upgrades.

Shift-clicking routes upgrades to the upgrade slots, batteries to the battery slot, recyclable inputs to the input slot, and machine contents back to the player inventory.

## Upgrades and Automation

The Recycler supports Overclocker, Transformer, Energy Storage, Ejector, and Pulling upgrades.

- Overclockers increase energy use and speed. Progress uses the integer part of the speed multiplier, so the first overclocker increases draw but the second and later overclockers begin shortening the 50-tick process.
- Transformer upgrades raise the accepted input tier by one level each, increasing the input limit from the base 32 EU/t.
- Energy Storage upgrades add 10,000 EU of buffer each.
- Ejector upgrades push Scrap from the output slot into adjacent inventories, with optional item filter and side settings.
- Pulling upgrades pull valid recyclable inputs from adjacent inventories, also using the item filter and side settings.

External item automation can insert valid inputs, insert batteries, insert upgrade items, and extract only the Scrap output slot.

## Recipe

<Recipe id="ic2_120:recycler" />

## Related

- [Scrap Box](../items/scrap_box.md) — right-click to open for a random item; made from Scrap
