---
navigation:
  title: Semifluid Generator
  parent: index.md
  position: 12
  icon: ic2_120:semifluid_generator
item_ids:
  - ic2_120:semifluid_generator
---

# Semifluid Generator

<BlockImage id="ic2_120:semifluid_generator" p:facing="north" p:active="true" scale="4" />

The Semifluid Generator burns liquid fuels such as biofuel and creosote oil to produce EU. It accepts fluid containers (buckets and cells) and supports fluid pipe connections.

Different fuels provide different energy values: biofuel yields 16 EU/t and 32,000 EU per bucket, while creosote oil yields 8 EU/t and 3,200 EU per bucket.

## Output

- **EU Output**: 10-16 EU/t (depending on fuel)
- **Energy Storage**: 10,000 EU
- **Tier**: 1
- **Internal Tank**: 8 buckets

### Fuel Values

| Fuel | EU/t | EU/Bucket |
|------|------|-----------|
| Biofuel | 16 | 32,000 |
| Creosote Oil | 8 | 3,200 |

## Slots

- Fuel slot (top): biofuel/creosote buckets or cells
- Empty container slot (middle): outputs empty containers after fuel is consumed
- Battery slot (bottom): chargeable battery or electric tool
- Upgrade slots (4): supports fluid pipe and other upgrades

The Semifluid Generator does not accept EU input. It outputs EU from every side except its front face.

## Recipe

<Recipe id="ic2_120:semifluid_generator" />
