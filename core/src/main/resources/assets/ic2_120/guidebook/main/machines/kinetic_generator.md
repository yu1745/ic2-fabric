---
navigation:
  title: Kinetic Generator
  parent: index.md
  position: 20
  icon: ic2_120:kinetic_generator
item_ids:
  - ic2_120:kinetic_generator
---

# Kinetic Generator

<BlockImage id="ic2_120:kinetic_generator" p:facing="north" p:active="true" scale="4" />

The Kinetic Generator is the bridge from the kinetic transmission network into the EU grid. It is a KU consumer: each tick it pulls kinetic energy from shafts, bevel gears, or adjacent kinetic machines connected to its front face, then converts that input at a fixed rate of 4 KU = 1 EU.

It only accepts KU on the face the machine is pointing toward. EU leaves from any other side, so keep the front clear for the kinetic line and run cables from the sides, top, back, or bottom.

## Conversion

- **Conversion rate**: 4 KU = 1 EU
- **Maximum KU input**: 2,048 KU/t
- **Maximum EU output**: 512 EU/t
- **Energy buffer**: 10,000 EU
- **Tier**: 3

The generator starts showing active once incoming power reaches 64 KU/t and stays active until input drops below 48 KU/t. This hysteresis keeps the block from flickering near the startup threshold.

KU that does not divide evenly into EU is saved as a small internal remainder, so partial conversion is not lost between ticks.

## Slots

The Kinetic Generator has no item slots. Its screen only shows status and the player inventory.

Because it has no machine inventory, hoppers, pipes, and item upgrades have nothing to insert or extract. Automation is about placement: feed KU into the front, and extract EU from any non-front side with cables or adjacent energy consumers.

## Kinetic Input

Transmission shafts carry KU only along their axis. Use a <ItemLink id="ic2_120:bevel_gear" /> when the line needs to turn 90 degrees or when a machine must connect from the side of a shaft. Shaft material limits the path throughput:

- <ItemLink id="ic2_120:wood_transmission_shaft" />: 128 KU/t
- <ItemLink id="ic2_120:iron_transmission_shaft" />: 512 KU/t
- <ItemLink id="ic2_120:steel_transmission_shaft" />: 2,048 KU/t
- <ItemLink id="ic2_120:carbon_transmission_shaft" />: 8,192 KU/t

Path loss is subtracted before the KU reaches the generator. Bevel gears and some shaft materials add loss, so long or redirected lines may deliver less than the source produces.

## Kinetic Sources

Several machines can feed this generator through the same transmission network:

- <ItemLink id="ic2_120:steam_kinetic_generator" /> outputs KU from any side after steam is converted through a turbine.
- <ItemLink id="ic2_120:wind_kinetic_generator" /> outputs KU from its back face; the rotor is on the front.
- <ItemLink id="ic2_120:water_kinetic_generator" /> also outputs KU from its back face and requires a rotor.
- <ItemLink id="ic2_120:manual_kinetic_generator" /> outputs KU to horizontal sides while it has stored crank power.

Wind and water kinetic generators need a rotor in their rotor slot before they can produce KU. Their rotor material affects generation and durability, but the Kinetic Generator itself has no rotor slot; it only receives the KU after the source and shaft network have done their work.

## Recipe

<Recipe id="ic2_120:kinetic_generator" />

## Related

- <ItemLink id="ic2_120:wood_transmission_shaft" />
- <ItemLink id="ic2_120:iron_transmission_shaft" />
- <ItemLink id="ic2_120:steel_transmission_shaft" />
- <ItemLink id="ic2_120:carbon_transmission_shaft" />
- <ItemLink id="ic2_120:bevel_gear" />
