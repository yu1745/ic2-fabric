---
navigation:
  title: Night Vision Goggles
  parent: index.md
  position: 313
  icon: ic2_120:night_vision_goggles
item_ids:
  - ic2_120:night_vision_goggles
---

# Night Vision Goggles

<ItemImage id="ic2_120:night_vision_goggles" scale="4" />

The Night Vision Goggles are a helmet-slot head-mounted device that grants the player Night Vision on demand. They are powered by an internal EU buffer rather than vanilla durability, so they keep working as long as the buffer is charged.

## Block View

| Night Vision Goggles |
|:-------------------:|
| <ItemImage id="ic2_120:night_vision_goggles" scale="2" /> |

## Stats

| Property | Value |
|----------|-------|
| Slot | Helmet |
| Armor | 1 |
| Durability multiplier | 5x |
| Tier | 2 |
| Internal capacity | 30,000 EU (configurable) |
| Per-tick drain | 1 EU/t while active (default) |
| Refresh interval | 220 ticks (`NIGHT_VISION_DURATION_TICKS`) |
| Vanilla durability | Disabled (`isDamageable = false`) |

The internal capacity and per-tick drain are read from `Ic2Config.current.armor.nightVisionGoggles` at construction time, so a server admin can rebalance the goggles without changing the item. The defaults give roughly 30,000 ticks (about 25 minutes) of continuous night vision per charge.

## How to Use

### Equipping

- Place the Night Vision Goggles in the helmet slot.
- Charge the internal buffer from a BatBox, CESU, MFE, MFSU, charging pad, or any other EU source that can talk to a tier-2 electric item.

### Toggling Night Vision

- **Client / server interaction** — the exact key binding is provided by the goggles' UI handler. Default behaviour is right-click or the assigned hotkey to flip the `NIGHT_VISION_ENABLED` flag.
- When enabled, the goggles apply the Night Vision status effect to the wearer. The effect is re-applied every `NIGHT_VISION_DURATION_TICKS` ticks (220 ticks, or 11 seconds) so the player never sees the effect pulse off.
- The remaining operational time is shown in the item's tooltip, computed as `maxEnergy / euPerTick` for a full charge and updated live.

### Automatic Light Protection

If the player steps into a well-lit area, the goggles refuse to waste EU on an effect the player does not need:

- When the ambient light level at the player's eye position is **8 or higher**, the goggles **disable night vision** on the spot and apply a short Blindness effect for **80 ticks (4 seconds)** to give the player a moment to readjust.
- When the player moves back into a darker area, normal toggle behaviour resumes.

This is a safety override, not a configuration option — it cannot be disabled by a config flag.

### No Vanilla Durability

The goggles set `isDamageable = false`, so they do not take vanilla damage and never show a durability bar. The only "wear" they experience is the EU buffer draining while night vision is active.

### Tooltip

```
Night Vision Goggles
EU: 30000 / 30000
Night Vision: Enabled
Time remaining: 25m 0s
```

## Crafting

<Recipe id="ic2_120:night_vision_goggles" />

**Pattern key** (3x3):

- **Top row:** `H B H` — **H** = Advanced Heat Exchanger, **B** = Advanced Re-Battery
- **Middle row:** `L G L` — **L** = Luminator Flat, **G** = Reinforced Glass
- **Bottom row:** `R C R` — **R** = Rubber, **C** = Advanced Circuit

## Related

- [Tools and Armor reference](../reference/tools_armor.md)
- [Batteries and Mobile Power](../reference/energy_items.md)
