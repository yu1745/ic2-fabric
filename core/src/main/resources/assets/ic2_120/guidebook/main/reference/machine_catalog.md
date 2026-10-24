---
navigation:
  title: Machine Documentation Index
  parent: index.md
  position: 210
  icon: minecraft:book
---

# Machine Documentation Index

This index contains detailed documentation for every machine.

---

## Processing Machines

| Machine | File | Voltage Tier | Energy Use | Upgrade Slots |
|------|------|----------|------|------|
| [Iron Furnace](../machines/iron_furnace.md) | iron-furnace.md | None (burns fuel) | Fuel | 0 |
| [Macerator](../machines/macerator.md) | macerator.md | Tier 1 (LV) | 2 EU/t | 4 |
| [Compressor](../machines/compressor.md) | compressor.md | Tier 1 (LV) | 2 EU/t | 4 |
| [Extractor](../machines/extractor.md) | extractor.md | Tier 1 (LV) | 2 EU/t | 4 |
| [Metal Former](../machines/metal_former.md) | metal-former.md | Tier 1 (LV) | 10 EU/t | 4 |
| [Electric Furnace](../machines/electric_furnace.md) | electric-furnace.md | Tier 1 (LV) | 3 EU/t | 0 |
| [Induction Furnace](../machines/induction_furnace.md) | induction-furnace.md | Tier 2 (MV) | variable | 2 |
| [Fluid Canner](../machines/fluid_canner.md) | fluid-bottler.md | Tier 1 (LV) | 2 EU/t | - |
| [Solid Canner](../machines/solid_canner.md) | solid-canner.md | Tier 1 (LV) | 2 EU/t | - |
| [Canner](../machines/canner.md) | canner.md | Tier 2 (MV) | 4 EU/t | - |
| [Block Cutter](../machines/block_cutter.md) | block-cutter.md | Tier 1 (LV) | 4 EU/t | - |
| [Recycler](../machines/recycler.md) | recycler.md | Tier 1 (LV) | 1 EU/t | - |
| [Ore Washing Plant](../machines/ore_washing_plant.md) | ore-washing-plant.md | Tier 1 (LV) | 16 EU/t | 4 |
| [Thermal Centrifuge](../machines/centrifuge.md) | centrifuge.md | Tier 2 (MV) | 48+1 EU/t | 4 |
| [Blast Furnace](../machines/blast_furnace.md) | blast-furnace.md | Tier 1 | Heat (HU) + Compressed Air | 2 (Ejector/Puller only) |

## Generators

| Machine | File | Voltage Tier | Output | Fuel / Energy Source |
|------|------|----------|------|---------------|
| [Generator](../generator.md) | generator.md | Tier 1 (LV) | 10 EU/t | Coal / Charcoal |
| [Stirling Generator](../machines/stirling_generator.md) | stirling-generator.md | Tier 2 (MV) | 50 EU/t | Heat (HU) |
| [Steam Generator](../machines/steam_generator.md) | steam-generator.md | Tier 2 (MV) | Steam | Water + Heat (HU) |
| [Geothermal Generator](../machines/geo_generator.md) | geo-generator.md | Tier 1 (LV) | 20 EU/t | Lava |
| [Solar Generator](../machines/solar_generator.md) | solar-generator.md | Tier 1 (LV) | 1 EU/t | Sunlight |
| [Wind Generator](../machines/wind_generator.md) | wind-generator.md | Tier 1 (LV) | 3 EU/t above Y=74 | Height threshold |
| [Kinetic Generator](../machines/kinetic_generator.md) | kinetic-generator.md | Tier 3 (HV) | 512 EU/t | Kinetic (KU) |
| [Creative Generator](../machines/creative_generator.md) | creative-generator.md | Tier 1 (LV) | 32 EU/t | Infinite (creative) |

## Kinetic Machines

| Machine | File | Output | Energy Source |
|------|------|------|----------|
| [Manual Kinetic Generator](../machines/manual_kinetic_generator.md) | manual-kinetic-generator.md | 4-16 KU/t | Hand crank |
| [Water Kinetic Generator](../machines/water_kinetic_generator.md) | water-kinetic-generator.md | 64-384 KU/t | Water flow + rotor |
| [Wind Kinetic Generator](../machines/wind_kinetic_generator.md) | wind-kinetic-generator.md | 128-768 KU/t | Wind + rotor |
| [Leash Kinetic Generator](../machines/leash_kinetic_generator.md) | leash-kinetic-generator.md | ≤512 KU/t | Animal circling |

## Heat-Related Machines

| Machine | File | Notes |
|------|------|------|
| [Solid Heat Generator](../machines/solid_heat_generator.md) | solid-heat-generator.md | Burns coal/charcoal/coke → 20 HU/t |
| [Fluid Heat Generator](../machines/fluid_heat_generator.md) | fluid-heat-generator.md | Burns biofuel → 32 HU/t |
| [Electric Heat Generator](../machines/electric_heat_generator.md) | electric-heat-generator.md | 1:1 EU→HU, coils up to 100 HU/t |
| [Radioisotope Heat Generator](../machines/rt_heat_generator.md) | rt-heat-generator.md | Runs forever on RTG pellets, up to 64 HU/t |
| [Fluid Heat Exchanger](../machines/liquid_heat_exchanger.md) | liquid-heat-exchanger.md | Hot coolant / lava → HU + coolant |
| [Solar Distiller](../machines/solar_distiller.md) | solar-distiller.md | Free distilled water from sunlight |
| [Condenser](../machines/condenser.md) | condenser.md | Steam + EU → distilled water |
| [Fermenter](../machines/fermenter.md) | fermenter.md | Heat + biomass → biofuel + fertilizer |

## Resource Machines

| Machine | File | Voltage Tier | Function |
|------|------|----------|------|
| [Pump](../machines/pump.md) | pump.md | Tier 1 (LV) | Fluid extraction |
| [Miner](../machines/miner.md) | miner.md | Tier 2 (MV) | Auto mining |
| [Advanced Miner](../machines/advanced_miner.md) | advanced-miner.md | Tier 3 (HV) | Advanced auto mining (filter / Silk Touch / wide area) |
| [Chunk Loader](../machines/chunk_loader.md) | chunk-loader.md | Tier 1 (LV) | Force-load chunks |
| [Animal Slaughterer](../machines/animal_slaughterer.md) | animal-slaughterer.md | Tier 1 (LV) | Auto slaughter |
| [Crop Harvester](../machines/crop_harvester.md) | crop-harvester.md | Tier 1 (LV) | Auto harvesting |
| [Cropmatron](../machines/cropmatron.md) | cropmatron.md | Tier 1 (LV) | Crop management |
| [UV Lamp](../machines/uv_lamp.md) | uv-lamp.md | Tier 1-5 (variable) | Crop growth boost |
| [Animalmatron](../machines/animalmatron.md) | animalmatron.md | Tier 1 (LV) | Animal husbandry |
| [Magnetizer](../machines/magnetizer.md) | magnetizer.md | Tier 1 (LV) | Magnetize iron blocks |

## Energy Storage and Transformers

| Machine | File | Capacity | Input / Output |
|------|------|------|------|
| [BatBox](../machines/energy_storage.md) | storage.md#batbox | 40,000 EU | 32 EU/t |
| [CESU](../machines/energy_storage.md) | storage.md#cesu | 300,000 EU | 128 EU/t |
| [MFE](../machines/energy_storage.md) | storage.md#mfe | 4,000,000 EU | 512 EU/t |
| [MFSU](../machines/energy_storage.md) | storage.md#mfsu | 40,000,000 EU | 2048 EU/t |
| [Transformer](../machines/transformer.md) | transformer.md | - | Voltage conversion |

## Storage Containers

| Machine | File | Capacity |
|------|------|------|
| [Storage Box](../machines/storage_box.md) | storage-box.md | 27-126 slots (5 materials) |
| [Fluid Tank](../machines/tank.md) | fluid-tanks.md | 32-1024 buckets (4 materials) |

## Nuclear

| Machine | File | Notes |
|------|------|------|
| [Nuclear Reactor](../machines/nuclear_reactor.md) | nuclear-reactor.md | Reactor + chamber (electric mode) |
| [Fluid-Cooled Reactor](../machines/nuclear_reactor_fluid_mode.md) | nuclear-reactor-fluid-mode.md | Vessel, fluid port, access hatch, redstone port (fluid mode) |

## UUMatter

| Machine | File | Notes |
|------|------|------|
| [UUMatter System](../machines/matter_generator.md) | uu-matter.md | Matter Generator + UU Scanner + Pattern Storage + Replicator |

## Coke Oven

| Machine | File | Notes |
|------|------|------|
| [Coke Kiln](../machines/coke_kiln.md) | coke-kiln.md | Multiblock structure, charcoal/coke + creosote |

## Teleportation

| Machine | File | Function |
|------|------|------|
| [Teleporter](../machines/teleporter.md) | teleporter.md | Entity teleportation |

## Other

| Machine | File | Notes |
|------|------|------|
| [Tesla Coil](../machines/tesla_coil.md) | tesla-coil.md | Redstone-triggered monster zapper |
| [Luminator Flat](../machines/luminator_flat.md) | luminator.md | Light source that mounts on all 6 faces |

---

## Related Documents

- [EU Energy System](../systems/eu_energy.md) - cables, overvoltage explosions
- [Power Generation and Storage](../guides/power_generation.md) - generator guide
- [Upgrade System](../guides/upgrades.md) - upgrade slot details
- [Kinetic System](../systems/kinetic_transmission.md) - KU transmission details
- [Heat System](../systems/heat_system.md) - HU heat
