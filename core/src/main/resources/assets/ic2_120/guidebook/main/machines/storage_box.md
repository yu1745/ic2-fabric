---
navigation:
  title: Storage Box
  parent: index.md
  position: 206
  icon: ic2_120:iron_storage_box
---

# Storage Box

<BlockImage id="ic2_120:iron_storage_box" scale="4" />

The Storage Box is a pure bulk item storage block. It comes in 5 material tiers, with capacity increasing per tier. It does **not consume EU, does not support upgrades, and is not involved in any machine mechanics** — it is purely for storing items.

## Five Tier Comparison

| Storage Box | Slots | Hardness | Blast Resistance | Core Recipe Materials |
|-------------|-------|----------|------------------|------------------------|
| Wooden Storage Box | 27 slots | 2.5 | 2.5 | Oak Log + Oak Planks |
| Bronze Storage Box | 45 slots | 5.0 | 6.0 | Bronze Plate + Bronze Casing |
| Iron Storage Box | 45 slots | 5.0 | 6.0 | Iron Plate + Iron Casing |
| Steel Storage Box | 63 slots | 5.0 | 6.0 | Steel Plate + Steel Casing |
| Iridium Storage Box | 126 slots | 6.0 | 8.0 | Iridium Plate + Steel Plate |

The Bronze Storage Box and Iron Storage Box share the same capacity (45 slots); they are simply material variants — craft whichever plates you have available.

## How to Use

### Storing and Withdrawing Items

Use it just like a regular chest: right-click to open the GUI, drag items in or out. All Storage Boxes display their inventory in a scrolling view (9 columns x variable rows); larger capacities have more scrollable rows.

### Inspecting Contents

While holding a Storage Box item, the tooltip shows the total number of items stored and the number of slots occupied:

```
Item count: 128 (3 slots)
```

### Redstone Comparator

Storage Boxes support redstone comparator output. Signal strength scales with inventory fullness (empty = 0, full = 15).

## Dismantling and Moving

A Storage Box behaves like a **Shulker Box** — no matter how it is broken, it drops as an item and **fully retains its contents**. This is by design, not a bug.

| Method | Contents Preserved | Notes |
|--------|--------------------|-------|
| **Wrench left-click** (instant dismantle) | **All preserved** | Costs 10 wrench durability (or 1,000 EU for the electric wrench) |
| **Bare hand / any tool** | **All preserved** | Normal mining speed, no extra tool durability consumed |

To move a Storage Box:
1. Break the box; it drops as an NBT-tagged item (containing all stored items)
2. Pick up the item
3. Place it at the new location
4. Everything inside is intact, ready to keep using

### Wrench vs No Wrench

| Property | Wrench Left-Click | Bare Hand / Other Tool |
|----------|-------------------|------------------------|
| Mining speed | Instant | Depends on block hardness and tool |
| Tool wear | 10 durability / 1,000 EU | No extra wear |

## Internal Mechanics

Each Storage Box uses the `Inventory` interface, with data stored in the BlockEntity's NBT. When the block is broken, `onStateReplaced` serializes the entire inventory into the dropped item's `BlockEntityTag`, and the data is deserialized back when placed. This is identical to the Shulker Box mechanism.

## What It Cannot Do

- **No EU support**: pure item storage, no power involved
- **No fluid support**: cannot hold liquids
- **No upgrade support**: capacity is determined by the material tier and cannot be expanded with upgrade components
- **No automatic input/output**: Storage Boxes only interact through the GUI; they do not expose automation ports (except through third-party mods' pipe systems, since they implement the `Inventory` interface)

## Related

- [Energy Storage](energy_storage.md) — stores EU energy, not items
- [Fluid Tank](tank.md) — stores fluids

