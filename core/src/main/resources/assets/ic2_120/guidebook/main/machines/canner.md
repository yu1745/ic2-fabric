---
navigation:
  title: Fluid/Solid Canning Machine
  parent: index.md
  position: 37
  icon: ic2_120:canner
item_ids:
  - ic2_120:canner
---

# Fluid/Solid Canning Machine

<BlockImage id="ic2_120:canner" p:facing="north" p:active="true" scale="4" />

The Fluid/Solid Canning Machine is a mode-based canner with two 8-bucket tanks. It can load solid recipes, drain filled fluid containers into its left tank, fill containers from its right tank, and mix a fluid with a solid ingredient into another fluid.

## Power

- **Energy tier:** 1
- **Max EU input:** 32 EU/t
- **EU storage:** 832 EU
- **Base draw:** 4 EU/t
- **Base time:** 200 ticks / 10 seconds
- **Base cost:** 800 EU per operation

Changing modes or swapping the tanks resets progress.

## Modes

| Mode | Input | Output |
|------|-------|--------|
| Solid Canning | left slot container + middle slot solid ingredient | right slot item |
| Drain | filled fluid container in the left slot | left tank + empty container in the right slot |
| Fill | empty bucket/cell in the left slot, or Foam Sprayer/CF Pack in the middle slot | filled item from the right tank |
| Enrich Fluid | left tank fluid + middle slot material | right tank fluid |

Solid Canning uses the same machine recipes as the Solid Canning Machine for Empty Fuel Rod + Uranium/MOX. Unlike the dedicated Solid Canning Machine, this combined machine does not use the food-to-tin-can fallback.

## Mixing Recipes

Enrich Fluid consumes 1 bucket from the left tank and the listed material, then creates 1 bucket in the right tank:

| Left tank | Material | Right tank |
|-----------|----------|------------|
| Water | 8 Lapis Dust | Coolant |
| Distilled Water | 1 Lapis Dust | Coolant |
| Water | Bio Chaff | Biomass |
| Water | Peat Ore | Biomass |
| Water | CF Powder | Construction Foam |
| Water | Grin Powder | Weed-Ex |

## Slots

- **Left slot:** mode-dependent container input
- **Middle slot:** solid material, Foam Sprayer, or CF Pack
- **Right slot:** item output
- **Battery slot:** discharges tier 1+ batteries into the internal buffer
- **Upgrade slots:** 4 slots

## Tanks and Automation

Both internal tanks hold 8,000 mB. External fluid access is split: pipes can insert into the left tank and extract from the right tank from any side. The machine also supports Fluid Pulling and Fluid Ejector upgrades. Pulling is used in Drain mode to pull fluid into the left tank, while ejecting is used in Fill and Enrich Fluid modes to push the right tank outward.

The right tank is the fill source, so use the tank-swap button after draining containers or mixing fluid if you want to reuse the produced fluid as the next operation's input.

Item automation routes valid containers to the left slot, valid materials to the middle slot, batteries to the battery slot, and upgrades to the upgrade slots. The left, middle, right, and battery slots are extractable, which lets automation recover containers or materials when changing modes.

## Upgrades

Supported upgrades are Overclocker, Transformer, Energy Storage, Ejector, Pulling, Fluid Ejector, and Fluid Pulling upgrades. Overclockers speed operations while increasing EU use. Transformer and Energy Storage upgrades raise voltage tolerance and internal storage. Item Ejector/Pulling upgrades work on item slots; fluid upgrades work on the tanks as described above.

## Recipe

<Recipe id="ic2_120:canner" />
