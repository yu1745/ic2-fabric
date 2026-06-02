---
navigation:
  title: Metal Former
  parent: index.md
  position: 41
  icon: ic2_120:metal_former
item_ids:
  - ic2_120:metal_former
---

# Metal Former

<BlockImage id="ic2_120:metal_former" p:facing="north" p:active="true" scale="4" />

The Metal Former is a three-mode electric shaping machine for the metal parts chain. One block handles rolling, cutting, and extruding recipes, so the same input item may have different outputs depending on the selected mode.

Use the mode button in the GUI to cycle modes in this order: Rolling, Cutting, Extruding. Changing mode resets the current progress.

## Operation

- **Tier:** 1
- **EU Storage:** 400 EU
- **Base input:** up to 32 EU/t
- **Energy use:** 10 EU/t while working
- **Processing time:** 200 ticks, about 10 seconds
- **Total energy:** about 2,000 EU per operation before upgrades

The machine only starts when the input stack has a recipe in the current mode and the output slot can accept the full result. If the input is missing, the selected mode has no matching recipe, or the output is blocked, progress resets to 0.

## Modes and Recipes

- **Rolling:** ingots become plates; plates become 2 casings. Tin, iron, copper, bronze, gold, lead, and steel are covered.
- **Cutting:** plates become cables, such as tin, copper, gold, and iron cable. Iron casings can also be cut into coins.
- **Extruding:** ingots become cables, and some shaped inputs become components: tin casings to tin cans, iron plates to fuel rods, iron casings to iron fences, and iron or steel blocks to shafts.

This links the early material chain together: ingot to plate, plate to casing, plate or ingot to cable, and selected plates, casings, or blocks to rods and machine parts.

## Slots

- Upper left slot: recipe input for the currently selected mode.
- Lower left slot: discharging battery. It accepts one battery item and can draw from batteries of tier 1 or higher.
- Middle right slot: output.
- Four slots on the far right: upgrades.

Shift-clicking routes upgrades to the upgrade slots, batteries to the battery slot, valid recipe inputs to the input slot, and machine contents back to the player inventory.

## Upgrades and Automation

The Metal Former supports Overclocker, Transformer, Energy Storage, Ejector, and Pulling upgrades.

- Overclockers increase energy use and speed. Progress uses the integer part of the speed multiplier, so the first overclocker increases draw but the second and later overclockers begin shortening the 200-tick process.
- Transformer upgrades raise the accepted input tier by one level each, increasing the input limit from the base 32 EU/t.
- Energy Storage upgrades add 10,000 EU of buffer each.
- Ejector upgrades push items from the output slot into adjacent inventories, with optional item filter and side settings.
- Pulling upgrades pull valid recipe inputs from adjacent inventories, also using the item filter and side settings.

External item automation can insert valid inputs, insert batteries, insert upgrade items, and extract only the output slot. Recipe input validation checks all Metal Former recipes, but processing still uses the current mode, so set the machine mode before automating a line.

## Recipe

<Recipe id="ic2_120:metal_former" />
