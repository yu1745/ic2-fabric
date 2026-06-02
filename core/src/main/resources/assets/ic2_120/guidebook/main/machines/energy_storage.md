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

Energy storage blocks buffer EU, charge electric items, and output power from their front face. They are also the safest way to separate generators from machines that draw uneven power.

## Tier Comparison

| Tier | Name | Capacity | Output | Voltage |
|------|------|----------|--------|---------|
| 1 | BatBox | 40,000 EU | 32 EU/t | LV |
| 2 | CESU | 300,000 EU | 128 EU/t | MV |
| 3 | MFE | 4,000,000 EU | 512 EU/t | HV |
| 4 | MFSU | 40,000,000 EU | 2,048 EU/t | EV |

## Usage

All storage blocks accept EU only from the five non-front faces and extract EU only from the front face. Rotate the block so the output face points toward the cable or machine you want to feed.

## Slots

- **Charge slot:** batteries and electric tools with tier less than or equal to the storage tier.
- **Fuel slot:** redstone adds 800 EU, Energium Dust adds 16,000 EU. Fuel is consumed only when there is enough empty space for the full value.
- **MFE/MFSU equipment view:** higher-tier screens also expose player equipment charging controls.

Storage blocks do not have upgrade slots. Use transformers when you need to bridge voltage tiers.

## Network Tips

BatBox, CESU, MFE, and MFSU output their tier rate: 32, 128, 512, and 2,048 EU/t. Machines or cables below that tier need a transformer or a lower-tier buffer between them and the storage output. MFE and MFSU are also valid adjacent energy sources for Teleporters, which draw teleport costs directly from nearby storage.

## Recipes

<Recipe id="ic2_120:batbox" />
<Recipe id="ic2_120:cesu" />
<Recipe id="ic2_120:mfe" />
<Recipe id="ic2_120:mfsu" />
