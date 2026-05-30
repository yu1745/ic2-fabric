---
navigation:
  title: Generator
  parent: index.md
  position: 10
  icon: ic2_120:generator
item_ids:
  - ic2_120:generator
---

# Generator

<BlockImage id="ic2_120:generator" p:facing="north" p:active="true" scale="4" />

The Generator is the first solid-fuel EU source. It burns furnace fuels and stores the generated power internally before pushing EU into adjacent machines, cables, or chargeable items.

## Output

The Generator produces **10 EU/t** while burning fuel. Its internal buffer stores **4,000 EU**, matching one coal worth of output.

Fuel burn time is based on the vanilla furnace fuel value divided by 4. For example, coal burns for 400 ticks in the Generator and produces 4,000 EU.

## Slots

- Top slot: chargeable battery or electric tool
- Bottom slot: solid fuel

The Generator does not accept EU input. It outputs from every side except its front face.

## Recipe

<Recipe id="ic2_120:generator" />
