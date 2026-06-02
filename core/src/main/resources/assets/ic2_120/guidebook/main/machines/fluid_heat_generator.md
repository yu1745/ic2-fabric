---
navigation:
  title: Liquid Fuel Firebox
  parent: index.md
  position: 54
  icon: ic2_120:fluid_heat_generator
item_ids:
  - ic2_120:fluid_heat_generator
---

# Liquid Fuel Firebox

<BlockImage id="ic2_120:fluid_heat_generator" scale="4" />

The Liquid Fuel Firebox (Fluid Heat Generator) burns **Biofuel** to produce heat units (HU) for adjacent heat machines. It is a dedicated Biofuel heat source; it does not burn lava, creosote oil, fuel, or arbitrary flammable fluids.

## Heat Output

When it has Biofuel and a valid heat consumer connected, the Liquid Fuel Firebox produces **32 HU/t**. Fuel is consumed at about **10 mB/s**, so one bucket lasts about 100 seconds.

Heat is not buffered. Each tick's HU must be accepted immediately by the machine on the heat-transfer face, or it is lost. The firebox does not burn fuel while no valid heat consumer is connected.

## Fuel Tank

The internal tank holds **8 buckets** of Biofuel. Biofuel can be inserted by:

- Right-clicking the block with a Biofuel container
- Putting a Biofuel bucket, Biofuel cell, or Biofuel fluid cell into the fuel slot
- Inserting Biofuel through Fabric fluid storage from any side

The machine accepts only Biofuel in its still or flowing form. Its exposed fluid storage is input-only, so external pipes cannot drain fuel from the tank directly.

## Slots

- Upper left slot: filled Biofuel container input
- Lower left slot: empty bucket or empty cell output
- Right column: 4 upgrade slots

Filled containers are processed one bucket at a time. The tank needs at least one bucket of free space, and the empty-container output slot must be able to receive the returned bucket or cell.

## Upgrades and Automation

The upgrade slots accept upgrade items for this machine's supported upgrade interface. In practice, the useful upgrade is the **Fluid Pulling Upgrade**, which lets the firebox pull Biofuel from adjacent fluid storages using the upgrade's filter and side settings.

The **Fluid Ejector Upgrade** can be installed, but the firebox tank is not externally extractable and has no processed output fluid, so it is normally not useful here. Overclocker, transformer, energy storage, and item ejector upgrades do not change the firebox's heat output.

Item automation can insert valid Biofuel containers into the fuel slot and valid upgrades into the upgrade slots. The fuel slot and empty-container slot can be extracted from.

## Connecting Heat Consumers

HU leaves through the firebox's single heat-transfer face, not through every side. Place the heat consumer directly against that face, and make sure the consumer's own heat-transfer face points back at the firebox.

Common consumers include the **Steam Generator**, **Stirling Generator**, **Blast Furnace**, and **Fermenter**. If the faces are not aligned, the firebox stays inactive and does not consume Biofuel.

## Recipe

<Recipe id="ic2_120:fluid_heat_generator" />
