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

The output rule is intentionally simple: at **Y > 74** it generates a fixed **3 EU/t**; at **Y 74 or below** it generates nothing. Weather, wind strength, and nearby obstacles do not affect this generator.

## Placement

Place the Wind Mill at Y=75 or higher. The GUI reports when the machine is below the required height.

## Output

- **Generation**: fixed 3 EU/t above Y=74
- **Internal generation cap**: 20 EU/t
- **Energy buffer**: 400 EU
- **Output tier**: LV / tier 1
- **External output limit**: up to 20 EU/t total

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

## Related

- [Wind Kinetic Generator](wind_kinetic_generator.md) — rotor-based KU wind generator
- [Wind Meter](wind_meter.md) — measures wind speed
