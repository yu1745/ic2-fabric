---
navigation:
  title: Leash Kinetic Generator
  parent: index.md
  position: 25
  icon: ic2_120:leash_kinetic_generator
item_ids:
  - ic2_120:leash_kinetic_generator
---

# Leash Kinetic Generator

<BlockImage id="ic2_120:leash_kinetic_generator" p:facing="north" p:active="true" scale="4" />

The Leash Kinetic Generator harnesses kinetic energy from animals walking in circles around it. When an animal is leashed to the generator and moves around it, the rotational motion is converted into KU.

Power output depends on the animal's angular velocity (how fast it circles) and the leash length. The generator tracks the animal's position over time to calculate average angular velocity and produces KU proportional to both speed and distance. KU output is capped at 512 KU/t.

## Output

- **KU Output**: Variable, up to 512 KU/t
- **Requires**: a leashed animal moving around the generator
- **Max Leash Range**: 10 blocks
- **Tier**: 1

### Power Factors

- Base KU: 8
- Angular velocity: 2 KU per degree/second
- Leash length: 1 KU per 10 cm of horizontal distance

## Angular Velocity Tracking

The generator samples the leashed animal's position every tick and maintains a history of 40 samples (~2 seconds). Angular velocity is calculated from the change in angle over time using this history. The minimum angular velocity required to generate power is 5 degrees/second — slow walking may not produce any KU.

**KU = 8 + (avgDegPerSec × 2) + (horizontalDist_cm / 10)**

The result is capped at 512 KU/t.

## Slots

The Leash Kinetic Generator does not have item slots. All interaction is done through leashing animals directly to the machine.

KU can be extracted from any side.

## Recipe

<Recipe id="ic2_120:leash_kinetic_generator" />

## Related

- <ItemLink id="minecraft:lead" />
