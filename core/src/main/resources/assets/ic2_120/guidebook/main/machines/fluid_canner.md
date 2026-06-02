---
navigation:
  title: Fluid Canner
  parent: index.md
  position: 36
  icon: ic2_120:fluid_canner
item_ids:
  - ic2_120:fluid_canner
---

# Fluid Canner

<BlockImage id="ic2_120:fluid_canner" p:facing="north" p:active="true" scale="4" />

The Fluid Canner moves one bucket of fluid between containers and its internal tank. It has no manual mode switch: each cycle it first tries to fill an empty container in the upper input slot, then drain a filled container from that slot, then fill an empty container in the lower input slot.

## Operation

- **Tank:** 8 buckets / 8,000 mB, one fluid type at a time
- **Tier:** 1
- **EU storage:** 250 EU
- **Max input:** 32 EU/t
- **Energy use:** 2 EU/t
- **Cycle time:** 100 ticks, using 200 EU per bucket

The tank accepts IC2 registered fluids through its fluid storage. Cells can carry any supported fluid, including Biomass, Biofuel, Steam, and Superheated Steam. Empty buckets are only filled as Water Buckets or Lava Buckets; use Empty Cells or fluid cells for other fluids.

## Slots

- Upper left slot: filled fluid container input, or an empty container that should be filled first
- Lower left slot: empty container input
- Right slot: filled or emptied container output
- Battery slot: discharges batteries into the machine
- Four upgrade slots: accepts supported machine upgrades

Draining a filled bucket or cell adds one bucket to the tank and returns the empty bucket or Empty Cell to the output slot. Filling consumes one bucket from the tank and outputs the matching filled cell or bucket. A Foam Sprayer or CF Pack can be filled directly with Construction Foam instead of producing an output item.

## Automation and Upgrades

Fluid pipes can insert into and extract from the internal tank on any side. Fluid Pulling Upgrades actively pull fluid from adjacent fluid storages into the tank, and Fluid Ejector Upgrades actively push tank contents outward. Item Pulling Upgrades pull valid containers into the two input slots, and Ejector Upgrades push the output slot.

Overclocker Upgrades speed up cycles and increase EU/t use. Energy Storage Upgrades add buffer capacity. Transformer Upgrades raise the accepted voltage tier and max input.

## Related

- <ItemLink id="ic2_120:fermenter" /> turns Biomass into Biofuel.
- <ItemLink id="ic2_120:steam_generator" /> and <ItemLink id="ic2_120:steam_kinetic_generator" /> use Steam in the power chain.
- <ItemLink id="ic2_120:empty_cell" />, <ItemLink id="ic2_120:fluid_cell" />, <ItemLink id="ic2_120:biomass_cell" />, and <ItemLink id="ic2_120:biofuel_cell" /> are common container choices.

## Recipe

<Recipe id="ic2_120:fluid_canner" />
