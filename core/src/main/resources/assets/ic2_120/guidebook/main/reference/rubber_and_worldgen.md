---
navigation:
  title: Rubber Trees and World Resources
  parent: index.md
  position: 217
  icon: ic2_120:rubber_sapling
item_ids:
  - ic2_120:rubber_sapling
  - ic2_120:rubber_log
  - ic2_120:rubber_wood
  - ic2_120:stripped_rubber_log
  - ic2_120:stripped_rubber_wood
  - ic2_120:rubber_leaves
  - ic2_120:rubber_planks
  - ic2_120:rubber_stairs
  - ic2_120:rubber_slab
  - ic2_120:rubber_door
  - ic2_120:rubber_trapdoor
  - ic2_120:rubber_button
  - ic2_120:rubber_pressure_plate
  - ic2_120:rubber_fence
  - ic2_120:rubber_fence_gate
  - ic2_120:resin
  - ic2_120:rubber
  - ic2_120:tin_ore
  - ic2_120:deepslate_tin_ore
  - ic2_120:lead_ore
  - ic2_120:deepslate_lead_ore
  - ic2_120:uranium_ore
  - ic2_120:deepslate_uranium_ore
  - ic2_120:peat_ore
  - ic2_120:iridium_ore_item
---

# Rubber Trees and World Resources

## Ores

World generation provides tin, lead, uranium, peat, and other resources, some of which have deepslate variants. Below Y 0, ores automatically generate as their deepslate variants.

| Ore | Y-Level Range | Vein Size | Veins per Chunk | Notes |
|------|:---:|:---:|:---:|-------|
| **Tin** | Y −24 to Y 80 | 9 | 10 | Densest near Y 28; extra deepslate veins at Y −64 to −48 and upper veins at Y 128–320 |
| **Lead** | Y −64 to Y 32 | 9 | 4 | Has a 50% chance to discard in air (caves), so underground is more reliable |
| **Uranium** | Y −64 to Y 63 | 4 | 7 | The rarest ore; veins are small (4 blocks) with a 70% air discard chance |
| **Peat** | Y −16 to Y 64 | 8 | 24 | Biome-restricted (configurable); generates only in allowed biomes |

### Iridium

Iridium ore does **not** generate as a world ore block. It is obtained through:

1. **Chest loot** — found in dungeon chests, stronghold corridors, ancient cities, village blacksmith chests, and other structures. See the Chest Loot section below for details.
2. **Iridium Shards** — a more common chest loot drop; 9 shards can be compressed into 1 iridium ore via the Compressor.
3. **UU Matter replication** — 120,000 EU for iridium ore, 13,333 EU for iridium shards.

## Chest Loot

IC2 injects loot into vanilla chest types worldwide. Key drops include:

| Chest Type | Tin Ingot | Copper Ingot | Iridium Shard | Iridium Ore | Other |
|------------|:---:|:---:|:---:|:---:|-------|
| Spawn Bonus Chest | — | — | — | — | Treetap (80%) |
| Mineshaft | 1–5 | 2–6 | 2–5 | — | Bronze Pickaxe, Filled Tin Cans |
| Simple Dungeon | 2–5 | 2–5 | 6–14 | 1–2 | — |
| Desert Pyramid | 1–5 | 2–5 | 1–2 | — | Bronze tools & armor |
| Stronghold Corridor | 1–5 | 2–5 | 4–14 | 1–4 | — |
| Village Blacksmith | 1–5 | 2–5 | 3–7 | — | Bronze Ingots, Rubber Saplings |
| Ancient City | — | — | 4–12 | 1–3 | — |

- Each chest rolls 1–3 items. Empty rolls (weight ~30–34) mean no IC2 loot that roll.
- Village blacksmith, armorer, toolsmith, and weaponsmith chests all share the same IC2 loot table.
- Mineshaft and Nether Fortress chests share an "industrial" loot table with tin/copper/iridium and bronze tools.

## Rubber Trees

- Rubber trees generate Rubber Logs, Rubber Leaves, and Rubber Saplings.
- A rubber log's side may carry a resin hole.
- Right-click a resin hole with a Treetap or an Electric Treetap to obtain Sticky Resin.
- Sticky Resin can be processed into Rubber, which is used in cable insulation, tools, armor, and machine parts.

## Rubber Wood Blocks

Rubber wood provides a full set of wood blocks: log, stripped log, planks, stairs, slab, door, trapdoor, button, pressure plate, fence, fence gate, sign, and more.

## Early-Game Resource Route

1. Mine tin, copper, lead, and uranium ore to set up the base metal chain.
2. Find rubber trees; collect resin and saplings.
3. Craft a Forge Hammer, Plate Cutting Shears, and basic cables.
4. Set up a generator, energy storage, and your first electric machines.

Materials reference: [Materials and Ores](materials_ores.md)

## Related

- [Treetap](../items/treetaps.md) — extracts Sticky Resin from rubber logs
- [IC2 Boats](../items/boats.md) — rubber and reinforced boat variants
