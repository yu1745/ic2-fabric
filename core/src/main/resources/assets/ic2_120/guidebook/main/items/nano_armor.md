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

Nano Armor is IC2's first energy-augmented armor tier. Each piece stores EU and contributes powered damage reduction while charged. The set sits between [Bronze Armor](bronze_armor.md) and [Quantum Armor](quantum_armor.md) in the tech ladder.

## Item View

| Nano Helmet | Nano Chestplate | Nano Leggings | Nano Boots |
|:---:|:---:|:---:|:---:|
| <ItemImage id="ic2_120:nano_helmet" scale="2" /> | <ItemImage id="ic2_120:nano_chestplate" scale="2" /> | <ItemImage id="ic2_120:nano_leggings" scale="2" /> | <ItemImage id="ic2_120:nano_boots" scale="2" /> |

## Stats

| Slot | Item | Armor | Toughness | EU Capacity | Tier | Powered Reduction |
|------|------|:---:|:---:|:---:|:---:|:---:|
| Helmet | nano_helmet | 3 | 2.0 | 1,000,000 EU | 3 | 12% |
| Chestplate | nano_chestplate | 8 | 2.0 | 1,000,000 EU | 3 | 36% |
| Leggings | nano_leggings | 6 | 2.0 | 1,000,000 EU | 3 | 24% |
| Boots | nano_boots | 3 | 2.0 | 1,000,000 EU | 3 | 8% |

Toughness of 2.0 matches the vanilla netherite tier. With all four pieces charged, Nano Armor provides up to 80% powered damage reduction before upgrading into Quantum Armor.

## Active Mechanics

- **Energy buffer** — each piece holds `1,000,000 EU` (`1 MEU`) and exposes tier 3 charging.
- **Powered damage reduction** — only charged Nano pieces contribute their reduction. Empty pieces fall back to their normal armor stats.
- **On-hit EU cost** — reduced damage costs `5,000 EU` per mitigated damage point, shared across the charged electric armor pieces that participated in the reduction.
- **Nano Helmet night vision** — toggle with `Alt+N`. While enabled, the helmet consumes EU and applies night vision in dark areas.

## Nano Helmet Night Vision

The <ItemLink id="ic2_120:nano_helmet" /> includes the night-vision system from the Night Vision Goggles recipe.

- **Default buffer:** 1,000,000 EU.
- **Default duration:** 3,571 seconds of active night vision from a full charge.
- **Controls:** press `Alt+N` while wearing the helmet.
- **Bright areas:** at light level 8 or higher, the helmet removes night vision and briefly applies blindness, matching the goggles behavior.
- **Tooltip:** shows whether night vision is on and the estimated remaining time.

## How to Use

- Equip all four pieces for the full 80% powered reduction.
- Keep the pieces charged; Nano Armor is maintained by EU charging, not by vanilla durability repair. A piece with no EU still has armor value, but it does not contribute powered reduction.
- Use `Alt+N` to toggle Nano Helmet night vision before entering dark areas.

## Crafting

| Nano Helmet | Nano Chestplate |
|:---:|:---:|
| <Recipe id="ic2_120:nano_helmet" /> | <Recipe id="ic2_120:nano_chestplate" /> |
| Nano Leggings | Nano Boots |
| <Recipe id="ic2_120:nano_leggings" /> | <Recipe id="ic2_120:nano_boots" /> |

## Related

- [Tools and Armor reference](../reference/tools_armor.md)
- [Batteries and Mobile Power](../reference/energy_items.md)
- [Quantum Armor](quantum_armor.md) — the next tier above Nano
- [Bronze Armor](bronze_armor.md)
- [Nano Saber](nano_saber.md) — energy melee weapon in the same tier
- [Night Vision Goggles](night_vision_goggles.md) — standalone night vision item; Nano Helmet includes the same system
