---
navigation:
  title: Creative Mode Tabs
  parent: index.md
  position: 208
  icon: minecraft:book
---

# Creative Mode Tabs

The mod's creative inventory is split into a few main tabs:

- `IC2 Materials`: ores, ingots, cables, plates, dusts, rubber, etc.
- `IC2 Machines`: generators, processing machines, energy storage, and charging pads.
- `Tools`: bronze tools, utility tools, boats, debug items, etc.
- `Seeds`: every seed type with default stats of 1.

  **Obtaining seeds with specific stats**:

  Use the `/ic2seed` command to obtain a seed with the exact stats you want. Cheats must be enabled in survival, or you can be in creative mode.

  - `/ic2seed list` — list every available seed type (with internal names)
  - `/ic2seed <seed type> <growth> <gain> <resistance>` — obtain a seed with the given stats

  **If you don't know the seed's internal name**:
  1. Run `/ic2seed list` to see the full list and the matching English names
  2. Then run `/ic2seed <name> <growth> <gain> <resistance>` to get the seed
  3. Or just use Tab-completion while typing the command

  **Stat glossary**:
  - **Growth**: crop growth speed, 0-31
  - **Gain**: harvest yield, 0-31
  - **Resistance**: environmental tolerance, 0-31

  **Examples**:
  ```
  /ic2seed stick_maturity 31 31 31    # A perfect stickreed
  /ic2seed reed 20 15 10              # A custom sugar cane
  ```
