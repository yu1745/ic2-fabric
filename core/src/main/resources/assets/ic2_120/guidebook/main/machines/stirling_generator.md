---
navigation:
  title: Stirling Generator
  parent: index.md
  position: 19
  icon: ic2_120:stirling_generator
item_ids:
  - ic2_120:stirling_generator
---

# Stirling Generator

<BlockImage id="ic2_120:stirling_generator" p:facing="north" p:active="true" scale="4" />

The Stirling Generator is a direct HU-to-EU converter. It accepts heat from one adjacent heat machine, stores the produced EU internally, and then exports that EU to nearby energy users or storage blocks.

It converts **2 HU into 1 EU**. The machine can accept up to **100 HU/t**, which becomes up to **50 EU/t**. Its internal EU buffer holds **30,000 EU**, and its energy tier is **2**.

## Heat Input

Heat transfer is direct block contact only. The Stirling Generator has one heat input face: its facing side. The heat source must be touching that face, and the source's own heat transfer face must point back at the Stirling Generator.

The Stirling Generator keeps a small heat buffer of up to **100 HU**. Each tick it converts as much buffered heat as it can, limited by the **50 EU/t** generation cap and by remaining space in the 30,000 EU buffer. If the EU buffer is full, heat can remain buffered, but no more EU is produced until energy leaves the machine.

Heat generators do not store unused HU, so alignment matters. If the faces are not matched, or if the Stirling Generator cannot accept more heat, the source's produced heat is not saved for later.

## EU Output

The Stirling Generator does not accept EU input. It outputs up to **50 EU/t total** from any side except its facing side. The facing side is reserved for heat input, so put cables, batteries, or machines on the other faces.

## Slots and Automation

The current Stirling Generator has **no machine item slots**. It has no battery charging slot, no battery discharge slot, and no item output slot. The GUI shows the player's inventory and the current HU/EU rates only.

Automation should treat it as a heat input plus EU output block:

- Supply HU with a directly adjacent heat source on the facing side.
- Extract EU from any non-facing side.
- Do not pipe items into the machine; there are no valid item insertion routes.

## Practical Setups

A Solid Heat Generator supplies **20 HU/t**, producing **10 EU/t** through the Stirling Generator. A full Electric Heat Generator or Liquid Heat Exchanger can supply **100 HU/t**, matching the Stirling Generator's **50 EU/t** maximum.

For fluid or reactor heat loops, place the Liquid Heat Exchanger's heat face against the Stirling Generator's heat face. Fill the exchanger with Heat Conductors so it can output the desired HU/t, then take EU from the Stirling Generator's sides or back.

## See Also

For higher efficiency, run the heat through a Steam Generator → <ItemLink id="ic2_120:steam_kinetic_generator" /> chain. When fed **superheated steam**, the Steam Kinetic Generator converts the same heat into EU at **1.5× the rate** of the Stirling Generator. See the Steam Kinetic Generator page for the chain setup.

## Recipe

<Recipe id="ic2_120:stirling_generator" />
