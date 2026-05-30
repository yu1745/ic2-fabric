---
navigation:
  title: Water Mill
  parent: index.md
  position: 13
  icon: ic2_120:water_generator
item_ids:
  - ic2_120:water_generator
---

# Water Mill

<BlockImage id="ic2_120:water_generator" p:facing="north" p:active="true" scale="4" />

The Water Mill generates EU from water. It produces power in two ways: by consuming water buckets or water cells at 1 EU/t (500 EU per bucket), and from nearby water blocks in a 3x3x3 area around the machine, each contributing 0.01 EU/t.

A common water tower design with surrounding water blocks can produce around 0.25 EU/t from the environment, in addition to the bucket-fed supply.

## Output

- **Bucket Output**: 1 EU/t (500 EU per bucket)
- **Environmental Output**: 0.01 EU/t per adjacent water block
- **Energy Storage**: 10,000 EU
- **Tier**: 1

## Slots

- Fuel slot (top): water buckets or water cells
- Empty container slot (middle): outputs empty containers after water is consumed
- Battery slot (bottom): chargeable battery or electric tool
- Upgrade slots (4): supports fluid pipe and other upgrades

The Water Mill does not accept EU input. It outputs EU from every side except its front face.

## Recipe

<Recipe id="ic2_120:water_generator" />
