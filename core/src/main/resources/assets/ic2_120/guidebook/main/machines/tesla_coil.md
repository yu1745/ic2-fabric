---
navigation:
  title: Tesla Coil
  parent: index.md
  position: 68
  icon: ic2_120:tesla_coil
item_ids:
  - ic2_120:tesla_coil
---

# Tesla Coil

<BlockImage id="ic2_120:tesla_coil" p:facing="north" scale="4" />

The Tesla Coil is a redstone-activated electrical defense block. Once fully charged, it periodically shocks the nearest living target in range.

## Energy and Storage

- **EU Storage**: 5,000 EU
- **Input:** 128 EU/t
- **Minimum operating charge:** 5,000 EU
- **Shot cost:** 128 EU
- **Shot interval:** 20 ticks
- **Range:** 9 blocks

## Operation

The coil only fires while it is receiving a redstone signal. It must first reach a full 5,000 EU charge; after that, every second it chooses the nearest living entity in range and discharges into it.

The first hit on a target deals heavy damage, while repeated hits on the same target deal lower follow-up damage. Each strike also wears armor. The coil does not distinguish between hostile mobs and careless players, so keep the redstone control deliberate.

## Usage

Use a lever, pressure plate, sensor circuit, or redstone safety switch to decide when the coil is allowed to fire. It works well for guarded corridors and mob handling rooms, but it should be isolated from normal walkways unless you enjoy testing armor durability the direct way.
