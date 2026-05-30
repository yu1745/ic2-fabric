---
navigation:
  title: Geothermal Generator
  parent: index.md
  position: 11
  icon: ic2_120:geo_generator
item_ids:
  - ic2_120:geo_generator
---

# Geothermal Generator

<BlockImage id="ic2_120:geo_generator" p:facing="north" p:active="true" scale="4" />

The Geothermal Generator burns lava to produce EU. It accepts lava buckets, lava cells, and fluid pipe connections, storing up to 8 buckets of lava internally. Each bucket of lava burns for 25 seconds (500 ticks) generating a total of 10,000 EU.

It provides a steady 20 EU/t output, making it one of the more powerful early-game generators. When the internal energy buffer is full, lava consumption pauses automatically to avoid waste.

## Output

- **EU Output**: 20 EU/t
- **Energy Storage**: 10,000 EU
- **Tier**: 1
- **Lava Consumption**: 2 mB/t (1 bucket per 25 seconds)

## Slots

- Fuel slot (top): lava buckets or lava cells
- Empty container slot (middle): outputs empty buckets or cells after fuel is consumed
- Battery slot (bottom): chargeable battery or electric tool

The Geothermal Generator does not accept EU input. It outputs EU from every side except its front face.

## Recipe

<Recipe id="ic2_120:geo_generator" />
