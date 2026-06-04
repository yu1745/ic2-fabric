---
navigation:
  title: Bronze Tools
  parent: index.md
  position: 345
  icon: ic2_120:bronze_pickaxe
item_ids:
  - ic2_120:bronze_pickaxe
  - ic2_120:bronze_axe
  - ic2_120:bronze_shovel
  - ic2_120:bronze_hoe
  - ic2_120:bronze_sword
  - ic2_120:weeding_spade
---

# Bronze Tools

<ItemImage id="ic2_120:bronze_pickaxe" scale="4" />

The Bronze Tools are IC2's entry-tier hand tools, sharing a single `BronzeToolMaterial`. They match iron-tier mining level and enchantability but mine at stone-tier speed, making them a useful bridge between stone and iron when bronze is your first smeltable alloy. The Weeding Spade is grouped with the bronze set because it is built from a bronze ingot and a stick, and it serves the farm side of the same progression.

## Overview

**Shared BronzeToolMaterial:** Durability 250, mining speed multiplier 4.0 (stone-tier), base attack damage 2.0, mining level 2 (iron-tier), enchantability 10, repair ingredient any bronze-tagged ingot.

The five core tools (pickaxe, axe, shovel, hoe, sword) take their final attack-damage and attack-speed values from a per-tool modifier on top of the material's base. The Weeding Spade is a separate Item with its own 120 durability and is not a `ToolMaterial` tool.

## Block View

| Bronze Pickaxe | Bronze Axe | Bronze Shovel |
|:--------------:|:----------:|:-------------:|
| <ItemImage id="ic2_120:bronze_pickaxe" scale="2" /> | <ItemImage id="ic2_120:bronze_axe" scale="2" /> | <ItemImage id="ic2_120:bronze_shovel" scale="2" /> |
| Bronze Hoe | Bronze Sword | Weeding Spade |
| <ItemImage id="ic2_120:bronze_hoe" scale="2" /> | <ItemImage id="ic2_120:bronze_sword" scale="2" /> | <ItemImage id="ic2_120:weeding_spade" scale="2" /> |

## Stats

| Tool | Mining Level | Durability | Base Attack | Attack Speed | Repair |
|------|:------------:|:----------:|:-----------:|:------------:|--------|
| Bronze Pickaxe | 2 (iron) | 250 | 1 + 2.0 material = 3.0 | -2.8 | Bronze ingot |
| Bronze Axe | 2 (iron) | 250 | 5 + 2.0 material = 7.0 | -3.0 | Bronze ingot |
| Bronze Shovel | 2 (iron) | 250 | 1.5 + 2.0 material = 3.5 | -3.0 | Bronze ingot |
| Bronze Hoe | 2 (iron) | 250 | -1 + 2.0 material = 1.0 | 0.0 | Bronze ingot |
| Bronze Sword | 2 (iron) | 250 | 3 + 2.0 material = 5.0 | -2.4 | Bronze ingot |
| Weeding Spade | n/a | 120 | n/a (non-weapon Item) | n/a | None — no `ToolMaterial` |

Damage values are reported as the constructor argument plus the `ToolMaterial.getAttackDamage()` of 2.0, matching vanilla's `Item.getAttackDamage()` summation. The Weeding Spade has no attack-damage modifier and no `getAttackDamage()` override, so it deals the player's base fist damage.

**Repair:** Combine a damaged Bronze tool with any bronze ingot in an anvil. The bronze ingot costs 1 durability unit of repair material per anvil step and combines with prior enchantments where compatible.

## How to Use

### Bronze Pickaxe

Standard pickaxe — mines all stone-tier and below blocks (stone, cobblestone, iron ore, copper ore, tin ore, etc.) and is the primary tool for breaking ore blocks until you reach iron.

### Bronze Axe

Standard axe — chops wood and strips logs, and functions as a melee weapon with the highest base damage of the bronze set. Effective against wood-tier blocks; less efficient on stone.

### Bronze Shovel

Standard shovel — digs dirt, sand, gravel, clay, and snow. Use it for fast terrain shaping and for clearing paths.

### Bronze Hoe

Standard hoe — tills dirt and grass into farmland. Right-click on a tillable block to convert it.

### Bronze Sword

Standard sword — your primary melee weapon until iron. The -2.4 attack speed and 5.0 total damage make it comparable to a stone sword in feel.

### Weeding Spade

The Weeding Spade is the dedicated tool for clearing weeds off crop sticks:

- **Right-click** a `CropBlock` whose `CROP_TYPE` is `WEED`. The block reverts to an empty crop stick and drops one weed item at the block's position.
- In survival mode, the spade loses 1 durability per use.
- In creative mode, the durability is not consumed.
- Right-clicking a non-weed crop block has no effect and consumes no durability.

Weeds cannot be harvested by hand and will choke crop growth if left alone. The Weeding Spade is the targeted, no-waste way to clear them and reuse the same crop-stick grid.

## Crafting

All five bronze tools use the standard Minecraft tool shape with a Bronze Ingot (M) and a Stick (S). The Weeding Spade is its own two-row recipe.

| Bronze Pickaxe | Bronze Axe | Bronze Shovel |
|:--------------:|:----------:|:-------------:|
| <Recipe id="ic2_120:bronze_pickaxe" /> | <Recipe id="ic2_120:bronze_axe" /> | <Recipe id="ic2_120:bronze_shovel" /> |
| Bronze Hoe | Bronze Sword | Weeding Spade |
| <Recipe id="ic2_120:bronze_hoe" /> | <Recipe id="ic2_120:bronze_sword" /> | <Recipe id="ic2_120:weeding_spade" /> |

**Pattern notes:**

- Pickaxe: `MMM / _S_ / _S_` (top row three bronze, middle and bottom column a stick)
- Axe: `MM / MS / _S` (mirror of standard axe)
- Shovel: `M / S / S`
- Hoe: `MM / _S / _S`
- Sword: `M / M / S`
- Weeding Spade: `M / S` (two-row: bronze on top, stick below)

The `INGOTS_BRONZE` tag is used in place of a single Bronze Ingot item, so any bronze-producing mod's bronze ingot works in every recipe above.

## Related

- [Tools and Armor reference](../reference/tools_armor.md)
- [Batteries and Mobile Power](../reference/energy_items.md)
- [Bronze Armor](bronze_armor.md)
