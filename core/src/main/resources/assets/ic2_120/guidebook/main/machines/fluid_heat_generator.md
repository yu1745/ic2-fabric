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

## Typical Setup: Fermenter Loop

The most common automatic setup is the Liquid Fuel Firebox sitting face-to-face against a <ItemLink id="ic2_120:fermenter" />, with one fluid upgrade bridging the two:

- Place the Fermenter and the Liquid Fuel Firebox directly next to each other so their fluid ports share a face.
- Install **either** a **Fluid Ejector Upgrade** in the Fermenter **or** a **Fluid Pulling Upgrade** in the Liquid Fuel Firebox. Both upgrades do the same job in this setup — push the Fermenter's Biofuel output into the firebox. Pick one, never both at once.
- Keep the Fermenter heated by a separate heat source, since the firebox's own heat face is pointed at the Fermenter and is not available for an external heater.
- Keep a steady supply of biomass going into the Fermenter, and a place to collect the empty buckets, fertilizer, and filled fertilizer the Fermenter produces.

A single Fermenter produces Biofuel at **10 mB/t**, and a single Liquid Fuel Firebox consumes Biofuel at **10 mB/t**. One Fermenter feeding one firebox is therefore a closed 1:1 loop with no net Biofuel. To get a real net surplus, scale up the Fermenter bank and let the firebox run on a fraction of the production.

**A typical working setup is about ten Fermenters around one Liquid Fuel Firebox.** The firebox burns the 10 mB/t it needs and the other ~90 mB/t of Biofuel becomes the net output. That surplus is usually piped to a bank of <ItemLink id="ic2_120:semifluid_generator" /> machines, since Biofuel is a registered semifluid fuel. A ten-Fermenter bank averages around **5.5 Semifluid Generators** of headroom (Biofuel surplus ÷ each generator's 16 mB/t draw), with the exact count depending on rounding and what the Fermenters are doing that minute.

## Recipe

<Recipe id="ic2_120:fluid_heat_generator" />
