---
navigation:
  title: Ore Washing Plant
  parent: index.md
  position: 30
  icon: ic2_120:ore_washing_plant
item_ids:
  - ic2_120:ore_washing_plant
---

# Ore Washing Plant

<BlockImage id="ic2_120:ore_washing_plant" p:facing="north" p:active="true" scale="4" />

The Ore Washing Plant is the wet middle step of ore processing. It takes crushed ore from the Macerator, consumes water and EU, and turns it into purified crushed ore plus washing byproducts before the material is smelted directly or sent onward to the Thermal Centrifuge.

It also has a gravel recipe, which washes gravel into Stone Dust.

## Operation

- **Tier:** 1
- **EU storage:** 16,000 EU
- **Maximum input:** 32 EU/t before Transformer upgrades
- **Energy use:** 16 EU/t while progress is advancing
- **Processing time:** 500 ticks, or 25 seconds
- **Total energy:** 8,000 EU per operation before upgrades
- **Water use:** 1,000 mB per operation
- **Water tank:** 8 buckets, or 8,000 mB

The machine only works when the input has an ore washing recipe, all recipe output slots can accept their full results, it has enough EU, and the tank has enough water for the next slice of progress. If the input is missing, invalid, or blocked by full output slots, progress resets to 0.

## Recipe

Each crushed ore recipe consumes 1,000 mB of water and produces one purified crushed ore, one stone dust, and a secondary byproduct:

- Crushed Copper Ore -> Purified Crushed Copper Ore, 2 Small Piles of Copper Dust, Stone Dust
- Crushed Tin Ore -> Purified Crushed Tin Ore, 2 Small Piles of Tin Dust, Stone Dust
- Crushed Iron Ore -> Purified Crushed Iron Ore, 2 Small Piles of Iron Dust, Stone Dust
- Crushed Gold Ore -> Purified Crushed Gold Ore, 2 Small Piles of Gold Dust, Stone Dust
- Crushed Uranium Ore -> Purified Crushed Uranium Ore, 2 Small Piles of Lead Dust, Stone Dust
- Crushed Lead Ore -> Purified Crushed Lead Ore, 3 Small Piles of Sulfur Dust, Stone Dust
- Crushed Silver Ore -> Purified Crushed Silver Ore, 2 Small Piles of Silver Dust, Stone Dust
- Gravel -> Stone Dust

In the full ore line, the usual route is Ore or Raw Ore -> Macerator -> Crushed Ore -> Ore Washing Plant -> Purified Crushed Ore. From there, purified crushed ore can be smelted into ingots, or processed in the Thermal Centrifuge where a centrifuge recipe exists for extra dust products.

## Slots

- **Ore input:** crushed ores or gravel with an ore washing recipe.
- **Water input:** water buckets or IC2 water cells. Empty buckets and empty cells are returned to the empty-container output slot.
- **Outputs:** three recipe output slots for purified crushed ore, byproduct dust, and stone dust.
- **Empty container output:** empty buckets or empty cells from the water input slot.
- **Battery:** one chargeable IC2 battery item for discharging into the machine.
- **Upgrades:** four upgrade slots.

Shift-clicking routes upgrades to the upgrade slots, batteries to the battery slot, valid ore washing inputs to the ore input slot, and water buckets or water cells to the water input slot.

## Upgrades and Automation

The Ore Washing Plant accepts Overclocker, Transformer, Energy Storage, Ejector, Pulling, and Fluid Pipe upgrades. Overclockers increase speed and EU/t cost. Transformer upgrades raise the accepted input tier from the base tier 1 limit. Energy Storage upgrades increase the internal EU buffer.

External item transfer can insert upgrades, one battery, valid recipe inputs, and water containers. It can extract only the three recipe outputs and the empty-container output. Ejector upgrades push those output slots to nearby inventories, while Pulling upgrades pull valid ore washing inputs into the ore input slot.

The internal fluid tank accepts only water and does not output fluid. Fluid pipes or Fabric fluid transfer can fill the tank from exposed sides, and Fluid Pipe upgrades can pull water from adjacent fluid handlers when configured as receivers.

## Recipe

<Recipe id="ic2_120:ore_washing_plant" />
