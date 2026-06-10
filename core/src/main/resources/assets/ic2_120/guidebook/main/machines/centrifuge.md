---
navigation:
  title: Thermal Centrifuge
  parent: index.md
  position: 33
  icon: ic2_120:centrifuge
item_ids:
  - ic2_120:centrifuge
---

# Thermal Centrifuge

<BlockImage id="ic2_120:centrifuge" p:facing="north" p:active="true" scale="4" />

The Thermal Centrifuge separates crushed ores, purified crushed ores, depleted nuclear fuel, and a few special materials into refined products. In the ore processing chain it is the high-heat step after the Macerator and Ore Washing Plant, turning cleaned material into dusts plus the small byproducts that make full ore processing worthwhile.

## Operation

- **Tier:** 2
- **EU Storage:** 10,000 EU
- **Base input:** up to 128 EU/t
- **Energy use:** 48 EU/t while processing
- **Processing time:** 500 ticks, about 25 seconds
- **Total processing energy:** about 24,000 EU per operation before upgrades, not counting warm-up
- **Heat:** recipes require 100 to 5,000 heat

Heat is a threshold, not a consumed ingredient. With a valid recipe inserted, the machine spends 1 EU/t to warm itself by 1 heat per tick until it reaches that recipe's minimum heat. A redstone signal makes it keep heating toward 5,000 heat, which is useful for high-temperature recipes. Without redstone, excess heat cools down to the current recipe's minimum heat, or to 0 when there is no recipe.

The machine only starts when the input stack matches a centrifuging recipe, the current heat is high enough, and every required output can fit. If the input is missing, the recipe is invalid, the heat is too low, or an output is blocked, it stops; blocked outputs reset progress to 0.

## Recipe

Centrifuge recipes are in the `ic2_120:centrifuging` recipe category and can consume more than one input item. Each operation can produce up to three outputs: the first output goes to the top output slot, the second to the middle output slot, and the third to the lower output slot.

Notable recipe groups include:

- Crushed copper, tin, iron, gold, lead, and uranium ore into their main dusts, stone dust, and small secondary dusts.
- Purified crushed copper, tin, iron, gold, lead, and uranium ore into dusts and secondary byproducts without the stone dust output.
- Depleted uranium fuel rods into Small Pile of Plutonium, Uranium 238, and iron dust.
- Depleted MOX fuel rods and RTG pellets into Plutonium products and iron dust.
- Cobblestone, quartz, slag, clay dust, and Peat Ore into material-chain ingredients such as stone dust, small lithium dust, coal dust, silicon dioxide dust, coal, and Bio Chaff.

Uranium processing is especially important: crushed or purified uranium produces Small Pile of Uranium 235 and Uranium 238, while depleted fuel rods return plutonium materials for later nuclear crafting.

## Slots

- Upper left slot: recipe input.
- Lower left slot: discharging battery. It accepts one battery item and discharges into the internal EU buffer.
- Three right-side slots: recipe outputs, filled from top to bottom by the recipe's output list.
- Four slots on the far right: upgrades.

Shift-clicking routes upgrades to the upgrade slots, batteries to the battery slot, valid centrifuge inputs to the input slot, and machine contents back to the player inventory.

## Upgrades and Automation

The Thermal Centrifuge supports Overclocker, Transformer, Energy Storage, Ejector, and Pulling upgrades.

- Overclockers increase processing speed and processing energy use. The 1 EU/t heating cost is not overclocked.
- Transformer upgrades raise the accepted input tier by one level each, increasing the input limit from the base 128 EU/t.
- Energy Storage upgrades add 10,000 EU of buffer each.
- Ejector upgrades push items from the three output slots into adjacent inventories, with optional item filter and side settings.
- Pulling upgrades pull valid recipe inputs from adjacent inventories, also using the item filter and side settings.

External item automation can insert valid inputs, insert batteries, insert upgrade items, and extract only from the three output slots.

## Recipe

<Recipe id="ic2_120:centrifuge" />
