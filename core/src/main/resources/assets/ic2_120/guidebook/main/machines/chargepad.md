---
navigation:
  title: Chargepad
  parent: index.md
  position: 47
  icon: ic2_120:batbox_chargepad
item_ids:
  - ic2_120:batbox_chargepad
  - ic2_120:cesu_chargepad
  - ic2_120:mfe_chargepad
  - ic2_120:mfsu_chargepad
---

# Chargepad

<BlockImage id="ic2_120:batbox_chargepad" scale="4" />

Chargepads are horizontal energy storage blocks with a charging surface. They keep the same capacity, side rules, charge slot, and fuel slot as the matching BatBox, CESU, MFE, or MFSU, then add automatic charging for players standing on top. Chargepads can rotate horizontally in four directions, but cannot face up or down.

## Block View

| BatBox Chargepad | CESU Chargepad | MFE Chargepad | MFSU Chargepad |
|:---------------:|:-------------:|:-------------:|:-------------:|
| <BlockImage id="ic2_120:batbox_chargepad" scale="2" /> | <BlockImage id="ic2_120:cesu_chargepad" scale="2" /> | <BlockImage id="ic2_120:mfe_chargepad" scale="2" /> | <BlockImage id="ic2_120:mfsu_chargepad" scale="2" /> |

## Tier Comparison

| Tier | Name | Capacity | Output | Voltage |
|------|------|----------|--------|---------|
| 1 | BatBox Chargepad | 40,000 EU | 32 EU/t | LV |
| 2 | CESU Chargepad | 300,000 EU | 128 EU/t | MV |
| 3 | MFE Chargepad | 4,000,000 EU | 512 EU/t | HV |
| 4 | MFSU Chargepad | 40,000,000 EU | 2,048 EU/t | EV |

## Usage

- Stand in the block space above the chargepad to charge electric items in your inventory and armor slots.
- The pad charges only items whose tier is less than or equal to the pad tier.
- Energy is drawn from the pad's internal buffer.
- Like normal storage blocks, EU enters through the five non-front faces and leaves through the front face.
- The GUI still has an item charge slot and a fuel slot. Redstone adds 800 EU and Energium Dust adds 16,000 EU when the buffer has room.
- The active state lights while it is charging a player.

## Layout

Place the output face toward the cable or machine that should receive excess power, and keep the top clear for the player. MFE and MFSU Chargepads can also serve as the adjacent storage block for a Teleporter while still acting as walk-over armor chargers.

## Recipe

<Recipe id="ic2_120:batbox_chargepad" />
<Recipe id="ic2_120:cesu_chargepad" />
<Recipe id="ic2_120:mfe_chargepad" />
<Recipe id="ic2_120:mfsu_chargepad" />
