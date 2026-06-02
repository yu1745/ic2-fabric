---
navigation:
  title: Water Mill
  parent: index.md
  position: 13
  icon: ic2_120:water_generator
item_ids:
  - ic2_120:water_generator
---

# Water Mill

<BlockImage id="ic2_120:water_generator" p:facing="north" p:active="true" scale="4" />

The Water Mill is a small direct EU generator. It has two independent water sources: an internal one-bucket water tank for container-fed generation, and nearby water blocks for passive environmental generation.

Water containers are emptied into the internal tank, then consumed at **1 EU/t** for **500 ticks**. One full bucket of water therefore becomes **500 EU**. The fuel slot accepts water buckets, water cells, distilled water cells, and universal fluid cells that contain water or distilled water. Empty buckets or empty cells are placed in the empty container slot.

The environmental mode scans the **3x3x3** cube around the machine, excluding the machine's own block. Each water block in that cube adds **0.01 EU/t**. A fully surrounded Water Mill can therefore add up to **0.26 EU/t** before the fractional output is accumulated into whole EU.

## Output

- **Tank Output**: 1 EU/t while water remains in the internal tank
- **Water Container Yield**: 500 EU per bucket, cell, or compatible fluid cell
- **Environmental Output**: 0.01 EU/t per nearby water block, up to 0.26 EU/t
- **Energy Storage**: 10,000 EU
- **Maximum EU Extraction**: 20 EU/t, once at least 32 EU is buffered
- **Tier**: 1

## Slots

- Fuel slot (top): water buckets, water cells, distilled water cells, or compatible fluid cells
- Empty container slot (middle): outputs empty containers after water is consumed
- Battery slot (right): charges tier 1 batteries and electric tools
- Upgrade slots (4): accept upgrade items

Item automation may insert water containers into the fuel slot, tier-compatible chargeable items into the battery slot, and upgrades into the upgrade slots. It may extract from the fuel, empty container, battery, and upgrade slots.

Fluid automation can insert vanilla water directly into the internal one-bucket tank from any side except the front face. The tank does not expose fluid extraction. The machine does not accept EU input, and it outputs EU from every side except its front face.

## Water Kinetic Generator Difference

The Water Mill makes **EU directly** and does not use a rotor. The Water Kinetic Generator is a separate kinetic machine: it needs a water rotor, checks the rotor's swept area in front of the block, outputs **KU** from its back face, and must feed a Kinetic Generator or kinetic transmission before you get EU.

## Recipe

<Recipe id="ic2_120:water_generator" />
