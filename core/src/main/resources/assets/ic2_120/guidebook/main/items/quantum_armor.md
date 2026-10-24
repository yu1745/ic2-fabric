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

Quantum Armor is IC2's end-game armor. It makes the player a self-contained power armored unit: a 10 million EU buffer per piece, strong damage reduction, environmental support, and movement augmentations. The chestplate also works as the final flight pack, using the same creative-style flight controls as the Jetpack and Electric Jetpack.

## Item View

| Quantum Helmet | Quantum Chestplate | Quantum Leggings | Quantum Boots |
|:---:|:---:|:---:|:---:|
| <ItemImage id="ic2_120:quantum_helmet" scale="2" /> | <ItemImage id="ic2_120:quantum_chestplate" scale="2" /> | <ItemImage id="ic2_120:quantum_leggings" scale="2" /> | <ItemImage id="ic2_120:quantum_boots" scale="2" /> |

## Stats

| Slot | Item | Armor | Toughness | Knockback Resistance | EU Capacity | Tier | Powered Reduction |
|------|------|:---:|:---:|:---:|:---:|:---:|:---:|
| Helmet | quantum_helmet | 4 | 3.0 | 0.4 | 10,000,000 EU | 4 | 15% |
| Chestplate | quantum_chestplate | 9 | 3.0 | 0.4 | 10,000,000 EU | 4 | 45% |
| Leggings | quantum_leggings | 6 | 3.0 | 0.4 | 10,000,000 EU | 4 | 30% |
| Boots | quantum_boots | 4 | 3.0 | 0.4 | 10,000,000 EU | 4 | 10% |

The 3.0 toughness and 0.4 knockback resistance stack additively with vanilla modifiers from other sources.

## Active Mechanics

- **Energy buffer** — each piece holds `10,000,000 EU` (`10 MEU`). Charge from IC2 power sources that can charge armor or high-tier electric items.
- **Damage reduction** — each piece contributes its configured damage reduction while charged; the chestplate shows its reduction value in the tooltip. A full set totals **100% powered damage reduction** (15% + 45% + 30% + 10%), granting complete damage immunity when all pieces are charged.
- **Quantum Helmet** — night vision support and helmet utility features.
- **Quantum Chestplate** — creative-style flight while charged, plus the chestplate's protective role.
- **Quantum Leggings** — speed boost support.
- **Quantum Boots** — jump boost and fall-protection support.

## Full-Set Negative Effect Immunity

When all four Quantum Armor pieces are equipped, the set continuously removes every status effect classified as harmful, including the Warden's Darkness effect, as well as IC2 radiation. This requires at least `100 EU` in the Quantum Helmet and consumes `100 EU` for each harmful effect removed. The immunity does not activate with the Quantum Helmet alone.

## Quantum Flight

The <ItemLink id="ic2_120:quantum_chestplate" /> is the highest-tier flight chestplate.

- **Storage:** 10,000,000 EU by default.
- **Duration:** 1,200 seconds of active flight on a full charge by default.
- **Controls:** double-tap jump to start or stop flying, jump to rise, sneak to descend.
- **Shutdown:** landing turns flying off through vanilla client logic. Removing the chestplate or draining the buffer restores the flight state you had before equipping it.
- **Water:** entering water does not cancel creative flight in vanilla, so a still-flying quantum chestplate continues consuming EU until flying is toggled off, the player lands, or the buffer drains.

## How to Use

- Equip all four pieces to maximize protection. The 0.4 knockback resistance makes the player very hard to push around in PvE.
- Keep every piece charged; Quantum Armor is maintained by EU charging, not by vanilla durability repair.
- Keep the chestplate charged before long flights; it is crafted from an Electric Jetpack and replaces it as the end-game flight option.

## Crafting

| Quantum Helmet | Quantum Chestplate |
|:---:|:---:|
| <Recipe id="ic2_120:quantum_helmet" /> | <Recipe id="ic2_120:quantum_chestplate" /> |
| Quantum Leggings | Quantum Boots |
| <Recipe id="ic2_120:quantum_leggings" /> | <Recipe id="ic2_120:quantum_boots" /> |

## Related

- [Tools and Armor reference](../reference/tools_armor.md)
- [Batteries and Mobile Power](../reference/energy_items.md)
- [Nano Armor](nano_armor.md) — the tier below Quantum
- [Jetpacks and Electric Jetpack](jetpack.md) — earlier flight options
- [Night Vision Goggles](night_vision_goggles.md) — standalone night vision item; Quantum Helmet includes the same system
- [Composite Chestplate](alloy_chestplate.md) — alternative energy chestplate with jetpack-compatible flight
