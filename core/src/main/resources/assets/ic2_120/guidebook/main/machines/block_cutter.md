---
navigation:
  title: Block Cutting Machine
  parent: index.md
  position: 38
  icon: ic2_120:block_cutter
item_ids:
  - ic2_120:block_cutter
---

# Block Cutting Machine

<BlockImage id="ic2_120:block_cutter" p:facing="north" p:active="true" scale="4" />

The Block Cutting Machine saws blocks into plates, slabs, planks, or sticks. It is a slow LV processing machine for compact material conversion: metal storage blocks become plates, stone and wood blocks become extra slabs, and logs become extra planks.

It must have a block cutting blade installed before it can work. The blade is a reusable tool item; the machine checks its hardness but does not consume or damage it.

## Operation

- **Tier:** 1
- **EU Storage:** 3,600 EU
- **Base input:** up to 32 EU/t
- **Energy use:** 4 EU/t while working
- **Processing time:** 450 ticks, about 22.5 seconds
- **Total energy:** 1,800 EU per operation before upgrades

The machine starts only when the input matches a cutting recipe, the output slot can accept the full result, enough EU is available, and the installed blade is harder than the recipe material. If the blade is missing or too weak, the input is invalid, the stack is too small for a 2-input recipe, or the output is blocked, progress resets to 0.

## Blades

- Iron Block Cutting Blade: hardness 5.0.
- Steel Block Cutting Blade: hardness 6.0.
- Diamond Block Cutting Blade: hardness 50.0.

Blade hardness must be strictly greater than the recipe material hardness. That means an iron blade handles wood, stone, copper, tin, lapis, deepslate, and similar recipes below 5.0; a steel blade also handles 5.0 metal blocks such as iron, gold, bronze, and lead; and the diamond blade is needed for the steel block recipe. The generated obsidian recipe has hardness 50.0, so the current diamond blade is still not hard enough for it.

## Recipe

Notable recipe groups include:

- Metal storage blocks to 9 plates: iron, gold, copper, tin, bronze, lead, steel, and lapis.
- Stone, brick, sandstone, quartz, prismarine, blackstone, deepslate, copper, and similar blocks to 9 matching slabs.
- Planks, including rubber planks, to 9 matching slabs.
- Two planks to 6 sticks.
- Logs, stripped logs, stems, bamboo blocks, and rubber logs to 6 matching planks.

These recipes link the Block Cutting Machine into both building-material production and the metal plate chain: blocks can be cut directly into plates, while wood can be stretched into more planks, slabs, or sticks than normal crafting.

## Slots

- Upper left slot: recipe input.
- Lower left slot: discharging battery. It accepts one battery item and drains it into the internal buffer.
- Middle right slot: output.
- Lower middle slot: block cutting blade.
- Four slots on the far right: upgrades.

Shift-clicking routes upgrades to the upgrade slots, batteries to the battery slot, blades to the blade slot, valid cutting inputs to the input slot, and machine contents back to the player inventory.

## Upgrades and Automation

The Block Cutting Machine supports Overclocker, Transformer, Energy Storage, Ejector, and Pulling upgrades.

- Overclockers increase energy use and speed. Progress uses the integer part of the speed multiplier, so the first overclocker increases draw but the second and later overclockers begin shortening the 450-tick process.
- Transformer upgrades raise the accepted input tier by one level each, increasing the input limit from the base 32 EU/t.
- Energy Storage upgrades add 10,000 EU of buffer each.
- Ejector upgrades push items from the output slot into adjacent inventories, with optional item filter and side settings.
- Pulling upgrades pull valid cutting inputs from adjacent inventories, also using the item filter and side settings.

External item automation can insert valid inputs, one battery, one blade, and upgrade items. It can extract only the output slot.

## Recipe

<Recipe id="ic2_120:block_cutter" />
