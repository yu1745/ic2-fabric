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

Chestplate-slot flight gear uses Minecraft's built-in creative flight controls. When a usable pack is worn, the server grants flight permission; double-tap jump to start or stop flying, use jump to rise, and use sneak to descend. Landing turns flying off automatically. Removing the pack, running out of fuel, or running out of EU restores the flight state you had before equipping it.

## Item View

| Jetpack | Electric Jetpack | QuantumSuit Bodyarmor |
|:-------:|:----------------:|:---------------------:|
| <ItemImage id="ic2_120:jetpack" scale="2" /> | <ItemImage id="ic2_120:electric_jetpack" scale="2" /> | <ItemImage id="ic2_120:quantum_chestplate" scale="2" /> |

## Jetpack (Biofuel)

The classic Jetpack runs on liquid biofuel stored directly in the item. It is the early flight option when you have fuel production but do not yet want to spend EU on mobility.

- **Storage:** 30,000 mB by default.
- **Duration:** 750 seconds of active flight on a full tank by default.
- **Consumption:** fuel is drained only while the player is actually flying.
- **Slot:** chestplate.

The Jetpack has no vanilla durability. Damage from combat does not break it; fuel is its only consumable resource.

## Electric Jetpack

The Electric Jetpack is an `ElectricArmorItem` powered by EU. It uses the same flight permission behavior as the biofuel Jetpack, but draws from its internal EU buffer instead of a fuel tank.

### Default Configuration

| Property | Default | Config Key |
|----------|--------:|------------|
| Max energy | 30,000 EU | `Ic2Config.current.armor.electricJetpack.maxEnergy` |
| Continuous flight duration | 750 s | `Ic2Config.current.armor.electricJetpack.flightDurationSeconds` |
| Per-tick drain | max energy / duration | `Ic2Config.getElectricJetpackEuPerTick()` |

The defaults give 12 minutes 30 seconds of active powered flight on a full charge. EU is consumed only while `flying` is active.

### QuantumSuit Bodyarmor

<ItemLink id="ic2_120:quantum_chestplate" /> is the end-game flight chestplate. It stores 10,000,000 EU by default and provides 1,200 seconds of active creative-style flight when fully charged. It also keeps the normal Quantum Armor protection and fire-resistance role.

### Charging

Charge the Electric Jetpack like any other `ElectricArmorItem`: in a BatBox, CESU, MFE, MFSU, or a charging pad; or top it up directly from compatible charged items in your inventory. QuantumSuit Bodyarmor uses the Quantum Armor energy buffer and accepts high-tier EU charging.

The Electric Jetpack also has no vanilla durability and cannot be broken by combat damage. Its only consumable resource is EU.

## How to Use

1. Fill or charge the pack.
2. Place it in the chestplate slot.
3. Double-tap jump to enter flight.
4. Hold jump to rise and sneak to descend.
5. Land or double-tap jump again to stop flying.

For long trips, carry spare fuel or charged batteries and a power source to top the pack up between flights.

## Crafting

| Jetpack | Electric Jetpack | QuantumSuit Bodyarmor |
|:-------:|:----------------:|:---------------------:|
| <Recipe id="ic2_120:jetpack" /> | <Recipe id="ic2_120:electric_jetpack" /> | <Recipe id="ic2_120:quantum_chestplate" /> |

## Related

- [Tools and Armor reference](../reference/tools_armor.md)
- [Batteries and Mobile Power](../reference/energy_items.md)
- [Quantum Armor](quantum_armor.md)
