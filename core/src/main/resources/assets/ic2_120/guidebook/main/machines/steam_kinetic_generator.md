---
navigation:
  title: Steam Kinetic Generator
  parent: index.md
  position: 24
  icon: ic2_120:steam_kinetic_generator
item_ids:
  - ic2_120:steam_kinetic_generator
---

# Steam Kinetic Generator

<BlockImage id="ic2_120:steam_kinetic_generator" p:facing="north" p:active="true" scale="4" />

The Steam Kinetic Generator (steam turbine) consumes steam to produce kinetic energy (KU). It requires a steam turbine item installed in its turbine slot. Regular steam produces 2 KU per mB, while superheated steam produces 4 KU per mB.

The turbine also condenses 10% of consumed steam into distilled water, which collects in an internal tank. If the distilled water tank fills up, the turbine becomes blocked and stops producing KU. Steam is output to adjacent condensers or vented if no condenser is available.

## Steam Consumption

**KU/t = steamConsumed_mB × KU_per_mB**

The generator draws steam from its internal 21,000 mB steam tank and produces KU proportional to consumption:

- **Regular steam**: 2 KU per mB
- **Superheated steam**: 4 KU per mB

When using superheated steam, KU output doubles for the same steam consumption rate, and turbine wear is reduced — making it both more powerful and more economical.

### Condensation

10% of consumed steam is condensed into distilled water. The distilled water tank has a capacity of 1,000 mB. If the tank fills up, the turbine jams and production stops until distilled water is drained (e.g. by an adjacent condenser or fluid pipe).

## Output

- **KU Output**: Variable (up to ~800 KU/t)
- **Steam Consumption**: Variable (up to 21,000 mB capacity)
- **Turbine Wear**: every 20 ticks (normal: 2 wear, superheated: 1 wear)
- **Distilled Water Tank**: 1,000 mB
- **Tier**: 3

### Steam Types

| Steam Type | KU/mB | Turbine Wear |
|-----------|-------|-------------|
| Regular | 2 KU | 2 damage/20 ticks |
| Superheated | 4 KU | 1 damage/20 ticks |

## Turbine Wear

The steam turbine item wears down gradually during operation. Wear is applied every 20 ticks. Regular steam causes 2 wear per application, while superheated steam causes only 1 wear — meaning superheated steam not only produces more power but also extends turbine lifespan. When fully worn, the turbine breaks and must be replaced.

## Slots

- Turbine slot: holds the steam turbine
- Upgrade slot: supports upgrades

The Steam Kinetic Generator outputs KU from any side and accepts steam via fluid pipe connections.

## Recipe

<Recipe id="ic2_120:steam_kinetic_generator" />

## Related

- <ItemLink id="ic2_120:steam_turbine" />
- <ItemLink id="ic2_120:steam_generator" />
