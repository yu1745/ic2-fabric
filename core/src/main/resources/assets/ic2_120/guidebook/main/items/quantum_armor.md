---
navigation:
  title: Quantum Armor
  parent: index.md
  position: 303
  icon: ic2_120:quantum_helmet
item_ids:
  - ic2_120:quantum_helmet
  - ic2_120:quantum_chestplate
  - ic2_120:quantum_leggings
  - ic2_120:quantum_boots
---

# Quantum Armor

<ItemImage id="ic2_120:quantum_helmet" scale="4" />

**Status: planned / not yet implemented in this version.** The four Quantum Armor items are registered and craftable, but the full energy-driven combat mechanics, flight, super-jump, and status-immunity behaviors described below are **planned mechanics for an upcoming release** and are not active in the current build. For now, Quantum Armor behaves as the highest-tier vanilla-style armor in the mod: maximum protection, 25x durability, and the highest toughness and knockback resistance of any IC2 set.

Quantum Armor is IC2's end-game armor. It is meant to make the player a self-contained power armored unit: a 10 million EU buffer per piece, on-hit damage absorption, environmental immunities, and movement augmentations.

## Block View

| Quantum Helmet | Quantum Chestplate | Quantum Leggings | Quantum Boots |
|:---:|:---:|:---:|:---:|:---:|
| <ItemImage id="ic2_120:quantum_helmet" scale="2" /> | <ItemImage id="ic2_120:quantum_chestplate" scale="2" /> | <ItemImage id="ic2_120:quantum_leggings" scale="2" /> | <ItemImage id="ic2_120:quantum_boots" scale="2" /> |

## Stats

| Slot | Item | Armor | Durability Multiplier | Toughness | Knockback Resistance | Repair Ingredient |
|------|------|:---:|:---:|:---:|:---:|---|
| Helmet | quantum_helmet | 4 | 25x | 3.0 | 0.4 | <ItemLink id="ic2_120:iridium_ingot" /> |
| Chestplate | quantum_chestplate | 9 | 25x | 3.0 | 0.4 | <ItemLink id="ic2_120:iridium_ingot" /> |
| Leggings | quantum_leggings | 6 | 25x | 3.0 | 0.4 | <ItemLink id="ic2_120:iridium_ingot" /> |
| Boots | quantum_boots | 4 | 25x | 3.0 | 0.4 | <ItemLink id="ic2_120:iridium_ingot" /> |

The 3.0 toughness and 0.4 knockback resistance stack additively with vanilla modifiers from other sources.

## Planned Mechanics (Not Yet Active)

The following behaviors are part of the design for Quantum Armor and **will be implemented in a future version**:

- **Energy buffer** — each piece holds `10,000,000 EU` (`10 MEU`). Charge from any IC2 power source targeting the worn armor.
- **Piercing damage reduction** — per-piece damage absorption: Helmet 15%, Chestplate 44%, Leggings 30%, Boots 15%. Together, the set can absorb essentially all incoming damage as long as the buffers are charged.
- **On-hit EU cost** — each piece that contributes to a hit consumes at least `900 EU` per hit, plus `30 EU` per point of damage the piece absorbs.
- **Full-set status immunities** — eliminates poison, wither, and radiation damage while the full set is worn.
- **Quantum Helmet** — water breathing (refills the air meter automatically while worn), night vision (`Alt+M` toggle), and passive hunger saturation refill.
- **Quantum Chestplate** — flight (creative-style flight while the buffer is charged) and full fire immunity.
- **Quantum Leggings** — `3x` sprint speed and `9x` faster movement on ice.
- **Quantum Boots** — fall damage reduction (similar to Nano Boots but stronger) and **Ctrl+Space super-jump** (hold both keys to charge a jump of up to 9 blocks, costing `1,000 EU` per use).

Until the mechanics ship, the set is best treated as the highest-tier baseline armor in the mod — the 25x durability and 3.0 toughness make it the toughest non-netherite-style set available.

## How to Use (Current Build)

- Equip all four pieces to maximize protection. The 0.4 knockback resistance makes the player very hard to push around in PvE.
- Repair each piece with <ItemLink id="ic2_120:iridium_ingot" /> in an anvil or crafting grid.
- EU charging is accepted by the items' NBT data; the combat, flight, and super-jump features are not yet wired up.

## Crafting

<Recipe id="ic2_120:quantum_helmet" />
<Recipe id="ic2_120:quantum_chestplate" />
<Recipe id="ic2_120:quantum_leggings" />
<Recipe id="ic2_120:quantum_boots" />

## Related

- [Tools and Armor reference](../reference/tools_armor.md)
- [Batteries and Mobile Power](../reference/energy_items.md)
- [Nano Armor](nano_armor.md) — the tier below Quantum
- [Jetpacks and Electric Jetpack](../reference/energy_items.md) — alternative flight options
