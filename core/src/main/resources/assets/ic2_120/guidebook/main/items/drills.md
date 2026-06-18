---
navigation:
  title: Drills
  parent: index.md
  position: 330
  icon: ic2_120:drill
item_ids:
  - ic2_120:drill
  - ic2_120:diamond_drill
  - ic2_120:iridium_drill
---

# Drills

<ItemImage id="ic2_120:drill" scale="4" />

The Drill family is a three-tier line of electric mining tools that replace the vanilla pickaxe and shovel in one slot. Each tier is an `IElectricTool`: it never loses vanilla durability, draws its power from an internal EU buffer, and registers in both the `minecraft:pickaxes` and `minecraft:shovels` item tags so it can mine any block that a tool of the corresponding material would. When the buffer runs dry, the tool degrades to the slowest base mining speed rather than breaking.

## Item View

| Drill | Diamond Drill | Iridium Drill |
|:-----:|:-------------:|:-------------:|
| <ItemImage id="ic2_120:drill" scale="2" /> | <ItemImage id="ic2_120:diamond_drill" scale="2" /> | <ItemImage id="ic2_120:iridium_drill" scale="2" /> |

## Stats

| Drill | Tier | Capacity | Per-Block EU Cost | Pickaxe Behavior | Shovel Behavior | Special |
|-------|:----:|---------:|------------------:|------------------|-----------------|---------|
| <ItemLink id="ic2_120:drill" /> | 1 | 10,000 EU | 50 EU | Iron pickaxe | Iron shovel | Entry-level electric drill |
| <ItemLink id="ic2_120:diamond_drill" /> | 1 | 10,000 EU | 80 EU | Diamond pickaxe | Diamond shovel | Upgrades the head to diamond capability |
| <ItemLink id="ic2_120:iridium_drill" /> | 3 | 1,000,000 EU | 800 EU (Silk Touch mode: 8,000 EU) | Netherite pickaxe | Netherite shovel | Right-click toggles Silk Touch; 10x EU in that mode |

The Drill and Diamond Drill share tier 1 and 10,000 EU capacity — the difference is what they can mine and how fast. The Iridium Drill is a separate tier-3 tool with a 100x larger buffer and netherite-level head, plus a Silk Touch toggle that costs ten times the usual EU per block.

## How to Use

### Equipping

Place the drill in your main hand. Because each drill is tagged as both a pickaxe and a shovel, it can break any block a vanilla tool of its head material can break — and at the speed of the fastest matching vanilla tool.

### Power and Degradation

The drill family does **not** consume vanilla durability; all wear is paid in EU.

- The cost listed in the table above is deducted from the internal buffer for every hard block successfully mined.
- If the buffer is below that cost, the tool still mines, but the mining speed falls back to `1.0` — the slow hand-equivalent — instead of the powered rate.
- The energy bar above the durability bar shows the buffer level for every drill in the family.

Charge any drill the same way you charge a hand-held battery: a charger, charging pad, or a BatBox/CESU/MFE/MFSU will fill the buffer directly.

### Iridium Drill Mode Switching

The Iridium Drill is the only drill in the family with a toggle. It has two modes:

- **Fortune mode** (default): applies the virtual enchantments Fortune III + Efficiency III, at the normal 800 EU/block cost.
- **Silk Touch mode**: applies the virtual enchantment Silk Touch I, at a 10x cost of 8,000 EU/block.

Switch modes with **right-click**. The mode is stored in the drill's NBT under the `SilkTouchEnabled` boolean and persists between sessions. The drill also has a Mode Switch key bound to it (see the in-game key-binds screen) — both controls do the same thing, and the mode is also re-evaluated every inventory tick so the virtual enchantments stay in sync with the current EU buffer.

When the buffer cannot cover the current mode's per-block cost, the virtual enchantments are cleared entirely; the drill falls back to the base netherite head speed with no Fortune, Efficiency, or Silk Touch applied.

## Crafting

The three drills form a straight upgrade chain: Drill → Diamond Drill (head upgrade) → Iridium Drill (chassis, head, and power upgrade together).

| Drill | Diamond Drill |
|:-----:|:-------------:|
| <Recipe id="ic2_120:drill" /> | <Recipe id="ic2_120:diamond_drill" /> |
| Iridium Drill | |
| <Recipe id="ic2_120:iridium_drill" /> | |

## Related

- [Tools and Armor reference](../reference/tools_armor.md)
- [Batteries and Mobile Power](../reference/energy_items.md)
- [Mining Laser](mining_laser.md) — the long-range alternative for bulk mining
- [Energy Crystal and Lapotron Crystal](energy_crystal.md) — the high-capacity cells that recharge these drills
