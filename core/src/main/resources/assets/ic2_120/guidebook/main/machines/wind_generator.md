---
navigation:
  title: Wind Mill
  parent: index.md
  position: 15
  icon: ic2_120:wind_generator
item_ids:
  - ic2_120:wind_generator
---

# Wind Mill

<BlockImage id="ic2_120:wind_generator" p:facing="north" p:active="true" scale="4" />

The Wind Mill generates EU from wind energy, with output depending on height and weather. Power output is calculated as `p = w * s * (h - 64) / 750`, where `w` is the weather multiplier, `s` is the wind strength (0-30), and `h` is the effective height (y-coordinate minus obstacle count).

For best results, place the Wind Mill high above the ground with clear space around it. Obstacles in a 9x9x7 area around the machine reduce its effective height and power output.

## Output

- **EU Output**: 0-10+ EU/t (variable, theoretical limit ~11.46 EU/t)
- **Energy Storage**: 400 EU
- **Tier**: 1

### Weather Multipliers

| Weather | Multiplier |
|---------|-----------|
| Clear | 1.0x |
| Rain | 1.2x |
| Thunderstorm | 1.5x |

## Slots

- Battery slot: chargeable battery or electric tool

The Wind Mill does not accept EU input. It outputs EU from every side except its front face.

## Recipe

<Recipe id="ic2_120:wind_generator" />
