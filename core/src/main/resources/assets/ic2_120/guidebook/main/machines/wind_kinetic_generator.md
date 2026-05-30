---
navigation:
  title: Wind Kinetic Generator
  parent: index.md
  position: 22
  icon: ic2_120:wind_kinetic_generator
item_ids:
  - ic2_120:wind_kinetic_generator
---

# Wind Kinetic Generator

<BlockImage id="ic2_120:wind_kinetic_generator" p:facing="north" p:active="true" scale="4" />

The Wind Kinetic Generator converts wind energy into kinetic energy (KU). It requires a rotor installed in its front face that spins in the wind. Different rotor materials provide different power multipliers and radii.

KU is output from the back of the machine, which can be connected to kinetic transmission shafts or a Kinetic Generator.

## Wind Mechanics

Effective wind speed is the product of three factors:

**Effective Wind = Mean Wind × Gust Factor × Weather**

### Mean Wind (Height Factor)

Wind follows a Gaussian distribution centered at Y=150 with a sigma of 35:

- At Y=150: mean wind ≈ 1.0 (peak)
- At Y=115 or Y=185: mean wind ≈ 0.68
- At Y=64: mean wind ≈ 0.04
- At Y=0: mean wind ≈ 0.03 (floor)

Formula: `meanWind = 0.03 + 0.97 × exp(-(y-150)²/(2×35²))`

### Per-Chunk Gust Factor

Each chunk has its own random wind gust factor that changes every 200 ticks (10 seconds). This creates natural wind variation between different locations:

- Range: 0.5 to 1.5
- Each chunk gets a unique pseudo-random value derived from its position, deterministically updated every 200 ticks
- Two wind generators in the same chunk share the same gust factor; generators in different chunks experience different gusts

### Weather Multiplier

- Clear: ×1.0
- Rain: ×1.2
- Thunderstorm: ×1.5

## Start/Stop Hysteresis

The generator uses hysteresis to prevent rapid toggling when wind is near the threshold:

- **Starting threshold**: `0.10 × rotorMultiplier` (e.g., carbon rotor: 0.40)
  - The generator only starts when effective wind **≥** start threshold
- **Stopping threshold**: `startThreshold × 0.85` (e.g., carbon rotor: 0.34)
  - Once running, it keeps running until wind drops **below** stop threshold

This means it's harder to start but easier to keep running, preventing oscillation.

## Output

KU output per tick is calculated as:

**KU = floor(128 × rotorMultiplier × effectiveWind)**

### Performance Examples (Carbon Rotor, Y=150)

| Weather | Gust | Effective Wind | KU/t |
|---------|------|---------------|------|
| Clear | 1.0 | 1.0 | 512 |
| Clear | 1.5 | 1.5 | 768 |
| Rain | 1.5 | 1.8 | 921 |
| Thunder | 1.5 | 2.25 | 1152 |

### Rotor Specifications

| Rotor | Radius | Multiplier | Start Threshold |
|-------|--------|-----------|----------------|
| Wooden | 2 | 1× | 0.10 |
| Iron | 3 | 2× | 0.20 |
| Steel | 4 | 3× | 0.30 |
| Carbon | 5 | 4× | 0.40 |

Larger radius means the rotor sweeps a wider area and is more likely to be jammed by nearby blocks or other wind generators.

## Rotor Wear

Rotors wear down only while actively generating KU. The wear rate is multiplied by the weather multiplier (rain 1.2×, thunder 1.5×). When fully worn, the rotor breaks and must be replaced. Check remaining lifetime in the GUI.

## Blocking Detection

The rotor will jam if:

- A solid block is in its rotation plane within the rotor's radius
- Another wind generator's rotor overlaps in the same plane

When jammed, the rotor stops spinning and no KU is generated. Clear the obstructing blocks to resume operation.

## Slots

- Rotor slot: holds the wind rotor

Right-click with a rotor to install it, or right-click with an empty hand to remove it.

## Recipe

<Recipe id="ic2_120:wind_kinetic_generator" />

## Related

- <ItemLink id="ic2_120:wind_meter" />
- <ItemLink id="ic2_120:wooden_rotor" />
- <ItemLink id="ic2_120:iron_rotor" />
- <ItemLink id="ic2_120:steel_rotor" />
- <ItemLink id="ic2_120:carbon_rotor" />
