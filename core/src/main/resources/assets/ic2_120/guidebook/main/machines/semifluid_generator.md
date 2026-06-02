---
navigation:
  title: Semifluid Generator
  parent: index.md
  position: 12
  icon: ic2_120:semifluid_generator
item_ids:
  - ic2_120:semifluid_generator
---

# Semifluid Generator

<BlockImage id="ic2_120:semifluid_generator" p:facing="north" p:active="true" scale="4" />

The Semifluid Generator turns stored liquid fuel into EU. In this build it accepts **Biofuel** and **Creosote** only; Biomass must be processed in a Fermenter first.

Fuel can be supplied with buckets, IC2 cells, generic filled fluid cells, adjacent fluid storage, or fluid pipe automation. The machine keeps one internal fuel tank and burns from that tank whenever its EU buffer has room.

## Output

- **Generator tier**: 1 (LV)
- **EU buffer**: 10,000 EU
- **Maximum EU output**: 32 EU/t total
- **Energy sides**: outputs from every side except the front face
- **EU input**: none
- **Fuel tank**: 8 buckets

### Fuel Values

| Fuel | EU/t | EU/Bucket |
|------|------|-----------|
| Biofuel | 16 | 32,000 |
| Creosote Oil | 8 | 3,200 |

The active texture is shown only while the fuel tank contains a valid fuel and the EU buffer is not full.

## Inventory

- **Fuel slot**: Biofuel Bucket, Creosote Bucket, Biofuel Cell, or a filled Fluid Cell containing Biofuel or Creosote.
- **Empty container slot**: receives empty buckets or empty cells after a full bucket of fuel is moved into the internal tank.
- **Battery slot**: charges batteries and electric tools from the generator's EU buffer.
- **Upgrade slots**: accepts upgrade items, but this machine only applies fluid pipe behavior. Overclocker, transformer, and energy storage upgrades do not change its generation rate, output limit, tier, or buffer.

The fuel slot is processed only when the internal tank has at least one full bucket of free space, so containers are never partially drained.

## Automation

The internal fuel tank is exposed as a fluid input on every side and can accept only Biofuel or Creosote. Place a tank or fluid pipe next to the generator to push fuel into it directly.

A **Fluid Pulling Upgrade** lets the generator actively pull fuel from adjacent fluid storage. Configure the upgrade with a Biofuel or Creosote filter if several fluids are nearby. A **Fluid Ejector Upgrade** has no useful output here because the generator's fuel tank is not extractable.

For renewable Biofuel, feed Biomass into a Fermenter, supply heat to the Fermenter, then move its Biofuel output into this generator with tanks, pipes, or a fluid pulling upgrade. Tanks are useful as a buffer between the Fermenter and the generator so the generator can keep running while the Fermenter works in batches.

Creosote from the Coke Kiln can be buffered in a tank and fed the same way.

## Recipe

<Recipe id="ic2_120:semifluid_generator" />
