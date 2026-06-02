---
navigation:
  title: Teleporter
  parent: index.md
  position: 67
  icon: ic2_120:teleporter
item_ids:
  - ic2_120:teleporter
---

# Teleporter

<BlockImage id="ic2_120:teleporter" p:facing="north" scale="4" />

The Teleporter moves one nearby player, mob, or other entity to a paired Teleporter in the same dimension. It does not store its own charge for a jump: the source Teleporter drains the required EU directly from adjacent MFE, MFSU, MFE Chargepad, or MFSU Chargepad blocks.

## Linking

Use a Frequency Transmitter to pair two Teleporters.

1. Sneak-right-click, or right-click with an unbound transmitter, on the first Teleporter to record it.
2. Right-click a second Teleporter in the same dimension.
3. Both Teleporters are written with each other's position.

The target must still be a Teleporter when used, must be in the same dimension, and its chunk must be loaded. A Teleporter cannot be linked to itself.

## Power

- **Machine tier**: 4
- **Required adjacent storage**: MFE, MFSU, MFE Chargepad, or MFSU Chargepad
- **Internal energy buffer**: none for teleporting

The source checks the total EU stored in all valid adjacent storage blocks before charging. If enough EU is available, it starts charging; when the charge completes, it drains the exact cost from those adjacent blocks. The target Teleporter does not pay the jump cost, but it must be valid and not cooling down.

## Activation

A Teleporter only starts a jump while it receives redstone power. While powered, it scans its activation volume and chooses the closest valid entity. The entity must be alive and must not be riding, carrying passengers, or leave the activation volume during charging.

Use the Teleporter GUI to choose the activation volume:

- **1x1x1**: tight trigger volume above the Teleporter
- **3x3x3**: wider trigger volume for pads, mobs, or automation setups

After a successful jump, both the source and target Teleporters enter a 20 tick cooldown.

## Cost And Charge Time

The cost is based on entity weight and straight-line distance between the two Teleporter blocks:

`EU = floor(5 * weight * (distance + 10)^0.7)`

Weights are:

- **Player**: 1000 base, +100 per equipped armor piece, plus inventory weight based on stack fullness, capped at 5100
- **Animal**: 100
- **Monster or other entity**: 500

Charging takes `40 + distance / 20` ticks, clamped between 40 and 120 ticks. Overclocker Upgrades in the two upgrade slots shorten this charging time, but it still cannot go below 40 ticks.

## Slots And Automation

The GUI has two upgrade slots. They accept upgrade items; in current Teleporter behavior, Overclocker Upgrades affect the pre-teleport charging time. Automated item insertion is routed only into these upgrade slots, and extraction is allowed from them.

## Practical Setup

Put an MFE, MFSU, or matching chargepad directly beside the source Teleporter, keep it charged, link the pair with a Frequency Transmitter, then drive the source with a redstone signal. For two-way travel, power and provide adjacent storage at both ends, because each end becomes the source when sending entities back.
