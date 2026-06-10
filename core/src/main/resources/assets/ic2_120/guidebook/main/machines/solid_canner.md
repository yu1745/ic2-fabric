---
navigation:
  title: Solid Canning Machine
  parent: index.md
  position: 35
  icon: ic2_120:solid_canner
item_ids:
  - ic2_120:solid_canner
---

# Solid Canning Machine

<BlockImage id="ic2_120:solid_canner" p:facing="north" p:active="true" scale="4" />

The Solid Canning Machine combines a solid container item with a solid filling item. In this build it mainly does two jobs: packing food into Tin Cans, and loading nuclear fuel into Empty Fuel Rods.

## Operation

- **Energy tier:** 1
- **Max EU input:** 32 EU/t
- **EU storage:** 416 EU
- **Base draw:** 2 EU/t
- **Base time:** 200 ticks / 10 seconds
- **Base cost:** 400 EU per operation

Progress resets when either input is missing, the inputs no longer match a valid recipe, there are not enough required items, or the output slot cannot accept the result.

## Recipe

Machine recipes load nuclear fuel:

| Container | Filler | Output |
|-----------|--------|--------|
| Empty Fuel Rod | Uranium | Uranium Fuel Rod |
| Empty Fuel Rod | MOX | MOX Fuel Rod |

Food canning is handled directly by the machine. Put Empty Tin Cans in the container slot and any edible item in the filler slot. The food's hunger value decides how many cans are required and how many Filled Tin Cans are produced; at least one can is required even for very small foods.

## Slots

- **Container slot:** Empty Tin Cans or Empty Fuel Rods
- **Filler slot:** food, Uranium, or MOX
- **Output slot:** Filled Tin Cans or filled fuel rods
- **Battery slot:** discharges tier 1+ batteries into the internal EU buffer
- **Upgrade slots:** 4 slots

## Upgrades and Automation

Supported upgrades are Overclocker, Transformer, Energy Storage, Ejector, and Pulling upgrades. Overclockers speed the operation while increasing EU use. Energy Storage upgrades raise the internal buffer, Transformer upgrades raise accepted voltage, Ejector upgrades can push the output slot, and Pulling upgrades can pull container or filler items into the two input slots.

External item automation routes Empty Tin Cans and Empty Fuel Rods to the container slot, valid food or fuel ingredients to the filler slot, batteries to the battery slot, and upgrades to the upgrade slots. Only the output slot is exposed for normal extraction.

## Recipe

<Recipe id="ic2_120:solid_canner" />
