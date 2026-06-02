---
navigation:
  title: Extractor
  parent: index.md
  position: 39
  icon: ic2_120:extractor
item_ids:
  - ic2_120:extractor
---

# Extractor

<BlockImage id="ic2_120:extractor" p:facing="north" p:active="true" scale="4" />

The Extractor separates useful material out of soft, packed, or treated items. Its main job is the rubber chain: Sticky Resin becomes 3 Rubber, while Rubber Wood and Rubber Saplings each become 1 Rubber. That rubber feeds early cable insulation and the machines that depend on it.

It also handles cleanup and unpacking recipes: dyed wool is stripped back to white wool, an Air Cell becomes an Empty Cell, brick, clay, snow, and nether brick blocks split into their component items, and a few processing byproducts such as Gunpowder, Netherrack Dust, Hydrated Tin Dust, and Filled Tin Cans can be extracted into their useful outputs.

## Operation

- **Tier:** 1
- **EU Storage:** 800 EU
- **Max Input:** 32 EU/t
- **Base Energy Use:** 2 EU/t
- **Base Processing Time:** 400 ticks, or 20 seconds
- **Base Cost:** 800 EU per item

## Slots

- Upper left slot: recipe input
- Right slot: output
- Lower left slot: battery discharge slot
- Right side slots: 4 upgrade slots

The discharge slot accepts one battery item and drains it into the internal buffer when there is room. The upgrade slots accept machine upgrades: Overclocker upgrades increase progress speed and energy demand, Energy Storage upgrades raise the buffer, Transformer upgrades raise the accepted input tier, and Ejector or Pulling upgrades automate moving output or input.

## Automation

Item automation follows the same routes as the GUI. Recipe inputs insert into the input slot, battery items insert into the discharge slot, upgrade items insert into the upgrade slots, and only the output slot can be extracted. Without Pulling or Ejector upgrades, external item transport can still use those routed slots directly.

## Recipe

<Recipe id="ic2_120:extractor" />
