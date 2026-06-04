---
navigation:
  title: Solar Helmet
  parent: index.md
  position: 304
  icon: ic2_120:solar_helmet
item_ids:
  - ic2_120:solar_helmet
---

# Solar Helmet

<ItemImage id="ic2_120:solar_helmet" scale="4" />

The Solar Helmet is a wearable charging device. While worn in the helmet slot during daytime with an unobstructed sky overhead, it trickle-charges any chargeable EU item in the player's inventory at `1 EU/t` (one EU per tick). It also grants the player a passive `Solar Generating` status effect while the charge is flowing.

## Block View

<ItemImage id="ic2_120:solar_helmet" scale="4" />

## Stats

| Slot | Item | Armor | Durability Multiplier | Repair Ingredient |
|------|------|:---:|:---:|---|
| Helmet | solar_helmet | 2 | 8x | <ItemLink id="ic2_120:bronze_ingot" /> |

The Solar Helmet does not have an internal EU buffer; it is a passive generator that pushes power directly into your items. It also does not consume EU while generating — it draws from sunlight.

## How to Use

### Wearing and Charging

- Equip the Solar Helmet in the helmet slot.
- Stand in **daylight** with a clear line of sight to the sky. Opaque blocks, glass (except specific clear-glass cases), water overhead, and being indoors will all stop the charge.
- While active, the helmet pushes `1 EU/t` into the first chargeable item it finds in your inventory. The internal priority matches IC2's standard battery-charging order: highest tier and lowest charge first, so partially drained top-tier batteries fill before lower-tier ones.
- The player receives the `Solar Generating` status effect while the charge is flowing. The effect is purely a visual / status indicator — it has no gameplay effect of its own.

### When It Stops Charging

The Solar Helmet stops pushing EU under any of these conditions:

- The player is in a dimension without a sky (e.g., the Nether or the End) — these are treated as "no sunlight" by the mod.
- The player is underground, indoors, or under a non-transparent block.
- The player is holding a non-chargeable item only — it has nothing to push EU into.
- The player is sneaking (configurable; default is to keep charging while sneaking).

### Synergy with Other Charging Sources

The Solar Helmet stacks with other IC2 charge sources: standing on a [Chargepad](../machines/chargepad.md), wearing an [Energy Pack / LapPack](../reference/energy_items.md), or having a wireless battery in the inventory all charge in parallel. The Solar Helmet only ever contributes its own `1 EU/t`.

## Crafting

<Recipe id="ic2_120:solar_helmet" />

Recipe ingredients: Iron Ingots, <ItemLink id="ic2_120:solar_generator" />, and <ItemLink id="ic2_120:insulated_copper_cable" />.

## Related

- [Tools and Armor reference](../reference/tools_armor.md)
- [Batteries and Mobile Power](../reference/energy_items.md)
- [Solar Generator](../machines/solar_generator.md) — the block version of the same idea
- [Bronze Armor](bronze_armor.md)
