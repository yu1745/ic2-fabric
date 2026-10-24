---
navigation:
  title: Coke Kiln
  parent: index.md
  position: 60
  icon: ic2_120:coke_kiln
item_ids:
  - ic2_120:coke_kiln
  - ic2_120:coke_kiln_grate
  - ic2_120:coke_kiln_hatch
---

# Coke Kiln

<BlockImage id="ic2_120:coke_kiln" scale="4" />

The Coke Kiln is a 3x3x3 passive multiblock that cooks solid fuels without EU. It produces items in the kiln block and stores creosote oil in the grate below the structure.

## Multiblock Structure

Build the kiln around the hatch:

- **Top center:** Coke Kiln Hatch, facing upward.
- **Middle center:** air.
- **Bottom center:** Coke Kiln Grate, facing downward.
- **Middle side centers:** exactly one Coke Kiln block and three Refractory Bricks.
- **All other shell blocks:** Refractory Bricks.

The Coke Kiln block is the item GUI and item automation point. The grate is the creosote tank and fluid extraction point.

<GameScene zoom="6" interactive={true} fullWidth={true}>
  <IsometricCamera yaw="45" pitch="30" />
  <ImportStructure src="coke_kiln_structure.snbt" />
</GameScene>

## Recipe

The Coke Kiln produces two primary recipes:

| Input | Output | Creosote Byproduct | Duration |
|---|---|---|---|
| Coal (1) | Coke (1) | 500 mB | 1800 ticks (90 seconds) |
| Log / Wood (1) | Charcoal (1) | 250 mB | 1800 ticks (90 seconds) |
| Peat Ore (1) | Coal (1) | 125 mB | 1800 ticks (90 seconds) |

- **Coke** burns longer than coal and is an essential ingredient for steel production in the Blast Furnace.
- **Creosote oil** is used as fuel for the Semifluid Generator and for crafting treated wood products.

## Slots

The Coke Kiln has 2 slots:

- **Input slot** (top): Fuel material (coal or logs)
- **Output slot** (bottom): Processed coke or charcoal

Creosote is not pushed automatically. The Coke Kiln Grate has an 8 bucket creosote tank that supports extraction only. Right-click the grate with fluid containers, or put a Pump Attachment on a pipe with its front face touching the grate to pull creosote into storage or a Semifluid Generator.

## Operation

Each operation takes **1800 ticks (90 seconds)**. Progress resets if the multiblock becomes invalid, the input no longer matches a recipe, the item output cannot accept the result, or the grate does not have enough free creosote capacity for the byproduct.
