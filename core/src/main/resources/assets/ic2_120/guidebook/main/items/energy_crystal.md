---
navigation:
  title: Energy Crystal and Lapotron Crystal
  parent: index.md
  position: 321
  icon: ic2_120:energy_crystal
item_ids:
  - ic2_120:energy_crystal
  - ic2_120:lapotron_crystal
  - ic2_120:energy_crystal_wireless
  - ic2_120:lapotron_crystal_wireless
---

# Energy Crystal and Lapotron Crystal

<ItemImage id="ic2_120:energy_crystal" scale="4" />

The <ItemLink id="ic2_120:energy_crystal" /> and the <ItemLink id="ic2_120:lapotron_crystal" /> are IC2's two highest-tier hand-held rechargeable batteries. They are the standard power source for the most energy-hungry electric gear in the mod — the mining laser, jetpack, and similar end-game tools.

## Item View

| Energy Crystal | Lapotron Crystal |
|:---:|:---:|
| <ItemImage id="ic2_120:energy_crystal" scale="2" /> | <ItemImage id="ic2_120:lapotron_crystal" scale="2" /> |

## Stats

Both crystals behave like a Re-Battery in the sense that they can be charged in any energy storage block, but they are designed for sustained high-throughput discharge. A single Lapotron Crystal can run a mining laser through many tunnels between charges.

| Item | Tier | Max EU | Notes |
|------|:---:|---:|-------|
| <ItemLink id="ic2_120:energy_crystal" /> | 3 | 1,000,000 EU | Mid-to-late game cell. The upgrade ingredient for the Energy Pack. |
| <ItemLink id="ic2_120:lapotron_crystal" /> | 4 | 10,000,000 EU | Highest-tier hand-held cell. The upgrade ingredient for the LapPack. |

For the canonical tier ladder including the Re-Battery family, see [Batteries and Mobile Power](../reference/energy_items.md).

## How to Use

### Powering High-Drain Tools

Use a crystal when a tool draws more EU per tick than a Re-Battery can comfortably supply. The mining laser, jetpack, electric chainsaw, and mining drill all benefit from the higher tier — they can pull at a higher rate and last far longer between swaps.

### Battery Pack Upgrades

The crystals are also the crafting material for the highest-tier battery packs:

- <ItemLink id="ic2_120:energy_pack" /> — combine an <ItemLink id="ic2_120:advanced_batpack" /> with an <ItemLink id="ic2_120:energy_crystal" /> in a crafting grid to upgrade.
- <ItemLink id="ic2_120:lappack" /> — combine an <ItemLink id="ic2_120:energy_pack" /> with an <ItemLink id="ic2_120:lapotron_crystal" /> to upgrade.

Wearing the resulting pack in the chestplate slot automatically tops up any powered tool of equal or lower tier in your inventory.

### Wireless Variants

There are wireless versions of both crystals. While they sit anywhere in the player's inventory, they passively drain from the surrounding EU network and top up electric tools the player is holding or wearing. They exist for both the Energy Crystal and the Lapotron Crystal tiers.

| Wireless Crystal | Tier | Notes |
|------|:---:|-------|
| <ItemLink id="ic2_120:energy_crystal_wireless" /> | 3 | Same tier as the Energy Crystal, with the passive top-up behavior. |
| <ItemLink id="ic2_120:lapotron_crystal_wireless" /> | 4 | Same tier as the Lapotron Crystal, with the passive top-up behavior. |

## Crafting

The crystal recipes are defined in the mod's data-driven recipe system. The exact ingredients can be inspected with JEI or by opening the recipe file referenced in the snippets below.

<Recipe id="ic2_120:energy_crystal" />
<Recipe id="ic2_120:lapotron_crystal" />

## Related

- [Tools and Armor reference](../reference/tools_armor.md)
- [Batteries and Mobile Power](../reference/energy_items.md)
- [Re-Battery Family](re_battery.md) — the two lower battery tiers
- [Mining Laser](mining_laser.md) — the canonical high-drain consumer of these crystals
