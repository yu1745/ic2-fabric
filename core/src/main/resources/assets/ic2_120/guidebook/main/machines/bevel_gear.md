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
- The bevel gear itself has the same KU throughput as the lowest-tier shaft in the system.

## Shaft layouts

The shaft next to a bevel gear must use the same axis as the side it touches: east or west uses `axis=x`, up or down uses `axis=y`, and north or south uses `axis=z`.

Two-shaft corner:

<GameScene zoom="7" interactive={true} fullWidth={true}>
  <IsometricCamera yaw="45" pitch="30" />
  <ImportStructure src="bevel_gear_two_shafts.snbt" />
</GameScene>

Three-shaft junction:

<GameScene zoom="6" interactive={true} fullWidth={true}>
  <IsometricCamera yaw="45" pitch="30" />
  <ImportStructure src="bevel_gear_three_shafts.snbt" />
</GameScene>

## Recipe

<Recipe id="ic2_120:bevel_gear_from_plates" />
