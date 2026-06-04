---
navigation:
  title: Re-Battery Family
  parent: index.md
  position: 320
  icon: ic2_120:re_battery
item_ids:
  - ic2_120:re_battery
  - ic2_120:advanced_re_battery
  - ic2_120:single_use_battery
  - ic2_120:re_battery_wireless
  - ic2_120:advanced_re_battery_wireless
---

# Re-Battery Family

<ItemImage id="ic2_120:re_battery" scale="4" />

The Re-Battery line is IC2's entry-level rechargeable EU storage. There are two rechargeable variants — the <ItemLink id="ic2_120:re_battery" /> and the <ItemLink id="ic2_120:advanced_re_battery" /> — plus a <ItemLink id="ic2_120:single_use_battery" /> that is consumed as it discharges. All three fit in the player's inventory, can be charged in any compatible energy storage, and feed powered tools and armor.

For the full tier table that also covers the higher-tier Energy Crystal and Lapotron Crystal, see [Batteries and Mobile Power](../reference/energy_items.md).

## Overview

The Re-Battery family is the cheapest way to get mobile EU. Rechargeable variants can be drained and refilled indefinitely; the single-use variant is a one-shot cell that disappears when empty.

| Item | Tier | Max EU | Notes |
|------|:---:|---:|-------|
| <ItemLink id="ic2_120:single_use_battery" /> | 1 | Configurable | One-shot cell. Discharges in any normal battery slot and is destroyed when empty. |
| <ItemLink id="ic2_120:re_battery" /> | 1 | Configurable | Basic rechargeable. The first reliable mobile EU source for early-game tools. |
| <ItemLink id="ic2_120:advanced_re_battery" /> | 2 | Configurable | Higher-capacity rechargeable. Carries the mid-game electric tools without frequent recharges. |

Default values can be tuned in the mod's `Ic2Config`; check the config file for the exact capacity and transfer rate for the build you are playing.

## Block View

| Single-Use Battery | Re-Battery | Advanced Re-Battery |
|:---:|:---:|:---:|
| <ItemImage id="ic2_120:single_use_battery" scale="2" /> | <ItemImage id="ic2_120:re_battery" scale="2" /> | <ItemImage id="ic2_120:advanced_re_battery" scale="2" /> |

## How to Use

### Rechargeable Behavior

Re-Batteries and Advanced Re-Batteries can be charged and discharged in:

- **Energy Storage blocks** — <ItemLink id="ic2_120:batbox" />, <ItemLink id="ic2_120:cesu" />, <ItemLink id="ic2_120:mfe" />, and <ItemLink id="ic2_120:mfsu" />. Place the battery in the storage's battery slot to charge or discharge.
- **Chargepad** — standing on the chargepad variant recharges any battery (and compatible gear) in the player's inventory. See [Chargepad](../machines/chargepad.md).
- **Battery Packs** — the <ItemLink id="ic2_120:batpack" /> and <ItemLink id="ic2_120:advanced_batpack" /> automatically top up any Re-Battery in your inventory whose tier is at or below the pack's tier.

### Single-Use Behavior

The <ItemLink id="ic2_120:single_use_battery" /> behaves like the Re-Battery for powering items, but it is consumed once it runs out. It is meant as a low-cost starting cell when no charger is available.

### Wireless Variants

Two wireless variants exist for the Re-Battery family. When they sit anywhere in the player's inventory, they automatically siphon EU from the network and push it into electric tools the player is wearing or holding. They are essentially a personal, passive top-up source.

| Wireless Battery | Tier | Notes |
|------|:---:|-------|
| <ItemLink id="ic2_120:re_battery_wireless" /> | 1 | Same tier as the basic Re-Battery, with the passive top-up behavior. |
| <ItemLink id="ic2_120:advanced_re_battery_wireless" /> | 2 | Higher tier; feeds tools up to the advanced-re-battery tier. |

Higher-tier wireless versions exist (the Wireless Energy Crystal and Wireless Lapotron Crystal) and are covered on the [Energy Crystal](energy_crystal.md) page.

## Crafting

The Re-Battery family recipes are defined in the mod's data-driven recipe system. The exact ingredients can be inspected with JEI or by opening the recipe file referenced in the snippets below.

<Recipe id="ic2_120:re_battery" />
<Recipe id="ic2_120:advanced_re_battery" />
<Recipe id="ic2_120:single_use_battery" />

## Related

- [Tools and Armor reference](../reference/tools_armor.md)
- [Batteries and Mobile Power](../reference/energy_items.md)
- [Energy Storage](../machines/energy_storage.md) — stationary blocks that charge batteries
- [Chargepad](../machines/chargepad.md) — pads that recharge gear while you stand on them
- [Energy Crystal and Lapotron Crystal](energy_crystal.md) — the next two battery tiers
