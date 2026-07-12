---
navigation:
  title: Chainsaw
  parent: index.md
  position: 331
  icon: ic2_120:chainsaw
item_ids:
  - ic2_120:chainsaw
---

# Chainsaw

<ItemImage id="ic2_120:chainsaw" scale="4" />

The Chainsaw is a tier-1 electric multi-tool that combines axe and sword combat with a built-in EU buffer. Its optional shear mode also handles shearable entities and shear-specific block drops. Every normal mined block or combat/shear action costs 100 EU, with no vanilla durability cost.

## Item View

| Chainsaw |
|:--------:|
| <ItemImage id="ic2_120:chainsaw" scale="2" /> |

## Stats

| Property | Value |
|----------|-------|
| Tier | 1 |
| Max Energy | 30,000 EU |
| Energy per Block | 100 EU |
| Mining Speed Reference | Diamond Axe |
| Item Tags | `minecraft:axes`, `minecraft:swords` |
| Stack Size | 1 |
| Vanilla Durability | None (EU-powered) |

The chainsaw is **not damageable**. It has no vanilla durability bar and no anvil repair recipe; "wear" is expressed entirely through its internal EU buffer. When the buffer is empty, the chainsaw still functions as a tool, but its mining speed collapses to the slow base rate (1.0x) until you recharge it.

## How to Use

### Equipping and Charging

- Place the Chainsaw in your hotbar as you would any tool.
- Recharge it from any tier-1-compatible EU source: a BatPack, a BatBox, a charging pad, or any cable that can output tier-1 voltage.
- The energy bar appears above the hotbar slot and fills as EU is added. At full charge you carry 30,000 EU, which is enough for 120 blocks of continuous chopping.

### Felling Trees

The chainsaw's specialty is bulk wood harvesting. Equip it and break any log, wood, or other axe-mineable block:

- Each block costs a flat **100 EU**, regardless of the block's hardness.
- A single oak tree (roughly 4-6 logs plus leaves) typically fits well within one full charge.
- The chainsaw is tagged `minecraft:axes` and `minecraft:swords`, so it can be used as both an axe and a powered melee tool.

### Shear Mode

- Hold the common mode key and right-click the chainsaw to toggle shear mode.
- In shear mode, right-click sheep, mooshrooms, snow golems, or other `Shearable` entities to shear them for 100 EU.
- Attack leaves, wool, cobwebs, vines, tripwire, grass, ferns, dead bushes, or hanging roots to use shear-specific drops for 100 EU.
- The shear mode state is stored in the `ChainsawDisableShear` NBT flag.

### Out of Power

When the buffer drops below 100 EU, the chainsaw can no longer afford the per-block cost. It will not refuse to mine — it simply reverts to the slow base mining speed until you top it up. There is no risk of "ruining" the tool by running it dry; just recharge and continue.

## Crafting

The chainsaw is built from iron plates wrapped around a power unit — a classic early-game electric tool recipe.

<Recipe id="ic2_120:chainsaw" />

## Related

- [Tools and Armor reference](../reference/tools_armor.md)
- [Batteries and Mobile Power](../reference/energy_items.md)
- [BatPack Family](batpack.md)
