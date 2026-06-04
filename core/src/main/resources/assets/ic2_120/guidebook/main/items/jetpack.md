---
navigation:
  title: Jetpack
  parent: index.md
  position: 311
  icon: ic2_120:jetpack
item_ids:
  - ic2_120:jetpack
  - ic2_120:electric_jetpack
---

# Jetpack

<ItemImage id="ic2_120:jetpack" scale="4" />

Two chestplate-slot flight devices. The classic Jetpack is a biofuel-driven flight unit built on the `JetpackItem` abstract. The Electric Jetpack is a later-tier `ElectricArmorItem` that runs on EU and adds a configurable toggle.

## Block View

| Jetpack | Electric Jetpack |
|:-------:|:----------------:|
| <ItemImage id="ic2_120:jetpack" scale="2" /> | <ItemImage id="ic2_120:electric_jetpack" scale="2" /> |

## Jetpack (Biofuel)

The classic Jetpack is built on the `JetpackItem` abstract, with all flight behavior and fuel consumption implemented in the abstract itself. It runs on biofuel rather than EU, so it is the flight option available before you have any electric infrastructure.

- **Fuel:** Biofuel, loaded into the backpack's internal tank.
- **Flight behavior:** governed by `JetpackItem`; see the source for the exact thrust, descent, and per-tick consumption logic.
- **Tier slot:** chestplate. While worn, the player gains hover and ascent ability as long as the fuel tank is not empty.

## Electric Jetpack

The Electric Jetpack is an `ElectricArmorItem` powered by EU. It only works while the player is wearing it in the chestplate slot and the internal EU buffer is non-empty.

### Default Configuration

| Property | Default | Config Key |
|----------|--------:|------------|
| Max energy | 32,000 EU | `Ic2Config.current.armor.electricJetpack.maxEnergy` |
| Continuous flight duration | 320 s | `Ic2Config.current.armor.electricJetpack.flightDurationSeconds` |
| Per-tick drain | 1 EU/t | `Ic2Config.current.armor.electricJetpack.euPerTick` |

The defaults give roughly 320 seconds (5 minutes 20 seconds) of continuous powered flight on a full charge. The drain runs while the pack is actively producing thrust; if the player is grounded and the toggle is off, no EU is consumed.

### Toggle and NBT

- **Right-click** the jetpack, or use its assigned hotkey, to flip the `FLIGHT_ENABLED` NBT flag.
- The flag is per-item, persisted in NBT, and is **client-synced** with the server. Switching it on one client and then reconnecting preserves the state.
- When the flag is off, the jetpack is worn but does nothing — handy for falling back to gravity on a descent.
- The remaining flight time is shown in the item's tooltip, computed as `maxEnergy / euPerTick` for a full charge and updated live as the buffer drains.

### Tooltip

```
Electric Jetpack
EU: 32000 / 32000
Flight: Enabled
Time remaining: 5m 20s
```

### Charging

Charge the Electric Jetpack like any other `ElectricArmorItem`: in a BatBox, CESU, MFE, MFSU, or a charging pad; or top it up directly from an Energy Crystal or higher in your inventory. Its tier is the same as the Lapotron Crystal, so it can be charged by any of IC2's standard electric sources.

## How to Use

1. Place the jetpack in the chestplate slot.
2. Charge it to full.
3. Toggle flight on (right-click or hotkey).
4. Jump to take off; release the jump key to descend.
5. Toggle off when you want gravity back, or to stop the EU drain while grounded.

For long trips, carry a charged Energy Crystal or LapPack and a power source to top the jetpack up between flights.

## Crafting

| Jetpack | Electric Jetpack |
|:-------:|:----------------:|
| <Recipe id="ic2_120:jetpack" /> | <Recipe id="ic2_120:electric_jetpack" /> |

**Pattern key**

- **Jetpack** (3x3): `CEC` / `CUC` / `R R` where **C** = Casing, **E** = Circuit, **U** = Empty Cell, **R** = Redstone.
- **Electric Jetpack** (3x3): `CAC` / `CBC` / `G G` where **C** = Casing, **A** = Advanced Circuit, **B** = BatBox, **G** = Glowstone Dust.

## Related

- [Tools and Armor reference](../reference/tools_armor.md)
- [Batteries and Mobile Power](../reference/energy_items.md)
