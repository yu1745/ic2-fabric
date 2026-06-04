---
navigation:
  title: EU Energy System
  parent: index.md
  position: 214
  icon: minecraft:book
---

# EU Energy System

The mod uses `EU` as its unit of electrical energy. Generators, energy storage, and consumer devices form a power grid through cables.

## Cable Quick Reference

| Cable | Max Throughput | Loss per Block | Notes |
|---|---:|---:|---|
| Tin Cable | 32 EU/t | 0.2 EU/block | Entry-level low voltage |
| Copper Cable | 128 EU/t | 0.2 EU/block | Common low/medium voltage |
| Gold Cable | 512 EU/t | 0.4 EU/block | Medium voltage |
| HV Cable (Iron) | 2048 EU/t | 0.8 EU/block | High voltage |
| Glass Fibre Cable | 8192 EU/t | 0.025 EU/block | Ultra-high voltage, no leakage |

## Electrocution and Cable Burn Rules

- **Electrocution**: When the `grid output tier` exceeds the cable's insulation tier, the cable leaks and damages nearby entities
- **Cable burn**: When the `grid output tier` exceeds the cable's voltage tolerance, low-tolerance cables burn out randomly (not all at once)
- **Glass Fibre Cable**: Highest insulation tier, never leaks and never burns out from overvoltage

## Overvoltage Explosion

When a machine's **effective voltage tolerance** on a connected side is lower than the grid's `outputLevel`, the machine will **explode directly**.

### Effective Voltage Tolerance

```
Effective voltage tolerance = machine base tier + bonus tiers from Transformer Upgrades
```

- Machine base tier: the machine's own voltage rating
- Transformer Upgrade: each installed upgrade adds +1 tier
- Per-side voltage: devices like transformers may have different ratings on different sides

### Explosion Rules

- **Generators are exempt from overvoltage checks**: generators hooked into a high-voltage grid will not explode
- **Machine explosion**: non-generator machines with insufficient tolerance will explode
- **Dropped items**: the machine itself and every item in its slots **all disappear**, nothing is dropped
- **Explosion damage**: tier 4 corresponds to 10 hearts (explosion power 2); damage doubles each tier up

### Voltage Tolerance Reference

| Tier | Name | Max Input EU/t |
|------|------|---:|
| 1 | LV | 32 |
| 2 | MV | 128 |
| 3 | HV | 512 |
| 4 | EV | 2,048 |
| 5 | Ultra-HV | 8,192 |

### Examples

- Energy Storage (Tier 1) connected to a Gold Cable (512 EU/t) grid: the storage will explode
- MFSU (Tier 4) connected to Glass Fibre (8192 EU/t) grid: works normally
- Macerator (Tier 1) + 2 Transformer Upgrades: effective tolerance = 3 (HV), can connect to a 512 EU/t grid

---

## Grid Transmission

### Energy Pool

Each grid is a shared energy pool; every cable member shares the same capacity and stored energy.

### Transfer Priority

1. First, supply consumers from the grid's buffer pool
2. Then, supply consumers directly from providers

### Path Loss

- The optimal path is recalculated every tick (Dijkstra)
- Loss is the sum of each cable's `cableLossMilliEu` along the path
- The actual throughput is limited by the lowest-capacity cable on the path

### Cable Throughput

Each cable has an independent transfer rate (transferRate) per tick; it decreases after a successful transfer, preventing same-tick overload.

---

## Related Pages

- [EU Cables and Transformers](../reference/eu_cables.md)
- [Batteries and Mobile Power](../reference/energy_items.md)
- [Power Generation and Storage](../guides/power_generation.md)
