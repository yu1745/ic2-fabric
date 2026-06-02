---
navigation:
  title: Fermenter
  parent: index.md
  position: 32
  icon: ic2_120:fermenter
item_ids:
  - ic2_120:fermenter
---

# Fermenter

<BlockImage id="ic2_120:fermenter" p:facing="north" p:active="true" scale="4" />

The Fermenter turns Biomass into Biofuel by consuming heat (HU). It has separate 8 bucket tanks for Biomass input and Biofuel output, a 40,000 HU heat buffer, and produces Fertilizer as a side product.

## Operation

- **Input tank:** 8,000 mB Biomass
- **Output tank:** 8,000 mB Biofuel
- **Heat buffer:** 40,000 HU
- **Cycle time:** 40 ticks
- **Per cycle:** 20 mB Biomass + 4,000 HU -> 400 mB Biofuel
- **Side product:** 1 Fertilizer for every 1,000 mB Biomass consumed

The machine only advances while it has enough buffered heat, at least 20 mB Biomass, and at least 400 mB free space in the Biofuel tank. At full speed it needs an average of 100 HU/t.

## Heat Setup

Heat is transferred directly between adjacent machines through their heat-transfer faces. Place the Fermenter directly against a heat source so the two heat-transfer faces touch each other.

A Liquid Heat Exchanger can run a Fermenter at full speed when it has 10 Heat Conductors installed. Place the Liquid Heat Exchanger and Fermenter face-to-face, then supply the exchanger with Lava or Hot Coolant and keep its output fluid drained.

## Slots

- Left upper slot: filled Biomass container input
- Left lower slot: empty container output from drained Biomass buckets/cells
- Right upper slot: empty bucket/cell input for Biofuel filling
- Right lower slot: filled Biofuel container output
- Bottom center slot: Fertilizer output
- Two bottom-right slots: Fluid Ejector or Fluid Pulling upgrades

Biomass buckets, Biomass cells, and fluid cells containing Biomass can fill the input tank. Empty buckets, Empty Cells, and empty fluid cells can be filled from the Biofuel tank.

## Automation

The Fermenter exposes fluid storage on all sides: pipes can insert Biomass into the input tank and extract Biofuel from the output tank. Fluid Pulling upgrades pull Biomass from nearby fluid storages, and Fluid Ejector upgrades push Biofuel outward.

## Recipe

<Recipe id="ic2_120:fermenter" />
