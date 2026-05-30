---
navigation:
  title: Steam Generator
  parent: index.md
  position: 18
  icon: ic2_120:steam_generator
item_ids:
  - ic2_120:steam_generator
---

# Steam Generator

<BlockImage id="ic2_120:steam_generator" p:facing="north" p:active="true" scale="4" />

The Steam Generator (boiler) consumes heat (HU) and water to produce steam or superheated steam. It receives heat from adjacent heat sources and water from fluid pipes or containers. One bucket of water produces 100 buckets of steam (100x expansion).

When the system temperature reaches 374°C, the generator starts producing superheated steam instead of regular steam. The maximum safe operating temperature is 500°C - exceeding this causes an explosion. The generator also accumulates calcification over time when using regular (non-distilled) water, which eventually blocks operation.

### Controls

The Steam Generator has a GUI with adjustable controls for input rate (mB/t) and pressure. These settings affect steam production and temperature. Water input rate ranges from 0 to 1000 mB/t, and pressure from 0 to 300 bar.

## Output

- **Steam Output**: 100 mB steam per 1 mB water (100x expansion)
- **Water Tank**: 10 buckets
- **Steam Tank**: 100,000 mB
- **Max Heat Input**: 1,200 HU/t
- **Tier**: 2
- **Max Temperature**: 500°C (explodes above)

### Steam Types

| Type | Temperature | Notes |
|------|-----------|-------|
| Steam | Below 374°C | Regular steam |
| Superheated Steam | 374°C+ | Higher efficiency |

## Slots

The Steam Generator does not have item slots. All fluid is handled through connected fluid pipes and adjacent heat sources. It accepts water input and steam output through fluid pipe connections from all sides.

## Recipe

<Recipe id="ic2_120:steam_generator" />
