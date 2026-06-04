---
navigation:
  title: Scanners
  parent: index.md
  position: 335
  icon: ic2_120:scanner
item_ids:
  - ic2_120:scanner
  - ic2_120:advanced_scanner
---

# Scanners

<ItemImage id="ic2_120:scanner" scale="4" />

The Scanners are a two-tier family of prospecting tools used to locate ore deposits in the world. Both share a single GUI: hold the scanner, right-click to open the screen, and press the "Scan" button to spend energy and burn one use. The OD Scanner covers a 13×13 footprint down to bedrock-layer depth, while the OV Scanner widens that to 25×25 and reaches deeper — but at a much steeper EU cost per scan.

## Block View

| OD Scanner | OV Scanner |
|:----------:|:----------:|
| <ItemImage id="ic2_120:scanner" scale="2" /> | <ItemImage id="ic2_120:advanced_scanner" scale="2" /> |

## Stats

| Scanner | Tier | Capacity | Per-Scan EU | Scan Radius | Scan Depth | Max Uses | Repair |
|---------|:----:|----------|-------------|-------------|------------|:--------:|--------|
| OD Scanner | 1 | 10,000 EU | 500 EU | 6 (13×13) | down to y=0 | 20 | none |
| OV Scanner | 3 | 1,000,000 EU | 5,000 EU | 12 (25×25) | down to y=-64 | 200 | none |

Both scanners are **not damageable** — they have no vanilla durability bar. The "uses remaining" counter is stored in NBT (`ScannerUses`) and decrements on each successful scan; once the counter hits zero the scanner is spent and must be replaced. There is no repair recipe and no anvil combination: craft a fresh one when the buffer runs out.

**The scanner HUD bar** (visible above the hotbar) reflects the current EU buffer, not the use counter. Hover the item to see both numbers in the tooltip.

## How to Use

### Charging

Scanners cannot self-charge. Place one in the input slot of any tier-appropriate EU storage block — BatBox, CESU, MFE, MFSU, or a Charging Pad — and let the block top it up. You can also right-click a charged Energy Crystal / Lapotron Crystal in your inventory while the scanner sits next to it, mirroring the standard electric-tool recharge loop.

The OD Scanner accepts any tier-1 source (single battery, BatBox); the OV Scanner requires tier-3 hardware (Energy Crystal and above, MFE/MFSU).

### Scanning

1. Hold the scanner and right-click to open the **Scanner** GUI.
2. The GUI displays current EU, capacity, uses remaining, and max uses.
3. Click the "Scan" button. The scanner consumes the per-scan EU cost, decrements the use counter, and emits a scan result highlighting the ore distribution in the area.
4. If the buffer does not contain enough EU for one scan, or the use counter has hit zero, the action is rejected.

### Choosing the Right Tier

- **OD Scanner** — the cheap workhorse. A 13×13 footprint is plenty for surface prospecting and shallow cave work, and the 500 EU per scan is forgiving on a 10,000 EU buffer.
- **OV Scanner** — for deep bases, strip-mining operations, and late-game prospecting. The 25×25 footprint reaches y=-64, so it covers everything from the surface down through deepslate. The 5,000 EU cost per scan means a fully charged OV Scanner can perform 200 scans before going dark, and 200 scans cover a substantial mining campaign.

Always confirm the use counter before you head out: a scanner with one use left is still useful, but a drained one is dead weight in the field.

## Crafting

The OD Scanner is the gating material for the OV Scanner, so plan your tier-1 production first.

| OD Scanner | OV Scanner |
|:----------:|:----------:|
| <Recipe id="ic2_120:scanner" /> | <Recipe id="ic2_120:advanced_scanner" /> |

- **OD Scanner:** glowstone dust over a circuit, with a Re-Battery in the center and insulated copper cables filling the bottom row (`gCg / ibi / ccc`).
- **OV Scanner:** glowstone dust flanking an advanced circuit, an Energy Crystal in the middle, insulated gold cables, and an OD Scanner as the core (`gCg / gAg / iIi`).

## Related

- [Tools and Armor reference](../reference/tools_armor.md)
- [Batteries and Mobile Power](../reference/energy_items.md)
- [Energy Crystal and Lapotron Crystal](energy_crystal.md)
