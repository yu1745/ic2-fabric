---
navigation:
  title: Nano Saber
  parent: index.md
  position: 332
  icon: ic2_120:nano_saber
item_ids:
  - ic2_120:nano_saber
---

# Nano Saber

<ItemImage id="ic2_120:nano_saber" scale="4" />

The <ItemLink id="ic2_120:nano_saber" /> is a tier-3 electric sword. Right-click to toggle it on or off; while active and powered, it cuts for a heavy 21 damage per hit. When inactive, or active but out of EU, it degrades to a vanilla sword with 5 damage.

## Item View

| Nano Saber |
|:----------:|
| <ItemImage id="ic2_120:nano_saber" scale="2" /> |

## Stats

The Nano Saber combines a 160,000 EU internal buffer with a 0-vanilla-durability `ToolMaterial` and an attack-speed modifier of -2.4. The `NanoSaberActive` NBT boolean tracks whether the saber is currently lit up. Damage is computed dynamically: `5.0` when inactive or empty, `21.0` when active with at least 1 EU in the buffer.

There is no vanilla durability bar; the only thing the bar shows is the EU buffer. When the EU is gone, the saber still swings — it just stops doing tier-3 damage and falls back to its inactive damage value.

| Property | Value |
|----------|-------|
| Tier | 3 (Energy Crystal) |
| Max capacity | 160,000 EU |
| Vanilla durability | None (EU-driven) |
| Mining speed | 1.0 |
| Mining level | 0 (no tier-based mining bonus) |
| Enchantability | 15 |
| Attack damage (inactive or empty) | 5 |
| Attack damage (active and powered) | 21 |
| Attack speed modifier | -2.4 |
| Per-hit EU cost (active) | 400 EU |

The per-hit cost is set by the in-code constant `ENERGY_PER_HIT = 400L`. Off-hit, the saber also idles: 4 EU/t in the hotbar, ~0.25 EU/t in the inventory.

## How to Use

### Activating

- Hold the <ItemLink id="ic2_120:nano_saber" /> in your main hand and right-click to toggle the `NanoSaberActive` NBT flag. A short action-bar message confirms the new state.
- Right-click again to deactivate. Deactivation also resets the saber's idle-drain accumulator and stops the active-state animation.
- The saber's item model uses the `NanoSaberActive` value to drive an activation animation (`ticker` advances each tick while active, every 5 ticks advances one of 10 frames).

### In Combat

- **Active and charged** — each hit deals 21 damage. Active attacks draw 400 EU per hit.
- **Inactive, or active but empty** — hits fall back to 5 damage (the base `DAMAGE_INACTIVE_TOTAL`). The saber will not refuse to swing; it just stops being a tier-3 weapon.

The active-vs-inactive split is computed via `getAttributeModifiers`: the attack-damage modifier resolves to `21.0 - 1.0 = 20.0` when active-and-powered and `5.0 - 1.0 = 4.0` otherwise. The attack-speed modifier is fixed at `-2.4`.

### Enchantments

The Nano Saber is built on a real `SwordItem` with a real `ToolMaterial` (`NanoSaberMaterial`), so the enchantment pipeline applies normally. Sharpness, Smite, Bane of Arthropods, Fire Aspect, Looting, Unbreaking, Mending, Sweeping Edge, and Curse of Vanishing all work. Note that the active-mode damage is set by the `Weapon modifier` attribute modifier, not by Sharpness — but Sharpness and friends still add on top.

### Charging

- The Nano Saber exposes a tier-3 EU port. Refill it from any tier-3 source: a charging <ItemLink id="ic2_120:energy_crystal" />, an <ItemLink id="ic2_120:mfe" />, or a tier-appropriate battery in your inventory.
- Right-clicking an <ItemLink id="ic2_120:energy_crystal" /> in your inventory while the saber is in your hotbar will pump EU into the saber from the crystal.

## Crafting

<Recipe id="ic2_120:nano_saber" />

The Nano Saber is a combat-tier recipe, so the input list is dominated by combat-side materials — see the recipe card for the exact ingredients.

## Related

- [Tools and Armor reference](../reference/tools_armor.md)
- [Batteries and Mobile Power](../reference/energy_items.md)
- [Energy Crystal and Lapotron Crystal](energy_crystal.md) — typical charging sources
- [Quantum Armor](quantum_armor.md) — the next tier up in combat equipment
