---
navigation:
  title: Advanced Solar Addon
  parent: index.md
  position: 205
  icon: ic2_120_advanced_solar_addon:advanced_solar_panel
---

# Advanced Solar Addon

<BlockImage id="ic2_120_advanced_solar_addon:advanced_solar_panel" scale="4" />

This addon module for Industrial Craft 2 (IC2) provides 4 solar generators, 1 molecular transformer, and 1 ultimate generator.

---

## 1. Solar Generator Series

All four solar generators **only work in the Overworld** and require a clear view of the sky above them (generation stops if an opaque block blocks the line of sight). Weather detection is supported for automatic shutdown on rainy/snowy days.

| Property | Advanced | Hybrid | Ultimate Hybrid | Quantum |
|------|------|------|----------|------|
| **Block Name** | Advanced Solar Panel | Hybrid Solar Panel | Ultimate Hybrid Solar Panel | Quantum Solar Panel |
| **Daytime Output** | 8 EU/t | 64 EU/t | 512 EU/t | 4,096 EU/t |
| **Nighttime Output** | 1 EU/t | 8 EU/t | 64 EU/t | 2,048 EU/t |
| **Internal Storage** | 32,000 EU | 100,000 EU | 1,000,000 EU | 10,000,000 EU |
| **Output Tier** | Tier 1 (LV) | Tier 2 (MV) | Tier 3 (HV) | Tier 5 (EV) |
| **Max Output** | 32 EU/t | 128 EU/t | 512 EU/t | 8,192 EU/t |
| **Battery Charging Slots** | None | None | None | 4 |

Nighttime generation is the defining feature of these advanced solar panels — while standard solar panels shut down completely at night, these continue producing power at a reduced rate.

### General Working Mechanism

All solar panels share the same internal working principle:

- **Output Direction**: EU is automatically pushed from the internal buffer to adjacent cables or machines (output on all six sides, no input)
- **Generation Conditions**: Daytime, clear weather → daytime output; daytime, rain/snow (in rain-bearing biomes) → no output; nighttime → nighttime output; requires a clear view of the sky directly above
- **Output Strategy**: Once the internal buffer reaches one packet of the panel's output tier, it pushes outward, up to the cap of one voltage tier per packet
- **GUI**: Right-click to open, showing current generation status, daytime output, nighttime output, and internal buffer

### Panel Details

#### 1. Advanced Solar Panel

- **Registry Name**: `advanced_solar_panel`
- **Crafting**: 1 Basic Solar Panel + Advanced Alloy + Blast-Proof Glass + Advanced Circuit + Radiant Reinforced Plate
- **Positioning**: The leap from basic to advanced solar. 8 EU/t daytime output is enough to reliably drive a basic machine pipeline, and 1 EU/t nighttime output ensures the storage box never fully drains at night.

#### 2. Hybrid Solar Panel

- **Registry Name**: `hybrid_solar_panel`
- **Crafting**: 1 Advanced Solar Panel + Carbon Plate + Reinforced Iridium Plate + Reinforced Sunnarium + Advanced Circuit + Lapis Block
- **Positioning**: Mid-tier workhorse. 64 EU/t daytime output can easily drive high-drain equipment like the induction furnace and compressor, and 8 EU/t nighttime output exceeds a standard panel's daytime output.

#### 3. Ultimate Hybrid Solar Panel

- **Registry Name**: `ultimate_solar_panel`
- **Crafting**: 1 Advanced Solar Panel + Reinforced Sunnarium + Coal Block + Lapis Block (note: the hybrid panel itself is not required — you upgrade directly from the advanced panel)
- **Positioning**: High-tier workhorse. 512 EU/t daytime output marks the entry into the HV era, and the 1M EU internal buffer is equivalent to an MFSU-class buffer.

#### 4. Quantum Solar Panel

- **Registry Name**: `quantum_solar_panel`
- **Crafting**: 8 Ultimate Hybrid Solar Panels surrounding 1 Quantum Core
- **Positioning**: **The ultimate solar solution.** 4,096 EU/t daytime and 2,048 EU/t nighttime — even at night it outputs 4x more than the ultimate panel's daytime rate. The 10M EU internal buffer and 4 battery charging slots make it not only a generator but also a large charging station.
- **Charging Capability**: 4 built-in battery charging slots can charge 4 batteries/electric tools at once (with automatic voltage tier compatibility detection)

---

## 2. Molecular Transformer

- **Registry Name**: `molecular_transformer`
- **Voltage Tier**: Tier 10 (Extreme High Voltage)
- **Max Input**: 2,097,152 EU/t
- **Internal Storage**: 10,000,000 EU
- **Slots**: 1 input slot + 1 output slot
- **Energy Flow**: Accepts EU only, **cannot output** (takes power, gives nothing back)

### Function

The Molecular Transformer is a high-energy **item conversion device** that uses massive amounts of EU to restructure matter at the molecular level, converting one item into another. Recipes are defined in the config file `ic2_120_advanced_solar_addon.json` and can be modified at runtime by the server via commands.

### Default Recipes

| Input | Output | Energy Cost |
|------|------|---------|
| Wither Skeleton Skull | Nether Star | 250,000,000 EU |
| Iron Ingot | Iridium Ore | 9,000,000 EU |
| Coal | Diamond | 9,000,000 EU |
| Glowstone | Sunnarium | 9,000,000 EU |
| Fluorite Dust | Sunnarium Component | 1,000,000 EU |
| Glowstone Dust | Sunnarium Component | 1,000,000 EU |
| Netherrack | Gunpowder | 70,000 EU |
| Sand | Gravel | 50,000 EU |
| Dirt | Clay | 50,000 EU |
| Yellow Wool | Glowstone | 500,000 EU |
| Blue Wool | Lapis Block | 500,000 EU |
| Red Wool | Redstone Block | 500,000 EU |
| Tin Ingot | Silver Ingot | 500,000 EU |
| Silver Ingot | Gold Ingot | 500,000 EU |

### Workflow

1. Place a valid input item in the input slot
2. The machine begins consuming EU, with a progress bar shown in the GUI
3. Once the required energy has been consumed, the input item is consumed and the output item appears in the output slot
4. Supports auto-input (hoppers/pipes feed convertible items from the top or sides) and auto-output (extract the product from the bottom or sides)

### About Recipe Configurability

All recipes are stored in the config file and **server administrators can freely add, remove, or modify them**. This means recipes may differ between servers — when in doubt, check the JEI recipe viewer.

---

## 3. Quantum Generator

- **Registry Name**: `quantum_generator`
- **Voltage Tier**: Tier 3 (HV)
- **Output**: 512 EU/t (constant)
- **Internal Storage**: 1,000,000 EU
- **Max Output**: 512 EU/t
- **Slots**: None
- **Redstone Control**: **Stops generating when receiving a redstone signal**

### Function

The Quantum Generator is a pure EU generator — no fuel, no sunlight, no consumables required. Once placed, it generates a constant 512 EU/t. As long as the power is consumed or stored in time, it is a maintenance-free permanent power source.

### Working Mechanism

- **Always On**: As long as it isn't suppressed by a redstone signal, it produces 512 EU per tick into the internal buffer
- **Auto Output**: Once the internal buffer accumulates energy, it is automatically pushed to adjacent cables or machines (**the front face cannot output**, the other five faces can)
- **Redstone Shutdown**: Receiving a redstone signal on any of the six sides stops generation — great for chaining with a redstone clock, temperature switch, or storage box full signal
- **No GUI**: The Quantum Generator has no GUI and no item slots; right-clicking does nothing

### Typical Applications

- **Base Power Supply**: A single Quantum Generator steadily outputs 512 EU/t, enough to support a mid-sized base's daily operation
- **Voltage Stabilization with a Storage Box**: Connect to an MFSU or CESU as a buffer to handle sudden high power draws
- **Redstone-Linked Shutdown**: Use a comparator to read a full storage box and send a redstone signal to automatically shut it off, avoiding wasted energy

---

## 4. Comprehensive Recommendations

### Solar Panel Upgrade Path

```
Basic Solar → Advanced Solar → Hybrid Solar → Ultimate Hybrid Solar → Quantum Solar
  1 EU/t       8 EU/t         64 EU/t        512 EU/t                4,096 EU/t
```

Tier-skipping is allowed: the Ultimate Solar Panel can be crafted directly from the Advanced Solar Panel without going through the Hybrid Panel.

### Recommended Power Plans

| Stage | Recommended Setup |
|------|---------|
| Early | A few Advanced Solar Panels + BatBox storage |
| Mid | Hybrid/Ultimate Solar Panels + CESU/MFE storage |
| Late | Quantum Solar Panels + MFSU array, or a continuously running Quantum Generator |
| Endgame | Quantum Solar Panel array paired with a Molecular Transformer to mass-produce Nether Stars/Diamonds |

### Energy Matching

- The Molecular Transformer is extremely power-hungry (a single diamond costs 9M EU) and will explode if connected to standard cables. Use **glass fibre cable** or **cryogenic superconducting cable** for transmission.
- A single Quantum Solar Panel outputs up to 8,192 EU/t and must use the matching cable tier; route through an MFSU first before distributing to devices.

---

## 5. Related Documentation

- [Thermal Generator](../generator.md) - Basic EU generation
- [Energy Storage](energy_storage.md) - Energy storage solutions
