---
navigation:
  title: Solar Distiller
  parent: index.md
  position: 58
  icon: ic2_120:solar_distiller
item_ids:
  - ic2_120:solar_distiller
---

# Solar Distiller

<BlockImage id="ic2_120:solar_distiller" scale="4" />

The Solar Distiller is a passive machine that turns water into distilled water. It does not use EU, heat, or fuel, but it is slow and only works under strict daylight conditions.

## Operation

- **Input tank:** 8 buckets of water
- **Output tank:** 8 buckets of distilled water
- **Rate:** 1 mB every 80 ticks
- **Power:** none
- **Dimension:** Overworld only

It works only in the Overworld, during daytime from about 333 to 11750 world time, when it is not raining and there are no opaque full blocks above it. If any condition fails, progress resets.

The output is always distilled water. It does not produce steam and has no mode switch.

## Real-Time Yield

The 1 mB every 80 ticks figure is small on its own, so it is worth translating it into wall-clock numbers:

- One full bucket of distilled water = 1,000 mB = 80,000 ticks of running.
- 80,000 ticks is 4,000 seconds, or roughly **67 minutes of continuous running** if the machine could run all day.
- It cannot. The daylight window is only 11,417 ticks (about 570 seconds) per Overworld day, and rain or an obstructed sky cuts that to zero.
- A typical clear Overworld day produces about **142 mB** of distilled water, so a single Solar Distiller needs roughly **7 in-game days of clear weather** to fill one bucket.

In other words, a single Solar Distiller is best treated as a slow background source — convenient for keeping a battery's water cell topped up, not a replacement for a Condenser in any serious steam setup.

## Slots

- **Water input:** water buckets, water cells, or universal fluid cells containing water.
- **Empty output:** empty buckets or empty cells returned from the water input.
- **Cell input:** empty cells or empty universal fluid cells.
- **Distilled output:** distilled water cells or filled universal fluid cells.
- **Upgrade slots:** two slots for fluid ejector and fluid pulling upgrades only.

External fluid access can insert water into the input tank and extract distilled water from the output tank. Fluid pulling upgrades can keep the water tank filled from nearby storage, while fluid ejector upgrades can push distilled water onward.

## Usage

Place the Solar Distiller under open sky and keep it supplied with water. Because the rate is very low, treat it as a passive distilled-water trickle for early cooling loops or battery-water preparation, not as a high-throughput replacement for Condensers in a steam plant.

## Recipe

<Recipe id="ic2_120:solar_distiller" />
