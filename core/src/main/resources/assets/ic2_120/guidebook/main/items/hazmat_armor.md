---
navigation:
  title: Hazmat Suit
  parent: index.md
  position: 301
  icon: ic2_120:hazmat_helmet
item_ids:
  - ic2_120:hazmat_helmet
  - ic2_120:hazmat_chestplate
  - ic2_120:hazmat_leggings
---

# Hazmat Suit

<ItemImage id="ic2_120:hazmat_helmet" scale="4" />

The Hazmat Suit is a three-piece set (helmet, chestplate, leggings) paired with the [Rubber Boots](rubber_boots.md) for the fourth slot. It is not designed for combat — its purpose is to keep you alive in dangerous environments: nuclear reactors, exposed uninsulated cables, and underwater.

## Block View

| Hazmat Helmet | Hazmat Chestplate | Hazmat Leggings | Rubber Boots |
|:---:|:---:|:---:|:---:|
| <ItemImage id="ic2_120:hazmat_helmet" scale="2" /> | <ItemImage id="ic2_120:hazmat_chestplate" scale="2" /> | <ItemImage id="ic2_120:hazmat_leggings" scale="2" /> | <ItemImage id="ic2_120:rubber_boots" scale="2" /> |

## Stats

| Slot | Item | Armor | Durability Multiplier | Repair Ingredient |
|------|------|:---:|:---:|---|
| Helmet | hazmat_helmet | 1 | 5x | <ItemLink id="ic2_120:rubber" /> |
| Chestplate | hazmat_chestplate | 3 | 5x | <ItemLink id="ic2_120:rubber" /> |
| Leggings | hazmat_leggings | 2 | 5x | <ItemLink id="ic2_120:rubber" /> |
| Boots | rubber_boots (shared) | 1 | 5x | <ItemLink id="ic2_120:rubber" /> |

The boots slot is shared with the [Rubber Boots](rubber_boots.md) page — wear the same item there and you get both sets' bonuses.

## Set Effects

Wearing all four pieces (Helmet + Chestplate + Leggings + Rubber Boots) grants:

- **Radiation immunity** — fully negates radiation damage while inside or near active nuclear reactors and irradiated zones.
- **Electrical insulation** — touching exposed, uninsulated cables no longer deals shock damage.
- **Hazmat Helmet: emergency underwater breathing** — when the player's air bubble drops to **60 ticks (3 seconds) or less**, the helmet automatically consumes a <ItemLink id="ic2_120:air_cell" /> (compressed air unit) from the inventory to refill the air meter. If the inventory has no air cells, the player still drowns as normal.

The set bonus does **not** require a full metal suit underneath; the rubber provides all the protection. You can wear other armor pieces *over* the Hazmat Suit visually, but the bonus only checks for the four Hazmat slots.

## How to Use

- Always pair the three Hazmat pieces with [Rubber Boots](rubber_boots.md) in the boots slot — without them, the radiation and shock immunity will not engage.
- Carry a stack of <ItemLink id="ic2_120:air_cell" /> when exploring flooded reactor chambers or deep underwater builds.
- Repair is done with <ItemLink id="ic2_120:rubber" /> in an anvil or crafting grid.
- The set has very low combat armor values (1/3/2/1) — swap to a combat set when leaving the danger zone.

## Crafting

| Piece | Recipe | Ingredients |
|-------|--------|-------------|
| Hazmat Helmet | <Recipe id="ic2_120:hazmat_helmet" /> | Orange Dye, Rubber, Glass, Iron Bars |
| Hazmat Chestplate | <Recipe id="ic2_120:hazmat_chestplate" /> | Rubber, Orange Dye |
| Hazmat Leggings | <Recipe id="ic2_120:hazmat_leggings" /> | Rubber, Orange Dye |
| Rubber Boots | <Recipe id="ic2_120:rubber_boots" /> | Rubber, White Wool |

## Related

- [Tools and Armor reference](../reference/tools_armor.md)
- [Batteries and Mobile Power](../reference/energy_items.md)
- [Rubber Boots](rubber_boots.md)
- [Rubber Trees and World Resources](../reference/rubber_and_worldgen.md)
