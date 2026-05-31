---
navigation:
  title: Helical Gear
  parent: index.md
  position: 63
  icon: ic2_120:bevel_gear
item_ids:
  - ic2_120:bevel_gear
---

# Helical Gear

<BlockImage id="ic2_120:bevel_gear" scale="4" />

The Helical Gear (Bevel Gear) changes the axis of kinetic power transmission. It redirects kinetic power (KU) from one direction to another — for example, converting a horizontal shaft line into a vertical one.

## Usage

- Place a bevel gear at the corner of a kinetic shaft line.
- Power enters from one face and exits from the adjacent face at a 90-degree angle.
- This allows compact shaft layouts and routing power around corners.
- The bevel gear itself has a fixed KU throughput of 2048 KU, equivalent to the steel transmission shaft.
- Bevel gears can be chained in series using the three-shaft junction pattern, allowing kinetic power to travel along a chain of bevel gears in a single direction until reaching the throughput limit.

## Shaft layouts

Two-shaft corner:

<GameScene zoom="7" interactive={true} fullWidth={true}>
  <IsometricCamera yaw="45" pitch="30" />
  <ImportStructure src="bevel_gear_two_shafts.snbt" />
</GameScene>

**Important:** Only the two layouts shown above are mechanically valid. Any other arrangement will cause the bevel gears to physically jam and lock up — they cannot rotate.

Three-shaft junction:

<GameScene zoom="6" interactive={true} fullWidth={true}>
  <IsometricCamera yaw="45" pitch="30" />
  <ImportStructure src="bevel_gear_three_shafts.snbt" />
</GameScene>

## Recipe

<Recipe id="ic2_120:bevel_gear_from_plates" />
