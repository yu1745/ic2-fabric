---
navigation:
  title: Crop Harvester
  parent: index.md
  position: 63
  icon: ic2_120:crop_harvester
item_ids:
  - ic2_120:crop_harvester
---

# Crop Harvester

<BlockImage id="ic2_120:crop_harvester" p:facing="north" scale="4" />

The Crop Harvester is the automatic collector for IC2 crops. It walks a fixed scan cursor through a **9x3x9** box centered on the machine: 4 blocks outward in X and Z, and 1 block above and below. When the cursor reaches a harvestable IC2 crop, the machine runs the same crop harvest logic as manual harvesting and moves the products into its internal output slots.

## Energy and Storage

- **Internal storage**: 10,000 EU
- **Energy tier**: 1 (LV)
- **Maximum input**: 32 EU/t by default
- **EU output**: none
- **Work cadence**: one scan position every 10 ticks
- **Cost**: 1 EU per scan, plus 20 EU for each harvested drop stack
- **Inventory**: 15 output slots, 4 upgrade slots, and 1 battery discharge slot

## Operation

The harvester advances one coordinate per work step, so a full pass over the 9x3x9 area takes 243 scan steps. Empty blocks, crop sticks, vanilla crops, and non-IC2 block entities are ignored. The status text in the GUI shows the current scan cursor and how many crop positions were checked and harvested in the last work step.

A crop is harvested when its age is exactly that crop's optimal harvest age or its maximum age. Most crops use maximum age, while several special crops are picked earlier by design, such as Potatoes, Venomilia, and Eating Plants. Weeds are not harvested.

After a successful harvest, the crop follows its own `ageAfterHarvest` rule. Many crops stay planted at a lower age and regrow, but crops with no post-harvest age are replaced by an empty crop stick. Drops are inserted into the 15 output slots. If the output is completely full before harvesting, the machine skips the crop. If harvesting succeeds but some resulting stacks cannot fit, the leftovers are spawned at the machine.

The machine needs enough stored EU for both the scan and at least one harvest stack before it attempts work. It can charge from adjacent energy networks and from a battery item placed in the discharge slot.

## Upgrades

The Crop Harvester accepts:

- **Energy Storage Upgrades** to increase the 10,000 EU buffer.
- **Transformer Upgrades** to raise the accepted input tier.
- **Ejector Upgrades** to push harvested products out of the output slots.

Automation may insert upgrade items into the upgrade slots and batteries into the discharge slot. The output slots are output-only for automation; they are meant to be extracted from, not filled by pipes.

## Usage

Place the harvester near the center of an IC2 crop field and keep all target crop blocks within 4 blocks horizontally and 1 block vertically of the machine. Leave room in the 15 output slots or attach item extraction; otherwise full storage will pause harvesting. For continuous farms, pair it with crop care machines that keep the plants watered, fertilized, and protected while the harvester handles only the collection step.

## Recipe

Craft it from a Machine Casing, a Chest, 2 Electronic Circuits, 2 Crop Sticks, and 3 Iron Plates.
