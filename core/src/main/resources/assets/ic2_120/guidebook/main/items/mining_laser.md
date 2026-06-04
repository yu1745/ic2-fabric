---
navigation:
  title: Mining Laser
  parent: index.md
  position: 322
  icon: ic2_120:mining_laser
item_ids:
  - ic2_120:mining_laser
---

# Mining Laser

<ItemImage id="ic2_120:mining_laser" scale="4" />

The <ItemLink id="ic2_120:mining_laser" /> is a high-drain, ranged mining tool that fires a visible EU-powered beam. It can break blocks, attack entities, and — depending on the firing mode — smelt ores in place or carve out a tunnel. It is the canonical end-game consumer of an <ItemLink id="ic2_120:energy_crystal" /> or <ItemLink id="ic2_120:lapotron_crystal" />.

This page is a brief overview. For the full description of every mode, the projectile rules, the random ignite mechanic, and entity damage scaling, see the dedicated [Mining Laser Guide](../guides/mining_laser.md).

## Overview

The Mining Laser is a hand-held item with a large internal EU buffer. It charges from any compatible battery in the player's inventory, then discharges the buffer in bursts when the player right-clicks. A single buffer holds enough EU for many shots in the lower-power modes and fewer shots in the high-power modes.

| Property | Value |
|----------|------|
| Tier | 4 (Lapotron Crystal) |
| Internal buffer | Configurable; default is large, set in `Ic2Config` |
| EU per shot | Varies by mode (see dedicated guide) |
| Charge source | Any Lapotron-tier battery or higher in inventory |
| Operation | Right-click to fire; **Mode Switch** + right-click to cycle modes |

Default values can be tuned in the mod's `Ic2Config`; check the config file for the exact buffer size for the build you are playing.

## Block View

| Mining Laser |
|:---:|
| <ItemImage id="ic2_120:mining_laser" scale="2" /> |

## Modes (Summary)

The Mining Laser has multiple firing modes. The detailed cost table, ranges, and entity damage values are on the dedicated guide page; this list is just to give a sense of what the tool can do.

- **Low-power precise** — a low-cost, very short-range mode suited for delicate single-block mining.
- **Mining** — the standard mid-range mode. The beam chews through multiple blocks in a single shot until the shot's reach is exhausted.
- **Long Range** — fast, long-distance projectile for sniping distant blocks or entities.
- **Super Heat** — high-power mode that smelts smeltable ores in place (e.g. iron ore → iron ingot). Sometimes referred to as "lava mode" because of how the targeted block is processed.
- **Scatter** — fires a fan of beams in a single right-click, ideal for clearing a wide area.
- **Explosive** — detonates a TNT-class explosion at the impact point, dealing a high true-damage hit to any entity caught in it.
- **3×3** — fires a forward-facing 3×3 wall of beams, useful for carving tunnels.

See the [Mining Laser Guide](../guides/mining_laser.md) for the full per-mode table, including EU cost, range, and entity damage.

## Network and Authority

The Mining Laser is an authoritative server-side tool. The relevant `MiningLaserServerSuppress` rule exists to make sure the firing decision is made on the server and any client-side attempts to spam the action are rejected. The practical implication is simple: the laser only fires when the server agrees there is enough buffer, regardless of what the client predicts.

## How to Use

1. Carry a charged <ItemLink id="ic2_120:lapotron_crystal" /> (or higher-tier power source) in your inventory.
2. Hold the <ItemLink id="ic2_120:mining_laser" /> in your main hand. The action bar briefly shows the current mode.
3. Right-click to fire. The shot consumes EU from the laser's internal buffer.
4. To switch mode, hold the **Mode Switch** key and right-click. The same keybind is shared by other IC2 tools; the default binding lives under **Options → Controls** in the mod's keybinding pack.
5. To refill, place the laser in the battery slot of a BatBox, CESU, MFE, or MFSU. See [Energy Storage](../machines/energy_storage.md).

## Crafting

The Mining Laser recipe is defined in the mod's data-driven recipe system. The exact ingredients can be inspected with JEI or by opening the recipe file referenced below.

<Recipe id="ic2_120:mining_laser" />

## Related

- [Tools and Armor reference](../reference/tools_armor.md)
- [Batteries and Mobile Power](../reference/energy_items.md)
- [Mining Laser Guide](../guides/mining_laser.md) — full mode table, projectile rules, and damage scaling
- [Energy Crystal and Lapotron Crystal](energy_crystal.md) — typical power sources for the laser
