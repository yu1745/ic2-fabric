---
navigation:
  title: Machine Upgrade System
  parent: index.md
  position: 204
  icon: minecraft:book
item_ids:
  - ic2_120:overclocker_upgrade
  - ic2_120:transformer_upgrade
  - ic2_120:energy_storage_upgrade
  - ic2_120:redstone_inverter_upgrade
  - ic2_120:ejector_upgrade
  - ic2_120:pulling_upgrade
  - ic2_120:fluid_ejector_upgrade
  - ic2_120:fluid_pulling_upgrade
---

# Machine Upgrade System

Many IC2-120 machines support **upgrade modules** that boost performance, add capabilities, or change how the machine behaves.

---

## Upgrade List

### Overclocker Upgrade

<ItemImage id="ic2_120:overclocker_upgrade" scale="2" />

**Speeds up the machine but increases energy use.**

**Code definition** (`OverclockerUpgradeComponent.kt`):
- `SPEED_PER_UPGRADE = 1f / 0.7f` ≈ 1.4286
- `ENERGY_PER_UPGRADE = 1.6f`

| Property | Value |
|------|------|
| Per upgrade | process time → 70% (speed × 1.4286) |
| Per upgrade | energy use → 160% (+60%) |
| Stacking | exponential |

**Formulas** with `n` Overclocker Upgrades:
- **Speed multiplier** = (1/0.7)^n ≈ 1.43^n
- **Energy multiplier** = 1.6^n

**Example** (base process time 100 s, base use 10 EU/t):

| Overclockers | Process Time | Speed Multi | Use (EU/t) | Energy Multi | Total EU |
|---:|---:|---:|---:|---:|---:|
| 0 | 100s | 1.0x | 10 | 1.0x | 1,000 |
| 1 | 70s | 1.43x | 16 | 1.6x | 1,120 |
| 2 | 49s | 2.04x | 25.6 | 2.56x | 1,254 |
| 3 | 34s | 2.92x | 41 | 4.1x | 1,403 |
| 4 | 24s | 4.18x | 65.5 | 6.55x | 1,573 |
| 5 | 17s | 5.96x | 105 | 10.5x | 1,768 |
| 8 | 9s | 11.2x | 428 | 42.8x | 3,709 |

**Note**: Overclocker Upgrades dramatically cut process time but the total energy consumed actually rises with more upgrades.

---

### Transformer Upgrade

<ItemImage id="ic2_120:transformer_upgrade" scale="2" />

**Raises a machine's input voltage tier.**

**Code definition** (`TransformerUpgradeComponent` + `EnergyTier`):
- Each upgrade adds +1 to `voltageTierBonus`; **effective voltage tier** = base `tier` + `voltageTierBonus` (see `ITieredMachine.effectiveTier`)
- Max input EU/t is `EnergyTier.euPerTickFromTier(effective tier)`: 32 EU/t for `tier ≤ 1`, then ×4 per additional tier (matching the nominal `ITiered.tier` on cables)

The table below assumes a **base tier of 1 (LV)** machine (one upgrade = one tier; multi-slot machines can stack more):

| Transformer Upgrades | Effective Tier | Name | Max Input (EU/t) | Matching Cables (Nominal Throughput) |
|---:|:---:|------|---:|---|
| 0 | 1 | LV | 32 | Tin, Insulated Tin |
| 1 | 2 | MV | 128 | Copper, Insulated Copper |
| 2 | 3 | HV | 512 | Gold, Insulated Gold, 2× Insulated Gold |
| 3 | 4 | EV | 2,048 | Iron, Insulated Iron, 2×/3× Insulated Iron |
| 4+ | 5 | Ultra-HV | 8,192 | Glass Fibre |

Note: 1×/2×/3× insulation on the same-tier Iron cable only affects **electrocution safety**; the **throughput is 2,048 EU/t** in all cases (see `CableBlocks.kt`).

**Uses**:
- Lets low-tier machines connect directly to a higher-voltage grid without an external step-down transformer
- **Pairs with Overclocker Upgrades**: Overclockers raise energy use, which the base input rate might not keep up with
- Example: a **Macerator** (LV, has upgrade slots) with 1 Transformer Upgrade has effective tier 2 and can pull from a **128 EU/t** line (e.g. a Copper / Insulated Copper grid)

**Note**: The **Electric Furnace** in this mod has no Transformer Upgrade slot, so it can't be used as an example for this upgrade.

**Pairing with Overclocker Upgrades**:

| Overclockers | Energy Multi | Furnace Base Use | Actual Use (EU/t) | Required Tier |
|---:|---:|---:|---:|---|
| 0 | 1.0x | 2 EU/t | 2 | LV (32) |
| 2 | 2.56x | 2 EU/t | 5.1 | LV (32) |
| 4 | 6.55x | 2 EU/t | 13.1 | LV (32) |
| 6 | 16.8x | 2 EU/t | 33.6 | MV (128) |
| 8 | 42.8x | 2 EU/t | 85.6 | MV (128) |
| 10 | 109x | 2 EU/t | 218 | HV (512) |

---

### Energy Storage Upgrade

<ItemImage id="ic2_120:energy_storage_upgrade" scale="2" />

**Adds internal energy capacity to the machine.**

**Code definition** (`IEnergyStorageUpgradeSupport.kt`):
- Each upgrade adds **10,000 EU** of capacity

| Property | Value |
|------|------|
| Per upgrade | +10,000 EU capacity |
| Stacking | linear |

**Capacity examples**:

| Upgrades | Bonus Capacity |
|---:|---:|
| 0 | 0 EU |
| 1 | 10,000 EU |
| 2 | 20,000 EU |
| 4 | 40,000 EU |
| 8 | 80,000 EU |

**Uses**:
- After installing a Transformer Upgrade, if the machine's base buffer is smaller than the per-tick input the upgrade allows, the Transformer Upgrade is effectively wasted: only `base buffer` EU can be charged per tick. A small-buffer machine with a Transformer Upgrade **must** also install Energy Storage Upgrades.
- Pairs with intermittent generators (Solar, Wind, etc.)
- Smooths grid fluctuations

---

### Redstone Inverter Upgrade

<ItemImage id="ic2_120:redstone_inverter_upgrade" scale="2" />

**Inverts the machine's redstone control logic.**

- **Without upgrade**: redstone signal → machine **stops**
- **With upgrade**: redstone signal → machine **runs**

**Uses**:
- Make the machine work **while** the redstone signal is on, instead of stopping
- Combine with redstone circuits for automation

---

### Ejector Upgrade

<ItemImage id="ic2_120:ejector_upgrade" scale="2" />

**Automatically ejects machine outputs into adjacent containers.**

**Code definition** (`EjectorUpgradeComponent.kt`):
- Eject fires immediately after each machine operation completes
- Supports item filters and direction settings

**Standard Ejector Upgrade**:
- Supports item filters
- Supports direction setting (down / up / north / south / west / east / any)

**Advanced Ejector Upgrade**:
- Same behavior as the standard version

**Item filter setup**:
- Hold the upgrade + target item in the off-hand + right-click → set filter
- Empty off-hand + right-click → clear filter (accepts everything)

**Direction setup**:
- Open the upgrade configuration screen and use six independent buttons to toggle down, up, north, south, west, and east.
- With all six directions disabled, the upgrade operates in any direction.
- Sneak + right-click remains a quick preset cycle.

The Ejector Upgrade is supported by machines that expose item output slots, including standard processing machines, the Miner, the Animal-Matron, and the Crop-Matron. Each machine explicitly chooses its work output slots; upgrade slots are never treated as ejectable machine output.

---

### Fluid Ejector Upgrade

<ItemImage id="ic2_120:fluid_ejector_upgrade" scale="2" />

**Automatically outputs the machine's internal fluid into adjacent containers.**

**Code definition** (`FluidPipeUpgradeComponent.kt`):
- Fluid eject fires after a machine operation completes
- Supports fluid filters and direction settings

**Fluid filter setup**:
- Hold the upgrade + a fluid container in the off-hand + right-click → set filter
- Sneak + right-click → clear filter

**Direction setup**:
- Open the upgrade configuration screen and toggle the six directions independently.
- With all six directions disabled, the upgrade operates in any direction.
- Sneak + right-click remains a quick preset cycle.

---

### Fluid Pulling Upgrade

<ItemImage id="ic2_120:fluid_pulling_upgrade" scale="2" />

**Automatically pulls fluid from adjacent containers into the machine.**

**Features**:
- Supports fluid filters
- Supports direction settings

**Setup**: same as the Fluid Ejector Upgrade

---

### Pulling Upgrade

<ItemImage id="ic2_120:pulling_upgrade" scale="2" />

**Automatically pulls valid items from adjacent containers into machine input slots.**

- Supports item filters and six independently configurable directions.
- Pulling targets are the machine's explicitly declared work input slots; upgrade slots are excluded.
- The upgrade is supported by standard item-processing machines, the Induction Furnace, the Animal-Matron, and the Crop-Matron.

Use the same filter and direction configuration screen as the Ejector Upgrade.

---

## Upgrade Slots

The number of upgrade slots varies by machine:

| Machine Type | Upgrade Slots | Examples |
|------|---:|------|
| Basic machines | 0-2 | Electric Furnace, Macerator, Compressor |
| Advanced machines | 2-4 | Induction Furnace, Thermal Centrifuge, Ore Washing Plant |
| Special machines | 4+ | Miner, Matter Generator |

Check each machine's GUI for the actual slot count.

---

## Recommended Builds

### High-Speed Processing Line
```
[Generator] → [Transformer] → [Overclocked Machine] → [Ejector Upgrade] → [Chest]
```
- Use Transformer Upgrades to let the machine take high-voltage input directly
- Stack multiple Overclocker Upgrades for faster processing
- Add an Ejector Upgrade to auto-collect outputs

**Example build** (Electric Furnace smelting iron):
- 4 Overclocker Upgrades → process time 24 s, use 65.5 EU/t
- 1 Transformer Upgrade → allows MV input (128 EU/t)
- 1 Ejector Upgrade → auto-ejects iron plates

### Energy Storage Buffer
```
[Intermittent Generator] → [Energy Storage] → [Machine with Energy Storage Upgrades]
```
- Stack multiple Energy Storage Upgrades on the machine
- Keeps the machine working while the generator is offline

**Example build** (Electric Furnace):
- 4 Energy Storage Upgrades → +40,000 EU capacity
- Can keep working for about 10 minutes offline (base 2 EU/t)

### Redstone Automation
```
Redstone signal → [Machine with Redstone Inverter] → [Ejector Upgrade]
```
- Machine runs while the redstone signal is on
- Output is auto-ejected

### Overclocker + Transformer

When the Overclocker count is high, the energy multiplier exceeds the base voltage's input rate, so a Transformer Upgrade is required:

| Overclockers | Furnace Use | Base Input (LV) | Upgrades Needed |
|---:|---:|---:|---|
| 0-5 | &lt;20 EU/t | 32 EU/t ✓ | None |
| 6-8 | 20-100 EU/t | 32 EU/t ✗ | 1 (MV) |
| 9-12 | 100-400 EU/t | 32 EU/t ✗ | 2 (HV) |
| 13+ | &gt;400 EU/t | 32 EU/t ✗ | 3+ (EV) |
