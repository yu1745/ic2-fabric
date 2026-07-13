---
navigation:
  title: Machine System
  parent: index.md
  position: 218
  icon: minecraft:book
---

# Machine System

IC2 Fabric machines run on the EU grid and connect via EU cables. Some machines support upgrade slots that improve their performance.

---

## Processing Machines

### Electric Furnace

- **Voltage tier**: Tier 1 (LV)
- **Energy buffer**: 416 EU
- **Max input**: 32 EU/t
- **Energy use**: 3 EU/t
- **Process time**: 130 ticks (6.5 seconds)
- **Total energy per operation**: 390 EU
- **Upgrade slots**: none

### Induction Furnace

- **Voltage tier**: Tier 2 (MV)
- **Energy buffer**: 1,600 EU
- **Max input**: 128 EU/t
- **Energy use**: 150 EU per operation + 1 EU/t heat upkeep
- **Process time**: 10 ticks (at 100% heat)
- **Heat system**: 0-100%; takes 100 seconds to heat from 0% to 100%
- **Notes**: processes two slots at once; supports advanced materials

### Macerator

- **Voltage tier**: Tier 1 (LV)
- **Energy buffer**: 416 EU
- **Max input**: 32 EU/t
- **Energy use**: 2 EU/t
- **Process time**: 130 ticks (6.5 seconds)
- **Total energy per operation**: 260 EU
- **Upgrade slots**: 4 (supports Overclock / Energy Storage / Transformer)

Crushes ores into dust, improving smelting yields.

### Compressor

- **Voltage tier**: Tier 1 (LV)
- **Energy buffer**: 416 EU
- **Max input**: 32 EU/t
- **Energy use**: 2 EU/t
- **Process time**: 130 ticks (6.5 seconds)
- **Total energy per operation**: 260 EU
- **Upgrade slots**: 4

Used for plate pressing and advanced materials, e.g. compressing iron ingots into iron plates.

### Extractor

- **Voltage tier**: Tier 1 (LV)
- **Energy buffer**: 416 EU
- **Max input**: 32 EU/t
- **Energy use**: 2 EU/t
- **Process time**: 130 ticks (6.5 seconds)
- **Total energy per operation**: 260 EU
- **Upgrade slots**: 4

Extracts fluid from plants, e.g. pulling resin from rubber trees.

### Metal Former

- **Voltage tier**: Tier 1 (LV)
- **Energy buffer**: 400 EU
- **Max input**: 32 EU/t
- **Energy use**: 10 EU/t
- **Process time**: 200 ticks (10 seconds)
- **Total energy per operation**: 2,000 EU
- **Upgrade slots**: 4
- **Modes**: Rolling / Cutting / Extruding (cycle with a wrench)

---

## Resource Machines

### Thermal Centrifuge

- **Voltage tier**: Tier 2 (MV)
- **Energy buffer**: 10,000 EU
- **Max input**: 128 EU/t
- **Process energy**: 48 EU/t
- **Heating energy**: 1 EU/t (not affected by Overclock)
- **Total energy**: 49 EU/t
- **Process time**: 500 ticks (25 seconds)
- **Total energy per operation**: 24,500 EU
- **Heat system**: 0-5000, +1 per tick
- **Upgrade slots**: 4

### Ore Washing Plant

- **Voltage tier**: Tier 1 (LV)
- **Energy buffer**: 16,000 EU
- **Max input**: 32 EU/t
- **Energy use**: 16 EU/t
- **Process time**: 500 ticks (25 seconds)
- **Total energy per operation**: 8,000 EU
- **Fluid use**: 1 bucket of water per operation
- **Upgrade slots**: 4

### Miner

- **Voltage tier**: Tier 3 (HV)
- **Energy buffer**: 10,000 EU (base) + Energy Storage upgrade
- **Max input**: 512 EU/t
- **Scan cost**: 64 EU per scan
- **Mining cost**: 500 EU per block (normal) / 1500 EU per block (diamond/iridium)
- **Extra-mine cost**: Silk Touch ×10
- **Upgrade slots**: 4
- **Notes**: requires an Ore Scanner to define the mining area; supports allow/deny lists and Silk Touch

---

## Power Generation

### Generator

- **Voltage tier**: Tier 1 (LV)
- **Energy buffer**: 4,000 EU
- **Output**: 10 EU/t
- **Fuel**: 1 coal = 4,000 EU
- **Wiring**: front face cannot output; all other faces accept cables

### Stirling Generator

- **Voltage tier**: Tier 1 (LV)
- **Output**: 10 EU/t
- **Fuel**: assorted fuels (coal, charcoal, lava bucket, etc.)

### Geothermal Generator

- **Voltage tier**: Tier 1 (LV)
- **Energy buffer**: 10,000 EU
- **Output**: 20 EU/t
- **Fluid use**: 1 bucket of lava = 24,000 EU (1200 ticks / 60 seconds at 20 EU/t)

### Solar Distiller

- **Fluid output**: coolant (distilled from water)
- **Requirements**: water source + sunlight
- **Fluid capacity**: see in-game values

### Wind Kinetic Generator

See [Kinetic System](kinetic_transmission.md)

---

## Fluid Machines

### Pump

- **Voltage tier**: Tier 1 (LV)
- **Energy buffer**: 800 EU
- **Max input**: 32 EU/t
- **Energy use**: 20 EU/bucket
- **Per-tick extraction**: 1000 mB
- **Upgrade slots**: 4
- **Requires**: a fluid pipe network connection

### Fluid Bottler

- **Fills**: loads fluid into containers
- **Requires**: a fluid pipe network

### Fluid Bottler

- **Bottling**: similar to a bottler

### Solid Canner

- **Fills solid items** into containers, e.g. compressed air cells

---

## Heat Machines

### Fluid Heat Generator

See [Heat System](heat_system.md)

- **Biofuel**: 32 HU/t, 10 mB/s

### Electric Heat Generator

See [Heat System](heat_system.md)

- **Coils**: 10 HU/t per coil; 100 HU/t fully loaded
- **1 EU = 1 HU**

### Solid Heat Generator

See [Heat System](heat_system.md)

- **Coal/charcoal**: 20 HU/t, 8000 HU per fuel

### Fluid Heat Exchanger

- **Pairs with the nuclear reactor**: converts HU into coolant

---

## Transformers

### Transformer

- **Function**: shifts grid voltage tier
- **LV → MV**: increases input capacity
- **MV → HV**: increases input capacity
- **HV → EV**: increases input capacity
- **Each Transformer Upgrade installed**: +1 voltage tier

---

## Storage Machines

### BatBox

- **Tier 1**: 40,000 EU / 32 EU/t
- **Wiring**: front face input only; all other faces are output

### CESU

- **Tier 2**: 300,000 EU / 128 EU/t

### MFE

- **Tier 3**: 4,000,000 EU / 512 EU/t

### MFSU

- **Tier 4**: 40,000,000 EU / 2048 EU/t

See [Power Generation and Storage](../guides/power_generation.md)

---

## Other Machines

### Recycler

- **Function**: recycles items into a small amount of resources
- **Failure chance**: yes

### Fermenter

- **Biomass fermentation**: produces biogas
- **Heat requirement**: 4000 HU per cycle
- **Output**: 400 mB biogas per cycle

### Coke Oven

- **Cokes coal into coke**: a charcoal by-product

### Coke Kiln

- **Industrial coking**

### Block Cutter

- **Cuts blocks into plates**

### Magnetizer

- **Magnetizes iron blocks for logistics**

### Crop Harvester

- **Automatically harvests mature crops**

### Cropmatron

- **Manages a 9×9 area**: auto-watering, fertilizing, and using weed-EX

### Luminator Flat

- **Large light source**

### Chunk Loader

- **Keeps chunks loaded for remote machines**

### Matter Generator

- **Converts EU into matter** (planned)

### Replicator

- **Replicates items** (planned)

### Teleporter

- **Long-distance teleportation** (planned)

### Tesla Coil

- **Wireless energy transmission** (planned)

---

## Upgrade Slot Reference

| Machine | Upgrade Slots | Supported Upgrades |
|------|---:|---|
| Electric Furnace | 0 | none |
| Induction Furnace | 2 | Energy Storage, Transformer, Ejector, Pulling |
| Macerator | 4 | Overclock, Energy Storage, Transformer, Ejector, Pulling |
| Compressor | 4 | Overclock, Energy Storage, Transformer, Ejector, Pulling |
| Extractor | 4 | Overclock, Energy Storage, Transformer, Ejector, Pulling |
| Metal Former | 4 | Overclock, Energy Storage, Transformer, Ejector, Pulling |
| Thermal Centrifuge | 4 | Overclock, Energy Storage, Transformer, Ejector, Pulling |
| Ore Washing Plant | 4 | Overclock, Energy Storage, Transformer, Ejector, Pulling |
| Miner | 4 | Overclock, Energy Storage, Transformer, Ejector |
| Pump | 4 | Overclock, Energy Storage, Transformer, Fluid Ejector/Inserter |
