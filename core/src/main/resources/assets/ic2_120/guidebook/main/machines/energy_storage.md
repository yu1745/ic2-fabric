---
navigation:
  title: Energy Storage
  parent: index.md
  position: 46
  icon: ic2_120:batbox
item_ids:
  - ic2_120:batbox
  - ic2_120:cesu
  - ic2_120:mfe
  - ic2_120:mfsu
---

# Energy Storage

<BlockImage id="ic2_120:batbox" scale="4" />

Energy storage blocks store EU and output it at a fixed voltage and rate. Higher tiers offer greater capacity and throughput. They can charge batteries and electric tools placed in their charge slot, and automatically output power to adjacent machines and cables.

## Tier Comparison

| Tier | Name | Capacity | Output | Voltage |
|------|------|----------|--------|---------|
| 1 | BatBox | 40,000 EU | 32 EU/t | LV |
| 2 | CESU | 300,000 EU | 128 EU/t | MV |
| 3 | MFE | 4,000,000 EU | 512 EU/t | HV |
| 4 | MFSU | 40,000,000 EU | 2,048 EU/t | EV |

## Usage

All energy storage blocks share the following features:

- **Charge Slot**: Place a battery, energy crystal, or electric tool in the top slot to charge it.
- **Auto Output**: Energy is automatically pushed out to adjacent machines, cables, or storage blocks.
- **Input**: Power is accepted on any face.
- **Upgrades**: All tiers support transformer, storage, and overclocker upgrades (except BatBox, which does not accept overclocker upgrades).

Higher-tier storage blocks are required to safely handle higher-voltage power. Connecting a low-voltage storage to a high-voltage source will cause the storage to explode.

## Recipes

<Recipe id="ic2_120:batbox" />
<Recipe id="ic2_120:cesu" />
<Recipe id="ic2_120:mfe" />
<Recipe id="ic2_120:mfsu" />
