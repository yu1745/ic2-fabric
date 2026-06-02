---
navigation:
  title: Fluid Tank
  parent: index.md
  position: 49
  icon: ic2_120:bronze_tank
item_ids:
  - ic2_120:bronze_tank
  - ic2_120:iron_tank
  - ic2_120:steel_tank
  - ic2_120:iridium_tank
---

# Fluid Tank

<BlockImage id="ic2_120:bronze_tank" scale="4" />

Fluid Tanks are single-block bulk fluid buffers. They accept one fluid type at a time, expose Fabric fluid storage on every side, and keep their contents in the dropped item when broken with a wrench or another correct harvesting tool.

## Tier Comparison

| Type | Capacity | Material | Blast Resistance |
|------|----------|----------|-----------------|
| Bronze Tank | 32 buckets (32,000 mB) | Bronze | 6 |
| Iron Tank | 32 buckets (32,000 mB) | Iron | 6 |
| Steel Tank | 128 buckets (128,000 mB) | Steel | 7 |
| Iridium Tank | 1,024 buckets (1,024,000 mB) | Iridium | 10 |

## Usage

- **Fill:** right-click with a filled bucket, mod bucket, filled IC2 cell, or filled universal fluid cell.
- **Drain:** right-click with an empty bucket, empty cell, or empty universal fluid cell to extract one bucket.
- **GUI:** right-click with an empty hand or non-fluid item to open the status screen.
- **Pipes:** any side can insert or extract fluid through the fluid API.
- **Comparator:** outputs 0 when empty, then 1-15 based on fill level.
- **Breaking:** creative mode clears contents and drops nothing; a wrench or correct tool drops the tank with its fluid; the wrong tool destroys it without a drop.

Tanks do not merge into a shared multiblock. Place several tanks next to a pipe network if you need more buffer capacity, and use Pump Attachments or fluid pipe upgrades to move fluid between machines and tanks.

## Recipes

<Recipe id="ic2_120:bronze_tank" />
<Recipe id="ic2_120:iron_tank" />
<Recipe id="ic2_120:steel_tank" />
<Recipe id="ic2_120:iridium_tank" />
