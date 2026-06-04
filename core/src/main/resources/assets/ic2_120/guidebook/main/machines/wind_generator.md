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

The Wind Mill is the direct EU wind generator. It does not use rotors and does not produce KU; if you want rotor-based kinetic power, use the Wind Kinetic Generator instead.

Every 128 ticks, the machine refreshes its wind strength and recalculates its generation rate. Wind strength is a saved value from 0 to 30 that drifts up or down over time. Output is calculated as:

`EU/t = weather * windStrength * (effectiveHeight - 64) / 750`

`effectiveHeight` is the machine's Y level minus nearby obstacle count. If the machine is below Y=64, or if `effectiveHeight - 64` is 0 or less, it generates 0 EU/t.

## Placement

Place the Wind Mill high up and keep the area around it clear. Obstacles are counted in a 9x9x7 box centered on the machine: 4 blocks out horizontally, 2 blocks down, and 4 blocks up. The Wind Mill itself is ignored, but every other non-air block in that volume lowers effective height by 1.

## Output

- **Generation**: variable; often fractional internally, accumulated until whole EU can be stored
- **Internal generation cap**: 20 EU/t
- **Energy buffer**: 400 EU
- **Output tier**: LV / tier 1
- **External output limit**: up to 20 EU/t total

### Weather Multipliers

| Weather | Multiplier |
|---------|-----------|
| Clear | 1.0x |
| Rain | 1.2x |
| Thunderstorm | 1.5x |

The Wind Mill stores generated EU in its internal buffer first. It only allows external extraction when the buffer has at least 20 EU, which helps avoid tiny trickle packets. Energy can leave from any side except the front face.

## Slots

- Battery slot: one chargeable battery or electric tool

The slot accepts chargeable items through the GUI, shift-click, or item automation. The machine can charge tier 1 batteries and electric tools from its buffer. The same slot is extractable for automation.

## Compared With Wind Kinetic Generator

- Wind Mill: direct EU, no rotor, no rotor wear, no rotor blocking plane.
- Wind Kinetic Generator: produces KU, requires a rotor, uses rotor radius/multiplier, can be jammed by rotor-area obstructions, and must feed a kinetic setup or Kinetic Generator.
- The two machines use different wind logic. Wind Mill uses the simple height/obstacle formula above, not the Wind Kinetic Generator's mean wind and per-chunk gust system.

## Recipe

<Recipe id="ic2_120:wind_generator" />
