---
navigation:
  title: Tools and Armor
  parent: index.md
  position: 212
  icon: minecraft:book
item_ids:
  - ic2_120:forge_hammer
  - ic2_120:cutter
  - ic2_120:frequency_transmitter
  - ic2_120:obscurator
  - ic2_120:tool_box
  - ic2_120:jetpack_attachment_plate
---

# Tools and Armor

## Basic Tools

- **Bronze tool set**: bronze pickaxe, axe, shovel, hoe, sword — durability and mining tier close to iron, mining speed close to stone.
- **Forge Hammer**: forges ingots into plates and plates into casings, 80 durability.
- **Plate Cutter**: cuts plates into cables, and strips insulation from cables, 60 durability.
- **Wooden Tap**: right-clicks the resin hole on a rubber tree log to obtain sticky resin.
- **Wrench**: rotates machines or configures certain machine modes.
- **Tool Box**: right-click to store hotbar tools, sneak + right-click to release them back into the inventory.
- **Weeding Trowel**: clears weeds from crop sticks.
- **EU Debug Stick**: inspects the EU state of devices; mainly used for debugging.
- **Wind Meter**: shows the current wind strength and the theoretical output.

---

## Electric Tools

Electric tools do not consume vanilla durability and use EU. When unpowered, functionality is limited or stops.

| Tool | Tier | Capacity | Typical Consumption | Use |
|------|------|----------|----------|-----|
| Mining Drill | 1 | 10,000 EU | 50 EU/block | Pickaxe/shovel hybrid |
| Diamond Drill | 2 | 10,000 EU | 80 EU/block | Higher mining capability |
| Iridium Drill | 3 | 1,000,000 EU | 800 EU/block, 10x for silk touch | High-tier pickaxe/shovel, can toggle silk touch |
| Chainsaw | 1 | 30,000 EU | 250 EU/block | Axe-type felling |
| Electric Resin Extractor | 1 | 10,000 EU | Per use | Extracts rubber tree resin |
| Electric Wrench | 1 | 10,000 EU | Per use | Machine rotation and disassembly |
| Nano Saber | 3 | 160,000 EU | 400 EU on hit, idle drain when active | Right-click to toggle; high damage when active and powered |

---

## Mining Laser

See: [Mining Laser](../guides/mining_laser.md)

---

## Scanning and Remote Tools

- **OD Scanner**: scans surrounding mineral resources, uses EU.
- **OV Scanner**: more advanced mineral scanner with longer range and stronger capability than the OD.
- **Frequency Transmitter**: used for remote binding scenarios such as teleporters.
- **Obscurator**: disguises the appearance of blocks.
- **Construction Foam Sprayer**: consumes construction foam; combined with the construction foam backpack it supports continuous building.

---

## Boats

- **Rubber Boat**, **Broken Rubber Boat**, **Carbon Fiber Boat**, **Electric Boat**
- They differ in handling and top speed, fitting different on-water scenarios.

---

## Wrench

The wrench is used to configure machine facing:
- **Right-click**: rotate machine facing clockwise
- **Shift + Right-click**: rotate machine facing counter-clockwise

Some machines allow the wrench to configure their functional mode.

---

## Armor

### Hazmat Suit Set

Provides radiation protection.

| Slot | Item | Armor | Durability Multiplier |
|------|------|-------|-----------------------|
| Helmet | hazmat_helmet | 1 | 5x |
| Chestplate | hazmat_chestplate | 3 | 5x |
| Leggings | hazmat_leggings | 2 | 5x |
| Boots | rubber_boots | 1 | 5x |

**Set effects**:
- Immunity to nuclear radiation damage when core temperature > 7,000
- **Hazmat Helmet**: underwater breathing (consumes compressed air units from the inventory)

### Bronze Armor Set

IC2's base armor — full four-piece protection.

| Slot | Armor |
|------|-------|
| Helmet | 2 |
| Chestplate | 6 |
| Leggings | 5 |
| Boots | 2 |

### Nano Armor Set

Energy-driven armor with 1,000,000 EU capacity per piece.

| Slot | Armor | Toughness |
|------|-------|-----------|
| Helmet | 3 | 2.0 |
| Chestplate | 8 | 2.0 |
| Leggings | 6 | 2.0 |
| Boots | 3 | 2.0 |

### Quantum Armor Set

IC2's top-tier energy armor with 10,000,000 EU capacity per piece.

| Slot | Armor | Toughness | Knockback Resistance |
|------|-------|-----------|----------------------|
| Helmet | 4 | 3.0 | 0.4 |
| Chestplate | 9 | 3.0 | 0.4 |
| Leggings | 6 | 3.0 | 0.4 |
| Boots | 4 | 3.0 | 0.4 |

### Solar Helmet

Automatically charges battery items in the player's inventory under sunlight (1 EU/tick).

### Battery Backpacks

A mobile power storage device worn in the chestplate slot. When worn, it automatically charges electric tools in the inventory.

| Backpack | Capacity | Tier | Auto-charge |
|----------|----------|------|-------------|
| BatPack | 60,000 EU | 1 | Inventory electric tools |
| Advanced BatPack | 600,000 EU | 2 | Inventory electric tools |
| Energy Pack | 2,000,000 EU | 3 | Inventory electric tools |
| LapPack | 60,000,000 EU | 4 | Inventory electric tools |

### Night Vision Goggles

A head-mounted device that provides night vision. Alt+N to toggle, 1 EU/tick to maintain.

### Jetpacks and Construction Backpacks

- **Jetpack**: a chestplate-slot flight device that uses fuel or internal resources.
- **Electric Jetpack**: a flight device that uses EU.
- **Construction Foam Backpack**: stores construction foam, used together with the foam sprayer.
- **Jetpack Attachment Plate**: used in related equipment recipes.

### Composite Chestplate

The composite chestplate provides high chest protection, suited for use when neither a battery backpack nor a jetpack is needed.

Related pages: [Batteries and Mobile Power](energy_items.md), [Rubber Trees and World Resources](rubber_and_worldgen.md)
