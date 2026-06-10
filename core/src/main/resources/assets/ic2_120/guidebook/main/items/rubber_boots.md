---
navigation:
  title: Rubber Boots
  parent: index.md
  position: 305
  icon: ic2_120:rubber_boots
item_ids:
  - ic2_120:rubber_boots
---

# Rubber Boots

<ItemImage id="ic2_120:rubber_boots" scale="4" />

The Rubber Boots are a hybrid utility item: they are armor, a power generator, and a key component of the [Hazmat Suit](hazmat_armor.md) set. They are craftable very early in the game from <ItemLink id="ic2_120:rubber" />, and they remain useful from the early game all the way through end-game builds because they passively charge anything in the player's inventory as they walk.

## Item View

<ItemImage id="ic2_120:rubber_boots" scale="4" />

## Stats

| Slot | Item | Armor | Durability Multiplier | Repair Ingredient |
|------|------|:---:|:---:|---|
| Boots | rubber_boots | 1 | 5x | <ItemLink id="ic2_120:rubber" /> |

The boots do not store EU themselves; they generate it from movement and push it into inventory items in real time.

## How to Use

### Walking Charger

While worn, the boots monitor how far the player walks. For every `1.0 m` of horizontal movement (configurable in the mod's common config; the default is 1.0 m per step), the boots push `4 EU` (configurable) into a chargeable item in the player's inventory. The charge scales with the boots' own durability and continues to operate even when the player is not sprinting.

- A sprinting player will fill batteries quickly because each tick of motion counts toward the distance threshold.
- The charge priority matches IC2's standard ordering: top-tier batteries first, then lower tiers, then partially drained items.
- Walking across a single block is typically enough to trigger one charge cycle.

### Mobility and Set Effects

- **Set bonus with Hazmat** — when worn alongside the three Hazmat pieces, the player receives full radiation immunity and underwater breathing on the Hazmat Helmet.

### When Charging Stops

The walking charger does not push EU if:

- The player is standing still or airborne (mid-jump, falling).
- The player is sneaking and the relevant config option (`chargeWhileSneaking`) is set to `false`.
- The inventory has no chargeable item.
- The boots are broken (durability 0).

## Crafting

<Recipe id="ic2_120:rubber_boots" />

Recipe ingredients: <ItemLink id="ic2_120:rubber" /> and <ItemLink id="minecraft:white_wool" />.

## Related

- [Tools and Armor reference](../reference/tools_armor.md)
- [Batteries and Mobile Power](../reference/energy_items.md)
- [Hazmat Suit](hazmat_armor.md) — uses these boots as the fourth slot
- [Rubber Trees and World Resources](../reference/rubber_and_worldgen.md)
