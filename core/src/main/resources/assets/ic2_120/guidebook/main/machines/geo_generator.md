---
navigation:
  title: Geothermal Generator
  parent: index.md
  position: 11
  icon: ic2_120:geo_generator
item_ids:
  - ic2_120:geo_generator
---

# Geothermal Generator

<BlockImage id="ic2_120:geo_generator" p:facing="north" p:active="true" scale="4" />

The Geothermal Generator turns ordinary Lava into EU. Its internal lava tank holds **8 buckets**, and each bucket burns for **1200 ticks (60 seconds)** at **20 EU/t**, for **24,000 EU per bucket**. Lava is consumed gradually, so the generator pauses automatically when its 10,000 EU buffer is full.

Only normal Lava is accepted. Pahoehoe Lava, Biofuel, Biomass, hot coolant, and other fluids are ignored.

## Output

- **EU Output**: 20 EU/t
- **Energy Storage**: 10,000 EU
- **Tier**: 1
- **Lava Tank**: 8 buckets
- **Lava Consumption**: about 0.83 mB/t, or 1 bucket every 60 seconds
- **Fuel Value**: 24,000 EU per bucket

## Slots

- Fuel slot: Lava Buckets or Lava Cells. The machine empties them into its internal tank only when there is room for a full bucket.
- Empty container slot: receives Empty Buckets or Empty Cells from the fuel slot.
- Battery slot: charges one battery or electric tool from the internal EU buffer.

The fuel slot does not process generic fluid cells directly. For generic fluid containers, right-click the block with the container or pipe the Lava in through the fluid interface.

## Automation

Fluid pipes and tanks can insert Lava into the Geothermal Generator from any side. The internal tank is input-only for automation, so adjacent pipes or tanks cannot pull Lava back out of it. This makes the generator a good endpoint for a lava line rather than a shared buffer.

Item automation can insert Lava Buckets or Lava Cells into the fuel slot and chargeable items into the battery slot. Empty Buckets, Empty Cells, fuel items, and the battery slot can be extracted by item automation.

The machine has no upgrade slots. Overclocker, Transformer, Energy Storage, Ejector, Fluid Ejector, and Pulling upgrades do not apply to it.

For a compact lava setup, use a Pump to collect Lava into its own tank, then use the Pump's fluid output automation or an intermediate Tank to feed the Geothermal Generator. A Tank next to the generator is useful because the generator only stores 8 buckets itself while Bronze and Iron Tanks hold 32 buckets.

The Geothermal Generator does not accept EU input. It outputs up to 20 EU/t from every side except its front face, and it can also spend that stored EU charging the item in its battery slot.

## Recipe

<Recipe id="ic2_120:geo_generator" />
