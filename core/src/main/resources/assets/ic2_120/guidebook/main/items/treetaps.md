---
navigation:
  title: Treetaps
  parent: index.md
  position: 334
  icon: ic2_120:treetap
item_ids:
  - ic2_120:treetap
  - ic2_120:electric_treetap
---

# Treetaps

<ItemImage id="ic2_120:treetap" scale="4" />

The Treetap and Electric Treetap extract <ItemLink id="ic2_120:resin" /> from the resin-filled faces of rubber tree logs. The hand tap is cheap and disposable, the electric tap is a tier-1 battery-driven upgrade with no vanilla durability.

Rubber trees spawn in the world as a special resource — see [World Resources and Rubber Trees](../reference/rubber_and_worldgen.md) for the spawn rules, log shape, and where to find them.

## Block View

| Treetap | Electric Treetap |
|:-------:|:----------------:|
| <ItemImage id="ic2_120:treetap" scale="2" /> | <ItemImage id="ic2_120:electric_treetap" scale="2" /> |

## Stats

| Type | Durability or Capacity | Use |
|------|------------------------|-----|
| Treetap (hand tool) | 10 uses (vanilla durability) | Right-click a rubber log's resin hole to extract sticky resin |
| Electric Treetap | 10,000 EU internal buffer, no vanilla durability | Same action as the hand tap, but EU-powered for unlimited uses |

The Treetap is a damageable item with `maxDamage = 10`: ten right-clicks on a resin hole and it breaks. The Electric Treetap is **not damageable** — it has no vanilla durability bar, draws from its 10,000 EU internal buffer, and recharges like any other tier-1 electric tool.

## How to Use

1. Find a rubber tree. The trunk logs have a face marked with the resin hole block — that's the extractable side. The rest of the log is ordinary rubber wood.
2. Hold the treetap in your main hand.
3. Right-click the resin hole face on the log.
4. <ItemLink id="ic2_120:resin" /> drops at the tapped face. Pick it up.
5. Re-tap a different hole, or wait for the same hole to refill. Each resin hole is a one-shot per face per cycle — the same face will not yield another resin drop until the tree regrows the resin on that face.

The Electric Treetap behaves identically from the player's perspective. The only difference is durability: it keeps tapping as long as it has at least some EU, and recharges from any tier-1 source.

### Charging the Electric Treetap

The Electric Treetap exposes a tier-1 EU port, so any of the following will refill its 10,000 EU buffer:

- Place it in the battery slot of a <ItemLink id="ic2_120:batbox" /> (or any higher-tier energy storage).
- Top up from a charged <ItemLink id="ic2_120:re_battery" /> in your inventory while it's in your hotbar.
- Drop it on a charging pad.

## Crafting

| Treetap | Electric Treetap |
|:-------:|:----------------:|
| <Recipe id="ic2_120:treetap" /> | <Recipe id="ic2_120:electric_treetap" /> |

The Treetap recipe is a simple three-row wooden shape (`" P " / "PPP" / "P  "`, where `P` is any planks tag entry) — cheap, with no metal cost. The Electric Treetap recipe wraps a Treetap around a <ItemLink id="ic2_120:small_power_unit" /> in a `"   " / " T " / " P "` pattern, and the Treetap is consumed on craft.

## Related

- [Tools and Armor reference](../reference/tools_armor.md)
- [Batteries and Mobile Power](../reference/energy_items.md)
- [World Resources and Rubber Trees](../reference/rubber_and_worldgen.md) — rubber tree spawn rules and log shape
