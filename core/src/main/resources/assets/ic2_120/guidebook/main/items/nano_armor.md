---
navigation:
  title: Nano Armor
  parent: index.md
  position: 302
  icon: ic2_120:nano_helmet
item_ids:
  - ic2_120:nano_helmet
  - ic2_120:nano_chestplate
  - ic2_120:nano_leggings
  - ic2_120:nano_boots
---

# Nano Armor

<ItemImage id="ic2_120:nano_helmet" scale="4" />

**Status: planned / not yet implemented in this version.** The four Nano Armor items are registered and craftable, but the energy-driven combat mechanics described below are **planned mechanics for an upcoming release** and are not active in the current build. For now, Nano Armor behaves as a high-durability, high-protection armor set that does not consume EU on damage.

Nano Armor is IC2's first energy-augmented armor tier. The design goal is to use an internal EU buffer to absorb hits, supplementing the base protection values with on-damage energy drain. The set sits between [Bronze Armor](bronze_armor.md) and [Quantum Armor](quantum_armor.md) in the tech ladder.

## Block View

| Nano Helmet | Nano Chestplate | Nano Leggings | Nano Boots |
|:---:|:---:|:---:|:---:|:---:|
| <ItemImage id="ic2_120:nano_helmet" scale="2" /> | <ItemImage id="ic2_120:nano_chestplate" scale="2" /> | <ItemImage id="ic2_120:nano_leggings" scale="2" /> | <ItemImage id="ic2_120:nano_boots" scale="2" /> |

## Stats

| Slot | Item | Armor | Durability Multiplier | Toughness | Repair Ingredient |
|------|------|:---:|:---:|:---:|---|
| Helmet | nano_helmet | 3 | 15x | 2.0 | <ItemLink id="ic2_120:carbon_fibre" /> |
| Chestplate | nano_chestplate | 8 | 15x | 2.0 | <ItemLink id="ic2_120:carbon_fibre" /> |
| Leggings | nano_leggings | 6 | 15x | 2.0 | <ItemLink id="ic2_120:carbon_fibre" /> |
| Boots | nano_boots | 3 | 15x | 2.0 | <ItemLink id="ic2_120:carbon_fibre" /> |

Toughness of 2.0 matches the vanilla netherite tier, giving the set meaningful protection even before any EU mechanics are layered on.

## Planned Mechanics (Not Yet Active)

The following behaviors are part of the design for Nano Armor and **will be implemented in a future version**:

- **Energy buffer** — each piece holds `1,000,000 EU` (`1 MEU`). The buffer is charged from any IC2 power source that targets the worn armor, or by placing the item in a charging pad.
- **Damage absorption** — when the player is hit, the armor consumes `5,000 EU` per point of damage absorbed (per piece that covers the hit). When the buffer is empty, the piece falls back to vanilla armor behavior.
- **Nano Boots fall reduction** — falls of 4 blocks or less deal no damage and consume no EU; falls of 5–12 blocks consume EU and still deal no damage; falls beyond 12 blocks fall through to vanilla fall damage.
- **Nano Helmet night vision** — toggle with `Alt+M`, drains EU while active (typical rate: a few EU/tick).

Until the mechanics ship, the set is best treated as a high-tier armor with netherite-equivalent toughness and 15x vanilla durability.

## How to Use (Current Build)

- Equip all four pieces to maximize protection. The 2.0 toughness already gives strong resistance to high-damage hits.
- Repair each piece with <ItemLink id="ic2_120:carbon_fibre" /> in an anvil or crafting grid.
- Charging EU into the armor is accepted by the item's NBT data, but the on-hit drain and night-vision features are not wired up yet.

## Crafting

<Recipe id="ic2_120:nano_helmet" />
<Recipe id="ic2_120:nano_chestplate" />
<Recipe id="ic2_120:nano_leggings" />
<Recipe id="ic2_120:nano_boots" />

## Related

- [Tools and Armor reference](../reference/tools_armor.md)
- [Batteries and Mobile Power](../reference/energy_items.md)
- [Quantum Armor](quantum_armor.md) — the next tier above Nano
- [Bronze Armor](bronze_armor.md)
