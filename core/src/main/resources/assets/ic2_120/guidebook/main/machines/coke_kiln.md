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

The Coke Kiln is a multiblock steam-age structure that processes coal and wood logs into high-quality coke and charcoal, producing creosote oil as a byproduct.

## Multiblock Structure

The Coke Kiln multiblock is built from the following components:

- **Coke Kiln**: The main processing block of the structure.
- **Coke Kiln Grate**: Forms the base of the kiln structure.
- **Coke Kiln Hatch**: Provides item I/O access to the kiln for automated feeding and extraction.

## Recipes

The Coke Kiln produces two primary recipes:

| Input | Output | Creosote Byproduct | Duration |
|---|---|---|---|
| Coal (1) | Coke (1) | 500 mB | 1800 ticks (90 seconds) |
| Log / Wood (1) | Charcoal (1) | 250 mB | 1800 ticks (90 seconds) |

- **Coke** burns longer than coal and is an essential ingredient for steel production in the Blast Furnace.
- **Creosote oil** is used as fuel for the Semifluid Generator and for crafting treated wood products.

## Slots

The Coke Kiln has 2 slots:

- **Input slot** (top): Fuel material (coal or logs)
- **Output slot** (bottom): Processed coke or charcoal

Creosote oil is automatically extracted from the output tank. Connect fluid pipes to collect the byproduct.

## Operation

Each operation takes **1800 ticks (90 seconds)**, consuming 1 piece of coal or log and producing the corresponding output along with creosote oil. The kiln processes one item at a time and does not require EU to operate — it runs on its own internal heat mechanism.
