---
navigation:
  title: Crop-Matron
  parent: index.md
  position: 61
  icon: ic2_120:cropmatron
item_ids:
  - ic2_120:cropmatron
---

# Crop-Matron

<BlockImage id="ic2_120:cropmatron" p:facing="north" scale="4" />

The Crop-Matron is the maintenance machine for IC2 crop farms. It does not harvest crops. Instead, it periodically scans a flat **9x1x9** crop layer centered on itself and feeds nearby crop sticks or planted crops with fertilizer, water, and Weed-EX protection.

## Energy

- **Storage**: 10,000 EU
- **Tier**: 1, accepts up to 32 EU/t
- **Work cycle**: one scan every 10 ticks before upgrades
- **Scan cost**: 1 EU for each position checked
- **Application cost**: 10 EU each time it successfully applies fertilizer, crop hydration, Weed-EX, or farmland moisture

Overclocker upgrades shorten the scan interval and increase energy cost. Energy storage upgrades increase the internal EU buffer, transformer upgrades raise the accepted input tier, and fluid pipe upgrades can pull water or Weed-EX from adjacent fluid storage.

## Range

The machine checks a 9 by 9 square on the **same Y level as the Crop-Matron**. Put the Crop-Matron level with the crop sticks, not one block below them. For each scanned column it also checks the block one level lower and can raise farmland moisture up to 7 when water is available.

The active texture only means the last scan actually applied something. A powered machine with full crops may appear idle because there was nothing to top up.

## Tanks and Inputs

- **Water tank**: 8 buckets. Accepts water or distilled water from buckets, cells, universal fluid cells, or fluid insertion.
- **Weed-EX tank**: 8 buckets. Accepts Weed-EX from buckets, cells, universal fluid cells, or fluid insertion.
- **Fertilizer slots**: seven slots, accepting IC2 Fertilizer only.
- **Upgrade slots**: four slots for overclocker, transformer, energy storage, fluid pipe, Ejector, and Pulling upgrades.

Empty buckets and empty cells are placed in the matching output slots. The output slots must have room or the machine will not drain another container.

## Crop Care

- **Fertilizer**: consumes one Fertilizer item when a crop or crop stick can accept nutrients. Machine-applied fertilizer adds nutrient storage in 90-point steps until the crop-side threshold is reached.
- **Hydration**: fills crop or crop-stick water storage up to 200, consuming water from the water tank.
- **Weed-EX**: fills crop or crop-stick Weed-EX storage up to 150, consuming Weed-EX from the Weed-EX tank. Weed-EX is protective; it suppresses weed spread and spontaneous weed growth on empty sticks, but it does not instantly remove an existing weed crop.
- **Farmland moisture**: if a scanned position has no crop-care block, the machine can still hydrate farmland directly below that position.

## Automation Notes

Item automation is routed by item type: upgrades go to the four upgrade slots, water containers to the water input, Weed-EX containers to the Weed-EX input, and Fertilizer to the seven fertilizer slots. Pulling upgrades can pull those work inputs from adjacent inventories, and Ejector upgrades can push empty-container and Weed-EX output items. The four upgrade slots are excluded from both upgrade operations.

Fluid automation is input-only. The exposed fluid storage accepts water/distilled water and Weed-EX but does not allow fluid extraction, so do not use the Crop-Matron as a fluid buffer.

## Crafting

<Recipe id="ic2_120:cropmatron" />
