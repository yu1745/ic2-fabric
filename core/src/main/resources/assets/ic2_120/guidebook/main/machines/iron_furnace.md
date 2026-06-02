---
navigation:
  title: Iron Furnace
  parent: index.md
  position: 26
  icon: ic2_120:iron_furnace
item_ids:
  - ic2_120:iron_furnace
---

# Iron Furnace

<BlockImage id="ic2_120:iron_furnace" p:facing="north" p:active="true" scale="4" />

The Iron Furnace is a fuel-burning furnace upgrade for the early game. It uses vanilla smelting recipes, but each item finishes in **160 ticks / 8 seconds** instead of the vanilla furnace's 200 ticks.

## Fuel Behavior

Most furnace fuels last **1.25x** their vanilla burn time in the Iron Furnace. Because each smelt takes 160 ticks, coal and charcoal smelt **10 items** instead of the vanilla furnace's 8.

Lava Buckets are special: they burn for **2,000 ticks**, enough for **12.5 items**. They are still valid fuel, but they are much less efficient here than in a vanilla furnace.

The Iron Furnace stores smelting experience internally. Taking products or breaking the block releases the stored experience.

## Slots

- **Input slot:** any item with a vanilla smelting recipe
- **Fuel slot:** valid furnace fuel
- **Output slot:** smelted result

Item automation inserts smeltable items into the input slot and valid fuel into the fuel slot. Only the output slot is exposed for extraction.

## Recipe

<Recipe id="ic2_120:iron_furnace" />
